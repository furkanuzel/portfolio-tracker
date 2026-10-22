package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.PortfolioResponse;
import com.furkanuzel.portfolio.model.Position;
import com.furkanuzel.portfolio.model.PositionView;
import com.furkanuzel.portfolio.model.Quote;
import com.furkanuzel.portfolio.model.TradeType;
import com.furkanuzel.portfolio.repository.PortfolioRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Business rules: input validation, profit/loss maths and conversion to Turkish lira. */
public final class PortfolioService {

    private static final Pattern TICKER = Pattern.compile("[A-Z0-9._-]{1,12}");
    private static final Pattern SYMBOL = Pattern.compile("[A-Z0-9.^=_-]{1,30}");
    private static final double MAX_AMOUNT = 1_000_000_000;

    private final PortfolioRepository repository;
    private final PriceProvider prices;
    private final boolean demo;

    public PortfolioService(PortfolioRepository repository, PriceProvider prices, boolean demo) {
        this.repository = repository;
        this.prices = prices;
        this.demo = demo;
    }

    public PortfolioResponse getPortfolio() {
        List<Position> positions = repository.findAll();
        List<String> warnings = new ArrayList<>();

        boolean needsUsd = positions.stream().anyMatch(p -> "USD".equals(p.currency()));
        Quote usd = needsUsd ? prices.getQuote(PriceProvider.USD_TRY) : Quote.unavailable();
        if (needsUsd && !usd.available()) {
            warnings.add("Dolar kuru alınamadı; USD pozisyonlar toplamlara dahil edilmedi.");
        }

        double totalCost = 0, totalValue = 0, totalDaily = 0, totalWeekly = 0;
        Instant marketDataAt = null;
        List<PositionView> views = new ArrayList<>();

        for (Position p : positions) {
            boolean isUsd = "USD".equals(p.currency());
            boolean rateKnown = !isUsd || usd.available();
            double rate = isUsd ? (usd.available() ? usd.price() : 0) : 1;
            Quote q = prices.getQuote(p.yahooSymbol());
            boolean priced = q.available() && rateKnown;

            double cost = p.quantity() * p.averageCost() * rate;
            double value = priced ? p.quantity() * q.price() * rate : cost;
            double profit = value - cost;
            double profitPercent = cost == 0 ? 0 : profit / cost * 100;
            double dailyPercent = priced && q.previousClose() > 0 ? (q.price() / q.previousClose() - 1) * 100 : 0;
            double dailyProfit = priced ? p.quantity() * (q.price() - q.previousClose()) * rate : 0;
            double weeklyProfit = priced ? p.quantity() * (q.price() - q.weeklyClose()) * rate : 0;
            double weeklyPercent = priced && cost > 0 ? weeklyProfit / cost * 100 : 0;
            double targetUpside = p.targetPrice() > 0 && q.price() > 0 ? (p.targetPrice() / q.price() - 1) * 100 : 0;

            if (!q.available() && p.quantity() > 0) {
                warnings.add(p.ticker() + " fiyatı alınamadı; maliyet değeriyle gösteriliyor.");
            }

            views.add(new PositionView(p.ticker(), p.yahooSymbol(), p.currency(), p.quantity(), p.averageCost(),
                    q.price(), q.previousClose(), q.weeklyClose(), rate, cost, value, profit, profitPercent,
                    dailyPercent, weeklyPercent, dailyProfit, weeklyProfit, p.targetPrice(), targetUpside,
                    q.marketTime(), priced));

            if (priced && (marketDataAt == null || q.marketTime().isAfter(marketDataAt))) marketDataAt = q.marketTime();
            if (rateKnown) {
                totalCost += cost;
                totalValue += value;
                totalDaily += dailyProfit;
                totalWeekly += weeklyProfit;
            }
        }

        double profit = totalValue - totalCost;
        double target = repository.getPortfolioTarget();
        return new PortfolioResponse(views, totalCost, totalValue, profit,
                totalCost == 0 ? 0 : profit / totalCost * 100, totalDaily, totalWeekly, target,
                target > 0 ? totalValue / target * 100 : 0,
                marketDataAt == null ? Instant.now() : marketDataAt, demo, warnings);
    }

    public void trade(String ticker, String symbol, String currency, String type, String quantity, String price) {
        String cleanTicker = requireMatch(ticker, TICKER, "Hisse kodu geçersiz");
        String cleanSymbol = requireMatch(symbol, SYMBOL, "Hisse sembolü geçersiz");
        String cleanCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!cleanCurrency.equals("TRY") && !cleanCurrency.equals("USD")) {
            throw new IllegalArgumentException("Para birimi TRY veya USD olmalı");
        }
        repository.applyTrade(cleanTicker, cleanSymbol, cleanCurrency, TradeType.parse(type),
                positiveAmount(quantity, "Adet"), positiveAmount(price, "Birim fiyat"));
    }

    public void updateTargetPrice(String ticker, String value) {
        repository.updateTargetPrice(requireMatch(ticker, TICKER, "Hisse kodu geçersiz"),
                nonNegativeAmount(value, "Hedef fiyat"));
    }

    public void savePortfolioTarget(String value) {
        repository.savePortfolioTarget(nonNegativeAmount(value, "Portföy hedefi"));
    }

    public void deleteClosedPosition(String ticker) {
        repository.deleteClosedPosition(requireMatch(ticker, TICKER, "Hisse kodu geçersiz"));
    }

    // ---- Validation ---------------------------------------------------------

    private static String requireMatch(String value, Pattern pattern, String message) {
        String clean = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!pattern.matcher(clean).matches()) throw new IllegalArgumentException(message);
        return clean;
    }

    static double positiveAmount(String raw, String label) {
        double value = parse(raw, label);
        if (value <= 0) throw new IllegalArgumentException(label + " sıfırdan büyük olmalı");
        return value;
    }

    static double nonNegativeAmount(String raw, String label) {
        if (raw == null || raw.isBlank()) return 0;
        double value = parse(raw, label);
        if (value < 0) throw new IllegalArgumentException(label + " negatif olamaz");
        return value;
    }

    private static double parse(String raw, String label) {
        try {
            double value = Double.parseDouble(raw == null ? "" : raw.trim().replace(',', '.'));
            if (!Double.isFinite(value) || value > MAX_AMOUNT) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " geçerli bir sayı olmalı");
        }
    }
}
