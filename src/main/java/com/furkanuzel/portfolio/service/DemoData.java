package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.Position;
import com.furkanuzel.portfolio.repository.PortfolioRepository;

/** Fictional positions loaded into an empty demo database. */
public final class DemoData {

    private DemoData() {
    }

    public static void seedIfEmpty(PortfolioRepository repository) {
        if (!repository.isEmpty()) return;
        repository.insert(new Position("THYAO", "THYAO.IS", "TRY", 120, 268.40, 360));
        repository.insert(new Position("ASELS", "ASELS.IS", "TRY", 300, 121.75, 175));
        repository.insert(new Position("BIMAS", "BIMAS.IS", "TRY", 40, 548.00, 0));
        repository.insert(new Position("FROTO", "FROTO.IS", "TRY", 15, 905.50, 1150));
        repository.insert(new Position("SISE", "SISE.IS", "TRY", 650, 44.10, 0));
        repository.insert(new Position("AAPL", "AAPL", "USD", 12, 196.30, 250));
        repository.insert(new Position("MSFT", "MSFT", "USD", 6, 402.80, 0));
        repository.savePortfolioTarget(500_000);
    }
}
