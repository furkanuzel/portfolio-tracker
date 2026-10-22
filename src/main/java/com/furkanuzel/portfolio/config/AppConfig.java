package com.furkanuzel.portfolio.config;

import java.nio.file.Path;

/**
 * Runtime settings, read from JVM system properties or environment variables.
 *
 * <ul>
 *   <li>{@code portfoy.demo} / {@code PORTFOY_DEMO} = true: offline demo with sample
 *       positions and simulated prices. Uses its own data folder, so real data is never
 *       touched.
 *   <li>{@code portfoy.dataDir} / {@code PORTFOY_DATA_DIR}: where the H2 database lives.
 *       Defaults to {@code ~/.portfoy-takip} (or {@code ~/.portfoy-takip-demo} in demo mode).
 * </ul>
 */
public record AppConfig(boolean demoMode, Path dataDirectory) {

    public static AppConfig fromEnvironment() {
        boolean demo = Boolean.parseBoolean(setting("portfoy.demo", "PORTFOY_DEMO", "false"));
        String defaultFolder = demo ? ".portfoy-takip-demo" : ".portfoy-takip";
        String dataDir = setting("portfoy.dataDir", "PORTFOY_DATA_DIR",
                Path.of(System.getProperty("user.home"), defaultFolder).toString());
        return new AppConfig(demo, Path.of(dataDir));
    }

    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(env);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
