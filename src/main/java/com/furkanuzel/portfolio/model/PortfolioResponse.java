package com.furkanuzel.portfolio.model;

import java.time.Instant;
import java.util.List;

/** Everything the dashboard needs in one JSON response. */
public record PortfolioResponse(
        List<PositionView> positions,
        double totalCostTry,
        double totalValueTry,
        double totalProfitLossTry,
        double totalProfitLossPercent,
        double dailyProfitTry,
        double weeklyProfitTry,
        double portfolioTargetTry,
        double targetProgressPercent,
        Instant updatedAt,
        boolean demo,
        List<String> warnings) {
}
