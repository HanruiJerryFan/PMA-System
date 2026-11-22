package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jerry.salesmanagement.config.AiAnalysisProperties;
import com.jerry.salesmanagement.pojo.dto.AiAnalysisResponse;
import com.jerry.salesmanagement.pojo.dto.AiAskRequest;
import com.jerry.salesmanagement.pojo.dto.AiRiskItemDto;
import com.jerry.salesmanagement.pojo.dto.AiStatusDto;
import com.jerry.salesmanagement.pojo.dto.CounterpartyAgingRowDto;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.pojo.dto.MaterialReconciliationItemDto;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectProfitRowDto;
import com.jerry.salesmanagement.pojo.dto.ProjectReconciliationOverviewItemDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingCardDto;
import com.jerry.salesmanagement.service.AiBusinessAnalysisService;
import com.jerry.salesmanagement.service.AiLlmClient;
import com.jerry.salesmanagement.service.ApprovalWorkbenchService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.FinanceAnalyticsService;
import com.jerry.salesmanagement.service.ProjectMaterialReconciliationService;
import com.jerry.salesmanagement.service.ProjectTrackingBoardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiBusinessAnalysisServiceImpl implements AiBusinessAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiBusinessAnalysisServiceImpl.class);
    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_RECONCILIATION_ITEMS = 30;

    @Autowired
    private AiAnalysisProperties properties;

    @Autowired
    private AiLlmClient aiLlmClient;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProjectTrackingBoardService projectTrackingBoardService;

    @Autowired
    private ApprovalWorkbenchService approvalWorkbenchService;

    @Autowired
    private ProjectMaterialReconciliationService reconciliationService;

    @Autowired
    private FinanceAnalyticsService financeAnalyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AiStatusDto getStatus() {
        AiStatusDto dto = new AiStatusDto();
        dto.setEnabled(properties.isEnabled());
        dto.setConfigured(properties.isConfigured());
        dto.setLocalEndpoint(properties.isLocalEndpoint());
        dto.setProvider(properties.getProvider());
        dto.setModel(properties.isConfigured() ? properties.getModel() : null);
        dto.setMaskSensitiveNames(properties.isMaskSensitiveNames());
        dto.setMode(properties.isConfigured() ? "LLM" : "RULE_BASED");
        dto.setAuthorizedScopes(resolveAuthorizedScopes());
        return dto;
    }

    @Override
    public AiAnalysisResponse generateBriefing(String projectUuid) {
        String normalizedProjectUuid = StringUtils.hasText(projectUuid) ? projectUuid.trim() : null;
        AnalysisSnapshot snapshot = collectSnapshot(normalizedProjectUuid);
        AiAnalysisResponse response = buildDeterministicResponse(snapshot, null);

        if (properties.isConfigured()) {
            try {
                NameMasker masker = snapshot.masker;
                String contextJson = snapshot.maskSensitive
                        ? maskEvidenceJson(snapshot.evidence, masker)
                        : toCompactJson(snapshot.evidence);
                String systemPrompt = """
                        你是 PMA（项目管理与财务核算系统）的智能经营分析助手。
                        请严格基于用户提供的 JSON 统计数据进行分析，禁止编造任何数字或结论，禁止推测未授权模块的指标。
                        请用简洁专业的中文输出一份高管经营简报，包含：1）总体进展；2）核心风险点；3）待办与行动建议。
                        """;
                String userPrompt = "请基于以下已授权业务快照生成经营简报：\n" + contextJson;
                String llmText = aiLlmClient.chat(systemPrompt, userPrompt);
                response.setSummary(snapshot.maskSensitive ? masker.unmask(llmText) : llmText);
                response.setMode("LLM");
                response.setModel(properties.getModel());
                response.setFallbackUsed(false);
            } catch (Exception e) {
                log.warn("AI briefing call failed and fell back to rule engine: {}", e.getMessage());
                response.setMode("RULE_BASED");
                response.setFallbackUsed(true);
                response.setFallbackReason("外部或本地 AI 模型暂不可用，已自动降级为确定性规则引擎分析（" + e.getMessage() + "）。");
            }
        }
        return response;
    }

    @Override
    public AiAnalysisResponse askQuestion(AiAskRequest request) {
        String question = request != null && StringUtils.hasText(request.getQuestion())
                ? request.getQuestion().trim()
                : "";
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Question is required");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("Question cannot exceed " + MAX_QUESTION_LENGTH + " characters");
        }
        String projectUuid = request != null && StringUtils.hasText(request.getProjectUuid())
                ? request.getProjectUuid().trim()
                : null;

        AnalysisSnapshot snapshot = collectSnapshot(projectUuid);
        AiAnalysisResponse response = buildDeterministicResponse(snapshot, question);

        if (properties.isConfigured() && !snapshot.unauthorizedIntentMessage) {
            try {
                NameMasker masker = snapshot.masker;
                String contextJson = snapshot.maskSensitive
                        ? maskEvidenceJson(snapshot.evidence, masker)
                        : toCompactJson(snapshot.evidence);
                String systemPrompt = """
                        你是 PMA（项目管理与财务核算系统）的智能问答助手。
                        规则：
                        1. 只能根据提供的已授权 JSON 数据回答，禁止捏造任何未出现在 JSON 中的数据、客户或项目编号。
                        2. 若问题涉及当前 JSON 中未提供的未授权模块指标，必须明确告知无对应权限或无相关数据。
                        3. 回答要直接、条理清晰，引用具体项目编号与金额指标。
                        """;
                String maskedQuestion = snapshot.maskSensitive ? masker.maskQuestion(question) : question;
                String userPrompt = "用户问题：" + maskedQuestion + "\n\n已授权业务数据快照：\n" + contextJson;
                String llmAnswer = aiLlmClient.chat(systemPrompt, userPrompt);
                response.setAnswer(snapshot.maskSensitive ? masker.unmask(llmAnswer) : llmAnswer);
                response.setMode("LLM");
                response.setModel(properties.getModel());
                response.setFallbackUsed(false);
            } catch (Exception e) {
                log.warn("AI Q&A call failed and fell back to rule engine: {}", e.getMessage());
                response.setMode("RULE_BASED");
                response.setFallbackUsed(true);
                response.setFallbackReason("外部或本地 AI 模型暂不可用，已自动降级为确定性规则引擎作答（" + e.getMessage() + "）。");
            }
        }
        return response;
    }

    private AnalysisSnapshot collectSnapshot(String projectUuid) {
        AnalysisSnapshot snapshot = new AnalysisSnapshot();
        snapshot.projectUuid = projectUuid;
        snapshot.scopes = resolveAuthorizedScopes();
        snapshot.maskSensitive = properties.isMaskSensitiveNames() && !properties.isLocalEndpoint();

        if (snapshot.scopes.contains("PROJECT")) {
            ProjectTrackingBoardDto board = projectTrackingBoardService.getTrackingBoard(null, null, null, null);
            snapshot.board = board;
            if (board != null && board.getItems() != null) {
                snapshot.scopedCards = StringUtils.hasText(projectUuid)
                        ? board.getItems().stream().filter(c -> projectUuid.equals(c.getUuid())).toList()
                        : board.getItems();
                snapshot.scopedCards.forEach(c -> snapshot.masker.register(c.getCustomerName()));
            }
        }

        if (snapshot.scopes.contains("APPROVAL")) {
            PendingApprovalSummaryDto rawApprovals = approvalWorkbenchService.getPendingApprovals();
            snapshot.approvals = StringUtils.hasText(projectUuid) && rawApprovals != null
                    ? scopeApprovals(rawApprovals, projectUuid)
                    : rawApprovals;
            if (snapshot.approvals != null) {
                snapshot.approvals.getProjectLists().forEach(i -> snapshot.masker.register(i.getCounterpartyName()));
                snapshot.approvals.getWarehouseDocs().forEach(i -> snapshot.masker.register(i.getCounterpartyName()));
                snapshot.approvals.getFinanceVouchers().forEach(i -> snapshot.masker.register(i.getCounterpartyName()));
            }
        }

        if (snapshot.scopes.contains("PROJECT") && snapshot.scopes.contains("INVENTORY")) {
            if (StringUtils.hasText(projectUuid)) {
                snapshot.projectReconciliation = reconciliationService.getProjectReconciliation(projectUuid);
                if (snapshot.projectReconciliation != null) {
                    snapshot.masker.register(snapshot.projectReconciliation.getCustomerName());
                }
            } else {
                snapshot.reconciliationOverview = reconciliationService.getOverview();
                snapshot.reconciliationOverview.forEach(r -> snapshot.masker.register(r.getCustomerName()));
            }
        }

        if (snapshot.scopes.contains("FINANCE")) {
            snapshot.finance = financeAnalyticsService.getOverview();
            if (snapshot.finance != null) {
                if (StringUtils.hasText(projectUuid)) {
                    snapshot.scopedProjectProfit = snapshot.finance.getProjectProfits().stream()
                            .filter(r -> projectUuid.equals(r.getProjectId()))
                            .findFirst()
                            .orElse(null);
                    if (snapshot.scopedProjectProfit != null) {
                        snapshot.masker.register(snapshot.scopedProjectProfit.getCustomerName());
                    }
                } else {
                    snapshot.finance.getProjectProfits().forEach(r -> snapshot.masker.register(r.getCustomerName()));
                    snapshot.finance.getReceivableByCounterparty().forEach(r -> snapshot.masker.register(r.getCustomerName()));
                    snapshot.finance.getPayableByCounterparty().forEach(r -> snapshot.masker.register(r.getCustomerName()));
                }
            }
        }

        snapshot.evidence = buildEvidenceMap(snapshot);
        return snapshot;
    }

    private AiAnalysisResponse buildDeterministicResponse(AnalysisSnapshot snapshot, String question) {
        List<String> highlights = new ArrayList<>();
        List<AiRiskItemDto> risks = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (snapshot.board != null) {
            List<ProjectTrackingCardDto> cards = snapshot.scopedCards;
            long completedCount = cards.stream().filter(ProjectTrackingCardDto::isCompleted).count();
            long overdueCount = cards.stream().filter(ProjectTrackingCardDto::isOverdue).count();
            long stalledCount = cards.stream().filter(c -> c.getRiskTags().contains("STALLED")).count();
            long expiringWarranty = cards.stream().filter(c -> "EXPIRING_SOON".equals(c.getWarrantyStatus())).count();
            long expiredWarranty = cards.stream().filter(c -> "EXPIRED".equals(c.getWarrantyStatus())).count();

            highlights.add(String.format(
                    "项目范围共 %d 个项目（进行中 %d，已完结 %d），超期 %d 个、阶段滞留 %d 个、质保临期 %d 个。",
                    cards.size(), cards.size() - completedCount, completedCount, overdueCount, stalledCount, expiringWarranty
            ));

            for (ProjectTrackingCardDto card : cards) {
                if (card.isOverdue()) {
                    risks.add(new AiRiskItemDto(
                            "HIGH",
                            "进度超期",
                            card.getProjectNumber() + " · " + card.getProjectName(),
                            "已超过计划结束日期 " + Math.abs(card.getDaysToPlanEnd() != null ? card.getDaysToPlanEnd() : 0) + " 天，当前阶段：" + card.getCurrentStageName(),
                            "/project/dashboard?projectUuid=" + card.getUuid()
                    ));
                }
                if (card.getRiskTags().contains("STALLED")) {
                    risks.add(new AiRiskItemDto(
                            "MEDIUM",
                            "阶段滞留",
                            card.getProjectNumber() + " · " + card.getProjectName(),
                            "当前阶段“" + card.getCurrentStageName() + "”已停留 " + card.getStageStayDays() + " 天",
                            "/project/tracking-board"
                    ));
                }
                if ("EXPIRING_SOON".equals(card.getWarrantyStatus()) || "EXPIRED".equals(card.getWarrantyStatus())) {
                    risks.add(new AiRiskItemDto(
                            "EXPIRED".equals(card.getWarrantyStatus()) ? "HIGH" : "MEDIUM",
                            "质保提醒",
                            card.getProjectNumber() + " · " + card.getProjectName(),
                            "EXPIRING_SOON".equals(card.getWarrantyStatus())
                                    ? "距离质保截止仅剩 " + card.getDaysToWarrantyEnd() + " 天"
                                    : "质保已过期 " + Math.abs(card.getDaysToWarrantyEnd() != null ? card.getDaysToWarrantyEnd() : 0) + " 天",
                            "/project/dashboard?projectUuid=" + card.getUuid()
                    ));
                }
            }
            if (overdueCount > 0 || stalledCount > 0) {
                recommendations.add("优先在「项目跟踪看板」复盘超期与长期滞留项目，核实卡点并推进节点。");
            }
            if (expiringWarranty > 0 || expiredWarranty > 0) {
                recommendations.add("尽快针对质保临期/已过期项目安排巡检收尾或续保沟通。");
            }
        }

        if (snapshot.approvals != null && snapshot.approvals.getTotalPendingCount() > 0) {
            highlights.add(String.format(
                    "统一待审核工作台共有 %d 项待审核（清单 %d、出入库单 %d、财务凭证 %d）。",
                    snapshot.approvals.getTotalPendingCount(),
                    snapshot.approvals.getProjectListCount(),
                    snapshot.approvals.getWarehouseDocCount(),
                    snapshot.approvals.getFinanceVoucherCount()
            ));
            risks.add(new AiRiskItemDto(
                    "MEDIUM",
                    "待审积压",
                    "共有 " + snapshot.approvals.getTotalPendingCount() + " 项单据待审核",
                    "及时审核有助于防止项目回款确认、成本核算及库存划拨延迟。",
                    "/approvals/pending"
            ));
            recommendations.add("前往「统一待审核工作台」集中清理积压的清单、出入库单与财务凭证。");
        }

        if (snapshot.projectReconciliation != null) {
            ProjectMaterialReconciliationDto recon = snapshot.projectReconciliation;
            highlights.add(String.format(
                    "项目采发对账：%d 行物料中，采购缺口 %d 行、待发货 %d 行、超发 %d 行。",
                    recon.getTotalMaterialLines(),
                    recon.getShortageLineCount(),
                    recon.getPendingShipmentLineCount(),
                    recon.getOverShippedLineCount()
            ));
            if (recon.getShortageLineCount() > 0 || recon.getOverShippedLineCount() > 0) {
                risks.add(new AiRiskItemDto(
                        recon.getOverShippedLineCount() > 0 ? "HIGH" : "MEDIUM",
                        "采发异常",
                        recon.getProjectNumber() + " · 采发对账异常",
                        "存在采购缺口 " + recon.getShortageLineCount() + " 行、超发 " + recon.getOverShippedLineCount() + " 行。",
                        "/project/reconciliation?projectUuid=" + recon.getProjectId()
                ));
            }
        } else if (snapshot.reconciliationOverview != null && !snapshot.reconciliationOverview.isEmpty()) {
            long shortageProjects = snapshot.reconciliationOverview.stream().filter(r -> r.getShortageLineCount() > 0).count();
            long overShippedProjects = snapshot.reconciliationOverview.stream().filter(r -> r.getOverShippedLineCount() > 0).count();
            highlights.add(String.format("物料采发对账覆盖 %d 个项目：采购缺口项目 %d 个，超发预警项目 %d 个。",
                    snapshot.reconciliationOverview.size(), shortageProjects, overShippedProjects));
            if (shortageProjects > 0 || overShippedProjects > 0) {
                recommendations.add("前往「项目清单与采发对账」核查采购缺口物料及超发原因。");
            }
        }

        if (snapshot.finance != null) {
            if (StringUtils.hasText(snapshot.projectUuid)) {
                ProjectProfitRowDto row = snapshot.scopedProjectProfit;
                if (row != null) {
                    highlights.add(String.format(
                            "项目财务汇总（%s）：合同额 %s，累计实收 %s，累计实付 %s，毛利 %s（应收 %s / 应付 %s）。",
                            row.getProjectNumber(),
                            row.getContractAmount(),
                            row.getActualIncomeAmount(),
                            row.getActualExpenseAmount(),
                            row.getGrossProfit(),
                            row.getReceivableAmount(),
                            row.getPayableAmount()
                    ));
                    if (row.getActualIncomeAmount() != null
                            && row.getActualIncomeAmount().compareTo(BigDecimal.ZERO) > 0
                            && row.getGrossProfit() != null
                            && row.getGrossProfit().compareTo(BigDecimal.ZERO) < 0) {
                        risks.add(new AiRiskItemDto(
                                "HIGH",
                                "负毛利",
                                row.getProjectNumber() + " · " + row.getProjectName(),
                                "已实现收入 " + row.getActualIncomeAmount() + " 低于支出 " + row.getActualExpenseAmount() + "，当前毛利 " + row.getGrossProfit(),
                                "/finance/analytics"
                        ));
                    }
                } else {
                    highlights.add("项目财务汇总：当前项目暂无关联销售合同或已完成财务凭证记录。");
                }
            } else {
                FinanceAnalyticsOverviewDto fin = snapshot.finance;
                highlights.add(String.format(
                        "财务汇总：累计实收 %s，累计实付 %s，毛利 %s（应收 %s / 应付 %s）。",
                        fin.getTotalActualIncome(),
                        fin.getTotalActualExpense(),
                        fin.getTotalGrossProfit(),
                        fin.getTotalReceivable(),
                        fin.getTotalPayable()
                ));
                if (fin.getReceivableAging() != null && fin.getReceivableAging().getBucketOver90().compareTo(BigDecimal.ZERO) > 0) {
                    risks.add(new AiRiskItemDto(
                            "HIGH",
                            "长账龄应收",
                            "90天以上应收账款 " + fin.getReceivableAging().getBucketOver90(),
                            "存在超 90 天未收回款项，建议重点催收并核对开票回款状态。",
                            "/finance/analytics"
                    ));
                    recommendations.add("前往「利润与账龄分析」针对 90 天以上应收客户建立专项催收计划。");
                }
                for (ProjectProfitRowDto row : fin.getProjectProfits()) {
                    if (row.getActualIncomeAmount().compareTo(BigDecimal.ZERO) > 0
                            && row.getGrossProfit().compareTo(BigDecimal.ZERO) < 0) {
                        risks.add(new AiRiskItemDto(
                                "HIGH",
                                "负毛利",
                                row.getProjectNumber() + " · " + row.getProjectName(),
                                "已实现收入 " + row.getActualIncomeAmount() + " 低于支出 " + row.getActualExpenseAmount() + "，当前毛利 " + row.getGrossProfit(),
                                "/finance/analytics"
                        ));
                    }
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("当前所辖业务指标整体平稳，建议继续保持按周巡检看板与对账数据。");
        }

        String summary = buildRuleSummary(highlights, risks, snapshot.scopes);
        String answer = question != null ? buildRuleAnswer(question, snapshot, highlights, risks) : summary;

        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setMode("RULE_BASED");
        response.setFallbackUsed(false);
        response.setProjectUuid(snapshot.projectUuid);
        response.setAuthorizedScopes(snapshot.scopes);
        response.setHighlights(highlights);
        response.setRisks(risks.stream().limit(20).toList());
        response.setRecommendations(recommendations);
        response.setSummary(summary);
        response.setAnswer(answer);
        response.setEvidence(snapshot.evidence);
        return response;
    }

    private String buildRuleSummary(List<String> highlights, List<AiRiskItemDto> risks, List<String> scopes) {
        if (highlights.isEmpty()) {
            return "当前账号仅授权有限范围（" + String.join("、", scopes) + "），暂无可汇总的业务统计数据。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\n", highlights));
        if (!risks.isEmpty()) {
            sb.append("\n\n重点风险共识别 ").append(risks.size()).append(" 项，首要关注：");
            risks.stream().limit(3).forEach(r ->
                    sb.append("\n- [").append(r.getCategory()).append("] ").append(r.getTitle()).append("：").append(r.getDetail())
            );
        } else {
            sb.append("\n\n当前权限范围内暂未发现高风险预警。");
        }
        return sb.toString();
    }

    private String buildRuleAnswer(
            String question,
            AnalysisSnapshot snapshot,
            List<String> highlights,
            List<AiRiskItemDto> risks
    ) {
        String q = question.toLowerCase(Locale.ROOT);
        boolean asksFinance = q.contains("利润") || q.contains("毛利") || q.contains("应收") || q.contains("应付")
                || q.contains("账龄") || q.contains("回款") || q.contains("收入") || q.contains("支出");
        boolean asksInventory = q.contains("采购缺口") || q.contains("对账") || q.contains("发货") || q.contains("库存") || q.contains("缺料");
        boolean asksApproval = q.contains("待审") || q.contains("审批") || q.contains("审核");
        boolean asksProject = q.contains("超期") || q.contains("滞留") || q.contains("质保")
                || q.contains("风险") || q.contains("阶段") || q.contains("看板") || q.contains("进度")
                || q.contains("项目风险") || q.contains("项目状态") || q.contains("项目概况") || q.contains("项目进展");

        List<String> unauthorizedMessages = new ArrayList<>();
        int askedDomainCount = 0;
        if (asksFinance) {
            askedDomainCount++;
            if (!snapshot.scopes.contains("FINANCE")) {
                unauthorizedMessages.add("当前账号未授予财务访问权限（finance.access），无法查询收支、毛利或应收应付账龄数据。");
            }
        }
        if (asksInventory) {
            askedDomainCount++;
            if (!snapshot.scopes.contains("INVENTORY")) {
                unauthorizedMessages.add("当前账号未授予库存访问权限（inventory.access），无法查询物料库存或采购/发货对账数据。");
            }
        }
        if (asksApproval) {
            askedDomainCount++;
            if (!snapshot.scopes.contains("APPROVAL")) {
                unauthorizedMessages.add("当前账号未授予审核权限，无法查看或统计待审核单据列表。");
            }
        }
        if (asksProject) {
            askedDomainCount++;
            if (!snapshot.scopes.contains("PROJECT")) {
                unauthorizedMessages.add("当前账号未授予项目访问权限（project.access），无法查询项目进度、阶段滞留或看板风险数据。");
            }
        }

        if (askedDomainCount > 0 && unauthorizedMessages.size() == askedDomainCount) {
            snapshot.unauthorizedIntentMessage = true;
            return String.join(" ", unauthorizedMessages);
        }

        if (asksApproval && snapshot.approvals != null) {
            return String.format(
                    "当前共有 %d 项待审核单据：项目清单 %d 份、出入库单 %d 份（已排除划拨自动关联入库单）、财务凭证 %d 份。可在「统一待审核工作台」一键处理。",
                    snapshot.approvals.getTotalPendingCount(),
                    snapshot.approvals.getProjectListCount(),
                    snapshot.approvals.getWarehouseDocCount(),
                    snapshot.approvals.getFinanceVoucherCount()
            );
        }

        if (asksFinance && snapshot.finance != null) {
            if (StringUtils.hasText(snapshot.projectUuid)) {
                ProjectProfitRowDto row = snapshot.scopedProjectProfit;
                if (row == null) {
                    return "当前选定项目暂无关联销售合同或已完成财务凭证数据。";
                }
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("项目 %s（%s）财务统计：合同额 %s，累计实收 %s，累计实付 %s，毛利 %s",
                        row.getProjectNumber(), row.getProjectName(),
                        row.getContractAmount(), row.getActualIncomeAmount(),
                        row.getActualExpenseAmount(), row.getGrossProfit()));
                if (row.getGrossMarginRate() != null) {
                    sb.append("（毛利率 ").append(row.getGrossMarginRate()).append("%）");
                }
                sb.append(String.format("；应收 %s，应付 %s。", row.getReceivableAmount(), row.getPayableAmount()));
                return sb.toString();
            }
            FinanceAnalyticsOverviewDto fin = snapshot.finance;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("财务统计总览：累计实收 %s，累计实付 %s，汇总毛利 %s",
                    fin.getTotalActualIncome(), fin.getTotalActualExpense(), fin.getTotalGrossProfit()));
            if (fin.getOverallGrossMarginRate() != null) {
                sb.append("（毛利率 ").append(fin.getOverallGrossMarginRate()).append("%）");
            }
            sb.append(String.format("；应收总额 %s（其中 90 天以上 %s），应付总额 %s。",
                    fin.getTotalReceivable(),
                    fin.getReceivableAging() != null ? fin.getReceivableAging().getBucketOver90() : BigDecimal.ZERO,
                    fin.getTotalPayable()));
            if (!fin.getReceivableByCounterparty().isEmpty()) {
                CounterpartyAgingRowDto topAr = fin.getReceivableByCounterparty().get(0);
                sb.append(" 最大应收往来方为 ").append(topAr.getCustomerName())
                        .append("（").append(topAr.getTotal()).append("，最长账龄 ").append(topAr.getMaxAgingDays()).append(" 天）。");
            }
            return sb.toString();
        }

        if (asksInventory && (snapshot.projectReconciliation != null || snapshot.reconciliationOverview != null)) {
            if (snapshot.projectReconciliation != null) {
                ProjectMaterialReconciliationDto recon = snapshot.projectReconciliation;
                List<String> shortageItems = recon.getItems().stream()
                        .filter(i -> "PROCUREMENT_SHORTAGE".equals(i.getStatus()))
                        .limit(5)
                        .map(MaterialReconciliationItemDto::getItemName)
                        .toList();
                return String.format("项目 %s 共有 %d 项物料对账：采购缺口 %d 项%s，待发货 %d 项，超发 %d 项。",
                        recon.getProjectNumber(),
                        recon.getTotalMaterialLines(),
                        recon.getShortageLineCount(),
                        shortageItems.isEmpty() ? "" : "（例如：" + String.join("、", shortageItems) + "）",
                        recon.getPendingShipmentLineCount(),
                        recon.getOverShippedLineCount());
            }
            long shortageProjects = snapshot.reconciliationOverview.stream().filter(r -> r.getShortageLineCount() > 0).count();
            return String.format("当前共统计 %d 个项目采发对账，其中 %d 个项目存在采购缺口，可在「采发对账」页面查看每行物料差异。",
                    snapshot.reconciliationOverview.size(), shortageProjects);
        }

        if (asksProject && snapshot.board != null) {
            List<AiRiskItemDto> projectRisks = risks.stream().limit(5).toList();
            if (projectRisks.isEmpty()) {
                return "当前筛选范围内未检测到超期、阶段滞留或质保异常项目。";
            }
            StringBuilder sb = new StringBuilder("根据项目看板规则引擎识别，当前最需关注的风险如下：");
            projectRisks.forEach(r ->
                    sb.append("\n- [").append(r.getCategory()).append("] ").append(r.getTitle()).append("：").append(r.getDetail())
            );
            return sb.toString();
        }

        return buildRuleSummary(highlights, risks, snapshot.scopes);
    }

    private Map<String, Object> buildEvidenceMap(AnalysisSnapshot snapshot) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("authorizedScopes", snapshot.scopes);
        if (snapshot.projectUuid != null) {
            evidence.put("projectUuid", snapshot.projectUuid);
        }
        if (snapshot.board != null) {
            evidence.put("projectSummary", StringUtils.hasText(snapshot.projectUuid)
                    ? buildScopedSummary(snapshot.board, snapshot.scopedCards)
                    : snapshot.board.getSummary());
            evidence.put("projects", snapshot.scopedCards.stream()
                    .limit(15)
                    .map(c -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("projectNumber", c.getProjectNumber());
                        row.put("projectName", c.getProjectName());
                        row.put("customerName", c.getCustomerName());
                        row.put("stage", c.getCurrentStageName());
                        row.put("stageStayDays", c.getStageStayDays());
                        row.put("overdue", c.isOverdue());
                        row.put("warrantyStatus", c.getWarrantyStatus());
                        row.put("riskTags", c.getRiskTags());
                        if (snapshot.scopes.contains("FINANCE")) {
                            row.put("receivedAmount", c.getReceivedAmount());
                            row.put("receivableAmount", c.getReceivableAmount());
                            row.put("estimatedGrossProfit", c.getEstimatedGrossProfit());
                        }
                        return row;
                    })
                    .toList());
        }
        if (snapshot.approvals != null) {
            evidence.put("pendingApprovals", Map.of(
                    "totalPendingCount", snapshot.approvals.getTotalPendingCount(),
                    "projectListCount", snapshot.approvals.getProjectListCount(),
                    "warehouseDocCount", snapshot.approvals.getWarehouseDocCount(),
                    "financeVoucherCount", snapshot.approvals.getFinanceVoucherCount()
            ));
        }
        if (snapshot.projectReconciliation != null) {
            evidence.put("reconciliation", buildCappedReconciliationEvidence(snapshot.projectReconciliation));
        } else if (snapshot.reconciliationOverview != null) {
            evidence.put("reconciliationSummary", snapshot.reconciliationOverview.stream().limit(15).toList());
        }
        if (snapshot.finance != null) {
            if (StringUtils.hasText(snapshot.projectUuid)) {
                evidence.put("projectFinance", snapshot.scopedProjectProfit);
            } else {
                Map<String, Object> finSummary = new LinkedHashMap<>();
                finSummary.put("totalActualIncome", snapshot.finance.getTotalActualIncome());
                finSummary.put("totalActualExpense", snapshot.finance.getTotalActualExpense());
                finSummary.put("totalGrossProfit", snapshot.finance.getTotalGrossProfit());
                finSummary.put("nonProjectExpense", snapshot.finance.getNonProjectExpense());
                finSummary.put("totalReceivable", snapshot.finance.getTotalReceivable());
                finSummary.put("totalPayable", snapshot.finance.getTotalPayable());
                finSummary.put("receivableAging", snapshot.finance.getReceivableAging());
                finSummary.put("payableAging", snapshot.finance.getPayableAging());
                evidence.put("financeSummary", finSummary);
                evidence.put("topReceivableParties", snapshot.finance.getReceivableByCounterparty().stream().limit(8).toList());
            }
        }
        return evidence;
    }

    private PendingApprovalSummaryDto scopeApprovals(PendingApprovalSummaryDto source, String projectUuid) {
        PendingApprovalSummaryDto scoped = new PendingApprovalSummaryDto();
        scoped.setCanAuditProjectLists(source.isCanAuditProjectLists());
        scoped.setCanAuditWarehouseDocs(source.isCanAuditWarehouseDocs());
        scoped.setCanAuditFinanceVouchers(source.isCanAuditFinanceVouchers());
        scoped.setProjectLists(source.getProjectLists().stream()
                .filter(i -> projectUuid.equals(i.getProjectId()))
                .toList());
        scoped.setWarehouseDocs(source.getWarehouseDocs().stream()
                .filter(i -> projectUuid.equals(i.getProjectId()))
                .toList());
        scoped.setFinanceVouchers(source.getFinanceVouchers().stream()
                .filter(i -> projectUuid.equals(i.getProjectId()))
                .toList());
        scoped.setProjectListCount(scoped.getProjectLists().size());
        scoped.setWarehouseDocCount(scoped.getWarehouseDocs().size());
        scoped.setFinanceVoucherCount(scoped.getFinanceVouchers().size());
        scoped.setTotalPendingCount(scoped.getProjectListCount() + scoped.getWarehouseDocCount() + scoped.getFinanceVoucherCount());
        return scoped;
    }

    private ProjectTrackingBoardDto.Summary buildScopedSummary(
            ProjectTrackingBoardDto board,
            List<ProjectTrackingCardDto> cards
    ) {
        ProjectTrackingBoardDto.Summary summary = new ProjectTrackingBoardDto.Summary();
        ProjectTrackingBoardDto.Permissions perms = board.getPermissions();
        summary.setTotalProjects(cards.size());
        int completed = (int) cards.stream().filter(ProjectTrackingCardDto::isCompleted).count();
        summary.setCompletedProjects(completed);
        summary.setActiveProjects(cards.size() - completed);
        summary.setOverdueCount((int) cards.stream().filter(ProjectTrackingCardDto::isOverdue).count());
        summary.setStalledCount((int) cards.stream().filter(c -> c.getRiskTags().contains("STALLED")).count());
        summary.setWarrantyExpiringCount((int) cards.stream().filter(c -> "EXPIRING_SOON".equals(c.getWarrantyStatus())).count());
        summary.setWarrantyExpiredCount((int) cards.stream().filter(c -> "EXPIRED".equals(c.getWarrantyStatus())).count());
        summary.setPendingAuditProjectCount((int) cards.stream()
                .filter(c -> ((c.getPendingListCount() != null ? c.getPendingListCount() : 0)
                        + (c.getPendingWarehouseDocCount() != null ? c.getPendingWarehouseDocCount() : 0)
                        + (c.getPendingFinanceVoucherCount() != null ? c.getPendingFinanceVoucherCount() : 0)) > 0)
                .count());
        boolean includeContract = perms == null || perms.isCanViewContracts() || perms.isCanViewFinance();
        boolean includeFinance = perms == null || perms.isCanViewFinance();
        if (includeContract) {
            summary.setTotalContractAmount(cards.stream()
                    .map(ProjectTrackingCardDto::getContractAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        if (includeFinance) {
            summary.setTotalReceivedAmount(cards.stream()
                    .map(ProjectTrackingCardDto::getReceivedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalReceivableAmount(cards.stream()
                    .map(ProjectTrackingCardDto::getReceivableAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return summary;
    }

    private Map<String, Object> buildCappedReconciliationEvidence(ProjectMaterialReconciliationDto recon) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", recon.getProjectId());
        result.put("projectNumber", recon.getProjectNumber());
        result.put("projectName", recon.getProjectName());
        result.put("customerName", recon.getCustomerName());
        result.put("totalMaterialLines", recon.getTotalMaterialLines());
        result.put("shortageLineCount", recon.getShortageLineCount());
        result.put("pendingShipmentLineCount", recon.getPendingShipmentLineCount());
        result.put("overShippedLineCount", recon.getOverShippedLineCount());
        result.put("fulfillmentRate", recon.getFulfillmentRate());
        List<MaterialReconciliationItemDto> items = recon.getItems() == null ? List.of() : recon.getItems();
        List<MaterialReconciliationItemDto> prioritized = items.stream()
                .sorted(Comparator.comparingInt(item -> ("NORMAL".equals(item.getStatus()) || "COMPLETED".equals(item.getStatus())) ? 1 : 0))
                .limit(MAX_RECONCILIATION_ITEMS)
                .toList();
        result.put("totalItemCount", items.size());
        result.put("truncated", items.size() > MAX_RECONCILIATION_ITEMS);
        result.put("items", prioritized);
        return result;
    }

    private String maskEvidenceJson(Map<String, Object> map, NameMasker masker) {
        try {
            JsonNode tree = objectMapper.valueToTree(map);
            JsonNode masked = maskJsonNode(tree, masker);
            return objectMapper.writeValueAsString(masked);
        } catch (Exception e) {
            return masker.mask(toCompactJson(map));
        }
    }

    private JsonNode maskJsonNode(JsonNode node, NameMasker masker) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(masker.mask(node.asText()));
        }
        if (node.isArray()) {
            ArrayNode arr = objectMapper.createArrayNode();
            for (JsonNode child : node) {
                arr.add(maskJsonNode(child, masker));
            }
            return arr;
        }
        if (node.isObject()) {
            ObjectNode obj = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> obj.set(entry.getKey(), maskJsonNode(entry.getValue(), masker)));
            return obj;
        }
        return node;
    }

    private List<String> resolveAuthorizedScopes() {
        List<String> scopes = new ArrayList<>();
        if (currentUserService.hasAnyAuthority("project.access", "project.manage")) {
            scopes.add("PROJECT");
        }
        if (currentUserService.hasAnyAuthority("contract.access", "contract.manage")) {
            scopes.add("CONTRACT");
        }
        if (currentUserService.hasAnyAuthority("inventory.access", "inventory.manage")) {
            scopes.add("INVENTORY");
        }
        if (currentUserService.hasAnyAuthority("finance.access", "finance.manage")) {
            scopes.add("FINANCE");
        }
        if (currentUserService.hasAnyAuthority(
                "project.list.audit",
                "project.manage",
                "inventory.warehouse-doc.audit",
                "inventory.manage",
                "finance.voucher.audit",
                "finance.manage"
        )) {
            scopes.add("APPROVAL");
        }
        return scopes;
    }

    private String toCompactJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return String.valueOf(map);
        }
    }

    private static class AnalysisSnapshot {
        private String projectUuid;
        private List<String> scopes = List.of();
        private ProjectTrackingBoardDto board;
        private List<ProjectTrackingCardDto> scopedCards = List.of();
        private PendingApprovalSummaryDto approvals;
        private ProjectMaterialReconciliationDto projectReconciliation;
        private List<ProjectReconciliationOverviewItemDto> reconciliationOverview;
        private FinanceAnalyticsOverviewDto finance;
        private ProjectProfitRowDto scopedProjectProfit;
        private Map<String, Object> evidence = new LinkedHashMap<>();
        private boolean maskSensitive;
        private final NameMasker masker = new NameMasker();
        private boolean unauthorizedIntentMessage;
    }

    static class NameMasker {
        private final Map<String, String> realToAlias = new LinkedHashMap<>();
        private final Map<String, String> aliasToReal = new LinkedHashMap<>();
        private int counter = 1;
        private Pattern maskPattern;
        private Pattern unmaskPattern;
        private Pattern questionMaskPattern;

        void register(String realName) {
            if (!StringUtils.hasText(realName)) {
                return;
            }
            String trimmed = realName.trim();
            if (!realToAlias.containsKey(trimmed)) {
                String alias = "客户#" + counter++;
                realToAlias.put(trimmed, alias);
                aliasToReal.put(alias, trimmed);
                maskPattern = null;
                unmaskPattern = null;
                questionMaskPattern = null;
            }
        }

        String mask(String text) {
            if (text == null || realToAlias.isEmpty()) {
                return text;
            }
            if (maskPattern == null) {
                maskPattern = compileSortedPattern(realToAlias.keySet(), 1);
            }
            return applyPattern(text, maskPattern, realToAlias);
        }

        String maskQuestion(String text) {
            if (text == null || realToAlias.isEmpty()) {
                return text;
            }
            if (questionMaskPattern == null) {
                questionMaskPattern = compileSortedPattern(realToAlias.keySet(), 2);
            }
            return applyPattern(text, questionMaskPattern, realToAlias);
        }

        String unmask(String text) {
            if (text == null || aliasToReal.isEmpty()) {
                return text;
            }
            if (unmaskPattern == null) {
                unmaskPattern = compileSortedPattern(aliasToReal.keySet(), 1);
            }
            return applyPattern(text, unmaskPattern, aliasToReal);
        }

        private Pattern compileSortedPattern(java.util.Set<String> keys, int minLength) {
            String alternation = keys.stream()
                    .filter(k -> k != null && k.length() >= minLength)
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            return alternation.isEmpty() ? null : Pattern.compile(alternation);
        }

        private String applyPattern(String text, Pattern pattern, Map<String, String> lookup) {
            if (pattern == null) {
                return text;
            }
            Matcher matcher = pattern.matcher(text);
            StringBuilder sb = new StringBuilder(text.length());
            while (matcher.find()) {
                String matched = matcher.group();
                String replacement = lookup.getOrDefault(matched, matched);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }
}
