package com.jerry.salesmanagement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ReferenceCheckMapper {

    long countCustomerContactsByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countProjectsByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countContractsByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countFinanceVouchersByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countWarehouseDocsByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countProductHistoryPricesByCustomerUuid(@Param("customerUuid") String customerUuid);

    long countProjectListItemsByMaterialUuid(@Param("materialUuid") String materialUuid);

    long countWarehouseDocItemsByMaterialUuid(@Param("materialUuid") String materialUuid);

    long countInventoryTransactionsByMaterialUuid(@Param("materialUuid") String materialUuid);

    long countProductHistoryPricesByMaterialUuid(@Param("materialUuid") String materialUuid);

    long countWarehouseDocsByWarehouseId(@Param("warehouseId") Long warehouseId);

    long countInventoryTransactionsByWarehouseId(@Param("warehouseId") Long warehouseId);

    long countProjectListsByProjectUuid(@Param("projectUuid") String projectUuid);

    long countContractsByProjectUuid(@Param("projectUuid") String projectUuid);

    long countFinanceVouchersByProjectUuid(@Param("projectUuid") String projectUuid);

    long countWarehouseDocsByProjectUuid(@Param("projectUuid") String projectUuid);

    long countInventoryTransactionsByProjectUuid(@Param("projectUuid") String projectUuid);

    long countContractClausesByContractUuid(@Param("contractUuid") String contractUuid);

    long countWarehouseDocsByContractNumber(@Param("contractNumber") String contractNumber);

    long countInventoryTransactionsByContractUuid(@Param("contractUuid") String contractUuid);

    long countContractsByContactUuid(@Param("contactUuid") String contactUuid);

    long countCustomersByIndustry(@Param("industryName") String industryName);

    long countProjectStatusHistoryByStageId(@Param("stageId") Long stageId);

    long countProjectsByProjectTypeId(@Param("projectTypeId") Long projectTypeId);

    long countFinanceVouchersByTaxRate(@Param("taxRate") BigDecimal taxRate);

    long countMaterialsByCategoryId(@Param("categoryId") Long categoryId);

    long countMaterialsBySubcategoryId(@Param("subcategoryId") Long subcategoryId);

    long countMaterialsByBrandId(@Param("brandId") Long brandId);

    long countMaterialsByBandCode(@Param("bandCode") String bandCode);

    record ReferenceUsage(String label, long count) {
    }

    static void ensureNotReferenced(String entityLabel, ReferenceUsage... usages) {
        List<String> activeReferences = new ArrayList<>();
        for (ReferenceUsage usage : usages) {
            if (usage != null && usage.count() > 0) {
                activeReferences.add(usage.label() + "(" + usage.count() + ")");
            }
        }
        if (!activeReferences.isEmpty()) {
            throw new IllegalArgumentException(
                    "该" + entityLabel + "已被业务数据引用，无法删除（" + String.join("、", activeReferences) + "）"
            );
        }
    }
}
