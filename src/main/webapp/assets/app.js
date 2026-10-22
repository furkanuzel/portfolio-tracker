const root = document.body.dataset.context;
let privacyMode = readPrivacy();

function readPrivacy() {
  try {
    return localStorage.getItem("portfolioPrivacy") === "hidden";
  } catch {
    return false;
  }
}
const hiddenValue = "••••••";
const tlFormatter = new Intl.NumberFormat("tr-TR", {
  style: "currency",
  currency: "TRY",
  maximumFractionDigits: 2,
});
const numFormatter = new Intl.NumberFormat("tr-TR", { maximumFractionDigits: 4 });
const tl = { format: (value) => (privacyMode ? hiddenValue : tlFormatter.format(value)) };
const num = { format: (value) => (privacyMode ? hiddenValue : numFormatter.format(value)) };
const signed = (v) => (privacyMode ? hiddenValue : `${v >= 0 ? "+" : ""}${v.toFixed(2)}%`);
const tone = (v) => (v >= 0 ? "positive" : "negative");
const marketClock = (value) =>
  new Date(value).toLocaleTimeString("tr-TR", { hour: "2-digit", minute: "2-digit" });
const escapeHtml = (value) =>
  String(value).replace(
    /[&<>"']/g,
    (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char],
  );

function showTemporaryMessage(text, type = "success") {
  const message = document.querySelector("#message");
  message.hidden = false;
  message.textContent = text;
  message.className = `message ${type}`;
  setTimeout(() => {
    message.classList.add("message-leaving");
    setTimeout(() => {
      message.hidden = true;
      message.className = "message";
    }, 350);
  }, 4000);
}

function drawAllocationChart(positions) {
  const items = positions.filter((p) => p.valueTry > 0),
    canvas = document.querySelector("#allocationChart"),
    legend = document.querySelector("#allocationLegend"),
    ctx = canvas.getContext("2d");
  const colors = [
    "#3b82f6",
    "#22c55e",
    "#f59e0b",
    "#a855f7",
    "#ef4444",
    "#06b6d4",
    "#f97316",
    "#84cc16",
  ];
  const total = items.reduce((sum, p) => sum + p.valueTry, 0),
    size = 360,
    ratio = window.devicePixelRatio || 1;
  canvas.width = size * ratio;
  canvas.height = size * ratio;
  canvas.style.width = `${size}px`;
  canvas.style.height = `${size}px`;
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  ctx.clearRect(0, 0, size, size);
  if (!total) {
    ctx.fillStyle = "#18304a";
    ctx.beginPath();
    ctx.arc(180, 180, 130, 0, Math.PI * 2);
    ctx.fill();
    legend.innerHTML = '<span class="muted">Dağılım için pozisyon bulunmuyor.</span>';
    return;
  }
  let start = -Math.PI / 2;
  items.forEach((item, index) => {
    const angle = (item.valueTry / total) * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(180, 180);
    ctx.arc(180, 180, 130, start, start + angle);
    ctx.closePath();
    ctx.fillStyle = colors[index % colors.length];
    ctx.fill();
    start += angle;
  });
  ctx.beginPath();
  ctx.arc(180, 180, 72, 0, Math.PI * 2);
  ctx.fillStyle = "#0f1f33";
  ctx.fill();
  ctx.fillStyle = "#8fa1b7";
  ctx.font = "14px Segoe UI";
  ctx.textAlign = "center";
  ctx.fillText("Toplam değer", 180, 172);
  ctx.fillStyle = "#fff";
  ctx.font = "bold 18px Segoe UI";
  ctx.fillText(tl.format(total), 180, 199);
  legend.innerHTML = items
    .map(
      (item, index) =>
        `<div><i style="background:${colors[index % colors.length]}"></i><span><strong>${escapeHtml(item.ticker)}</strong><small>${privacyMode ? hiddenValue : `${((item.valueTry / total) * 100).toFixed(1)}%`} · ${tl.format(item.valueTry)}</small></span></div>`,
    )
    .join("");
}

function renderWarnings(warnings) {
  const box = document.querySelector("#warnings");
  box.hidden = warnings.length === 0;
  box.innerHTML = warnings.map((w) => `<div>${escapeHtml(w)}</div>`).join("");
}

function renderPositionRow(p) {
  const price = p.priceAvailable
    ? `<strong>${num.format(p.currentPrice)}</strong><small>${marketClock(p.updatedAt)} verisi</small>`
    : '<span class="muted">Alınamadı</span>';
  const targetValue = privacyMode ? "" : p.targetPrice || "";
  const targetPlaceholder = privacyMode ? hiddenValue : "Hedef";
  const upside =
    p.targetPrice > 0
      ? `<small class="${tone(p.targetUpsidePercent)}">${signed(p.targetUpsidePercent)} potansiyel</small>`
      : "";
  const action =
    Math.abs(p.quantity) < 0.000001
      ? `<button type="button" class="remove-position" data-ticker="${escapeHtml(p.ticker)}">Kaldır</button>`
      : '<span class="muted">—</span>';

  return `<tr>
    <td><strong>${escapeHtml(p.ticker)}</strong><small>${escapeHtml(p.currency)}</small></td>
    <td>${num.format(p.quantity)}</td>
    <td>${num.format(p.averageCost)}</td>
    <td><strong>${tl.format(p.costTry)}</strong></td>
    <td>${price}</td>
    <td class="${tone(p.dailyPercent)}"><strong>${signed(p.dailyPercent)}</strong><small>${tl.format(p.dailyProfitTry)}</small></td>
    <td class="${tone(p.profitLossTry)}"><strong>${tl.format(p.profitLossTry)}</strong><small>${signed(p.profitLossPercent)}</small></td>
    <td>
      <div class="target-editor">
        <input data-target-input="${escapeHtml(p.ticker)}" type="number" min="0" step="0.0001" value="${targetValue}" placeholder="${targetPlaceholder}">
        <button type="button" data-save-target="${escapeHtml(p.ticker)}">Kaydet</button>
      </div>${upside}
    </td>
    <td><strong>${tl.format(p.valueTry)}</strong></td>
    <td>${action}</td>
  </tr>`;
}

async function loadPortfolio() {
  const button = document.querySelector("#refresh");
  button.disabled = true;
  try {
    const response = await fetch(`${root}/api/portfolio`, { cache: "no-store" });
    if (!response.ok) throw new Error("Portföy yüklenemedi");
    const data = await response.json();
    document.querySelector("#totalValue").textContent = tl.format(data.totalValueTry);
    document.querySelector("#totalCost").textContent = tl.format(data.totalCostTry);
    const profit = document.querySelector("#totalProfit");
    profit.textContent = tl.format(data.totalProfitLossTry);
    profit.className = tone(data.totalProfitLossTry);
    const percent = document.querySelector("#totalPercent");
    percent.textContent = signed(data.totalProfitLossPercent);
    percent.className = tone(data.totalProfitLossPercent);
    const weekly = document.querySelector("#weeklyProfit"),
      weeklyRate = data.totalCostTry > 0 ? (data.weeklyProfitTry / data.totalCostTry) * 100 : 0;
    weekly.textContent = tl.format(data.weeklyProfitTry);
    weekly.className = tone(data.weeklyProfitTry);
    const weeklyPercent = document.querySelector("#weeklyPercent");
    weeklyPercent.textContent = signed(weeklyRate);
    weeklyPercent.className = tone(weeklyRate);
    document.querySelector("#portfolioTarget").textContent =
      data.portfolioTargetTry > 0 ? tl.format(data.portfolioTargetTry) : "Belirlenmedi";
    document.querySelector("#targetPercent").textContent =
      data.portfolioTargetTry > 0
        ? privacyMode
          ? hiddenValue
          : `%${data.targetProgressPercent.toFixed(1)}`
        : "—";
    document.querySelector("#targetProgress").style.width = privacyMode
      ? "0%"
      : `${Math.min(data.targetProgressPercent, 100)}%`;
    document.querySelector("#updatedAt").textContent =
      `Son fiyat zamanı: ${marketClock(data.updatedAt)}`;
    document.querySelector("#marketStatus").textContent = data.demo
      ? "Demo verisi · simüle edilmiş fiyatlar"
      : "Yaklaşık 15 dk gecikmeli · 5 dk yenileme";
    renderWarnings(data.warnings || []);
    document.querySelector("#positions").innerHTML = data.positions.length
      ? data.positions.map(renderPositionRow).join("")
      : '<tr><td colspan="10" class="loading">Henüz pozisyon bulunmuyor.</td></tr>';
    drawAllocationChart(data.positions);
  } catch (error) {
    document.querySelector("#marketStatus").textContent = "Bağlantı hatası";
    document.querySelector("#positions").innerHTML =
      '<tr><td colspan="10" class="loading">Portföy yüklenemedi.</td></tr>';
  } finally {
    button.disabled = false;
  }
}

async function saveTarget(ticker, value) {
  const body = new URLSearchParams({ value });
  if (ticker) body.set("ticker", ticker);
  const response = await fetch(`${root}/api/targets`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body,
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || "Hedef kaydedilemedi");
}

const privacyToggle = document.querySelector("#privacyToggle");
function syncPrivacyToggle() {
  privacyToggle.setAttribute("aria-pressed", String(privacyMode));
  privacyToggle.setAttribute("aria-label", privacyMode ? "Tutarları göster" : "Tutarları gizle");
  privacyToggle.title = privacyMode ? "Tutarları göster" : "Tutarları gizle";
}
privacyToggle.addEventListener("click", () => {
  privacyMode = !privacyMode;
  try {
    localStorage.setItem("portfolioPrivacy", privacyMode ? "hidden" : "visible");
  } catch {
    /* storage unavailable: keep the setting for this session only */
  }
  syncPrivacyToggle();
  loadPortfolio();
});
syncPrivacyToggle();
document.querySelector("#refresh").addEventListener("click", loadPortfolio);
document.querySelector("#portfolioTargetForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button"),
    input = document.querySelector("#portfolioTargetInput"),
    value = input.value;
  button.disabled = true;
  try {
    await saveTarget("", value);
    showTemporaryMessage("Portföy hedefi kaydedildi.");
    await loadPortfolio();
    input.value = "";
  } catch (error) {
    showTemporaryMessage(error.message, "error");
  } finally {
    button.disabled = false;
  }
});
document.querySelector("#positions").addEventListener("click", async (event) => {
  const targetButton = event.target.closest("[data-save-target]");
  if (targetButton) {
    const ticker = targetButton.dataset.saveTarget,
      input = document.querySelector(`[data-target-input="${CSS.escape(ticker)}"]`);
    targetButton.disabled = true;
    try {
      await saveTarget(ticker, input.value || 0);
      showTemporaryMessage(`${ticker} hedef fiyatı kaydedildi.`);
      await loadPortfolio();
    } catch (error) {
      showTemporaryMessage(error.message, "error");
    } finally {
      targetButton.disabled = false;
    }
    return;
  }
  const button = event.target.closest(".remove-position");
  if (!button) return;
  const ticker = button.dataset.ticker;
  if (!confirm(`${ticker} portföy listesinden kaldırılsın mı?`)) return;
  button.disabled = true;
  try {
    const body = new URLSearchParams({ ticker });
    const response = await fetch(`${root}/api/position/delete`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body,
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || "Hisse kaldırılamadı");
    await loadPortfolio();
  } catch (error) {
    alert(error.message);
    button.disabled = false;
  }
});

