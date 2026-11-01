"use strict";
// ---- Latest release -------------------------------------------------------------------------
(async function release() {
  const status = document.getElementById("rel-status");
  const box = document.getElementById("rel-assets");
  const repo = window.YMUSIC_REPO;
  if (!repo || repo === "OWNER/REPO") { status.textContent = "Isi YMUSIC_REPO di web/config.js dengan repo fork Anda."; return; }
  try {
    const res = await fetch("https://api.github.com/repos/" + repo + "/releases/latest", { headers: { Accept: "application/vnd.github+json" } });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const rel = await res.json();
    status.textContent = "Versi terbaru: " + (rel.name || rel.tag_name);
    (rel.assets || []).filter(a => a.name.endsWith(".apk")).forEach(a => {
      const link = document.createElement("a");
      link.href = a.browser_download_url;
      link.textContent = a.name.replace(/^YMusic-/, "").replace("-release.apk", "") + " (" + (a.size / 1048576).toFixed(0) + " MB)";
      link.rel = "noopener";
      box.appendChild(link);
    });
    if (!box.children.length) status.textContent += " (belum ada file APK)";
  } catch (e) {
    status.textContent = "Belum ada rilis, atau GitHub tidak dapat dihubungi.";
  }
})();

// ---- Theme studio (same code format as the app: ymtheme1|mode|accent|surface|fontPct|cornerPct) --
(function studio() {
  const $ = id => document.getElementById(id);
  const phone = $("phone");
  const MAX_LUM = 0.3;
  const lin = c => { c /= 255; return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); };
  const lum = ([r, g, b]) => 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
  const rgb = hex => [0, 2, 4].map(i => parseInt(hex.slice(i, i + 2), 16));
  const toHex = a => a.map(v => Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, "0")).join("").toUpperCase();
  const mix = (a, b, t) => a.map((v, i) => v + (b[i] - v) * t);
  function darkenAccent(hex) {
    let c = rgb(hex);
    if (lum(c) <= MAX_LUM) return hex.toUpperCase();
    let lo = 0, hi = 1;
    for (let i = 0; i < 16; i++) { const m = (lo + hi) / 2; (lum(c.map(v => v * m)) <= MAX_LUM ? (lo = m) : (hi = m)); }
    return toHex(c.map(v => v * lo));
  }
  function ground(hex, dark) {
    const c = rgb(hex), l = lum(c);
    if (dark) return l <= 0.12 ? c : mix([0, 0, 0], c, 0.12 / l);
    return l >= 0.6 ? c : mix(c, [255, 255, 255], 0.6);
  }
  const css = a => "rgb(" + a.map(Math.round).join(",") + ")";
  const PRESETS = [
    ["Midnight", "dark", "5C6BC0", "151A2E"], ["Ocean", "dark", "0277BD", "0E1E2B"],
    ["Forest", "dark", "2E7D32", "122018"], ["Rose", "dark", "C2185B", "26141C"],
    ["Sunset", "amoled", "E8732A", ""], ["Paper", "light", "8D6E63", "FAF6EF"],
  ];
  let surfaceSet = false;

  function state() {
    return {
      mode: $("mode").value,
      accent: toHex(rgb($("accent").value.slice(1))) === "E8732A" ? "" : $("accent").value.slice(1).toUpperCase(),
      surface: surfaceSet ? $("surface").value.slice(1).toUpperCase() : "",
      font: +$("font").value, corner: +$("corner").value,
    };
  }
  function render() {
    const s = state();
    const dark = s.mode === "" ? matchMedia("(prefers-color-scheme: dark)").matches : s.mode !== "light";
    let bg = s.mode === "amoled" ? [0, 0, 0] : dark ? [0x27, 0x27, 0x27] : [255, 255, 255];
    if (surfaceSet) bg = ground($("surface").value.slice(1), dark);
    const card = mix(bg, dark ? [255, 255, 255] : [0, 0, 0], 0.06);
    const acc = darkenAccent(($("accent").value || "#E8732A").slice(1));
    phone.style.setProperty("--p-bg", css(bg));
    phone.style.setProperty("--p-card", css(card));
    phone.style.setProperty("--p-ink", dark ? "#e2e2e2" : "#333");
    phone.style.setProperty("--p-acc", "#" + acc);
    phone.style.setProperty("--p-r", (12 * s.corner / 100) + "px");
    phone.style.setProperty("--p-fs", s.font / 100);
    $("font-out").textContent = s.font + "%";
    $("corner-out").textContent = s.corner + "%";
    $("code").value = ["ymtheme1", s.mode, s.accent, s.surface, s.font, s.corner].join("|");
  }
  function load(code) {
    const p = code.trim().split("|");
    if (p.length !== 6 || p[0] !== "ymtheme1" || !["", "light", "dark", "amoled"].includes(p[1])) return;
    const hexOk = h => h === "" || /^[0-9A-Fa-f]{6}$/.test(h);
    if (!hexOk(p[2]) || !hexOk(p[3]) || isNaN(+p[4]) || isNaN(+p[5])) return;
    $("mode").value = p[1];
    $("accent").value = "#" + (p[2] || "E8732A");
    surfaceSet = p[3] !== "";
    if (surfaceSet) $("surface").value = "#" + p[3];
    $("font").value = Math.max(85, Math.min(130, +p[4]));
    $("corner").value = Math.max(0, Math.min(200, +p[5]));
    render();
  }
  PRESETS.forEach(([name, mode, accent, surface]) => {
    const b = document.createElement("button");
    b.type = "button";
    const dot = document.createElement("b");
    dot.style.background = "#" + darkenAccent(accent);
    b.append(dot, name);
    b.onclick = () => load(["ymtheme1", mode, accent, surface, $("font").value, $("corner").value].join("|"));
    $("presets").appendChild(b);
  });
  ["mode", "accent", "font", "corner"].forEach(id => $(id).addEventListener("input", render));
  $("surface").addEventListener("input", () => { surfaceSet = true; render(); });
  $("surface-reset").onclick = () => { surfaceSet = false; render(); };
  $("code").addEventListener("input", e => load(e.target.value));
  $("copy").onclick = async () => { try { await navigator.clipboard.writeText($("code").value); $("copy").textContent = "Tersalin"; } catch (e) { $("code").select(); } };
  render();
})();
