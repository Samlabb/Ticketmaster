package com.ticket.analytics.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsMetrics metrics;

    public AnalyticsController(AnalyticsMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return metrics.snapshot();
    }
}