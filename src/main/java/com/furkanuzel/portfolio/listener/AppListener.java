package com.furkanuzel.portfolio.listener;

import com.furkanuzel.portfolio.config.AppConfig;
import com.furkanuzel.portfolio.repository.PortfolioRepository;
import com.furkanuzel.portfolio.service.DemoData;
import com.furkanuzel.portfolio.service.DemoPriceService;
import com.furkanuzel.portfolio.service.DemoSymbolSearch;
import com.furkanuzel.portfolio.service.PortfolioService;
import com.furkanuzel.portfolio.service.PriceProvider;
import com.furkanuzel.portfolio.service.SymbolSearch;
import com.furkanuzel.portfolio.service.YahooPriceService;
import com.furkanuzel.portfolio.service.YahooSymbolSearch;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Files;

/** Wires the services together once, when the web application starts. */
@WebListener
public class AppListener implements ServletContextListener {

    public static final String PORTFOLIO_SERVICE = "portfolioService";
    public static final String SYMBOL_SEARCH = "symbolSearch";
    public static final String DEMO_MODE = "demoMode";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            AppConfig config = AppConfig.fromEnvironment();
            Files.createDirectories(config.dataDirectory());
            PortfolioRepository repository = new PortfolioRepository(config.dataDirectory());

            PriceProvider prices;
            SymbolSearch search;
            if (config.demoMode()) {
                DemoData.seedIfEmpty(repository);
                prices = new DemoPriceService();
                search = new DemoSymbolSearch();
            } else {
                prices = new YahooPriceService();
                search = new YahooSymbolSearch();
            }

            ServletContext context = event.getServletContext();
            context.setAttribute(PORTFOLIO_SERVICE, new PortfolioService(repository, prices, config.demoMode()));
            context.setAttribute(SYMBOL_SEARCH, search);
            context.setAttribute(DEMO_MODE, config.demoMode());
        } catch (Exception e) {
            throw new IllegalStateException("Uygulama başlatılamadı", e);
        }
    }
}
