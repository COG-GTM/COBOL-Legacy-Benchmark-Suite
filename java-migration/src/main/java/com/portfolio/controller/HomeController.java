package com.portfolio.controller;

import com.portfolio.service.DatabaseStatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DatabaseStatisticsService statsService;

    public HomeController(DatabaseStatisticsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("tableCounts", statsService.getTableCounts());
        model.addAttribute("transactionStats", statsService.getTransactionStats());
        return "main-menu";
    }
}
