package com.furkanuzel.portfolio.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/portfolio: positions with live prices and totals, as JSON. */
@WebServlet("/api/portfolio")
public class PortfolioApiServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, portfolio(request).getPortfolio());
    }
}
