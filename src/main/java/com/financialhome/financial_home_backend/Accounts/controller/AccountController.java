package com.financialhome.financial_home_backend.Accounts.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    @GetMapping
    public String getAccounts() {
        return "Accounts";
    }
}
