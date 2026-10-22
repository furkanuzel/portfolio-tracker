package com.furkanuzel.portfolio.model;

import java.time.Instant;

/** A position with live prices and all values converted to Turkish lira. */
public record PositionView(
        String ticker,
        String symbol,
        String currency,
        double quantity,
        double averageCost,
        double currentPrice,
        double previousClose,
        double weeklyClose,
        double exchangeRate,
        double costTry,
        double valueTry,
        double profitLossTry,
        double profitLossPercent,
        double dailyPercent,
        double weeklyPercent,
        double dailyProfitTry,
        double weeklyProfitTry,
        double targetPrice,
        double targetUpsidePercent,
        Instant updatedAt,
        boolean priceAvailable) {
}
