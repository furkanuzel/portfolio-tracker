package com.furkanuzel.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.furkanuzel.portfolio.model.Quote;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YahooPriceServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Quote parse(String json) throws Exception {
        return YahooPriceService.parseQuote(mapper.readTree(json), Instant.EPOCH);
    }

    @Test
    void usesPenultimateCandleInsteadOfRangePreviousClose() throws Exception {
        Quote quote = parse("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":76.70,"chartPreviousClose":85.65},
                "indicators":{"quote":[{"close":[72.0,73.0,74.0,75.0,75.65,76.70]}]}}]}}
                """);
        assertEquals(76.70, quote.price(), 0.0001);
        assertEquals(75.65, quote.previousClose(), 0.0001);
        assertEquals(72.0, quote.weeklyClose(), 0.0001);
        assertTrue(quote.available());
    }

    @Test
    void skipsNullCandles() throws Exception {
        Quote quote = parse("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":32.74,"chartPreviousClose":31.0},
                "indicators":{"quote":[{"close":[30.0,null,31.5,33.5,32.74]}]}}]}}
                """);
        assertEquals(33.5, quote.previousClose(), 0.0001);
    }

    @Test
    void fallsBackToMetadataWithOnlyOneCandle() throws Exception {
        Quote quote = parse("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":10.5,"regularMarketPreviousClose":10.0,"chartPreviousClose":7.0},
                "indicators":{"quote":[{"close":[10.5]}]}}]}}
                """);
        assertEquals(10.0, quote.previousClose(), 0.0001);
    }

    @Test
    void readsMarketTimestampFromMetadata() throws Exception {
        Quote quote = parse("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":10.5,"regularMarketPreviousClose":10.0,"regularMarketTime":1789460100},
                "indicators":{"quote":[{"close":[10.0,10.5]}]}}]}}
                """);
        assertEquals(Instant.ofEpochSecond(1789460100), quote.marketTime());
    }

    @Test
    void emptyResponseIsUnavailable() throws Exception {
        assertFalse(parse("{}").available());
    }
}
