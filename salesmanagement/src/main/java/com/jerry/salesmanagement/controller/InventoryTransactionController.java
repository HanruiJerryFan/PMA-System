package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.common.ApiResponse;
import com.jerry.salesmanagement.pojo.InventoryTransaction;
import com.jerry.salesmanagement.service.InventoryTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory-transactions")
@CrossOrigin
public class InventoryTransactionController {

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @GetMapping
    public ApiResponse<List<InventoryTransaction>> getAll() {
        try {
            List<InventoryTransaction> transactions = inventoryTransactionService.getAll();
            for (InventoryTransaction transaction : transactions) {
                ensureUuid(transaction);
            }
            return ApiResponse.success(transactions);
        } catch (Exception e) {
            return ApiResponse.error("Failed to query inventory transactions: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}")
    public ApiResponse<InventoryTransaction> getByUuid(@PathVariable String uuid) {
        try {
            InventoryTransaction transaction = inventoryTransactionService.getByUuid(uuid);
            ensureUuid(transaction);
            return transaction != null ? ApiResponse.success(transaction) : ApiResponse.notFound("Inventory transaction not found");
        } catch (Exception e) {
            return ApiResponse.error("Failed to query inventory transaction: " + e.getMessage());
        }
    }

    @GetMapping("/byContract/{contractUuid}")
    public ApiResponse<List<InventoryTransaction>> getByContractUuid(@PathVariable String contractUuid) {
        try {
            List<InventoryTransaction> transactions = inventoryTransactionService.getByContractUuid(contractUuid);
            for (InventoryTransaction transaction : transactions) {
                ensureUuid(transaction);
            }
            return ApiResponse.success(transactions);
        } catch (Exception e) {
            return ApiResponse.error("Failed to query contract inventory transactions: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<InventoryTransaction> create(@RequestBody InventoryTransaction inventoryTransaction) {
        try {
            ensureUuid(inventoryTransaction);
            return ApiResponse.success(inventoryTransactionService.create(inventoryTransaction));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create inventory transaction: " + e.getMessage());
        }
    }

    @PutMapping("/{uuid}")
    public ApiResponse<InventoryTransaction> update(@PathVariable String uuid, @RequestBody InventoryTransaction inventoryTransaction) {
        try {
            inventoryTransaction.setUuid(uuid);
            return ApiResponse.success(inventoryTransactionService.update(inventoryTransaction));
        } catch (Exception e) {
            return ApiResponse.error("Failed to update inventory transaction: " + e.getMessage());
        }
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        try {
            inventoryTransactionService.delete(uuid);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete inventory transaction: " + e.getMessage());
        }
    }

    private void ensureUuid(InventoryTransaction inventoryTransaction) {
        if (inventoryTransaction != null && (inventoryTransaction.getUuid() == null || inventoryTransaction.getUuid().isEmpty())) {
            inventoryTransaction.setUuid(UUID.randomUUID().toString());
        }
    }
}
