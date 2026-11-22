package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerry.salesmanagement.common.WarehouseDocumentRules;
import com.jerry.salesmanagement.config.AiAnalysisProperties;
import com.jerry.salesmanagement.mapper.ContractBasicInfoMapper;
import com.jerry.salesmanagement.mapper.ContractTypeMapper;
import com.jerry.salesmanagement.mapper.CustomerContactMapper;
import com.jerry.salesmanagement.mapper.CustomerIndustryDictMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.mapper.CustomerTypeMapper;
import com.jerry.salesmanagement.mapper.FinanceVoucherMapper;
import com.jerry.salesmanagement.mapper.InventoryTransactionMapper;
import com.jerry.salesmanagement.mapper.MaterialMasterMapper;
import com.jerry.salesmanagement.mapper.ProductBandMapper;
import com.jerry.salesmanagement.mapper.ProductBrandMapper;
import com.jerry.salesmanagement.mapper.ProductCategoryMapper;
import com.jerry.salesmanagement.mapper.ProductSubcategoryMapper;
import com.jerry.salesmanagement.mapper.ProjectListItemMapper;
import com.jerry.salesmanagement.mapper.ProjectListMapper;
import com.jerry.salesmanagement.mapper.ProjectMapper;
import com.jerry.salesmanagement.mapper.ProjectStatusHistoryMapper;
import com.jerry.salesmanagement.mapper.ReferenceCheckMapper;
import com.jerry.salesmanagement.mapper.RegionMapper;
import com.jerry.salesmanagement.mapper.StageMapper;
import com.jerry.salesmanagement.mapper.TaxRateDictMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentItemMapper;
import com.jerry.salesmanagement.mapper.WarehouseDocumentMapper;
import com.jerry.salesmanagement.mapper.WarehouseMapper;
import com.jerry.salesmanagement.pojo.ContractBasicInfo;
import com.jerry.salesmanagement.pojo.ContractType;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerContact;
import com.jerry.salesmanagement.pojo.CustomerIndustryDict;
import com.jerry.salesmanagement.pojo.CustomerType;
import com.jerry.salesmanagement.pojo.FinanceSubject;
import com.jerry.salesmanagement.pojo.FinanceVoucher;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.pojo.MaterialMaster;
import com.jerry.salesmanagement.pojo.ProductBand;
import com.jerry.salesmanagement.pojo.ProductBrand;
import com.jerry.salesmanagement.pojo.ProductCategory;
import com.jerry.salesmanagement.pojo.ProductSubcategory;
import com.jerry.salesmanagement.pojo.Project;
import com.jerry.salesmanagement.pojo.ProjectList;
import com.jerry.salesmanagement.pojo.ProjectListItem;
import com.jerry.salesmanagement.pojo.ProjectStatusHistory;
import com.jerry.salesmanagement.pojo.Region;
import com.jerry.salesmanagement.pojo.Stage;
import com.jerry.salesmanagement.pojo.SysUser;
import com.jerry.salesmanagement.pojo.TaxRateDict;
import com.jerry.salesmanagement.pojo.Warehouse;
import com.jerry.salesmanagement.pojo.WarehouseDocument;
import com.jerry.salesmanagement.pojo.WarehouseDocumentItem;
import com.jerry.salesmanagement.pojo.dto.AiAnalysisResponse;
import com.jerry.salesmanagement.pojo.dto.AiAskRequest;
import com.jerry.salesmanagement.pojo.dto.FinanceAnalyticsOverviewDto;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalItemDto;
import com.jerry.salesmanagement.pojo.dto.PendingApprovalSummaryDto;
import com.jerry.salesmanagement.pojo.dto.ProjectMaterialReconciliationDto;
import com.jerry.salesmanagement.pojo.dto.ProjectProfitRowDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingBoardDto;
import com.jerry.salesmanagement.pojo.dto.ProjectTrackingCardDto;
import com.jerry.salesmanagement.service.AiLlmClient;
import com.jerry.salesmanagement.service.ApprovalWorkbenchService;
import com.jerry.salesmanagement.service.AttachmentService;
import com.jerry.salesmanagement.service.CodeSequenceService;
import com.jerry.salesmanagement.service.CurrentUserService;
import com.jerry.salesmanagement.service.CustomerService;
import com.jerry.salesmanagement.service.EntryAuditService;
import com.jerry.salesmanagement.service.FinanceAnalyticsService;
import com.jerry.salesmanagement.service.FinanceSubjectService;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import com.jerry.salesmanagement.service.ProjectMaterialReconciliationService;
import com.jerry.salesmanagement.service.ProjectTrackingBoardService;
import com.jerry.salesmanagement.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefectRemediationTests {

    private CurrentUserService stubUser(Long userId, boolean isSystem, Set<String> authorities) {
        return new CurrentUserService() {
            @Override
            public SysUser getCurrentUser() {
                SysUser user = new SysUser();
                user.setId(userId);
                user.setUsername(isSystem ? "System" : "tester");
                return user;
            }

            @Override
            public Long getCurrentUserId() {
                return userId;
            }

            @Override
            public Long requireCurrentUserId() {
                return userId;
            }

            @Override
            public boolean hasAnyAuthority(String... required) {
                return java.util.Arrays.stream(required).anyMatch(authorities::contains);
            }

            @Override
            public boolean isSystemUser() {
                return isSystem;
            }

            @Override
            public boolean isSystemOrAdmin() {
                return isSystem;
            }
        };
    }

    @Test
    void materialMasterAccepts11CharGwAndDmCodes() {
        MaterialMasterServiceImpl service = new MaterialMasterServiceImpl();
        MaterialMasterMapper materialMapper = mock(MaterialMasterMapper.class);
        ProductCategoryMapper categoryMapper = mock(ProductCategoryMapper.class);
        ProductSubcategoryMapper subcategoryMapper = mock(ProductSubcategoryMapper.class);
        ProductBrandMapper brandMapper = mock(ProductBrandMapper.class);
        ProductBandMapper bandMapper = mock(ProductBandMapper.class);
        SystemConfigService configService = mock(SystemConfigService.class);
        CodeSequenceService sequenceService = mock(CodeSequenceService.class);

        ReflectionTestUtils.setField(service, "materialMasterMapper", materialMapper);
        ReflectionTestUtils.setField(service, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(service, "subcategoryMapper", subcategoryMapper);
        ReflectionTestUtils.setField(service, "brandMapper", brandMapper);
        ReflectionTestUtils.setField(service, "bandMapper", bandMapper);
        ReflectionTestUtils.setField(service, "systemConfigService", configService);
        ReflectionTestUtils.setField(service, "codeSequenceService", sequenceService);
        ReflectionTestUtils.setField(service, "currentUserService", stubUser(2L, false, Set.of("product.manage")));

        ProductCategory category = new ProductCategory();
        category.setId(1L);
        category.setCode("SB");
        category.setIsActive(true);
        ProductSubcategory subcategory = new ProductSubcategory();
        subcategory.setId(2L);
        subcategory.setCode("01");
        subcategory.setIsActive(true);
        ProductBrand brand = new ProductBrand();
        brand.setId(3L);
        brand.setCode("HY");
        brand.setIsActive(true);
        ProductBand gwBand = new ProductBand();
        gwBand.setCode("GW");
        gwBand.setIsActive(true);
        ProductBand dmBand = new ProductBand();
        dmBand.setCode("DM");
        dmBand.setIsActive(true);

        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(subcategoryMapper.selectById(2L)).thenReturn(subcategory);
        when(brandMapper.selectById(3L)).thenReturn(brand);
        when(bandMapper.selectAll()).thenReturn(List.of(gwBand, dmBand));
        when(configService.getString("material.code.pattern", "{category}{subcategory}{brand}{sequence}{band}"))
                .thenReturn("{category}{subcategory}{brand}{sequence}{band}");
        when(configService.getInt("material.code.sequence_length", 3)).thenReturn(3);
        when(configService.getString("material.code.sequence_scope", "CATEGORY_SUBCATEGORY_BRAND"))
                .thenReturn("CATEGORY_SUBCATEGORY_BRAND");
        AtomicInteger seqCounter = new AtomicInteger(1);
        when(sequenceService.nextValue(anyString(), any(), anyInt(), anyString()))
                .thenAnswer(inv -> seqCounter.getAndIncrement());

        MaterialMaster gwItem = new MaterialMaster();
        gwItem.setCategoryId(1L);
        gwItem.setSubcategoryId(2L);
        gwItem.setBrandId(3L);
        gwItem.setFrequency("GW");
        gwItem.setProductName("公网对讲机");
        gwItem.setUnit("台");
        gwItem.setIsActive(true);

        MaterialMaster createdGw = service.create(gwItem);
        assertEquals(11, createdGw.getMaterialCode().length());
        assertEquals("SB01HY001GW", createdGw.getMaterialCode());

        MaterialMaster dmItem = new MaterialMaster();
        dmItem.setCategoryId(1L);
        dmItem.setSubcategoryId(2L);
        dmItem.setBrandId(3L);
        dmItem.setFrequency("DM");
        dmItem.setProductName("多模终端");
        dmItem.setUnit("台");
        dmItem.setIsActive(true);

        MaterialMaster createdDm = service.create(dmItem);
        assertEquals(11, createdDm.getMaterialCode().length());
        assertEquals("SB01HY002DM", createdDm.getMaterialCode());

        when(materialMapper.selectByUuid("m-1")).thenReturn(createdGw);
        MaterialMaster mutateClassification = new MaterialMaster();
        mutateClassification.setUuid("m-1");
        mutateClassification.setCategoryId(99L);
        mutateClassification.setSubcategoryId(2L);
        mutateClassification.setBrandId(3L);
        mutateClassification.setFrequency("GW");
        mutateClassification.setProductName("公网对讲机");
        mutateClassification.setUnit("台");
        mutateClassification.setIsActive(true);
        ProductCategory otherCategory = new ProductCategory();
        otherCategory.setId(99L);
        otherCategory.setCode("PJ");
        otherCategory.setIsActive(true);
        when(categoryMapper.selectById(99L)).thenReturn(otherCategory);

        assertThrows(IllegalArgumentException.class, () -> service.update(mutateClassification));
    }

    @Test
    void warehouseDocumentProjectIdAndInventoryTransactionAndReservedWarehouseRules() {
        WarehouseDocumentServiceImpl docService = new WarehouseDocumentServiceImpl();
        WarehouseDocumentMapper docMapper = mock(WarehouseDocumentMapper.class);
        WarehouseDocumentItemMapper docItemMapper = mock(WarehouseDocumentItemMapper.class);
        InventoryTransactionMapper txnMapper = mock(InventoryTransactionMapper.class);
        InventoryTransactionService txnService = mock(InventoryTransactionService.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        CustomerTypeMapper customerTypeMapper = mock(CustomerTypeMapper.class);
        MaterialMasterMapper materialMapper = mock(MaterialMasterMapper.class);
        ProductBrandMapper brandMapper = mock(ProductBrandMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
        CodeSequenceService sequenceService = mock(CodeSequenceService.class);
        CustomerService customerService = mock(CustomerService.class);
        EntryAuditService entryAuditService = mock(EntryAuditService.class);

        ReflectionTestUtils.setField(docService, "warehouseDocumentMapper", docMapper);
        ReflectionTestUtils.setField(docService, "warehouseDocumentItemMapper", docItemMapper);
        ReflectionTestUtils.setField(docService, "inventoryTransactionMapper", txnMapper);
        ReflectionTestUtils.setField(docService, "inventoryTransactionService", txnService);
        ReflectionTestUtils.setField(docService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(docService, "customerTypeMapper", customerTypeMapper);
        ReflectionTestUtils.setField(docService, "materialMasterMapper", materialMapper);
        ReflectionTestUtils.setField(docService, "productBrandMapper", brandMapper);
        ReflectionTestUtils.setField(docService, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(docService, "warehouseMapper", warehouseMapper);
        ReflectionTestUtils.setField(docService, "codeSequenceService", sequenceService);
        ReflectionTestUtils.setField(docService, "customerService", customerService);
        ReflectionTestUtils.setField(docService, "currentUserService", stubUser(5L, false, Set.of("inventory.manage")));
        ReflectionTestUtils.setField(docService, "entryAuditService", entryAuditService);

        Warehouse central = new Warehouse();
        central.setId(2L);
        central.setWarehouseCode(WarehouseDocumentRules.CENTRAL_WAREHOUSE);
        central.setWarehouseName("集采库");
        Warehouse projectWh = new Warehouse();
        projectWh.setId(1L);
        projectWh.setWarehouseCode(WarehouseDocumentRules.PROJECT_WAREHOUSE);
        projectWh.setWarehouseName("项目库");
        when(warehouseMapper.selectAll()).thenReturn(List.of(projectWh, central));

        WarehouseDocument projectMissing = new WarehouseDocument();
        projectMissing.setDocType("INBOUND_NOTE");
        projectMissing.setBusinessCategory("PROJECT_PURCHASE");
        projectMissing.setProjectId(null);
        assertThrows(IllegalArgumentException.class, () -> docService.create(projectMissing));

        Customer supplier = new Customer();
        supplier.setUuid("sup-1");
        supplier.setCustomerName("供应商甲");
        supplier.setCustomerTypeId(4L);
        CustomerType supplierType = new CustomerType();
        supplierType.setId(4L);
        supplierType.setTypeCode("4");
        when(customerMapper.selectByUuid("sup-1")).thenReturn(supplier);
        when(customerTypeMapper.selectById(4L)).thenReturn(supplierType);

        MaterialMaster material = new MaterialMaster();
        material.setUuid("m-1");
        material.setMaterialCode("SB01HY001GW");
        material.setProductName("信道机");
        material.setProductModel("M100");
        material.setUnit("台");
        material.setIsActive(true);
        when(materialMapper.selectByUuid("m-1")).thenReturn(material);
        when(sequenceService.nextValue(anyString(), any(), anyInt(), anyString())).thenReturn(1);
        when(txnMapper.selectWarehouseMaterialBalance(anyLong(), anyString())).thenReturn(new BigDecimal("5.00"));

        WarehouseDocument centralInbound = new WarehouseDocument();
        centralInbound.setDocType("INBOUND_NOTE");
        centralInbound.setBusinessCategory("CENTRALIZED_PURCHASE");
        centralInbound.setProjectId("should-be-cleared");
        centralInbound.setCounterpartyCustomerId("sup-1");
        centralInbound.setDocDate(new Date());
        WarehouseDocumentItem item = new WarehouseDocumentItem();
        item.setMaterialId("m-1");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("100.00"));
        centralInbound.setItems(List.of(item));

        when(docMapper.selectByDocNumber(anyString())).thenAnswer(inv -> centralInbound);
        docService.create(centralInbound);
        assertNull(centralInbound.getProjectId());

        // InventoryTransactionServiceImpl blocks direct CUD of WAREHOUSE_DOC rows
        InventoryTransactionServiceImpl invService = new InventoryTransactionServiceImpl();
        InventoryTransactionMapper invMapper = mock(InventoryTransactionMapper.class);
        ReflectionTestUtils.setField(invService, "inventoryTransactionMapper", invMapper);

        InventoryTransaction directCreate = new InventoryTransaction();
        directCreate.setSourceRefType("WAREHOUSE_DOC");
        assertThrows(IllegalArgumentException.class, () -> invService.create(directCreate));

        InventoryTransaction existingDocTxn = new InventoryTransaction();
        existingDocTxn.setUuid("txn-1");
        existingDocTxn.setSourceRefType("WAREHOUSE_DOC");
        when(invMapper.selectByUuid("txn-1")).thenReturn(existingDocTxn);
        assertThrows(IllegalArgumentException.class, () -> invService.update(existingDocTxn));
        assertThrows(IllegalArgumentException.class, () -> invService.delete("txn-1"));

        // WarehouseServiceImpl protects reserved CK-JZCG & CK-XMCG
        WarehouseServiceImpl warehouseService = new WarehouseServiceImpl();
        WarehouseMapper whMapper = mock(WarehouseMapper.class);
        ReflectionTestUtils.setField(warehouseService, "warehouseMapper", whMapper);
        when(whMapper.selectById(2L)).thenReturn(central);
        when(whMapper.selectById(1L)).thenReturn(projectWh);

        Warehouse renameCentral = new Warehouse();
        renameCentral.setId(2L);
        renameCentral.setWarehouseCode("CK-OTHER");
        renameCentral.setWarehouseName("集采库");
        assertThrows(IllegalArgumentException.class, () -> warehouseService.update(renameCentral));
        assertThrows(IllegalArgumentException.class, () -> warehouseService.delete(2L));

        Warehouse renameProject = new Warehouse();
        renameProject.setId(1L);
        renameProject.setWarehouseCode("CK-OTHER2");
        renameProject.setWarehouseName("项目库");
        assertThrows(IllegalArgumentException.class, () -> warehouseService.update(renameProject));
    }

    @Test
    void projectListItemAuditedParentLockAndStatusHistoryProgressionLocks() {
        ProjectListItemServiceImpl itemService = new ProjectListItemServiceImpl();
        ProjectListItemMapper itemMapper = mock(ProjectListItemMapper.class);
        ProjectListMapper listMapper = mock(ProjectListMapper.class);
        EntryAuditService entryAuditService = mock(EntryAuditService.class);
        ReflectionTestUtils.setField(itemService, "mapper", itemMapper);
        ReflectionTestUtils.setField(itemService, "projectListMapper", listMapper);
        ReflectionTestUtils.setField(itemService, "entryAuditService", entryAuditService);
        ReflectionTestUtils.setField(itemService, "currentUserService", stubUser(2L, false, Set.of("project.list.entry")));

        ProjectList auditedList = new ProjectList();
        auditedList.setUuid("list-1");
        auditedList.setProjectId("proj-1");
        auditedList.setAuditorUser(99L);
        when(listMapper.selectByUuid("list-1")).thenReturn(auditedList);
        when(entryAuditService.canOverrideEntryAuditUsers()).thenReturn(false);

        ProjectListItem item = new ProjectListItem();
        item.setUuid("li-1");
        item.setProjectListId("list-1");
        item.setMaterialId("m-1");
        item.setQuantity(BigDecimal.ONE);
        when(itemMapper.selectByUuid("li-1")).thenReturn(item);

        assertThrows(IllegalArgumentException.class, () -> itemService.create(item));
        assertThrows(IllegalArgumentException.class, () -> itemService.update(item));
        assertThrows(IllegalArgumentException.class, () -> itemService.delete("li-1"));

        // ProjectStatusHistoryServiceImpl
        ProjectStatusHistoryServiceImpl historyService = new ProjectStatusHistoryServiceImpl();
        ProjectStatusHistoryMapper historyMapper = mock(ProjectStatusHistoryMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        StageMapper stageMapper = mock(StageMapper.class);
        CustomerService customerService = mock(CustomerService.class);
        SystemConfigService systemConfigService = mock(SystemConfigService.class);

        ReflectionTestUtils.setField(historyService, "mapper", historyMapper);
        ReflectionTestUtils.setField(historyService, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(historyService, "stageMapper", stageMapper);
        ReflectionTestUtils.setField(historyService, "customerService", customerService);
        ReflectionTestUtils.setField(historyService, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(historyService, "currentUserService", stubUser(2L, false, Set.of("project.manage")));

        Project project = new Project();
        project.setUuid("proj-1");
        when(projectMapper.selectByUuid("proj-1")).thenReturn(project);
        when(systemConfigService.getInt("project.delete.lock_stage_sort_order", 5)).thenReturn(5);

        Stage inactiveStage = new Stage();
        inactiveStage.setId(10L);
        inactiveStage.setSortOrder(3);
        inactiveStage.setIsActive(false);
        when(stageMapper.selectById(10L)).thenReturn(inactiveStage);

        ProjectStatusHistory inactiveCreate = new ProjectStatusHistory();
        inactiveCreate.setProjectId("proj-1");
        inactiveCreate.setStageId(10L);
        assertThrows(IllegalArgumentException.class, () -> historyService.create(inactiveCreate));

        Stage earlyStage = new Stage();
        earlyStage.setId(1L);
        earlyStage.setSortOrder(2);
        earlyStage.setIsActive(true);
        Stage lockedStage = new Stage();
        lockedStage.setId(2L);
        lockedStage.setSortOrder(6);
        lockedStage.setIsActive(true);
        when(stageMapper.selectById(1L)).thenReturn(earlyStage);
        when(stageMapper.selectById(2L)).thenReturn(lockedStage);

        ProjectStatusHistory latestLocked = new ProjectStatusHistory();
        latestLocked.setId(100L);
        latestLocked.setUuid("h-locked");
        latestLocked.setProjectId("proj-1");
        latestLocked.setStageId(2L);
        latestLocked.setCreatedAt(new Date());
        when(historyMapper.selectByProjectId("proj-1")).thenReturn(List.of(latestLocked));
        when(historyMapper.selectByUuid("h-locked")).thenReturn(latestLocked);

        ProjectStatusHistory backwardCreate = new ProjectStatusHistory();
        backwardCreate.setProjectId("proj-1");
        backwardCreate.setStageId(1L);
        assertThrows(IllegalArgumentException.class, () -> historyService.create(backwardCreate));

        ProjectStatusHistory moveProject = new ProjectStatusHistory();
        moveProject.setUuid("h-locked");
        moveProject.setProjectId("proj-other");
        moveProject.setStageId(2L);
        assertThrows(IllegalArgumentException.class, () -> historyService.update(moveProject));

        ProjectStatusHistory demoteLocked = new ProjectStatusHistory();
        demoteLocked.setUuid("h-locked");
        demoteLocked.setProjectId("proj-1");
        demoteLocked.setStageId(1L);
        assertThrows(IllegalArgumentException.class, () -> historyService.update(demoteLocked));
        assertThrows(IllegalArgumentException.class, () -> historyService.delete("h-locked"));
    }

    @Test
    void dictionaryContractAndCustomerReferentialIntegrityAndImmutability() {
        ReferenceCheckMapper refMapper = mock(ReferenceCheckMapper.class);

        // TaxRateDictServiceImpl
        TaxRateDictServiceImpl taxService = new TaxRateDictServiceImpl();
        TaxRateDictMapper taxMapper = mock(TaxRateDictMapper.class);
        ReflectionTestUtils.setField(taxService, "mapper", taxMapper);
        ReflectionTestUtils.setField(taxService, "referenceCheckMapper", refMapper);
        TaxRateDict existingTax = new TaxRateDict();
        existingTax.setId(1L);
        existingTax.setRate(new BigDecimal("0.13"));
        existingTax.setLabel("13%");
        existingTax.setSortOrder(1);
        existingTax.setIsActive(true);
        when(taxMapper.selectById(1L)).thenReturn(existingTax);
        when(taxMapper.selectByRate(new BigDecimal("0.13"))).thenReturn(existingTax);
        when(refMapper.countFinanceVouchersByTaxRate(new BigDecimal("0.13"))).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> taxService.delete(1L));

        TaxRateDict changeRate = new TaxRateDict();
        changeRate.setId(1L);
        changeRate.setRate(new BigDecimal("0.09"));
        changeRate.setLabel("9%");
        changeRate.setSortOrder(1);
        changeRate.setIsActive(true);
        assertThrows(IllegalArgumentException.class, () -> taxService.update(changeRate));

        TaxRateDict labelOnly = new TaxRateDict();
        labelOnly.setId(1L);
        labelOnly.setRate(new BigDecimal("0.13"));
        labelOnly.setLabel("增值税13%");
        labelOnly.setSortOrder(1);
        labelOnly.setIsActive(true);
        assertDoesNotThrow(() -> taxService.update(labelOnly));

        // CustomerIndustryDictServiceImpl
        CustomerIndustryDictServiceImpl indService = new CustomerIndustryDictServiceImpl();
        CustomerIndustryDictMapper indMapper = mock(CustomerIndustryDictMapper.class);
        ReflectionTestUtils.setField(indService, "customerIndustryDictMapper", indMapper);
        ReflectionTestUtils.setField(indService, "referenceCheckMapper", refMapper);
        CustomerIndustryDict existingInd = new CustomerIndustryDict();
        existingInd.setId(1L);
        existingInd.setIndustryName("电力");
        existingInd.setIsActive(true);
        when(indMapper.selectById(1L)).thenReturn(existingInd);
        when(refMapper.countCustomersByIndustry("电力")).thenReturn(1L);
        CustomerIndustryDict renameInd = new CustomerIndustryDict();
        renameInd.setId(1L);
        renameInd.setIndustryName("能源电力");
        renameInd.setIsActive(true);
        assertThrows(IllegalArgumentException.class, () -> indService.update(renameInd));
        assertThrows(IllegalArgumentException.class, () -> indService.delete(1L));

        // CustomerContactServiceImpl
        CustomerContactServiceImpl contactService = new CustomerContactServiceImpl();
        CustomerContactMapper contactMapper = mock(CustomerContactMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ReflectionTestUtils.setField(contactService, "customerContactMapper", contactMapper);
        ReflectionTestUtils.setField(contactService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(contactService, "referenceCheckMapper", refMapper);
        ReflectionTestUtils.setField(contactService, "customerService", mock(CustomerService.class));

        Customer cust1 = new Customer();
        cust1.setUuid("cust-1");
        Customer cust2 = new Customer();
        cust2.setUuid("cust-2");
        when(customerMapper.selectByUuid("cust-1")).thenReturn(cust1);
        when(customerMapper.selectByUuid("cust-2")).thenReturn(cust2);

        CustomerContact existingContact = new CustomerContact();
        existingContact.setUuid("cc-1");
        existingContact.setClientId("cust-1");
        existingContact.setContactName("王五");
        when(contactMapper.selectByUuid("cc-1")).thenReturn(existingContact);
        when(refMapper.countContractsByContactUuid("cc-1")).thenReturn(1L);

        CustomerContact moveContact = new CustomerContact();
        moveContact.setUuid("cc-1");
        moveContact.setClientId("cust-2");
        moveContact.setContactName("王五");
        assertThrows(IllegalArgumentException.class, () -> contactService.update(moveContact));
        assertThrows(IllegalArgumentException.class, () -> contactService.delete("cc-1"));

        // ContractBasicInfoServiceImpl
        ContractBasicInfoServiceImpl contractService = new ContractBasicInfoServiceImpl();
        ContractBasicInfoMapper contractMapper = mock(ContractBasicInfoMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ContractTypeMapper contractTypeMapper = mock(ContractTypeMapper.class);
        ReflectionTestUtils.setField(contractService, "contractbasicInfoMapper", contractMapper);
        ReflectionTestUtils.setField(contractService, "referenceCheckMapper", refMapper);
        ReflectionTestUtils.setField(contractService, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(contractService, "contractTypeMapper", contractTypeMapper);
        ReflectionTestUtils.setField(contractService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(contractService, "customerContactMapper", contactMapper);
        ReflectionTestUtils.setField(contractService, "customerService", mock(CustomerService.class));

        Project proj = new Project();
        proj.setUuid("p-1");
        when(projectMapper.selectByUuid("p-1")).thenReturn(proj);
        ContractType cType = new ContractType();
        cType.setId(1L);
        when(contractTypeMapper.selectById(1L)).thenReturn(cType);

        ContractBasicInfo existingContract = new ContractBasicInfo();
        existingContract.setUuid("ht-1");
        existingContract.setContractNumber("HT-001");
        existingContract.setClientId("cust-1");
        when(contractMapper.selectAll()).thenReturn(List.of(existingContract));
        when(contractMapper.selectByUuid("ht-1")).thenReturn(existingContract);
        when(refMapper.countContractClausesByContractUuid("ht-1")).thenReturn(1L);

        ContractBasicInfo dupContract = new ContractBasicInfo();
        dupContract.setProjectBasicInfoId("p-1");
        dupContract.setContractTypeId(1L);
        dupContract.setContractNumber("HT-001");
        dupContract.setClientId("cust-1");
        assertThrows(IllegalArgumentException.class, () -> contractService.create(dupContract));

        ContractBasicInfo badSubItem = new ContractBasicInfo();
        badSubItem.setProjectBasicInfoId("p-1");
        badSubItem.setContractTypeId(1L);
        badSubItem.setContractNumber("HT-002");
        badSubItem.setClientId("cust-1");
        badSubItem.setSubItemNo("001");
        assertThrows(IllegalArgumentException.class, () -> contractService.create(badSubItem));

        ContractBasicInfo negativeAmount = new ContractBasicInfo();
        negativeAmount.setProjectBasicInfoId("p-1");
        negativeAmount.setContractTypeId(1L);
        negativeAmount.setContractNumber("HT-003");
        negativeAmount.setClientId("cust-1");
        negativeAmount.setContractAmount(new BigDecimal("-10"));
        assertThrows(IllegalArgumentException.class, () -> contractService.create(negativeAmount));

        ContractBasicInfo foreignContact = new ContractBasicInfo();
        foreignContact.setProjectBasicInfoId("p-1");
        foreignContact.setContractTypeId(1L);
        foreignContact.setContractNumber("HT-004");
        foreignContact.setClientId("cust-2");
        foreignContact.setContactId("cc-1");
        assertThrows(IllegalArgumentException.class, () -> contractService.create(foreignContact));

        assertThrows(IllegalArgumentException.class, () -> contractService.delete("ht-1"));

        // ProductCategoryServiceImpl & ProductBandServiceImpl
        ProductCategoryServiceImpl catService = new ProductCategoryServiceImpl();
        ProductCategoryMapper catMapper = mock(ProductCategoryMapper.class);
        ReflectionTestUtils.setField(catService, "categoryMapper", catMapper);
        ReflectionTestUtils.setField(catService, "referenceCheckMapper", refMapper);
        ProductCategory existingCat = new ProductCategory();
        existingCat.setId(1L);
        existingCat.setCode("SB");
        existingCat.setName("设备");
        existingCat.setSortOrder(1);
        existingCat.setIsActive(true);
        when(catMapper.selectById(1L)).thenReturn(existingCat);
        when(catMapper.selectAll()).thenReturn(List.of(existingCat));
        when(refMapper.countMaterialsByCategoryId(1L)).thenReturn(3L);
        ProductCategory changeCat = new ProductCategory();
        changeCat.setId(1L);
        changeCat.setCode("PJ");
        changeCat.setName("设备");
        changeCat.setSortOrder(1);
        changeCat.setIsActive(true);
        assertThrows(IllegalArgumentException.class, () -> catService.update(changeCat));
        assertThrows(IllegalArgumentException.class, () -> catService.delete(1L));

        ProductBandServiceImpl bandService = new ProductBandServiceImpl();
        ProductBandMapper bandMapper = mock(ProductBandMapper.class);
        ReflectionTestUtils.setField(bandService, "mapper", bandMapper);
        ReflectionTestUtils.setField(bandService, "referenceCheckMapper", refMapper);
        ProductBand existingBand = new ProductBand();
        existingBand.setId(1L);
        existingBand.setCode("GW");
        existingBand.setDescription("公网");
        existingBand.setSortOrder(1);
        existingBand.setIsActive(true);
        when(bandMapper.selectById(1L)).thenReturn(existingBand);
        when(bandMapper.selectAll()).thenReturn(List.of(existingBand));
        when(refMapper.countMaterialsByBandCode("GW")).thenReturn(1L);
        ProductBand changeBand = new ProductBand();
        changeBand.setId(1L);
        changeBand.setCode("DM");
        changeBand.setDescription("多模");
        changeBand.setSortOrder(1);
        changeBand.setIsActive(true);
        assertThrows(IllegalArgumentException.class, () -> bandService.update(changeBand));
        assertThrows(IllegalArgumentException.class, () -> bandService.delete(1L));

        // CustomerServiceImpl blank tax ID trimming + region/customerType freeze
        CustomerServiceImpl customerService = new CustomerServiceImpl();
        RegionMapper regionMapper = mock(RegionMapper.class);
        CustomerTypeMapper customerTypeMapper = mock(CustomerTypeMapper.class);
        CodeSequenceService codeSequenceService = mock(CodeSequenceService.class);
        ReflectionTestUtils.setField(customerService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerService, "regionMapper", regionMapper);
        ReflectionTestUtils.setField(customerService, "customerTypeMapper", customerTypeMapper);
        ReflectionTestUtils.setField(customerService, "codeSequenceService", codeSequenceService);
        ReflectionTestUtils.setField(customerService, "currentUserService", stubUser(1L, false, Set.of("customer.manage")));

        Region region = new Region();
        region.setId(10);
        region.setAreaCode(110100L);
        CustomerType custType = new CustomerType();
        custType.setId(5L);
        custType.setTypeCode("5");
        when(regionMapper.selectById(10)).thenReturn(region);
        when(customerTypeMapper.selectById(5L)).thenReturn(custType);
        when(codeSequenceService.nextValue(anyString(), any(), anyInt(), anyString())).thenReturn(1);

        Customer newCust = new Customer();
        newCust.setCustomerName("测试客户");
        newCust.setRegionId(10L);
        newCust.setCustomerTypeId(5L);
        newCust.setTaxIdentificationNumber("   ");
        Customer createdCust = customerService.create(newCust);
        assertNull(createdCust.getTaxIdentificationNumber());
        assertEquals("110150001", createdCust.getCustomerCode());

        Customer existingDbCust = new Customer();
        existingDbCust.setUuid("cust-frozen");
        existingDbCust.setCustomerCode("110150001");
        existingDbCust.setCustomerName("测试客户");
        existingDbCust.setRegionId(10L);
        existingDbCust.setCustomerTypeId(5L);
        when(customerMapper.selectByUuid("cust-frozen")).thenReturn(existingDbCust);

        Customer changeRegion = new Customer();
        changeRegion.setUuid("cust-frozen");
        changeRegion.setCustomerName("测试客户");
        changeRegion.setRegionId(20L);
        changeRegion.setCustomerTypeId(5L);
        assertThrows(IllegalArgumentException.class, () -> customerService.update(changeRegion));
    }

    @Test
    void financeVoucherUpdateLocksAndAttachmentCleanup() {
        FinanceVoucherServiceImpl voucherService = new FinanceVoucherServiceImpl();
        FinanceVoucherMapper voucherMapper = mock(FinanceVoucherMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        TaxRateDictMapper taxRateDictMapper = mock(TaxRateDictMapper.class);
        CodeSequenceService codeSequenceService = mock(CodeSequenceService.class);
        CustomerService customerService = mock(CustomerService.class);
        EntryAuditService entryAuditService = mock(EntryAuditService.class);
        FinanceSubjectService financeSubjectService = mock(FinanceSubjectService.class);
        AttachmentService attachmentService = mock(AttachmentService.class);

        ReflectionTestUtils.setField(voucherService, "mapper", voucherMapper);
        ReflectionTestUtils.setField(voucherService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(voucherService, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(voucherService, "taxRateDictMapper", taxRateDictMapper);
        ReflectionTestUtils.setField(voucherService, "codeSequenceService", codeSequenceService);
        ReflectionTestUtils.setField(voucherService, "customerService", customerService);
        ReflectionTestUtils.setField(voucherService, "currentUserService", stubUser(3L, false, Set.of("finance.manage")));
        ReflectionTestUtils.setField(voucherService, "entryAuditService", entryAuditService);
        ReflectionTestUtils.setField(voucherService, "financeSubjectService", financeSubjectService);
        ReflectionTestUtils.setField(voucherService, "attachmentService", attachmentService);

        LocalDate occurredOn = LocalDate.of(2026, 6, 15);
        FinanceVoucher existing = new FinanceVoucher();
        existing.setId(1L);
        existing.setUuid("fv-1");
        existing.setVoucherNo("2026061501");
        existing.setOccurredOn(occurredOn);
        existing.setTransactionDirection("RECEIVE");
        existing.setTaxRate(new BigDecimal("0.06"));
        existing.setActualIncomeAmount(new BigDecimal("500.00"));
        when(voucherMapper.selectByUuid("fv-1")).thenReturn(existing);
        when(voucherMapper.selectByVoucherNo("2026061501")).thenReturn(existing);

        FinanceSubject l1 = new FinanceSubject();
        l1.setId(1L);
        l1.setSubjectLevel(1);
        l1.setSubjectName("项目");
        FinanceSubject l2 = new FinanceSubject();
        l2.setId(4L);
        l2.setSubjectLevel(2);
        l2.setSubjectCode("EQUIPMENT_PURCHASE");
        l2.setSubjectName("设备采购");
        when(financeSubjectService.validateSubject(1, 1L)).thenReturn(l1);
        when(financeSubjectService.validateSubject(2, 4L)).thenReturn(l2);

        Project project = new Project();
        project.setUuid("p-1");
        when(projectMapper.selectByUuid("p-1")).thenReturn(project);

        Customer counterparty = new Customer();
        counterparty.setUuid("c-1");
        counterparty.setCustomerName("客户A");
        when(customerMapper.selectByUuid("c-1")).thenReturn(counterparty);

        TaxRateDict disabled06 = new TaxRateDict();
        disabled06.setRate(new BigDecimal("0.06"));
        disabled06.setIsActive(false);
        TaxRateDict disabled03 = new TaxRateDict();
        disabled03.setRate(new BigDecimal("0.03"));
        disabled03.setIsActive(false);
        when(taxRateDictMapper.selectByRate(new BigDecimal("0.06"))).thenReturn(disabled06);
        when(taxRateDictMapper.selectByRate(new BigDecimal("0.03"))).thenReturn(disabled03);

        // Switching to a different disabled taxRate must fail
        FinanceVoucher switchDisabledRate = new FinanceVoucher();
        switchDisabledRate.setUuid("fv-1");
        switchDisabledRate.setVoucherNo("9999999999");
        switchDisabledRate.setOccurredOn(occurredOn);
        switchDisabledRate.setLevel1SubjectId(1L);
        switchDisabledRate.setLevel2SubjectId(4L);
        switchDisabledRate.setProjectId("p-1");
        switchDisabledRate.setSummary("测试凭证");
        switchDisabledRate.setTransactionDirection("PAY");
        switchDisabledRate.setTaxRate(new BigDecimal("0.03"));
        switchDisabledRate.setCounterpartyCustomerId("c-1");
        switchDisabledRate.setInvoiceStatus("NOT_INVOICED");
        switchDisabledRate.setIsCompleted(true);
        switchDisabledRate.setActualExpenseAmount(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> voucherService.update(switchDisabledRate));

        // Keeping the existing disabled taxRate, trying to alter voucherNo, and switching RECEIVE -> PAY
        FinanceVoucher validUpdate = new FinanceVoucher();
        validUpdate.setUuid("fv-1");
        validUpdate.setVoucherNo("2026069999");
        validUpdate.setOccurredOn(occurredOn);
        validUpdate.setLevel1SubjectId(1L);
        validUpdate.setLevel2SubjectId(4L);
        validUpdate.setProjectId("p-1");
        validUpdate.setSummary("切付款并保留历史停用税率");
        validUpdate.setTransactionDirection("PAY");
        validUpdate.setTaxRate(new BigDecimal("0.06"));
        validUpdate.setCounterpartyCustomerId("c-1");
        validUpdate.setInvoiceStatus("NOT_INVOICED");
        validUpdate.setIsCompleted(true);
        validUpdate.setActualIncomeAmount(BigDecimal.ZERO);
        validUpdate.setActualExpenseAmount(new BigDecimal("93.00"));

        voucherService.update(validUpdate);
        assertEquals("2026061501", validUpdate.getVoucherNo());
        assertNull(validUpdate.getActualIncomeAmount());
        assertEquals(new BigDecimal("100.00"), validUpdate.getBookedAmount());

        // Delete cleans up finance-vouchers attachments
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(codeSequenceService).deleteAndReclaimTail(eq("finance_voucher:202606"), any(), any());

        voucherService.delete("fv-1");
        verify(attachmentService).deleteByBusiness("finance-vouchers", "fv-1");
    }

    @Test
    void reconciliationHandlesEmptyListsAndNegativeNetSuppliedSafely() {
        ProjectMaterialReconciliationServiceImpl reconService = new ProjectMaterialReconciliationServiceImpl();
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ProjectListMapper listMapper = mock(ProjectListMapper.class);
        ProjectListItemMapper listItemMapper = mock(ProjectListItemMapper.class);
        WarehouseDocumentMapper docMapper = mock(WarehouseDocumentMapper.class);
        WarehouseDocumentItemMapper docItemMapper = mock(WarehouseDocumentItemMapper.class);
        InventoryTransactionMapper txnMapper = mock(InventoryTransactionMapper.class);

        ReflectionTestUtils.setField(reconService, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(reconService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(reconService, "projectListMapper", listMapper);
        ReflectionTestUtils.setField(reconService, "projectListItemMapper", listItemMapper);
        ReflectionTestUtils.setField(reconService, "warehouseDocumentMapper", docMapper);
        ReflectionTestUtils.setField(reconService, "warehouseDocumentItemMapper", docItemMapper);
        ReflectionTestUtils.setField(reconService, "inventoryTransactionMapper", txnMapper);
        ReflectionTestUtils.setField(reconService, "currentUserService", stubUser(1L, false, Set.of("project.access", "inventory.access")));

        Project project = new Project();
        project.setUuid("p-empty");
        project.setProjectNumber("AB-9001");
        project.setProjectName("空清单项目");
        when(projectMapper.selectByUuid("p-empty")).thenReturn(project);
        when(listMapper.selectByProjectId("p-empty")).thenReturn(List.of());
        when(docMapper.selectAll()).thenReturn(List.of());
        when(txnMapper.selectAll()).thenReturn(List.of());

        ProjectMaterialReconciliationDto emptyDto = reconService.getProjectReconciliation("p-empty");
        assertNotNull(emptyDto);
        assertEquals(0, emptyDto.getTotalMaterialLines());
        assertTrue(emptyDto.getItems().isEmpty());

        // Project with planned procurement 10 and TRANSFER_XTJ out 5 -> netSupplied = -5, shortage
        Project negProj = new Project();
        negProj.setUuid("p-neg");
        negProj.setProjectNumber("AB-9002");
        negProj.setProjectName("负供货测试");
        when(projectMapper.selectByUuid("p-neg")).thenReturn(negProj);

        ProjectList procList = new ProjectList();
        procList.setUuid("l-proc");
        procList.setProjectId("p-neg");
        procList.setListType("PROCUREMENT");
        when(listMapper.selectByProjectId("p-neg")).thenReturn(List.of(procList));

        ProjectListItem procItem = new ProjectListItem();
        procItem.setUuid("li-proc");
        procItem.setProjectListId("l-proc");
        procItem.setMaterialId("m-1");
        procItem.setMaterialCode("SB01HY001GW");
        procItem.setItemName("电台");
        procItem.setQuantity(new BigDecimal("10"));
        procItem.setUnitPrice(new BigDecimal("100.00"));
        procItem.setTotalAmount(new BigDecimal("1000.00"));
        when(listItemMapper.selectByProjectListIds(List.of("l-proc"))).thenReturn(List.of(procItem));

        WarehouseDocument xtjDoc = new WarehouseDocument();
        xtjDoc.setDocNumber("HBX202600001");
        xtjDoc.setDocType("TRANSFER_XTJ");
        xtjDoc.setProjectId("p-neg");
        when(docMapper.selectAll()).thenReturn(List.of(xtjDoc));

        WarehouseDocumentItem xtjItem = new WarehouseDocumentItem();
        xtjItem.setDocNumber("HBX202600001");
        xtjItem.setMaterialId("m-1");
        xtjItem.setMaterialCode("SB01HY001GW");
        xtjItem.setMaterialName("电台");
        xtjItem.setQuantity(new BigDecimal("5"));
        xtjItem.setUnitPrice(new BigDecimal("100.00"));
        xtjItem.setAmount(new BigDecimal("500.00"));
        when(docItemMapper.selectByDocNumbers(List.of("HBX202600001"))).thenReturn(List.of(xtjItem));

        ProjectMaterialReconciliationDto negDto = reconService.getProjectReconciliation("p-neg");
        assertEquals(1, negDto.getTotalMaterialLines());
        assertEquals(new BigDecimal("-5.0000"), negDto.getItems().get(0).getNetSuppliedQty());
        assertEquals(new BigDecimal("15.0000"), negDto.getItems().get(0).getProcurementGapQty());
        assertEquals("PROCUREMENT_SHORTAGE", negDto.getItems().get(0).getStatus());
    }

    @Test
    void aiAnalysisNameMaskerSingleProjectScopingAndIntentClassification() throws Exception {
        AiBusinessAnalysisServiceImpl.NameMasker masker = new AiBusinessAnalysisServiceImpl.NameMasker();
        masker.register("星辰科技");
        masker.register("星辰科技有限公司");
        String masked = masker.mask("星辰科技有限公司与星辰科技合作");
        assertEquals("客户#2与客户#1合作", masked);
        assertEquals("星辰科技有限公司与星辰科技合作", masker.unmask(masked));

        AiBusinessAnalysisServiceImpl.NameMasker quoteMasker = new AiBusinessAnalysisServiceImpl.NameMasker();
        quoteMasker.register("\"星辰\"公司");
        ObjectMapper objectMapper = new ObjectMapper();

        AiAnalysisProperties props = new AiAnalysisProperties();
        props.setEnabled(true);
        props.setBaseUrl("https://api.example.com/v1");
        props.setModel("gpt-test");
        props.setMaskSensitiveNames(true);

        ProjectTrackingBoardService boardService = mock(ProjectTrackingBoardService.class);
        ApprovalWorkbenchService approvalService = mock(ApprovalWorkbenchService.class);
        ProjectMaterialReconciliationService reconService = mock(ProjectMaterialReconciliationService.class);
        FinanceAnalyticsService financeService = mock(FinanceAnalyticsService.class);
        AiLlmClient llmClient = mock(AiLlmClient.class);

        ProjectTrackingBoardDto board = new ProjectTrackingBoardDto();
        ProjectTrackingCardDto card1 = new ProjectTrackingCardDto();
        card1.setUuid("p1");
        card1.setProjectNumber("AB-1001");
        card1.setProjectName("项目一");
        card1.setCustomerName("\"星辰\"公司");
        ProjectTrackingCardDto card2 = new ProjectTrackingCardDto();
        card2.setUuid("p2");
        card2.setProjectNumber("AB-1002");
        card2.setProjectName("项目二");
        card2.setCustomerName("无关客户");
        board.setItems(List.of(card1, card2));
        when(boardService.getTrackingBoard(null, null, null, null)).thenReturn(board);

        PendingApprovalSummaryDto approvals = new PendingApprovalSummaryDto();
        PendingApprovalItemDto plP1 = new PendingApprovalItemDto();
        plP1.setProjectId("p1");
        plP1.setTitle("P1清单");
        PendingApprovalItemDto plP2 = new PendingApprovalItemDto();
        plP2.setProjectId("p2");
        plP2.setTitle("P2清单");
        PendingApprovalItemDto wdP1 = new PendingApprovalItemDto();
        wdP1.setProjectId("p1");
        wdP1.setTitle("P1入库");
        PendingApprovalItemDto fvP2 = new PendingApprovalItemDto();
        fvP2.setProjectId("p2");
        fvP2.setTitle("P2凭证");
        approvals.setProjectLists(List.of(plP1, plP2));
        approvals.setWarehouseDocs(List.of(wdP1));
        approvals.setFinanceVouchers(List.of(fvP2));
        when(approvalService.getPendingApprovals()).thenReturn(approvals);

        FinanceAnalyticsOverviewDto finance = new FinanceAnalyticsOverviewDto();
        ProjectProfitRowDto profitP1 = new ProjectProfitRowDto();
        profitP1.setProjectId("p1");
        profitP1.setProjectNumber("AB-1001");
        profitP1.setCustomerName("\"星辰\"公司");
        profitP1.setGrossProfit(new BigDecimal("800.00"));
        ProjectProfitRowDto profitP2 = new ProjectProfitRowDto();
        profitP2.setProjectId("p2");
        profitP2.setProjectNumber("AB-1002");
        profitP2.setCustomerName("无关客户");
        profitP2.setGrossProfit(new BigDecimal("1200.00"));
        finance.setProjectProfits(List.of(profitP1, profitP2));
        when(financeService.getOverview()).thenReturn(finance);
        when(llmClient.chat(anyString(), anyString())).thenReturn("ok");

        AiBusinessAnalysisServiceImpl scopedService = new AiBusinessAnalysisServiceImpl();
        ReflectionTestUtils.setField(scopedService, "properties", props);
        ReflectionTestUtils.setField(scopedService, "aiLlmClient", llmClient);
        ReflectionTestUtils.setField(
                scopedService,
                "currentUserService",
                stubUser(1L, false, Set.of("project.access", "project.list.audit", "finance.access"))
        );
        ReflectionTestUtils.setField(scopedService, "projectTrackingBoardService", boardService);
        ReflectionTestUtils.setField(scopedService, "approvalWorkbenchService", approvalService);
        ReflectionTestUtils.setField(scopedService, "reconciliationService", reconService);
        ReflectionTestUtils.setField(scopedService, "financeAnalyticsService", financeService);
        ReflectionTestUtils.setField(scopedService, "objectMapper", objectMapper);

        AiAnalysisResponse briefing = scopedService.generateBriefing("p1");
        @SuppressWarnings("unchecked")
        Map<String, Object> pendingApprovals = (Map<String, Object>) briefing.getEvidence().get("pendingApprovals");
        assertEquals(2, pendingApprovals.get("totalPendingCount"));
        assertEquals(1, pendingApprovals.get("projectListCount"));
        assertEquals(1, pendingApprovals.get("warehouseDocCount"));
        assertEquals(0, pendingApprovals.get("financeVoucherCount"));
        ProjectProfitRowDto scopedFinance = (ProjectProfitRowDto) briefing.getEvidence().get("projectFinance");
        assertEquals("p1", scopedFinance.getProjectId());

        // Quoted customer name masking still yields valid JSON
        String maskedJson = quoteMasker.mask("\"星辰\"公司");
        assertEquals("客户#1", maskedJson);
        JsonNode parsedEvidence = objectMapper.readTree(
                (String) ReflectionTestUtils.invokeMethod(scopedService, "maskEvidenceJson", briefing.getEvidence(), quoteMasker)
        );
        assertEquals("客户#1", parsedEvidence.get("projectFinance").get("customerName").asText());

        // Project-only user asking "目前项目毛利是多少" must be denied with finance unauthorized message
        AiBusinessAnalysisServiceImpl projectOnlyService = new AiBusinessAnalysisServiceImpl();
        AiLlmClient unusedLlm = mock(AiLlmClient.class);
        ReflectionTestUtils.setField(projectOnlyService, "properties", props);
        ReflectionTestUtils.setField(projectOnlyService, "aiLlmClient", unusedLlm);
        ReflectionTestUtils.setField(projectOnlyService, "currentUserService", stubUser(2L, false, Set.of("project.access")));
        ReflectionTestUtils.setField(projectOnlyService, "projectTrackingBoardService", boardService);
        ReflectionTestUtils.setField(projectOnlyService, "objectMapper", objectMapper);

        AiAskRequest askReq = new AiAskRequest();
        askReq.setQuestion("目前项目毛利是多少");
        AiAnalysisResponse askResp = projectOnlyService.askQuestion(askReq);
        assertTrue(askResp.getAnswer().contains("finance.access"));
        verifyNoInteractions(unusedLlm);
    }
}
