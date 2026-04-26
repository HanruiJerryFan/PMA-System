package com.jerry.salesmanagement.controller;

import com.jerry.salesmanagement.pojo.LoginRecord;
import com.jerry.salesmanagement.service.LoginRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/login-logs")
@CrossOrigin
public class LoginRecordController {

    @Autowired
    private LoginRecordService loginRecordService;

    @GetMapping
    public List<LoginRecord> getAll(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return loginRecordService.getByUserId(userId);
        }
        return loginRecordService.getAll();
    }

    @GetMapping("/{id}")
    public LoginRecord getById(@PathVariable Long id) {
        return loginRecordService.getById(id);
    }

    @GetMapping("/byUser/{userId}")
    public List<LoginRecord> getByUserId(@PathVariable Long userId) {
        return loginRecordService.getByUserId(userId);
    }

    @PostMapping
    public LoginRecord create(@RequestBody LoginRecord record) {
        // record 中至少包含 userId, loginStartTime
        return loginRecordService.create(record);
    }

    @PutMapping("/{id}")
    public LoginRecord update(@PathVariable Long id, @RequestBody LoginRecord record) {
        record.setId(id);
        return loginRecordService.update(record);
    }
}
