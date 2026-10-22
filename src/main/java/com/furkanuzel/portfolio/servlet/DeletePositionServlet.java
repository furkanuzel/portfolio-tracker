package com.furkanuzel.portfolio.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** POST /api/position/delete: removes a fully sold (zero-lot) position. */
@WebServlet("/api/position/delete")
public class DeletePositionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!isSameOrigin(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, Map.of("success", false));
            return;
        }
        try {
            portfolio(request).deleteClosedPosition(request.getParameter("ticker"));
            writeJson(response, HttpServletResponse.SC_OK, Map.of("success", true));
        } catch (RuntimeException e) {
            writeError(response, e);
        }
    }
}
