package com.furkanuzel.portfolio.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/targets: with a {@code ticker}, sets that stock's target price;
 * without one, sets the target value for the whole portfolio.
 */
@WebServlet("/api/targets")
public class TargetsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isSameOrigin(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, Map.of("success", false));
            return;
        }
        try {
            String ticker = request.getParameter("ticker");
            String value = request.getParameter("value");
            if (ticker == null || ticker.isBlank()) portfolio(request).savePortfolioTarget(value);
            else portfolio(request).updateTargetPrice(ticker, value);
            writeJson(response, HttpServletResponse.SC_OK, Map.of("success", true));
        } catch (RuntimeException e) {
            writeError(response, e);
        }
    }
}
