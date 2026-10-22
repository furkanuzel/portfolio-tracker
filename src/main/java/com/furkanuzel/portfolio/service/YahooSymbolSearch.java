package com.furkanuzel.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furkanuzel.portfolio.model.SymbolSearchResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stock and ETF search through Yahoo Finance. Borsa Istanbul symbols (.IS) are priced in TRY. */
public final class YahooSymbolSearch implements SymbolSearch {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<SymbolSearchResult> search(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.length() < 2) return List.of();

        try {
            String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
            URI uri = URI.create("https://query2.finance.yahoo.com/v1/finance/search?q=" + encoded
                    + "&quotesCount=12&newsCount=0&listsCount=0&enableFuzzyQuery=true");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0 PortfolioTracker/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return List.of();

            List<SymbolSearchResult> results = new ArrayList<>();
            for (JsonNode quote : mapper.readTree(response.body()).path("quotes")) {
                String type = quote.path("quoteType").asText();
                if (!"EQUITY".equals(type) && !"ETF".equals(type)) continue;

                String symbol = quote.path("symbol").asText("");
                if (symbol.isBlank()) continue;
                boolean istanbul = symbol.endsWith(".IS");
                String ticker = istanbul ? symbol.substring(0, symbol.length() - 3) : symbol;
                String name = quote.path("longname").asText(quote.path("shortname").asText(ticker));
                String exchange = quote.path("exchDisp").asText(quote.path("exchange").asText(""));
                String currency = istanbul ? "TRY" : quote.path("currency").asText("USD");
                results.add(new SymbolSearchResult(ticker.toUpperCase(Locale.ROOT), symbol, name, exchange, currency));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
