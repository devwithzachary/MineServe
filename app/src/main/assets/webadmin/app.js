(() => {
  "use strict";

  const state = {
    servers: [],
    selectedServerId: null,
    activeTab: "console",
    currentPath: "",
    editingFile: null,
    cmdHistory: [],
    cmdHistoryIdx: -1,
    pollTimer: null,
    softwareTypes: [],
    perfHistory: {}
  };

  const $ = (id) => document.getElementById(id);

  function escapeHtml(str) {
    return String(str ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function showToast(msg, type = "success") {
    const container = $("toast-container");
    const el = document.createElement("div");
    el.className = `toast ${type}`;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3400);
  }

  async function apiFetch(url, options = {}) {
    const res = await fetch(url, {
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {})
      },
      ...options
    });
    const text = await res.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; } catch (_) {}
    if (res.status === 401 && url !== "/api/login") {
      showLogin();
      throw new Error(data.error || "Session expired");
    }
    if (!res.ok) {
      throw new Error(data.error || `Request failed (${res.status})`);
    }
    return data;
  }

  function showLogin() {
    if (state.pollTimer) {
      clearInterval(state.pollTimer);
      state.pollTimer = null;
    }
    $("app-view").classList.add("hidden");
    $("login-view").classList.remove("hidden");
  }

  function showApp() {
    $("login-view").classList.add("hidden");
    $("app-view").classList.remove("hidden");
    loadSystemInfo();
    loadServers();
    if (!state.pollTimer) {
      state.pollTimer = setInterval(pollActiveData, 2500);
    }
  }

  async function checkAuth() {
    try {
      await apiFetch("/api/me");
      showApp();
    } catch (_) {
      showLogin();
    }
  }

  async function loadSystemInfo() {
    try {
      const info = await apiFetch("/api/system");
      $("system-info").textContent = `${info.device} (Android ${info.android})`;
    } catch (_) {
      $("system-info").textContent = "Host connected";
    }
  }

  async function loadServers() {
    try {
      state.servers = await apiFetch("/api/servers");
      renderServerList();
      if (state.selectedServerId) {
        const exists = state.servers.some((s) => s.id === state.selectedServerId);
        if (!exists) {
          selectServer(null);
        } else {
          refreshSelectedServerHeader();
        }
      } else if (state.servers.length > 0) {
        selectServer(state.servers[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  }

  function renderServerList() {
    const container = $("server-list");
    if (state.servers.length === 0) {
      container.innerHTML = `<div class="player-sub" style="padding: 8px;">No servers yet.</div>`;
      return;
    }
    container.innerHTML = state.servers.map((s) => `
      <div class="server-item ${s.id === state.selectedServerId ? "active" : ""}" data-id="${escapeHtml(s.id)}">
        <div style="min-width:0;">
          <div class="server-item-name">${escapeHtml(s.name)}</div>
          <div class="server-item-sub">${escapeHtml(s.type)} ${escapeHtml(s.version)} · :${s.port}</div>
        </div>
        <span class="dot ${escapeHtml(s.status)}"></span>
      </div>
    `).join("");

    container.querySelectorAll(".server-item").forEach((el) => {
      el.addEventListener("click", () => {
        selectServer(el.dataset.id);
        $("sidebar").classList.remove("open");
      });
    });
  }

  function refreshSelectedServerHeader() {
    const server = state.servers.find((s) => s.id === state.selectedServerId);
    if (!server) return;
    $("active-server-name").textContent = server.name;
    $("active-server-meta").textContent = `${server.type} ${server.version} · Port ${server.port} · ${server.allocatedRamMb} MB RAM`;
    const badge = $("active-server-badge");
    badge.textContent = server.status;
    badge.className = `status-badge ${server.status}`;

    const running = server.status === "RUNNING" || server.status === "STARTING";
    $("btn-start").disabled = running;
    $("btn-stop").disabled = server.status === "STOPPED";
    $("btn-restart").disabled = server.status === "STOPPED";
  }

  function selectServer(serverId) {
    state.selectedServerId = serverId;
    state.currentPath = "";
    state.editingFile = null;
    $("file-editor-pane").classList.add("hidden");
    $("files-layout")?.classList?.remove("editing");
    renderServerList();

    if (!serverId) {
      $("active-server-name").textContent = "Select a Server";
      $("active-server-meta").textContent = "";
      $("server-controls").classList.add("hidden");
      $("server-workspace").classList.add("hidden");
      $("empty-state").classList.remove("hidden");
      return;
    }

    $("empty-state").classList.add("hidden");
    $("server-controls").classList.remove("hidden");
    $("server-workspace").classList.remove("hidden");
    refreshSelectedServerHeader();
    loadServerDetails();
    loadActiveTabData();
  }

  async function loadServerDetails() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}`);
      const idx = state.servers.findIndex((s) => s.id === data.id);
      if (idx >= 0) state.servers[idx] = { ...state.servers[idx], ...data };
      refreshSelectedServerHeader();
      renderServerList();

      const m = data.metrics || {};
      const cpu = Number(m.cpuPercentage || 0);
      const ramUsed = Number(m.ramUsedMb || 0);
      const ramMax = Number(m.ramMaxMb || data.allocatedRamMb || 1);
      const tpsNum = typeof m.tps === "number" ? m.tps : 20.0;
      const msptNum = typeof m.mspt === "number" ? m.mspt : 0.0;

      $("metric-cpu").textContent = `${Math.round(cpu)}%`;
      $("metric-ram").textContent = `${ramUsed} / ${data.allocatedRamMb || 0} MB`;
      const tps = tpsNum.toFixed(1);
      if (data.status === "RUNNING") {
        recordPerfSample(data.id, cpu, ramUsed, ramMax, tpsNum, msptNum);
      }
      const mspt = typeof m.mspt === "number" ? m.mspt.toFixed(1) : "0.0";
      $("metric-tps").textContent = `${tps} / ${mspt} ms`;
      $("metric-players").textContent = `${m.onlinePlayerCount || 0}`;
    } catch (_) {}
  }

  function loadActiveTabData() {
    if (!state.selectedServerId) return;
    if (state.activeTab === "console") loadConsole();
    else if (state.activeTab === "performance") loadPerformance();
    else if (state.activeTab === "automation") loadAutomation();
    else if (state.activeTab === "files") loadFiles(state.currentPath);
    else if (state.activeTab === "world") loadWorld();
    else if (state.activeTab === "livemap") loadLiveMap();
    else if (state.activeTab === "settings") { loadSettings(); loadNetwork(); loadDiscordWebhook(); loadProxySettings(); }
    else if (state.activeTab === "players") loadPlayers();
    else if (state.activeTab === "backups") loadBackups();
    else if (state.activeTab === "plugins") loadPlugins();
  }

  async function pollActiveData() {
    if (!state.selectedServerId) {
      await loadServers();
      return;
    }
    await loadServerDetails();
    if (state.activeTab === "console") {
      await loadConsole();
    } else if (state.activeTab === "performance") {
      await loadPerformance();
    } else if (state.activeTab === "settings") {
      await loadNetwork();
    } else if (state.activeTab === "players") {
      await loadPlayers();
    }
  }

  // Console
  async function loadConsole() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/console`);
      const out = $("console-output");
      const wasAtBottom = out.scrollHeight - out.scrollTop - out.clientHeight < 40;
      out.textContent = (data.lines || []).join("\n");
      if (wasAtBottom) out.scrollTop = out.scrollHeight;
    } catch (_) {}
  }

  // Files
  function formatSize(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  async function loadFiles(path = "") {
    if (!state.selectedServerId) return;
    state.currentPath = path;
    $("files-current-path").textContent = "/" + path;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/files?path=${encodeURIComponent(path)}`);
      const list = $("files-list");
      const files = data.files || [];
      if (files.length === 0) {
        list.innerHTML = `<div class="player-sub" style="padding: 16px;">Empty directory</div>`;
        return;
      }
      list.innerHTML = files.map((f) => `
        <div class="file-row">
          <button class="file-name-btn" data-path="${escapeHtml(f.relativePath)}" data-dir="${f.isDirectory}" data-editable="${f.isEditable}">
            <span>${f.isDirectory ? "📁" : "📄"}</span>
            <span>${escapeHtml(f.name)}</span>
          </button>
          <div class="file-meta">
            <span>${f.isDirectory ? "Folder" : formatSize(f.sizeBytes)}</span>
            <button class="btn btn-ghost btn-sm file-del-btn" data-path="${escapeHtml(f.relativePath)}" title="Delete">🗑️</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".file-name-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
          const rel = btn.dataset.path;
          if (btn.dataset.dir === "true") {
            loadFiles(rel);
          } else if (btn.dataset.editable === "true") {
            openFileEditor(rel);
          } else {
            showToast("Binary or large file cannot be edited in browser", "error");
          }
        });
      });

      list.querySelectorAll(".file-del-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const rel = btn.dataset.path;
          if (!confirm(`Delete "${rel}"?`)) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/files/delete`, {
              method: "POST",
              body: JSON.stringify({ path: rel })
            });
            showToast("Deleted");
            if (state.editingFile === rel) {
              closeFileEditor();
            }
            loadFiles(state.currentPath);
          } catch (e) {
            showToast(e.message, "error");
          }
        });
      });
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  async function openFileEditor(relPath) {
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/files/read?path=${encodeURIComponent(relPath)}`);
      state.editingFile = relPath;
      $("editor-filename").textContent = relPath;
      $("editor-textarea").value = data.content || "";
      $("file-editor-pane").classList.remove("hidden");
      document.querySelector(".files-layout").classList.add("editing");
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  function closeFileEditor() {
    state.editingFile = null;
    $("file-editor-pane").classList.add("hidden");
    document.querySelector(".files-layout").classList.remove("editing");
  }

  // Settings & Properties
  async function loadSettings() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/settings`);
      const srv = data.server || {};
      const props = data.properties || {};

      $("set-name").value = srv.name || "";
      $("set-ram").value = srv.allocatedRamMb || 2048;
      $("set-java").value = String(srv.javaVersion || 21);
      $("set-port").value = props.serverPort || srv.port || 25565;
      $("set-jvm-args").value = srv.jvmArgs || "";
      $("set-autostart").checked = !!srv.autoStart;
      $("set-eula").checked = !!srv.eulaAccepted;

      $("prop-motd").value = props.motd || "";
      $("prop-max-players").value = props.maxPlayers ?? 20;
      $("prop-gamemode").value = props.gamemode || "survival";
      $("prop-difficulty").value = props.difficulty || "normal";
      $("prop-view-distance").value = props.viewDistance ?? 10;
      $("prop-sim-distance").value = props.simulationDistance ?? 8;
      $("prop-level-name").value = props.levelName || "world";
      $("prop-level-seed").value = props.levelSeed || "";

      $("prop-pvp").checked = !!props.pvp;
      $("prop-hardcore").checked = !!props.hardcore;
      $("prop-online-mode").checked = !!props.onlineMode;
      $("prop-whitelist").checked = !!props.whiteList;
      $("prop-enforce-whitelist").checked = !!props.enforceWhitelist;
      $("prop-cmd-block").checked = !!props.enableCommandBlock;
      $("prop-spawn-monsters").checked = !!props.spawnMonsters;
      $("prop-spawn-animals").checked = !!props.spawnAnimals;
      $("prop-spawn-npcs").checked = !!props.spawnNpcs;
      $("prop-allow-flight").checked = !!props.allowFlight;
      $("prop-allow-nether").checked = !!props.allowNether;
      $("prop-force-gamemode").checked = !!props.forceGamemode;
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Players
  async function loadPlayers() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/players`);
      renderOnlinePlayers(data.onlinePlayers || []);
      renderSimpleList("players-whitelist-list", data.whitelist || [], "name", "whitelist");
      renderOpsList(data.ops || []);
      renderBannedList("players-banned-list", data.bannedPlayers || [], "name", "ban");
      renderBannedList("players-banned-ips-list", data.bannedIps || [], "ip", "ban-ip");
    } catch (_) {}
  }

  function renderOnlinePlayers(players) {
    const el = $("players-online-list");
    if (players.length === 0) {
      el.innerHTML = `<div class="player-sub">No players online</div>`;
      return;
    }
    el.innerHTML = players.map((name) => `
      <div class="player-row">
        <span>${escapeHtml(name)}</span>
        <div class="player-actions">
          <button class="btn btn-secondary btn-sm online-op-btn" data-name="${escapeHtml(name)}">Op</button>
          <button class="btn btn-ghost btn-sm online-kick-btn" data-name="${escapeHtml(name)}">Kick</button>
          <button class="btn btn-danger btn-sm online-ban-btn" data-name="${escapeHtml(name)}">Ban</button>
        </div>
      </div>
    `).join("");

    el.querySelectorAll(".online-op-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ops", { action: "add", name: b.dataset.name, level: 4 }));
    });
    el.querySelectorAll(".online-kick-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("kick", { name: b.dataset.name, reason: "Kicked via Web Admin" }));
    });
    el.querySelectorAll(".online-ban-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ban", { action: "add", name: b.dataset.name, reason: "Banned via Web Admin" }));
    });
  }

  function renderSimpleList(containerId, items, keyField, endpoint) {
    const el = $(containerId);
    if (items.length === 0) {
      el.innerHTML = `<div class="player-sub">Empty</div>`;
      return;
    }
    el.innerHTML = items.map((item) => `
      <div class="player-row">
        <span>${escapeHtml(item[keyField])}</span>
        <button class="btn btn-ghost btn-sm remove-btn" data-val="${escapeHtml(item[keyField])}">Remove</button>
      </div>
    `).join("");
    el.querySelectorAll(".remove-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction(endpoint, { action: "remove", [keyField]: b.dataset.val }));
    });
  }

  function renderOpsList(ops) {
    const el = $("players-ops-list");
    if (ops.length === 0) {
      el.innerHTML = `<div class="player-sub">No operators</div>`;
      return;
    }
    el.innerHTML = ops.map((op) => `
      <div class="player-row">
        <div>
          <div>${escapeHtml(op.name)}</div>
          <div class="player-sub">Level ${op.level}</div>
        </div>
        <button class="btn btn-ghost btn-sm deop-btn" data-name="${escapeHtml(op.name)}">Deop</button>
      </div>
    `).join("");
    el.querySelectorAll(".deop-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ops", { action: "remove", name: b.dataset.name }));
    });
  }

  function renderBannedList(containerId, items, keyField, endpoint) {
    const el = $(containerId);
    if (items.length === 0) {
      el.innerHTML = `<div class="player-sub">Empty</div>`;
      return;
    }
    el.innerHTML = items.map((item) => `
      <div class="player-row">
        <div>
          <div>${escapeHtml(item[keyField])}</div>
          <div class="player-sub">${escapeHtml(item.reason || "Banned")}</div>
        </div>
        <button class="btn btn-ghost btn-sm pardon-btn" data-val="${escapeHtml(item[keyField])}">Pardon</button>
      </div>
    `).join("");
    el.querySelectorAll(".pardon-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction(endpoint, { action: "remove", [keyField]: b.dataset.val }));
    });
  }

  async function playerListAction(endpoint, payload) {
    if (!state.selectedServerId) return;
    try {
      await apiFetch(`/api/servers/${state.selectedServerId}/players/${endpoint}`, {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showToast("Player list updated");
      await loadPlayers();
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Performance
  function formatUptime(sec) {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    return h > 0 ? `${h}h ${m}m` : (m > 0 ? `${m}m ${s}s` : `${s}s`);
  }

  function recordPerfSample(serverId, cpu, ramUsed, ramMax, tps, mspt) {
    if (!serverId) return;
    if (!state.perfHistory[serverId]) state.perfHistory[serverId] = [];
    const hist = state.perfHistory[serverId];
    const now = Date.now();
    if (hist.length > 0 && now - hist[hist.length - 1].ts < 1800) return;
    const ramPct = ramMax > 0 ? Math.min(100, Math.max(0, (ramUsed / ramMax) * 100)) : 0;
    hist.push({
      ts: now,
      cpu: Math.min(100, Math.max(0, cpu)),
      ramPct,
      ramUsed,
      ramMax,
      tps: Math.min(20, Math.max(0, tps)),
      mspt: Math.max(0, mspt)
    });
    if (hist.length > 60) hist.shift();
  }

  function renderPerfChart() {
    const canvas = $("perf-chart-canvas");
    if (!canvas || !state.selectedServerId) return;
    const hist = state.perfHistory[state.selectedServerId] || [];
    const summaryEl = $("perf-chart-summary");

    const rect = canvas.parentElement.getBoundingClientRect();
    const width = Math.max(300, Math.floor(rect.width || 600));
    const height = Math.max(180, Math.floor(rect.height || 220));
    const dpr = window.devicePixelRatio || 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;

    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);

    const padL = 38;
    const padR = 14;
    const padT = 14;
    const padB = 24;
    const plotW = width - padL - padR;
    const plotH = height - padT - padB;

    // Gridlines & Y labels (0%, 25%, 50%, 75%, 100%)
    ctx.font = "11px ui-monospace, monospace";
    ctx.textAlign = "right";
    ctx.textBaseline = "middle";
    for (let pct = 0; pct <= 100; pct += 25) {
      const y = padT + plotH - (pct / 100) * plotH;
      ctx.strokeStyle = "rgba(148, 163, 184, 0.14)";
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(padL, y);
      ctx.lineTo(padL + plotW, y);
      ctx.stroke();

      ctx.fillStyle = "#94a3b8";
      ctx.fillText(`${pct}%`, padL - 6, y);
    }

    if (hist.length === 0) {
      ctx.fillStyle = "#94a3b8";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.font = "13px system-ui, sans-serif";
      ctx.fillText("Start the server to record live CPU, RAM, and TPS telemetry.", width / 2, height / 2);
      if (summaryEl) summaryEl.textContent = "Waiting for running server samples...";
      return;
    }

    const latest = hist[hist.length - 1];
    if (summaryEl) {
      summaryEl.textContent = `${hist.length} samples · CPU ${latest.cpu.toFixed(1)}% · RAM ${latest.ramUsed}/${latest.ramMax} MB (${latest.ramPct.toFixed(0)}%) · TPS ${latest.tps.toFixed(1)} · MSPT ${latest.mspt.toFixed(1)} ms`;
    }

    const drawSeries = (values, color, fillAlpha) => {
      if (values.length < 1) return;
      ctx.save();
      ctx.beginPath();
      values.forEach((v, i) => {
        const x = hist.length === 1 ? padL + plotW : padL + (i / (hist.length - 1)) * plotW;
        const y = padT + plotH - (Math.min(100, Math.max(0, v)) / 100) * plotH;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.stroke();

      if (fillAlpha > 0 && values.length > 1) {
        const lastX = padL + plotW;
        ctx.lineTo(lastX, padT + plotH);
        ctx.lineTo(padL, padT + plotH);
        ctx.closePath();
        ctx.fillStyle = fillAlpha;
        ctx.fill();
      }
      ctx.restore();
    };

    drawSeries(hist.map((h) => h.ramPct), "#10b981", "rgba(16, 185, 129, 0.10)");
    drawSeries(hist.map((h) => h.cpu), "#38bdf8", "rgba(56, 189, 248, 0.12)");
    drawSeries(hist.map((h) => (h.tps / 20) * 100), "#f59e0b", null);

    // X-axis timestamps (oldest & newest)
    const fmtTime = (ts) => new Date(ts).toLocaleTimeString([], { hour12: false, minute: "2-digit", second: "2-digit" });
    ctx.fillStyle = "#94a3b8";
    ctx.font = "11px ui-monospace, monospace";
    ctx.textBaseline = "top";
    ctx.textAlign = "left";
    ctx.fillText(fmtTime(hist[0].ts), padL, padT + plotH + 6);
    ctx.textAlign = "right";
    ctx.fillText(fmtTime(latest.ts), padL + plotW, padT + plotH + 6);
  }

  async function loadPerformance() {
    if (!state.selectedServerId) return;
    try {
      const p = await apiFetch(`/api/servers/${state.selectedServerId}/performance`);
      const cpu = Number(p.cpuPercentage || 0);
      const ramUsed = Number(p.ramUsedMb || 0);
      const ramMax = Number(p.ramMaxMb || p.allocatedRamMb || 1);
      const tps = Number(p.tps ?? 20);
      const mspt = Number(p.mspt ?? 0);

      $("perf-cpu").textContent = `${Math.round(cpu)}%`;
      $("perf-ram").textContent = `${ramUsed} / ${ramMax} MB`;
      $("perf-tps").textContent = tps.toFixed(1);
      $("perf-mspt").textContent = `${mspt.toFixed(1)} ms`;
      $("perf-uptime").textContent = formatUptime(p.uptimeSeconds || 0);
      $("perf-disk").textContent = formatSize(p.serverStorageBytes || 0);
      $("perf-free-disk").textContent = `${((p.freeDeviceStorageBytes || 0) / (1024 * 1024 * 1024)).toFixed(1)} GB`;
      $("perf-aikar-status").textContent = p.aikarApplied
        ? "Aikar's G1GC flags are active on this server."
        : "Using default JVM flags. Apply Aikar's preset for lower GC pauses.";

      if (p.status === "RUNNING") {
        recordPerfSample(state.selectedServerId, cpu, ramUsed, ramMax, tps, mspt);
      }
      renderPerfChart();
    } catch (_) {}
  }

  // Automation
  async function loadAutomation() {
    if (!state.selectedServerId) return;
    try {
      const a = await apiFetch(`/api/servers/${state.selectedServerId}/automation`);
      $("auto-idle-sleep").checked = !!a.idleSleepEnabled;
      $("auto-idle-mins").value = a.idleSleepTimeoutMinutes || 10;
      $("auto-wake-ping").checked = !!a.autoWakeOnPing;

      const list = $("automation-tasks-list");
      const tasks = a.scheduledTasks || [];
      if (tasks.length === 0) {
        list.innerHTML = `<div class="player-sub">No scheduled tasks configured.</div>`;
        return;
      }
      list.innerHTML = tasks.map((t) => `
        <div class="player-row">
          <div>
            <div>${escapeHtml(t.name)} <span class="player-sub">(${escapeHtml(t.taskTypeDisplay)})</span></div>
            <div class="player-sub">${escapeHtml(t.frequencyDescription)}</div>
          </div>
          <div class="player-actions">
            <button class="btn btn-secondary btn-sm task-toggle-btn" data-id="${escapeHtml(t.id)}">${t.enabled ? "Disable" : "Enable"}</button>
            <button class="btn btn-danger btn-sm task-del-btn" data-id="${escapeHtml(t.id)}">Delete</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".task-toggle-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          await apiFetch(`/api/servers/${state.selectedServerId}/automation/tasks/toggle`, {
            method: "POST", body: JSON.stringify({ taskId: b.dataset.id })
          });
          loadAutomation();
        });
      });
      list.querySelectorAll(".task-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          await apiFetch(`/api/servers/${state.selectedServerId}/automation/tasks/delete`, {
            method: "POST", body: JSON.stringify({ taskId: b.dataset.id })
          });
          loadAutomation();
        });
      });
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // World
  async function loadWorld() {
    if (!state.selectedServerId) return;
    try {
      const w = await apiFetch(`/api/servers/${state.selectedServerId}/world`);
      $("world-level-name").textContent = w.levelName || "world";
      $("world-totals").textContent = `Total Size: ${formatSize(w.totalSizeBytes || 0)} · Estimated Chunks: ${w.totalEstimatedChunks || 0}`;
      const list = $("world-dimensions-list");
      list.innerHTML = (w.dimensions || []).map((d) => `
        <div class="player-row">
          <div>
            <div>${escapeHtml(d.displayName)}</div>
            <div class="player-sub">${d.exists ? `${formatSize(d.sizeBytes)} · ${d.regionFileCount} regions · ${d.estimatedChunks} chunks` : "Not generated yet"}</div>
          </div>
          <button class="btn btn-danger btn-sm dim-reset-btn" data-dim="${escapeHtml(d.type)}" ${d.exists ? "" : "disabled"}>Reset</button>
        </div>
      `).join("");

      list.querySelectorAll(".dim-reset-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          if (!confirm(`Reset ${b.dataset.dim}? A world backup will be created first.`)) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/world/reset`, {
              method: "POST", body: JSON.stringify({ dimension: b.dataset.dim, createBackup: true })
            });
            showToast("Dimension reset complete");
            loadWorld();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Live Map
  async function loadLiveMap() {
    if (!state.selectedServerId) return;
    try {
      const m = await apiFetch(`/api/servers/${state.selectedServerId}/livemap`);
      const frame = $("livemap-frame");
      const installBtn = $("livemap-install-btn");
      const openBtn = $("livemap-open-btn");
      const uninstallBtn = $("livemap-uninstall-btn");
      const portRow = $("livemap-port-row");
      const portInput = $("livemap-port-input");
      const conflictBanner = $("livemap-conflict-banner");

      installBtn.classList.toggle("hidden", m.installed || !m.supportsPluginsOrMods);
      uninstallBtn.classList.toggle("hidden", !m.installed);
      portRow.classList.toggle("hidden", !m.installed);
      if (m.installed && document.activeElement !== portInput) {
        portInput.value = m.port;
      }

      const showMap = m.installed && m.isServerRunning && !m.portConflict;
      openBtn.classList.toggle("hidden", !showMap);

      if (!m.installed) {
        $("livemap-status-text").textContent = m.supportsPluginsOrMods
          ? "Squaremap is not installed on this server."
          : "This server software does not support Squaremap.";
        conflictBanner.classList.add("hidden");
        frame.classList.add("hidden");
        frame.src = "about:blank";
      } else if (m.portConflict) {
        const suggested = m.suggestedPort || 8081;
        $("livemap-status-text").textContent = `Port conflict: Squaremap is on port ${m.port}, which is used by Web Admin.`;
        conflictBanner.innerHTML = `
          <span>Squaremap cannot run on port ${m.port} while the Web Admin panel is using it. Switch Squaremap to port ${suggested} and restart the Minecraft server.</span>
          <button type="button" id="livemap-fix-port-btn" class="btn btn-primary btn-sm">Switch to Port ${suggested}</button>
        `;
        conflictBanner.classList.remove("hidden");
        $("livemap-fix-port-btn").addEventListener("click", async () => {
          await saveLiveMapPort(suggested);
        });
        frame.classList.add("hidden");
        frame.src = "about:blank";
      } else if (!m.isServerRunning) {
        conflictBanner.classList.add("hidden");
        $("livemap-status-text").textContent = `${m.pluginName} is installed (port ${m.port}). Start the server to view the live map.`;
        frame.classList.add("hidden");
        frame.src = "about:blank";
      } else {
        conflictBanner.classList.add("hidden");
        const mapUrl = `${window.location.protocol}//${window.location.hostname}:${m.port}`;
        $("livemap-status-text").textContent = `${m.pluginName} active at ${mapUrl}`;
        openBtn.onclick = () => window.open(mapUrl, "_blank", "noopener");
        if (frame.src !== mapUrl) frame.src = mapUrl;
        frame.classList.remove("hidden");
      }
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  async function saveLiveMapPort(port) {
    if (!state.selectedServerId) return;
    try {
      await apiFetch(`/api/servers/${state.selectedServerId}/livemap/port`, {
        method: "POST",
        body: JSON.stringify({ port: Number(port) })
      });
      showToast(`Squaremap port set to ${port}. Restart the server to apply.`);
      loadLiveMap();
    } catch (err) {
      showToast(err.message, "error");
    }
  }

  // Network (Tunnels & Port Forwarding)
  async function loadNetwork() {
    if (!state.selectedServerId) return;
    try {
      const n = await apiFetch(`/api/servers/${state.selectedServerId}/network`);
      const tc = n.tunnelConfig || {};
      const ts = n.tunnelState || {};
      $("net-tunnel-provider").value = tc.provider || "BORE";
      $("net-bore-host").value = tc.customRelayHost || "bore.pub";
      $("net-bore-port").value = tc.customRelayPort || 7835;
      $("net-tunnel-autostart").checked = !!tc.autoStart;

      if (ts.status === "connected") {
        $("tunnel-status-text").textContent = `Connected: ${ts.fullAddress}`;
      } else if (ts.status === "connecting") {
        $("tunnel-status-text").textContent = ts.claimUrl ? `Connecting (Claim: ${ts.claimUrl})` : (ts.message || "Connecting...");
      } else if (ts.status === "error") {
        $("tunnel-status-text").textContent = `Error: ${ts.errorMessage}`;
      } else {
        $("tunnel-status-text").textContent = "Disconnected";
      }

      const pc = n.portForwardConfig || {};
      const ps = n.portForwardState || {};
      $("net-pf-protocol").value = pc.protocol || "BOTH";
      $("net-pf-lease").value = pc.leaseDurationSeconds || 0;
      $("net-pf-auto").checked = !!pc.autoForward;

      if (ps.status === "mapped") {
        $("pf-status-text").textContent = `Mapped: ${ps.directAddress} (${ps.gatewayName})`;
      } else if (ps.status === "discovering") {
        $("pf-status-text").textContent = ps.message || "Discovering router...";
      } else if (ps.status === "failed") {
        $("pf-status-text").textContent = `Failed: ${ps.reason}`;
      } else {
        $("pf-status-text").textContent = n.localIp ? `Idle (Local IP: ${n.localIp})` : "Idle";
      }
    } catch (_) {}
  }

  // Backups
  async function loadDiscordWebhook() {
    if (!state.selectedServerId) return;
    try {
      const d = await apiFetch(`/api/servers/${state.selectedServerId}/discord`);
      $("discord-enabled").checked = !!d.enabled;
      $("discord-url").value = d.webhookUrl || "";
      $("discord-start").checked = d.notifyServerStart !== false;
      $("discord-stop").checked = d.notifyServerStop !== false;
      $("discord-crash").checked = d.notifyServerCrash !== false;
      $("discord-join").checked = d.notifyPlayerJoin !== false;
      $("discord-leave").checked = d.notifyPlayerLeave !== false;
      $("discord-backup").checked = d.notifyBackupComplete !== false;
      $("discord-skins").checked = d.includePlayerSkins !== false;
    } catch (_) {}
  }

  async function loadProxySettings() {
    if (!state.selectedServerId) return;
    try {
      const p = await apiFetch(`/api/servers/${state.selectedServerId}/proxy`);
      const body = $("proxy-card-body");
      if (p.isProxy) {
        $("proxy-card-title").textContent = "Proxy Network & Linked Servers";
        $("proxy-card-desc").textContent = `Link backend servers to ${p.proxyType}. Applying writes velocity.toml / config.yml.`;
        const list = p.candidates || [];
        const rows = list.length === 0
          ? `<div class="player-sub" style="margin-bottom:10px;">No backend servers found on this device.</div>`
          : list.map((c) => `
              <label class="checkbox-label" style="display:flex;margin-bottom:6px;">
                <input type="checkbox" class="proxy-route-chk" value="${escapeHtml(c.id)}" ${c.linked ? "checked" : ""}>
                <span>${escapeHtml(c.name)} (${escapeHtml(c.type)} · 127.0.0.1:${c.port} · alias: ${escapeHtml(c.alias)})</span>
              </label>
            `).join("");
        body.innerHTML = `
          <label class="checkbox-label" style="margin-bottom:10px;"><input type="checkbox" id="proxy-online-mode" ${p.onlineMode ? "checked" : ""}><span>Proxy Online Mode (Mojang Auth)</span></label>
          <div style="margin-bottom:10px;">${rows}</div>
          <div class="player-actions">
            <button type="button" id="proxy-apply-btn" class="btn btn-primary btn-sm">Apply Proxy Config</button>
            <button type="button" id="proxy-bind-btn" class="btn btn-secondary btn-sm">Apply &amp; Bind Backends to 127.0.0.1</button>
          </div>
        `;
        const saveProxy = async (bindLoopback) => {
          const linkedServerIds = Array.from(body.querySelectorAll(".proxy-route-chk:checked")).map((el) => el.value);
          await apiFetch(`/api/servers/${state.selectedServerId}/proxy`, {
            method: "POST",
            body: JSON.stringify({
              onlineMode: $("proxy-online-mode").checked,
              linkedServerIds,
              bindBackendsLoopback: bindLoopback
            })
          });
          showToast(bindLoopback ? "Proxy config applied and backends bound to 127.0.0.1" : "Proxy config applied");
        };
        $("proxy-apply-btn").addEventListener("click", () => saveProxy(false).catch((e) => showToast(e.message, "error")));
        $("proxy-bind-btn").addEventListener("click", () => saveProxy(true).catch((e) => showToast(e.message, "error")));
      } else {
        $("proxy-card-title").textContent = "Internal Loopback Binding (Proxy Backend)";
        $("proxy-card-desc").textContent = "Bind server-ip to 127.0.0.1 and set online-mode=false so only Velocity or BungeeCord can reach this server.";
        body.innerHTML = `
          <label class="checkbox-label" style="margin-bottom:10px;"><input type="checkbox" id="backend-loopback-chk" ${p.loopbackOnly ? "checked" : ""}><span>Bind strictly to 127.0.0.1 (Proxy Backend Mode)</span></label>
          <button type="button" id="backend-loopback-save" class="btn btn-primary btn-sm">Save Loopback Mode</button>
        `;
        $("backend-loopback-save").addEventListener("click", async () => {
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/proxy`, {
              method: "POST",
              body: JSON.stringify({ loopbackOnly: $("backend-loopback-chk").checked })
            });
            showToast("Loopback binding updated");
          } catch (e) {
            showToast(e.message, "error");
          }
        });
      }
    } catch (_) {}
  }

  async function loadBackups() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/backups`);
      const list = $("backups-list");
      const backups = data.backups || [];
      if (backups.length === 0) {
        list.innerHTML = `<div class="player-sub">No backups yet.</div>`;
        return;
      }
      list.innerHTML = backups.map((b) => `
        <div class="player-row">
          <div>
            <div>${escapeHtml(b.fileName)}</div>
            <div class="player-sub">${b.isWorldOnly ? "World Only" : "Full Server"} · ${formatSize(b.sizeBytes)} · ${new Date(b.timestamp).toLocaleString()}</div>
          </div>
          <div class="player-actions">
            <a class="btn btn-secondary btn-sm" href="/api/servers/${state.selectedServerId}/backups/download?file=${encodeURIComponent(b.fileName)}" download>Download</a>
            <button class="btn btn-ghost btn-sm backup-restore-btn" data-file="${escapeHtml(b.fileName)}">Restore</button>
            <button class="btn btn-danger btn-sm backup-del-btn" data-file="${escapeHtml(b.fileName)}">Delete</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".backup-restore-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          if (!confirm(`Restore "${b.dataset.file}"? Stop the server first.`)) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/backups/restore`, {
              method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
            });
            showToast("Backup restored");
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
      list.querySelectorAll(".backup-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          if (!confirm(`Delete backup "${b.dataset.file}"?`)) return;
          await apiFetch(`/api/servers/${state.selectedServerId}/backups/delete`, {
            method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
          });
          showToast("Backup deleted");
          loadBackups();
        });
      });
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Plugins / Mods
  async function loadPlugins() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/plugins`);
      const list = $("plugins-installed-list");
      const items = data.items || [];
      if (items.length === 0) {
        list.innerHTML = `<div class="player-sub">No plugins or mods installed.</div>`;
        return;
      }
      list.innerHTML = items.map((p) => `
        <div class="player-row">
          <div>
            <div>${escapeHtml(p.name)}</div>
            <div class="player-sub">${escapeHtml(p.fileName)} · ${formatSize(p.sizeBytes)} · ${p.enabled ? "Enabled" : "Disabled"}</div>
          </div>
          <div class="player-actions">
            <button class="btn btn-secondary btn-sm plug-toggle-btn" data-file="${escapeHtml(p.fileName)}">${p.enabled ? "Disable" : "Enable"}</button>
            <button class="btn btn-danger btn-sm plug-del-btn" data-file="${escapeHtml(p.fileName)}">Delete</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".plug-toggle-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          await apiFetch(`/api/servers/${state.selectedServerId}/plugins/toggle`, {
            method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
          });
          loadPlugins();
        });
      });
      list.querySelectorAll(".plug-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          if (!confirm(`Delete "${b.dataset.file}"?`)) return;
          await apiFetch(`/api/servers/${state.selectedServerId}/plugins/delete`, {
            method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
          });
          loadPlugins();
        });
      });
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Create Server Modal
  async function openCreateModal() {
    $("create-error").classList.add("hidden");
    $("create-progress-box").classList.add("hidden");
    $("submit-create-btn").disabled = false;
    $("create-eula").checked = false;

    const usedPorts = new Set(state.servers.map((s) => s.port));
    let nextPort = 25565;
    while (usedPorts.has(nextPort)) nextPort++;
    $("create-port").value = nextPort;

    $("create-modal").classList.remove("hidden");

    if (state.softwareTypes.length === 0) {
      try {
        const data = await apiFetch("/api/software/types");
        state.softwareTypes = data.types || [];
        $("create-type").innerHTML = state.softwareTypes.map((t) =>
          `<option value="${escapeHtml(t.name)}">${escapeHtml(t.displayName)}</option>`
        ).join("");
      } catch (e) {
        $("create-error").textContent = e.message;
        $("create-error").classList.remove("hidden");
      }
    }
    await loadVersionsForType($("create-type").value || "PAPER");
  }

  async function loadVersionsForType(type) {
    const sel = $("create-version");
    sel.innerHTML = `<option value="">Loading versions...</option>`;
    sel.disabled = true;
    try {
      const data = await apiFetch(`/api/software/versions?type=${encodeURIComponent(type)}`);
      const versions = data.versions || [];
      sel.innerHTML = versions.map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join("");
    } catch (e) {
      sel.innerHTML = `<option value="1.21.4">1.21.4</option>`;
    } finally {
      sel.disabled = false;
    }
  }

  async function pollCreationJob(jobId) {
    while (true) {
      await new Promise((r) => setTimeout(r, 700));
      const job = await apiFetch(`/api/servers/jobs/${encodeURIComponent(jobId)}`);
      $("create-progress-msg").textContent = job.message || "Working...";
      $("create-progress-pct").textContent = `${job.progress || 0}%`;
      $("create-progress-fill").style.width = `${job.progress || 0}%`;

      if (job.status === "done") {
        $("create-modal").classList.add("hidden");
        showToast("Server created!");
        await loadServers();
        if (job.serverId) selectServer(job.serverId);
        return;
      }
      if (job.status === "error") {
        throw new Error(job.message || "Server creation failed");
      }
    }
  }

  // Event Listeners
  function bindEvents() {
    $("login-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      $("login-error").classList.add("hidden");
      $("login-btn").disabled = true;
      try {
        await apiFetch("/api/login", {
          method: "POST",
          body: JSON.stringify({
            username: $("login-user").value.trim(),
            password: $("login-pass").value
          })
        });
        $("login-pass").value = "";
        showApp();
      } catch (err) {
        $("login-error").textContent = err.message;
        $("login-error").classList.remove("hidden");
      } finally {
        $("login-btn").disabled = false;
      }
    });

    $("logout-btn").addEventListener("click", async () => {
      try { await apiFetch("/api/logout", { method: "POST" }); } catch (_) {}
      showLogin();
    });

    $("mobile-menu-btn").addEventListener("click", () => $("sidebar").classList.add("open"));
    $("mobile-close-sidebar").addEventListener("click", () => $("sidebar").classList.remove("open"));

    document.querySelectorAll(".tab-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.activeTab = btn.dataset.tab;
        document.querySelectorAll(".tab-btn").forEach((b) => b.classList.toggle("active", b === btn));
        document.querySelectorAll(".tab-panel").forEach((p) => p.classList.toggle("active", p.id === `tab-${state.activeTab}`));
        loadActiveTabData();
      });
    });

    // Server lifecycle controls
    $("btn-start").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/start`, { method: "POST" });
      showToast("Start requested");
      loadServerDetails();
    });
    $("btn-stop").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/stop`, { method: "POST" });
      showToast("Stop requested");
      loadServerDetails();
    });
    $("btn-restart").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/restart`, { method: "POST" });
      showToast("Restart requested");
      loadServerDetails();
    });

    // Console
    $("console-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const input = $("console-input");
      const cmd = input.value.trim();
      if (!cmd || !state.selectedServerId) return;
      state.cmdHistory.unshift(cmd);
      if (state.cmdHistory.length > 50) state.cmdHistory.pop();
      state.cmdHistoryIdx = -1;
      input.value = "";
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/console`, {
          method: "POST",
          body: JSON.stringify({ command: cmd })
        });
        setTimeout(loadConsole, 200);
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("console-input").addEventListener("keydown", (e) => {
      if (e.key === "ArrowUp" && state.cmdHistory.length > 0) {
        e.preventDefault();
        state.cmdHistoryIdx = Math.min(state.cmdHistoryIdx + 1, state.cmdHistory.length - 1);
        e.target.value = state.cmdHistory[state.cmdHistoryIdx];
      } else if (e.key === "ArrowDown") {
        e.preventDefault();
        state.cmdHistoryIdx = Math.max(state.cmdHistoryIdx - 1, -1);
        e.target.value = state.cmdHistoryIdx >= 0 ? state.cmdHistory[state.cmdHistoryIdx] : "";
      }
    });

    // Files
    $("files-up-btn").addEventListener("click", () => {
      if (!state.currentPath) return;
      const parts = state.currentPath.split("/").filter(Boolean);
      parts.pop();
      loadFiles(parts.join("/"));
    });
    $("files-refresh-btn").addEventListener("click", () => loadFiles(state.currentPath));
    $("editor-close-btn").addEventListener("click", closeFileEditor);
    $("editor-save-btn").addEventListener("click", async () => {
      if (!state.selectedServerId || !state.editingFile) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/files/write`, {
          method: "POST",
          body: JSON.stringify({ path: state.editingFile, content: $("editor-textarea").value })
        });
        showToast("File saved");
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Settings
    $("settings-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/settings`, {
          method: "POST",
          body: JSON.stringify({
            server: {
              name: $("set-name").value.trim(),
              allocatedRamMb: parseInt($("set-ram").value, 10) || 2048,
              javaVersion: parseInt($("set-java").value, 10) || 21,
              port: parseInt($("set-port").value, 10) || 25565,
              jvmArgs: $("set-jvm-args").value.trim(),
              autoStart: $("set-autostart").checked,
              eulaAccepted: $("set-eula").checked
            },
            properties: {
              motd: $("prop-motd").value,
              serverPort: parseInt($("set-port").value, 10) || 25565,
              maxPlayers: parseInt($("prop-max-players").value, 10) || 20,
              gamemode: $("prop-gamemode").value,
              difficulty: $("prop-difficulty").value,
              viewDistance: parseInt($("prop-view-distance").value, 10) || 10,
              simulationDistance: parseInt($("prop-sim-distance").value, 10) || 8,
              levelName: $("prop-level-name").value.trim() || "world",
              levelSeed: $("prop-level-seed").value,
              pvp: $("prop-pvp").checked,
              hardcore: $("prop-hardcore").checked,
              onlineMode: $("prop-online-mode").checked,
              whiteList: $("prop-whitelist").checked,
              enforceWhitelist: $("prop-enforce-whitelist").checked,
              enableCommandBlock: $("prop-cmd-block").checked,
              spawnMonsters: $("prop-spawn-monsters").checked,
              spawnAnimals: $("prop-spawn-animals").checked,
              spawnNpcs: $("prop-spawn-npcs").checked,
              allowFlight: $("prop-allow-flight").checked,
              allowNether: $("prop-allow-nether").checked,
              forceGamemode: $("prop-force-gamemode").checked
            }
          })
        });
        showToast("Settings saved");
        await loadServers();
        await loadServerDetails();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("delete-server-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const srv = state.servers.find((s) => s.id === state.selectedServerId);
      if (!confirm(`Delete server "${srv?.name || state.selectedServerId}"? This removes all world data permanently.`)) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/delete`, { method: "POST" });
        showToast("Server deleted");
        selectServer(null);
        await loadServers();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Players forms
    $("whitelist-add-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = $("whitelist-add-input").value.trim();
      if (!name) return;
      await playerListAction("whitelist", { action: "add", name });
      $("whitelist-add-input").value = "";
    });
    $("op-add-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = $("op-add-input").value.trim();
      const level = parseInt($("op-level-input").value, 10) || 4;
      if (!name) return;
      await playerListAction("ops", { action: "add", name, level });
      $("op-add-input").value = "";
    });
    $("ban-add-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = $("ban-add-input").value.trim();
      const reason = $("ban-reason-input").value.trim();
      if (!name) return;
      await playerListAction("ban", { action: "add", name, reason });
      $("ban-add-input").value = "";
      $("ban-reason-input").value = "";
    });
    $("banip-add-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const ip = $("banip-add-input").value.trim();
      const reason = $("banip-reason-input").value.trim();
      if (!ip) return;
      await playerListAction("ban-ip", { action: "add", ip, reason });
      $("banip-add-input").value = "";
      $("banip-reason-input").value = "";
    });

    // Performance, Automation, World, Live Map, Network, Backups, Plugins
    $("apply-aikar-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/performance/aikar`, { method: "POST" });
      showToast("Aikar's G1GC flags applied");
      loadPerformance();
    });

    $("automation-power-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/automation`, {
        method: "POST",
        body: JSON.stringify({
          idleSleepEnabled: $("auto-idle-sleep").checked,
          idleSleepTimeoutMinutes: parseInt($("auto-idle-mins").value, 10) || 10,
          autoWakeOnPing: $("auto-wake-ping").checked
        })
      });
      showToast("Automation settings saved");
    });

    $("automation-task-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/automation/tasks/add`, {
        method: "POST",
        body: JSON.stringify({
          name: $("task-name").value.trim(),
          taskType: $("task-type").value,
          frequency: $("task-freq").value,
          intervalMinutes: parseInt($("task-interval").value, 10) || 360,
          command: $("task-cmd").value.trim()
        })
      });
      $("task-name").value = "";
      $("task-cmd").value = "";
      showToast("Scheduled task added");
      loadAutomation();
    });

    $("perf-clear-chart-btn").addEventListener("click", () => {
      if (!state.selectedServerId) return;
      state.perfHistory[state.selectedServerId] = [];
      renderPerfChart();
    });

    $("world-export-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const btn = $("world-export-btn");
      btn.disabled = true;
      btn.textContent = "Exporting...";
      try {
        const res = await fetch(`/api/servers/${state.selectedServerId}/world/export`, {
          credentials: "same-origin"
        });
        if (!res.ok) {
          const errJson = await res.json().catch(() => ({}));
          throw new Error(errJson.error || `Export failed (${res.status})`);
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `${state.selectedServerId}-world.zip`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        showToast("World exported!");
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Export World (.zip)";
      }
    });

    $("world-import-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      const fileInput = $("world-import-file");
      const file = fileInput.files && fileInput.files[0];
      if (!file) return;
      const createBackup = $("world-import-backup").checked;
      const btn = $("world-import-btn");
      btn.disabled = true;
      btn.textContent = "Importing...";
      try {
        const res = await fetch(`/api/servers/${state.selectedServerId}/world/import?createBackup=${createBackup}`, {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/octet-stream" },
          body: file
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.error || `Import failed (${res.status})`);
        showToast(data.message || "World imported successfully!");
        fileInput.value = "";
        loadWorld();
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Import World";
      }
    });

    $("optimize-world-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const threshold = $("prune-threshold").value;
      const pruneNether = $("prune-nether").checked;
      const pruneEnd = $("prune-end").checked;
      const createBackup = $("prune-backup").checked;
      if (!confirm("Run chunk pruner with these options?")) return;
      const btn = $("optimize-world-btn");
      btn.disabled = true;
      btn.textContent = "Pruning...";
      try {
        const res = await apiFetch(`/api/servers/${state.selectedServerId}/world/optimize`, {
          method: "POST",
          body: JSON.stringify({ threshold, pruneNether, pruneEnd, createBackup })
        });
        const summary = `Scanned ${res.regionsScanned} regions (${res.scannedChunks || 0} chunks), pruned ${res.chunksPruned} chunks, freed ${formatSize(res.bytesSaved || 0)} (${res.percentFreed || 0}%).`;
        $("prune-last-result").textContent = summary;
        showToast(`Pruned ${res.chunksPruned} chunks (${formatSize(res.bytesSaved || 0)} freed)`);
        loadWorld();
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Run Chunk Pruner";
      }
    });

    $("livemap-port-save-btn").addEventListener("click", async () => {
      const p = parseInt($("livemap-port-input").value, 10);
      await saveLiveMapPort(p);
    });

    $("livemap-install-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      showToast("Installing Squaremap...");
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/livemap/install`, { method: "POST" });
        showToast("Squaremap installed! Restart server to activate.");
        loadLiveMap();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("livemap-uninstall-btn").addEventListener("click", async () => {
      if (!state.selectedServerId || !confirm("Uninstall Squaremap?")) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/livemap/uninstall`, { method: "POST" });
      showToast("Squaremap removed");
      loadLiveMap();
    });

    const sendTunnelAction = async (action) => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/network/tunnel`, {
        method: "POST",
        body: JSON.stringify({
          action,
          provider: $("net-tunnel-provider").value,
          autoStart: $("net-tunnel-autostart").checked,
          customRelayHost: $("net-bore-host").value.trim(),
          customRelayPort: parseInt($("net-bore-port").value, 10) || 7835,
          playitSecret: $("net-playit-secret").value.trim()
        })
      });
      showToast(action === "start" ? "Tunnel starting..." : (action === "stop" ? "Tunnel stopped" : "Tunnel settings saved"));
      setTimeout(loadNetwork, 400);
    };
    $("tunnel-start-btn").addEventListener("click", () => sendTunnelAction("start"));
    $("tunnel-stop-btn").addEventListener("click", () => sendTunnelAction("stop"));
    $("tunnel-save-btn").addEventListener("click", () => sendTunnelAction("save"));

    const sendPfAction = async (action) => {
      if (!state.selectedServerId) return;
      await apiFetch(`/api/servers/${state.selectedServerId}/network/portforward`, {
        method: "POST",
        body: JSON.stringify({
          action,
          protocol: $("net-pf-protocol").value,
          leaseDurationSeconds: parseInt($("net-pf-lease").value, 10) || 0,
          autoForward: $("net-pf-auto").checked
        })
      });
      showToast(action === "start" ? "Mapping router port..." : (action === "stop" ? "Port released" : "Port forwarding settings saved"));
      setTimeout(loadNetwork, 400);
    };
    $("pf-start-btn").addEventListener("click", () => sendPfAction("start"));
    $("pf-stop-btn").addEventListener("click", () => sendPfAction("stop"));
    $("pf-save-btn").addEventListener("click", () => sendPfAction("save"));

    $("discord-webhook-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/discord`, {
          method: "POST",
          body: JSON.stringify({
            enabled: $("discord-enabled").checked,
            webhookUrl: $("discord-url").value.trim(),
            notifyServerStart: $("discord-start").checked,
            notifyServerStop: $("discord-stop").checked,
            notifyServerCrash: $("discord-crash").checked,
            notifyPlayerJoin: $("discord-join").checked,
            notifyPlayerLeave: $("discord-leave").checked,
            notifyBackupComplete: $("discord-backup").checked,
            includePlayerSkins: $("discord-skins").checked
          })
        });
        showToast("Discord webhook settings saved");
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("discord-test-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const btn = $("discord-test-btn");
      btn.disabled = true;
      btn.textContent = "Sending...";
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/discord/test`, {
          method: "POST",
          body: JSON.stringify({ webhookUrl: $("discord-url").value.trim() })
        });
        showToast("Test message sent to Discord!");
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Test Webhook";
      }
    });

    $("backup-world-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      showToast("Creating world backup...");
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/backups/create`, {
          method: "POST", body: JSON.stringify({ isWorldOnly: true })
        });
        showToast("World backup created");
        loadBackups();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("backup-full-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      showToast("Creating full server backup...");
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/backups/create`, {
          method: "POST", body: JSON.stringify({ isWorldOnly: false })
        });
        showToast("Full backup created");
        loadBackups();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("modrinth-search-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      const q = $("modrinth-query").value.trim();
      if (!q) return;
      const list = $("modrinth-results-list");
      list.innerHTML = `<div class="player-sub">Searching Modrinth...</div>`;
      try {
        const res = await apiFetch(`/api/servers/${state.selectedServerId}/plugins/search?q=${encodeURIComponent(q)}`);
        const results = res.results || [];
        if (results.length === 0) {
          list.innerHTML = `<div class="player-sub">No results found.</div>`;
          return;
        }
        list.innerHTML = results.map((r) => `
          <div class="player-row">
            <div style="min-width:0;">
              <div>${escapeHtml(r.name)} <span class="player-sub">by ${escapeHtml(r.author)}</span></div>
              <div class="player-sub">${escapeHtml(r.description)}</div>
            </div>
            <button class="btn btn-primary btn-sm modrinth-install-btn" data-id="${escapeHtml(r.id)}">Install</button>
          </div>
        `).join("");
        list.querySelectorAll(".modrinth-install-btn").forEach((b) => {
          b.addEventListener("click", async () => {
            b.disabled = true;
            b.textContent = "...";
            try {
              const ins = await apiFetch(`/api/servers/${state.selectedServerId}/plugins/install`, {
                method: "POST", body: JSON.stringify({ projectId: b.dataset.id })
              });
              showToast(`Installed ${ins.fileName}`);
              b.textContent = "Installed";
              loadPlugins();
            } catch (err) {
              showToast(err.message, "error");
              b.disabled = false;
              b.textContent = "Install";
            }
          });
        });
      } catch (err) {
        list.innerHTML = `<div class="player-sub">${escapeHtml(err.message)}</div>`;
      }
    });

    // Create Server Modal
    $("open-create-modal-btn").addEventListener("click", openCreateModal);
    $("empty-create-btn").addEventListener("click", openCreateModal);
    $("close-create-modal").addEventListener("click", () => $("create-modal").classList.add("hidden"));
    $("cancel-create-btn").addEventListener("click", () => $("create-modal").classList.add("hidden"));
    $("create-type").addEventListener("change", (e) => loadVersionsForType(e.target.value));

    $("create-server-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      $("create-error").classList.add("hidden");
      if (!$("create-eula").checked) {
        $("create-error").textContent = "You must agree to the Minecraft EULA before creating a server.";
        $("create-error").classList.remove("hidden");
        return;
      }

      $("submit-create-btn").disabled = true;
      $("create-progress-box").classList.remove("hidden");
      $("create-progress-msg").textContent = "Starting...";
      $("create-progress-pct").textContent = "0%";
      $("create-progress-fill").style.width = "0%";

      try {
        const res = await apiFetch("/api/servers", {
          method: "POST",
          body: JSON.stringify({
            name: $("create-name").value.trim(),
            type: $("create-type").value,
            version: $("create-version").value,
            port: parseInt($("create-port").value, 10) || 25565,
            ramMb: parseInt($("create-ram").value, 10) || 2048,
            motd: $("create-motd").value.trim() || "A MineServe Minecraft Server",
            eulaAccepted: true
          })
        });
        await pollCreationJob(res.jobId);
      } catch (err) {
        $("create-error").textContent = err.message;
        $("create-error").classList.remove("hidden");
        $("submit-create-btn").disabled = false;
      }
    });
  }

  bindEvents();
  checkAuth();
})();

