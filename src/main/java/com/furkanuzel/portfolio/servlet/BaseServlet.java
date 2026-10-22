package com.furkanuzel.portfolio.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.furkanuzel.portfolio.listener.AppListener;
import com.furkanuzel.portfolio.service.PortfolioService;
import com.furkanuzel.portfolio.service.SymbolSearch;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/** Shared helpers: service lookup, JSON output and a same-origin check for POSTs. */
abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected PortfolioService portfolio(HttpServletRequest request) {
        return (PortfolioService) request.getServletContext().getAttribute(AppListener.PORTFOLIO_SERVICE);
    }

    protected SymbolSearch symbolSearch(HttpServletRequest request) {
        return (SymbolSearch) request.getServletContext().getAttribute(AppListener.SYMBOL_SEARCH);
    }

    protected void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSON.writeValue(response.getWriter(), body);
    }

    protected void writeError(HttpServletResponse response, Exception e) throws IOException {
        String message = e instanceof IllegalArgumentException && e.getMessage() != null
                ? e.getMessage()
                : "İşlem tamamlanamadı";
        writeJson(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("success", false, "message", message));
    }

    /** Rejects cross-site form posts: a browser always sends Origin on POST from another site. */
    protected boolean isSameOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank() || "null".equals(origin)) return true;
        try {
            URI uri = URI.create(origin);
            int port = uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
            return uri.getHost() != null
                    && uri.getHost().equalsIgnoreCase(request.getServerName())
                    && port == request.getServerPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
