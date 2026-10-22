package com.furkanuzel.portfolio.service;

import com.furkanuzel.portfolio.model.SymbolSearchResult;

import java.util.List;

public interface SymbolSearch {
    List<SymbolSearchResult> search(String query);
}
