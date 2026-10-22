package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.SymbolSearchResult;

import java.util.List;
import java.util.Locale;

/** Offline search over the symbols that {@link DemoPriceService} can price. */
public final class DemoSymbolSearch implements SymbolSearch {

    private static final List<SymbolSearchResult> SYMBOLS = List.of(
            new SymbolSearchResult("THYAO", "THYAO.IS", "Türk Hava Yolları", "Istanbul", "TRY"),
            new SymbolSearchResult("ASELS", "ASELS.IS", "Aselsan", "Istanbul", "TRY"),
            new SymbolSearchResult("BIMAS", "BIMAS.IS", "BİM Birleşik Mağazalar", "Istanbul", "TRY"),
            new SymbolSearchResult("KCHOL", "KCHOL.IS", "Koç Holding", "Istanbul", "TRY"),
            new SymbolSearchResult("FROTO", "FROTO.IS", "Ford Otosan", "Istanbul", "TRY"),
            new SymbolSearchResult("SISE", "SISE.IS", "Şişecam", "Istanbul", "TRY"),
            new SymbolSearchResult("EREGL", "EREGL.IS", "Ereğli Demir Çelik", "Istanbul", "TRY"),
            new SymbolSearchResult("AAPL", "AAPL", "Apple Inc.", "NASDAQ", "USD"),
            new SymbolSearchResult("MSFT", "MSFT", "Microsoft Corporation", "NASDAQ", "USD"),
            new SymbolSearchResult("NVDA", "NVDA", "NVIDIA Corporation", "NASDAQ", "USD"));

    @Override
    public List<SymbolSearchResult> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.forLanguageTag("tr"));
        if (q.length() < 2) return List.of();
        return SYMBOLS.stream()
                .filter(s -> s.ticker().toLowerCase(Locale.ROOT).contains(q)
                        || s.name().toLowerCase(Locale.forLanguageTag("tr")).contains(q))
                .toList();
    }
}
