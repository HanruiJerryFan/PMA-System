package com.jerry.salesmanagement.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class AgingBucketDto {
    private BigDecimal bucket0To30 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal bucket31To60 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal bucket61To90 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal bucketOver90 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public void add(long agingDays, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        if (agingDays <= 30) {
            bucket0To30 = bucket0To30.add(scaled).setScale(2, RoundingMode.HALF_UP);
        } else if (agingDays <= 60) {
            bucket31To60 = bucket31To60.add(scaled).setScale(2, RoundingMode.HALF_UP);
        } else if (agingDays <= 90) {
            bucket61To90 = bucket61To90.add(scaled).setScale(2, RoundingMode.HALF_UP);
        } else {
            bucketOver90 = bucketOver90.add(scaled).setScale(2, RoundingMode.HALF_UP);
        }
        total = total.add(scaled).setScale(2, RoundingMode.HALF_UP);
    }
}
