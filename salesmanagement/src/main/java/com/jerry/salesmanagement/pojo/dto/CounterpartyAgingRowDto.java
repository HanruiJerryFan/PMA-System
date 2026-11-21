package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CounterpartyAgingRowDto {
    private String customerId;
    private String customerName;
    private String direction;
    private BigDecimal bucket0To30;
    private BigDecimal bucket31To60;
    private BigDecimal bucket61To90;
    private BigDecimal bucketOver90;
    private BigDecimal total;
    private int voucherCount;
    private String oldestOccurredOn;
    private long maxAgingDays;
}