const params = new URLSearchParams(location.search);
if (params.has("saved")) showTemporaryMessage("İşlem kaydedildi.");
if (params.has("error")) showTemporaryMessage(params.get("error"), "error");
if (params.has("saved") || params.has("error")) {
  const cleanUrl = new URL(location.href);
  cleanUrl.searchParams.delete("saved");
  cleanUrl.searchParams.delete("error");
  history.replaceState({}, "", cleanUrl.pathname + cleanUrl.search + cleanUrl.hash);
}

const stockSearch = document.querySelector("#stockSearch"),
  stockResults = document.querySelector("#stockResults"),
  tickerInput = document.querySelector("#ticker"),
  symbolInput = document.querySelector("#symbol"),
  currencyInput = document.querySelector("#currency"),
  stockSelection = document.querySelector("#stockSelection");
let searchTimer,
  searchRequest,
  activeResult = -1,
  currentResults = [];
function closeStockResults() {
  stockResults.hidden = true;
  stockSearch.setAttribute("aria-expanded", "false");
  activeResult = -1;
}
function setActiveResult(index) {
  const options = [...stockResults.querySelectorAll('[role="option"]')];
  if (!options.length) return;
  activeResult = (index + options.length) % options.length;
  options.forEach((option, i) => option.classList.toggle("active", i === activeResult));
  options[activeResult].scrollIntoView({ block: "nearest" });
}
function chooseStock(item) {
  tickerInput.value = item.ticker;
  symbolInput.value = item.symbol;
  currencyInput.value = item.currency === "TRY" ? "TRY" : "USD";
  stockSearch.value = `${item.ticker} — ${item.name}`;
  stockSearch.setCustomValidity("");
  stockSelection.innerHTML = `<strong>${escapeHtml(item.ticker)}</strong><span>${escapeHtml(item.name)}</span><small>${escapeHtml(item.symbol)} · ${escapeHtml(item.exchange || item.currency)}</small>`;
  stockSelection.hidden = false;
  closeStockResults();
}
function renderStockResults(items) {
  currentResults = items;
  activeResult = -1;
  if (!items.length) {
    stockResults.innerHTML = '<div class="stock-empty">Sonuç bulunamadı.</div>';
    stockResults.hidden = false;
    return;
  }
  stockResults.innerHTML = items
    .map(
      (item, index) =>
        `<button type="button" role="option" data-index="${index}"><strong>${escapeHtml(item.ticker)}</strong><span>${escapeHtml(item.name)}</span><small>${escapeHtml(item.exchange || "")} · ${escapeHtml(item.symbol)}</small></button>`,
    )
    .join("");
  stockResults.hidden = false;
  stockSearch.setAttribute("aria-expanded", "true");
}
async function searchStocks() {
  const query = stockSearch.value.trim();
  if (query.length < 2) {
    closeStockResults();
    return;
  }
  if (searchRequest) searchRequest.abort();
  searchRequest = new AbortController();
  stockResults.innerHTML = '<div class="stock-empty">Hisseler aranıyor…</div>';
  stockResults.hidden = false;
  try {
    const response = await fetch(`${root}/api/symbol-search?q=${encodeURIComponent(query)}`, {
      signal: searchRequest.signal,
      cache: "no-store",
    });
    if (!response.ok) throw new Error();
    renderStockResults(await response.json());
  } catch (error) {
    if (error.name !== "AbortError")
      stockResults.innerHTML = '<div class="stock-empty">Arama şu anda kullanılamıyor.</div>';
  }
}
stockSearch.addEventListener("input", () => {
  tickerInput.value = "";
  symbolInput.value = "";
  stockSelection.hidden = true;
  stockSearch.setCustomValidity("");
  clearTimeout(searchTimer);
  searchTimer = setTimeout(searchStocks, 300);
});
stockSearch.addEventListener("keydown", (event) => {
  if (stockResults.hidden) return;
  if (event.key === "ArrowDown") {
    event.preventDefault();
    setActiveResult(activeResult + 1);
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    setActiveResult(activeResult - 1);
  } else if (event.key === "Enter" && activeResult >= 0) {
    event.preventDefault();
    chooseStock(currentResults[activeResult]);
  } else if (event.key === "Escape") closeStockResults();
});
stockResults.addEventListener("click", (event) => {
  const option = event.target.closest("[data-index]");
  if (option) chooseStock(currentResults[Number(option.dataset.index)]);
});
document.addEventListener("click", (event) => {
  if (!event.target.closest(".stock-picker")) closeStockResults();
});
document.querySelector(".trade-panel form").addEventListener("submit", (event) => {
  if (!tickerInput.value || !symbolInput.value) {
    event.preventDefault();
    stockSearch.setCustomValidity("Listeden bir hisse seçmelisiniz.");
    stockSearch.reportValidity();
  }
});

loadPortfolio();
setInterval(loadPortfolio, 300000);
