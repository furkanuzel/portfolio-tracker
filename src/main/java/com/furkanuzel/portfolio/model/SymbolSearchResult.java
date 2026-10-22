package com.furkanuzel.portfolio.model;

public record SymbolSearchResult(
        String ticker,
        String symbol,
        String name,
        String exchange,
        String currency) {
}
