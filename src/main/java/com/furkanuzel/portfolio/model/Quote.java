package com.furkanuzel.portfolio.model;

import java.time.Instant;

/** Latest price plus the reference closes used for daily and weekly change. */
public record Quote(
        double price,
        double previousClose,
        double weeklyClose,
        Instant fetchedAt,
        Instant marketTime,
        boolean available) {

    public static Quote unavailable() {
        Instant now = Instant.now();
        return new Quote(0, 0, 0, now, now, false);
    }
}
