package com.furkanuzel.portfolio.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** POST /trade: the buy/sell form. Redirects back to the dashboard with a status message. */
@WebServlet("/trade")
public class TradeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String home = request.getContextPath() + "/";
        if (!isSameOrigin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            portfolio(request).trade(
                    request.getParameter("ticker"),
                    request.getParameter("symbol"),
                    request.getParameter("currency"),
                    request.getParameter("type"),
                    request.getParameter("quantity"),
                    request.getParameter("price"));
            response.sendRedirect(home + "?saved=1");
        } catch (IllegalArgumentException e) {
            response.sendRedirect(home + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            response.sendRedirect(home + "?error=" + URLEncoder.encode("İşlem kaydedilemedi", StandardCharsets.UTF_8));
        }
    }
}
