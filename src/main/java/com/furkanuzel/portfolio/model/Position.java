package com.furkanuzel.portfolio.model;

/** A holding as stored in the database. Prices are in the position's own currency. */
public record Position(
        String ticker,
        String yahooSymbol,
        String currency,
        double quantity,
        double averageCost,
        double targetPrice) {
}
