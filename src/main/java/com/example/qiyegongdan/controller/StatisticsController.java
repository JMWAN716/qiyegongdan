package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.ApiResponse;
import com.example.qiyegongdan.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StatisticsService.Dashboard> dashboard() {
        return ApiResponse.ok(statisticsService.dashboard());
    }

    @GetMapping("/trend")
    public ApiResponse<List<StatisticsService.NameValue>> trend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(statisticsService.trend(days));
    }
}