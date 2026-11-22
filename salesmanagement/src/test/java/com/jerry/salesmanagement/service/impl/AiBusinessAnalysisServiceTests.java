package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerry.salesmanagement.config.AiAnalysisProperties;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.dto.AgingBucketDto;
import com.jerry.salesmanagement.pojo.dto.AiAnalysisResponse;
import com.jerry.salesmanagement.pojo.dto.AiAskRequest;
import com.jerry.salesmanagement.pojo.dto.AiStatusDto;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
import com.jerry.salesmanagement.pojo.dto.ProjectProfitRowDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingCardDto;
import com.jerry.salesmanagement.service.AiLlmClient;
import com.jerry.salesmanagement.service.ApprovalWorkbenchService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.FinanceAnalyticsService;
import com.jerry.salesmanagement.service.ProjectMaterialReconciliationService;
import com.jerry.salesmanagement.service.ProjectTrackingBoardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AiBusinessAnalysisServiceTests {

    private CurrentUserService stubUser(Set<String> authorities) {
        return new CurrentUserService() {
            @Override public SysUser getCurrentUser() { return null; }
            @Override public Long getCurrentUserId() { return 1L; }
            @Override public Long requireCurrentUserId() { return 1L; }
            @Override public boolean hasAnyAuthority(String... required) {
                return java.util.Arrays.stream(required).anyMatch(authorities::contains);
            }
            @Override public boolean isSystemOrAdmin() { return false; }
        };
    }

    private AiBusinessAnalysisServiceImpl createService(
            AiAnalysisProperties properties,
            AiLlmClient llmClient,
            Set<String> authorities,
            ProjectTrackingBoardService boardService,
            ApprovalWorkbenchService approvalService,
            ProjectMaterialReconciliationService reconService,
            FinanceAnalyticsService financeService
    ) {
        AiBusinessAnalysisServiceImpl service = new AiBusinessAnalysisServiceImpl();
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "aiLlmClient", llmClient);
        ReflectionTestUtils.setField(service, "currentUserService", stubUser(authorities));
        ReflectionTestUtils.setField(service, "projectTrackingBoardService", boardService);
        ReflectionTestUtils.setField(service, "approvalWorkbenchService", approvalService);
        ReflectionTestUtils.setField(service, "reconciliationService", reconService);
        ReflectionTestUtils.setField(service, "financeAnalyticsService", financeService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        return service;
    }

    private ProjectTrackingBoardDto sampleBoard() {
        ProjectTrackingBoardDto dto = new ProjectTrackingBoardDto();
        ProjectTrackingCardDto card = new ProjectTrackingCardDto();
        card.setUuid("p1");
        card.setProjectNumber("AB-1001");
        card.setProjectName("敏感工程");
        card.setCustomerName("保密客户A");
        card.setCurrentStageName("执行中-供货");
        card.setStageStayDays(40L);
        card.setOverdue(true);
        card.setDaysToPlanEnd(-5L);
        card.setWarrantyStatus("EXPIRING_SOON");
        card.setDaysToWarrantyEnd(10L);
        card.setRiskTags(List.of("OVERDUE", "STALLED", "WARRANTY_EXPIRING"));
        dto.setItems(List.of(card));
        return dto;
    }

    private FinanceAnalyticsOverviewDto sampleFinance() {
        FinanceAnalyticsOverviewDto dto = new FinanceAnalyticsOverviewDto();
        dto.setTotalActualIncome(new BigDecimal("1000.00"));
        dto.setTotalActualExpense(new BigDecimal("400.00"));
        dto.setTotalGrossProfit(new BigDecimal("600.00"));
        dto.setOverallGrossMarginRate(new BigDecimal("60.00"));
        dto.setTotalReceivable(new BigDecimal("500.00"));
        dto.setTotalPayable(new BigDecimal("100.00"));
        AgingBucketDto ar = new AgingBucketDto();
        ar.add(100L, new BigDecimal("500.00"));
        dto.setReceivableAging(ar);
        ProjectProfitRowDto row = new ProjectProfitRowDto();
        row.setProjectId("p1");
        row.setProjectNumber("AB-1001");
        row.setProjectName("敏感工程");
        row.setCustomerName("保密客户A");
        row.setActualIncomeAmount(new BigDecimal("1000.00"));
        row.setActualExpenseAmount(new BigDecimal("400.00"));
        row.setGrossProfit(new BigDecimal("600.00"));
        dto.setProjectProfits(List.of(row));
        return dto;
    }

    @Test
    void worksInRuleBasedModeWhenUnconfiguredAndRespectsRbacScopes() {
        AiAnalysisProperties props = new AiAnalysisProperties();
        ProjectTrackingBoardService boardService = mock(ProjectTrackingBoardService.class);
        when(boardService.getTrackingBoard(null, null, null, null)).thenReturn(sampleBoard());
        ApprovalWorkbenchService approvalService = mock(ApprovalWorkbenchService.class);
        ProjectMaterialReconciliationService reconService = mock(ProjectMaterialReconciliationService.class);
        FinanceAnalyticsService financeService = mock(FinanceAnalyticsService.class);
        AiLlmClient llmClient = mock(AiLlmClient.class);

        // User only has project.access — must never touch finance or inventory or approval services
        AiBusinessAnalysisServiceImpl service = createService(
                props, llmClient, Set.of("project.access"),
                boardService, approvalService, reconService, financeService
        );

        AiStatusDto status = service.getStatus();
        assertFalse(status.isEnabled());
        assertFalse(status.isConfigured());
        assertEquals("RULE_BASED", status.getMode());
        assertEquals(List.of("PROJECT"), status.getAuthorizedScopes());

        AiAnalysisResponse briefing = service.generateBriefing(null);
        assertEquals("RULE_BASED", briefing.getMode());
        assertFalse(briefing.isFallbackUsed());
        assertTrue(briefing.getRisks().stream().anyMatch(r -> "进度超期".equals(r.getCategory())));
        verifyNoInteractions(approvalService, reconService, financeService, llmClient);

        AiAskRequest financeQuestion = new AiAskRequest();
        financeQuestion.setQuestion("目前项目毛利和90天以上应收账款是多少？");
        AiAnalysisResponse deniedReply = service.askQuestion(financeQuestion);
        assertTrue(deniedReply.getAnswer().contains("未授予财务访问权限"));
        verifyNoInteractions(financeService, llmClient);
    }

    @Test
    void masksCustomerNamesOnExternalLlmAndRestoresResponse() {
        AiAnalysisProperties props = new AiAnalysisProperties();
        props.setEnabled(true);
        props.setBaseUrl("https://api.openai.com/v1");
        props.setApiKey("sk-secret-test");
        props.setModel("gpt-test");
        props.setMaskSensitiveNames(true);

        ProjectTrackingBoardService boardService = mock(ProjectTrackingBoardService.class);
        when(boardService.getTrackingBoard(null, null, null, null)).thenReturn(sampleBoard());
        ApprovalWorkbenchService approvalService = mock(ApprovalWorkbenchService.class);
        when(approvalService.getPendingApprovals()).thenReturn(new PendingApprovalSummaryDto());
        ProjectMaterialReconciliationService reconService = mock(ProjectMaterialReconciliationService.class);
        when(reconService.getOverview()).thenReturn(List.of());
        FinanceAnalyticsService financeService = mock(FinanceAnalyticsService.class);
        when(financeService.getOverview()).thenReturn(sampleFinance());

        AiLlmClient llmClient = mock(AiLlmClient.class);
        when(llmClient.chat(anyString(), anyString())).thenReturn("经分析，项目 AB-1001 对应的客户#1存在超期风险，建议跟进。");

        AiBusinessAnalysisServiceImpl service = createService(
                props,
                llmClient,
                Set.of("project.access", "project.manage", "inventory.access", "finance.access"),
                boardService, approvalService, reconService, financeService
        );

        AiAnalysisResponse response = service.generateBriefing(null);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(anyString(), promptCaptor.capture());

        // Real customer name must NOT be sent to external LLM endpoint when masking is on
        assertFalse(promptCaptor.getValue().contains("保密客户A"));
        assertTrue(promptCaptor.getValue().contains("客户#1"));

        // Response restores original customer name for authorized internal user
        assertEquals("LLM", response.getMode());
        assertFalse(response.isFallbackUsed());
        assertTrue(response.getSummary().contains("保密客户A"));
    }

    @Test
    void fallsBackToRuleBasedModeWhenLlmEndpointFails() {
        AiAnalysisProperties props = new AiAnalysisProperties();
        props.setEnabled(true);
        props.setBaseUrl("http://127.0.0.1:11434/v1");
        props.setModel("qwen2.5:7b");

        assertTrue(props.isConfigured());
        assertTrue(props.isLocalEndpoint());

        ProjectTrackingBoardService boardService = mock(ProjectTrackingBoardService.class);
        when(boardService.getTrackingBoard(null, null, null, null)).thenReturn(sampleBoard());
        FinanceAnalyticsService financeService = mock(FinanceAnalyticsService.class);
        when(financeService.getOverview()).thenReturn(sampleFinance());

        AiLlmClient llmClient = mock(AiLlmClient.class);
        when(llmClient.chat(anyString(), anyString())).thenThrow(new IllegalStateException("Connection refused"));

        AiBusinessAnalysisServiceImpl service = createService(
                props,
                llmClient,
                Set.of("project.access", "finance.access"),
                boardService, mock(ApprovalWorkbenchService.class), mock(ProjectMaterialReconciliationService.class), financeService
        );

        AiAskRequest req = new AiAskRequest();
        req.setQuestion("90天以上应收有多少？");
        AiAnalysisResponse res = service.askQuestion(req);
        assertEquals("RULE_BASED", res.getMode());
        assertTrue(res.isFallbackUsed());
        assertTrue(res.getFallbackReason().contains("Connection refused"));
        assertTrue(res.getAnswer().contains("500.00"));
    }
}
