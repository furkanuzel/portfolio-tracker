package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.Quote;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * Simulated prices for demo mode: no network needed. Each symbol follows a smooth,
 * deterministic curve, so a given day always shows the same numbers.
 */
public final class DemoPriceService implements PriceProvider {

    static final Map<String, Double> BASE_PRICES = Map.ofEntries(
            Map.entry("THYAO.IS", 312.0),
            Map.entry("ASELS.IS", 148.5),
            Map.entry("BIMAS.IS", 521.0),
            Map.entry("KCHOL.IS", 186.4),
            Map.entry("FROTO.IS", 1012.0),
            Map.entry("SISE.IS", 41.6),
            Map.entry("EREGL.IS", 27.9),
            Map.entry("AAPL", 228.0),
            Map.entry("MSFT", 431.0),
            Map.entry("NVDA", 134.0),
            Map.entry(USD_TRY, 41.2));

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    @Override
    public Quote getQuote(String symbol) {
        Double base = BASE_PRICES.get(symbol);
        if (base == null) return Quote.unavailable();
        long day = LocalDate.now(ISTANBUL).toEpochDay();
        Instant now = Instant.now();
        return new Quote(priceOn(symbol, base, day), priceOn(symbol, base, day - 1),
                priceOn(symbol, base, day - 5), now, now, true);
    }

    private static double priceOn(String symbol, double base, long day) {
        if (USD_TRY.equals(symbol)) return round(base * (1 + 0.002 * Math.sin(day / 3.0)));
        double phase = Math.abs(symbol.hashCode() % 97) / 10.0;
        double trend = 0.06 * Math.sin(day / 11.0 + phase);
        double wiggle = 0.018 * Math.sin(day * 1.7 + phase * 3);
        return round(base * (1 + trend + wiggle));
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
