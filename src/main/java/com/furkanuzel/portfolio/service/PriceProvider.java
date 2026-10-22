package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.Quote;

/** Source of market prices. Implementations must never throw; return {@link Quote#unavailable()}. */
public interface PriceProvider {

    /** Yahoo Finance symbol for the US dollar / Turkish lira rate. */
    String USD_TRY = "TRY=X";

    Quote getQuote(String symbol);
}
