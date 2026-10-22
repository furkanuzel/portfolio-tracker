<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.furkanuzel.portfolio.listener.AppListener" %>
<%
    boolean demo = Boolean.TRUE.equals(application.getAttribute(AppListener.DEMO_MODE));
    String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Portföy Takip</title>
    <link rel="icon" href="<%= ctx %>/assets/portfoy-logo.png">
    <link rel="stylesheet" href="<%= ctx %>/assets/app.css">
    <link rel="stylesheet" href="<%= ctx %>/assets/autocomplete.css">
    <link rel="stylesheet" href="<%= ctx %>/assets/analytics.css">
    <link rel="stylesheet" href="<%= ctx %>/assets/privacy.css">
</head>
<body data-context="<%= ctx %>">
<main class="shell">
    <% if (demo) { %>
    <div class="demo-banner" role="note">
        <strong>Demo modu</strong>
        <span>Örnek pozisyonlar ve simüle edilmiş fiyatlar. Gerçek verilerin bu moddan etkilenmez.</span>
    </div>
    <% } %>

    <header class="topbar">
        <div>
            <p class="eyebrow">PORTFÖY TAKİP</p>
            <h1>Varlıklarının net görünümü</h1>
        </div>
        <div class="header-actions">
            <button id="privacyToggle" class="privacy-toggle" type="button" aria-pressed="false"
                    aria-label="Tutarları gizle" title="Tutarları gizle">
                <svg class="eye-open" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/>
                    <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg class="eye-closed" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m3 3 18 18M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 5.2A11 11 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.1 3M6.6 6.6C3.6 8.4 2 12 2 12s3.5 7 10 7a10 10 0 0 0 4.1-.9"/>
                </svg>
            </button>
            <div class="market">
                <span></span>
                <strong id="marketStatus">Veri bekleniyor</strong>
                <small id="updatedAt">—</small>
            </div>
        </div>
    </header>

    <div id="warnings" class="warnings" hidden></div>

    <section class="kpis analytics-kpis">
        <article><span>Güncel değer</span><strong id="totalValue">—</strong></article>
        <article><span>Toplam maliyet</span><strong id="totalCost">—</strong></article>
        <article>
            <span>Haftalık kazanç <small>(maliyete göre)</small></span>
            <strong id="weeklyProfit">—</strong><small id="weeklyPercent">—</small>
        </article>
        <article>
            <span>Kâr / zarar</span>
            <strong id="totalProfit">—</strong><small id="totalPercent">—</small>
        </article>
        <article class="target-kpi">
            <span>Portföy hedefi</span>
            <div><strong id="portfolioTarget">Belirlenmedi</strong><small id="targetPercent">—</small></div>
            <div class="progress"><i id="targetProgress"></i></div>
        </article>
    </section>

    <section class="workspace">
        <article class="panel table-panel">
            <div class="panel-title">
                <div>
                    <p class="eyebrow">POZİSYONLAR</p>
                    <h2>Portföy</h2>
                </div>
                <button id="refresh" type="button">Şimdi yenile</button>
            </div>
            <div class="table-scroll">
                <table>
                    <thead>
                    <tr>
                        <th>Varlık</th><th>Lot</th><th>Maliyet</th><th>Toplam maliyet</th><th>Fiyat</th>
                        <th>Günlük getiri</th><th>Kâr/Zarar</th><th>Hedef fiyat</th><th>Değer</th><th>İşlem</th>
                    </tr>
                    </thead>
                    <tbody id="positions">
                    <tr><td colspan="10" class="loading">Fiyatlar alınıyor…</td></tr>
                    </tbody>
                </table>
            </div>
            <p class="delay-note">
                Günlük getiri, hissenin önceki kapanış fiyatına göre hesaplanır.
                BIST fiyatları yaklaşık 15 dakika gecikmeli olabilir.
            </p>
        </article>

        <aside class="panel trade-panel">
            <p class="eyebrow">YENİ İŞLEM</p>
            <h2>Alış veya satış ekle</h2>
            <form method="post" action="<%= ctx %>/trade">
                <div class="stock-picker">
                    <label for="stockSearch">Hisse ara
                        <input id="stockSearch" type="search" placeholder="Kod veya şirket adı yazın"
                               autocomplete="off" aria-autocomplete="list" aria-controls="stockResults"
                               aria-expanded="false" required>
                    </label>
                    <div id="stockResults" class="stock-results" role="listbox" hidden></div>
                </div>
                <input id="ticker" name="ticker" type="hidden">
                <input id="symbol" name="symbol" type="hidden">
                <div id="stockSelection" class="stock-selection" hidden></div>
                <div class="form-row">
                    <label>İşlem
                        <select name="type">
                            <option value="BUY">Alış</option>
                            <option value="SELL">Satış</option>
                        </select>
                    </label>
                    <label>Para
                        <select id="currency" name="currency">
                            <option>TRY</option>
                            <option>USD</option>
                        </select>
                    </label>
                </div>
                <div class="form-row">
                    <label>Adet <input name="quantity" type="number" min="0.0001" step="0.0001" required></label>
                    <label>Birim fiyat <input name="price" type="number" min="0.0001" step="0.0001" required></label>
                </div>
                <button class="primary" type="submit">İşlemi kaydet</button>
            </form>
            <div id="message" class="message" hidden></div>

            <div class="portfolio-target-form">
                <p class="eyebrow">HEDEF</p>
                <h2>Toplam portföy hedefi</h2>
                <form id="portfolioTargetForm">
                    <label>Hedef tutar (TL)
                        <input id="portfolioTargetInput" type="number" min="0" step="1000" placeholder="Örn. 250000">
                    </label>
                    <button type="submit">Hedefi kaydet</button>
                </form>
            </div>
        </aside>
    </section>

    <section class="panel allocation-panel">
        <div>
            <p class="eyebrow">DAĞILIM</p>
            <h2>Portföy ağırlıkları</h2>
        </div>
        <div class="chart-wrap">
            <canvas id="allocationChart" width="360" height="360" aria-label="Portföy dağılım grafiği"></canvas>
            <div id="allocationLegend" class="chart-legend"></div>
        </div>
    </section>
</main>
<script src="<%= ctx %>/assets/app.js"></script>
</body>
</html>
