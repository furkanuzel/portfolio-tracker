package com.furkanuzel.portfolio.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/symbol-search?q=...: autocomplete for the trade form. */
@WebServlet("/api/symbol-search")
public class SymbolSearchServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, symbolSearch(request).search(request.getParameter("q")));
    }
}
