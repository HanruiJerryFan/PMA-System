package com.jerry.salesmanagement.service.impl;

import com.jerry.salesmanagement.mapper.CustomerActivityRuleMapper;
import com.jerry.salesmanagement.mapper.CustomerMapper;
import com.jerry.salesmanagement.pojo.Customer;
import com.jerry.salesmanagement.pojo.CustomerActivityRule;
import com.jerry.salesmanagement.service.CustomerActivityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class CustomerActivityRuleServiceImpl implements CustomerActivityRuleService {

    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "NORMAL", "INACTIVE", "DORMANT");

    @Autowired
    private CustomerActivityRuleMapper mapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public CustomerActivityRule getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<CustomerActivityRule> getAll() {
        return mapper.selectAll();
    }

    @Override
    public CustomerActivityRule create(CustomerActivityRule rule) {
        validate(rule);
        mapper.insert(rule);
        return rule;
    }

    @Override
    public CustomerActivityRule update(CustomerActivityRule rule) {
        validate(rule);
        mapper.update(rule);
        return rule;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public String resolveStatusByIdleDays(Integer idleDays) {
        int normalizedIdleDays = idleDays == null ? Integer.MAX_VALUE : Math.max(idleDays, 0);
        return mapper.selectAll().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getIsActive()))
                .sorted(Comparator.comparing(CustomerActivityRule::getSortOrder))
                .filter(rule -> inRange(normalizedIdleDays, rule))
                .map(CustomerActivityRule::getStatusCode)
                .findFirst()
                .orElse("DORMANT");
    }

    @Override
    public int refreshAllCustomerActivityStatuses() {
        int updated = 0;
        for (Customer customer : customerMapper.selectAll()) {
            String nextStatus = resolveStatusByIdleDays(calculateIdleDays(customer));
            if (!nextStatus.equals(customer.getActivityStatus())) {
                customerMapper.updateActivityStatusByUuid(customer.getUuid(), nextStatus);
                updated++;
            }
        }
        return updated;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledRefresh() {
        refreshAllCustomerActivityStatuses();
    }

    private Integer calculateIdleDays(Customer customer) {
        if (customer.getLastActiveAt() == null) {
            return null;
        }
        LocalDate lastActiveDate = customer.getLastActiveAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Math.toIntExact(ChronoUnit.DAYS.between(lastActiveDate, LocalDate.now()));
    }

    private boolean inRange(int idleDays, CustomerActivityRule rule) {
        int min = rule.getMinIdleDays() == null ? 0 : rule.getMinIdleDays();
        Integer max = rule.getMaxIdleDays();
        return idleDays >= min && (max == null || idleDays <= max);
    }

    private void validate(CustomerActivityRule rule) {
        if (!StringUtils.hasText(rule.getStatusCode())) {
            throw new IllegalArgumentException("Status code is required");
        }
        rule.setStatusCode(rule.getStatusCode().trim().toUpperCase());
        if (!STATUS_VALUES.contains(rule.getStatusCode())) {
            throw new IllegalArgumentException("Status code must be ACTIVE, NORMAL, INACTIVE, or DORMANT");
        }
        if (rule.getMinIdleDays() == null || rule.getMinIdleDays() < 0) {
            throw new IllegalArgumentException("Min idle days must be greater than or equal to 0");
        }
        if (rule.getMaxIdleDays() != null && rule.getMaxIdleDays() < rule.getMinIdleDays()) {
            throw new IllegalArgumentException("Max idle days must be greater than or equal to min idle days");
        }
        if (rule.getSortOrder() == null) {
            throw new IllegalArgumentException("Sort order is required");
        }
        if (rule.getIsActive() == null) {
            throw new IllegalArgumentException("Active flag is required");
        }
        CustomerActivityRule existing = mapper.selectByStatusCode(rule.getStatusCode());
        if (existing != null && !existing.getId().equals(rule.getId())) {
            throw new IllegalArgumentException("Status code already exists");
        }
    }
}
