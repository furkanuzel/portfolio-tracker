package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.PortfolioResponse;
import com.furkanuzel.portfolio.model.PositionView;
import com.furkanuzel.portfolio.model.Quote;
import com.furkanuzel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioServiceTest {

    @TempDir
    Path dataDir;

    private final Map<String, Quote> quotes = new HashMap<>();
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        PriceProvider prices = symbol -> quotes.getOrDefault(symbol, Quote.unavailable());
        service = new PortfolioService(new PortfolioRepository(dataDir), prices, false);
    }

    private void price(String symbol, double price, double previous, double weekly) {
        quotes.put(symbol, new Quote(price, previous, weekly, Instant.EPOCH, Instant.EPOCH, true));
    }

    @Test
    void buyingTwiceAveragesTheCost() {
        price("ABC.IS", 12, 11, 10);
        service.trade("abc", "abc.is", "TRY", "BUY", "10", "10");
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "10", "14");

        PositionView p = service.getPortfolio().positions().get(0);
        assertEquals(20, p.quantity(), 1e-9);
        assertEquals(12, p.averageCost(), 1e-9);
        assertEquals(240, p.costTry(), 1e-9);
        assertEquals(240, p.valueTry(), 1e-9);
    }

    @Test
    void profitAndDailyChangeAreCalculatedFromQuotes() {
        price("ABC.IS", 15, 12, 10);
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "10", "10");

        PortfolioResponse r = service.getPortfolio();
        assertEquals(50, r.totalProfitLossTry(), 1e-9);
        assertEquals(50, r.totalProfitLossPercent(), 1e-9);
        assertEquals(30, r.dailyProfitTry(), 1e-9);
        assertEquals(25, r.positions().get(0).dailyPercent(), 1e-9);
        assertEquals(50, r.weeklyProfitTry(), 1e-9);
    }

    @Test
    void usdPositionsAreConvertedToLira() {
        price(PriceProvider.USD_TRY, 40, 40, 40);
        price("AAPL", 110, 100, 100);
        service.trade("AAPL", "AAPL", "USD", "BUY", "2", "100");

        PortfolioResponse r = service.getPortfolio();
        assertEquals(8000, r.totalCostTry(), 1e-9);
        assertEquals(8800, r.totalValueTry(), 1e-9);
        assertTrue(r.warnings().isEmpty());
    }

    @Test
    void missingUsdRateExcludesUsdPositionsInsteadOfAssumingOne() {
        price("AAPL", 110, 100, 100);
        service.trade("AAPL", "AAPL", "USD", "BUY", "2", "100");

        PortfolioResponse r = service.getPortfolio();
        assertEquals(0, r.totalValueTry(), 1e-9);
        assertFalse(r.positions().get(0).priceAvailable());
        assertFalse(r.warnings().isEmpty());
    }

    @Test
    void cannotSellMoreThanHeld() {
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "5", "10");
        assertThrows(IllegalArgumentException.class,
                () -> service.trade("ABC", "ABC.IS", "TRY", "SELL", "6", "10"));
    }

    @Test
    void sellingEverythingAllowsRemoval() {
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "5", "10");
        assertThrows(IllegalArgumentException.class, () -> service.deleteClosedPosition("ABC"));
        service.trade("ABC", "ABC.IS", "TRY", "SELL", "5", "12");
        service.deleteClosedPosition("ABC");
        assertTrue(service.getPortfolio().positions().isEmpty());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> service.trade("ABC", "ABC.IS", "TRY", "BUY", "NaN", "10"));
        assertThrows(IllegalArgumentException.class, () -> service.trade("ABC", "ABC.IS", "TRY", "BUY", "Infinity", "10"));
        assertThrows(IllegalArgumentException.class, () -> service.trade("ABC", "ABC.IS", "TRY", "BUY", "-1", "10"));
        assertThrows(IllegalArgumentException.class, () -> service.trade("ABC", "ABC.IS", "EUR", "BUY", "1", "10"));
        assertThrows(IllegalArgumentException.class, () -> service.trade("A B", "ABC.IS", "TRY", "BUY", "1", "10"));
        assertThrows(IllegalArgumentException.class, () -> service.trade("ABC", "ABC.IS", "TRY", "HOLD", "1", "10"));
    }

    @Test
    void acceptsCommaAsDecimalSeparator() {
        price("ABC.IS", 10, 10, 10);
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "1,5", "10,25");
        assertEquals(1.5, service.getPortfolio().positions().get(0).quantity(), 1e-9);
    }

    @Test
    void portfolioTargetTracksProgress() {
        price("ABC.IS", 10, 10, 10);
        service.trade("ABC", "ABC.IS", "TRY", "BUY", "10", "10");
        service.savePortfolioTarget("200");
        assertEquals(50, service.getPortfolio().targetProgressPercent(), 1e-9);
    }
}
