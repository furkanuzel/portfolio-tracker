package com.furkanuzel.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furkanuzel.portfolio.model.Quote;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Daily candles from Yahoo Finance's public chart endpoint, cached for 50 seconds.
 * If a request fails, the last good quote is returned so the dashboard keeps working.
 */
public final class YahooPriceService implements PriceProvider {

    private static final Duration CACHE = Duration.ofSeconds(50);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Quote> cache = new ConcurrentHashMap<>();

    @Override
    public Quote getQuote(String symbol) {
        Quote cached = cache.get(symbol);
        if (cached != null && cached.fetchedAt().plus(CACHE).isAfter(Instant.now())) return cached;
        try {
            String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            URI uri = URI.create("https://query1.finance.yahoo.com/v8/finance/chart/" + encoded + "?interval=1d&range=1mo");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0 PortfolioTracker/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
            Quote quote = parseQuote(mapper.readTree(response.body()), Instant.now());
            if (quote.available()) cache.put(symbol, quote);
            return quote.available() || cached == null ? quote : cached;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cached != null ? cached : Quote.unavailable();
        } catch (Exception e) {
            return cached != null ? cached : Quote.unavailable();
        }
    }

    /**
     * Reads the chart response. The last candle is the current session, the one before it is
     * the real previous close ({@code chartPreviousClose} is the close before the whole range,
     * not yesterday's), and the sixth from last is the close five trading days ago.
     */
    static Quote parseQuote(JsonNode root, Instant fetchedAt) {
        JsonNode result = root.path("chart").path("result").path(0);
        JsonNode meta = result.path("meta");
        double price = meta.path("regularMarketPrice").asDouble();

        List<Double> closes = new ArrayList<>();
        for (JsonNode close : result.path("indicators").path("quote").path(0).path("close")) {
            if (!close.isNull() && close.asDouble() > 0) closes.add(close.asDouble());
        }

        double previous = closes.size() >= 2
                ? closes.get(closes.size() - 2)
                : firstPositive(meta, "regularMarketPreviousClose", "previousClose", "chartPreviousClose");
        double weekly = closes.size() >= 6 ? closes.get(closes.size() - 6) : previous;
        long marketEpoch = meta.path("regularMarketTime").asLong();
        Instant marketTime = marketEpoch > 0 ? Instant.ofEpochSecond(marketEpoch) : fetchedAt;
        return new Quote(price, previous, weekly, fetchedAt, marketTime, price > 0 && previous > 0);
    }

    private static double firstPositive(JsonNode node, String... fields) {
        for (String field : fields) {
            double value = node.path(field).asDouble();
            if (value > 0) return value;
        }
        return 0;
    }
}
