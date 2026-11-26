(() => {
  "use strict";

  const state = {
    servers: [],
    selectedServerId: null,
    activeTab: "console",
    subTabs: {
      content: "plugins",
      worldmap: "world",
      management: "settings"
    },
    currentPath: "",
    editingFile: null,
    cmdHistory: [],
    cmdHistoryIdx: -1,
    pollTimer: null,
    softwareTypes: [],
    perfHistory: {},
    systemInfo: null,
    cloudLinked: false,
    whatsNewChecked: false,
    themeMode: "dark",
    wasAuthenticated: false,
    playersData: null,
    playersFilter: "online",
    playersSearch: "",
    installedPlugins: [],
    pluginsFilter: "",
    cachedSettings: null
  };

  const $ = (id) => document.getElementById(id);

  function getCssToken(name, fallback = "") {
    const val = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return val || fallback;
  }

  function resolveTheme(mode) {
    if (mode === "system") {
      return window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
    }
    return mode === "light" ? "light" : "dark";
  }

  function applyTheme(mode) {
    const normalized = mode === "light" || mode === "system" ? mode : "dark";
    state.themeMode = normalized;
    try { localStorage.setItem("mineserve_web_theme", normalized); } catch (_) {}
    document.documentElement.dataset.theme = resolveTheme(normalized);
    document.querySelectorAll("[data-theme-choice]").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.themeChoice === normalized);
    });
    if (getEffectiveTab() === "performance") {
      renderPerfChart();
    }
  }

  function initTheme() {
    let saved = "dark";
    try { saved = localStorage.getItem("mineserve_web_theme") || "dark"; } catch (_) {}
    applyTheme(saved);
    if (window.matchMedia) {
      const mq = window.matchMedia("(prefers-color-scheme: light)");
      const handler = () => {
        if (state.themeMode === "system") {
          document.documentElement.dataset.theme = resolveTheme("system");
          if (getEffectiveTab() === "performance") renderPerfChart();
        }
      };
      if (mq.addEventListener) mq.addEventListener("change", handler);
      else if (mq.addListener) mq.addListener(handler);
    }
  }

  function getEffectiveTab() {
    if (state.activeTab === "content") return state.subTabs.content || "plugins";
    if (state.activeTab === "worldmap") return state.subTabs.worldmap || "world";
    if (state.activeTab === "management") return state.subTabs.management || "settings";
    return state.activeTab;
  }

  function setActiveTab(tab, subTab = null) {
    state.activeTab = tab;
    if (subTab && (tab === "content" || tab === "worldmap" || tab === "management")) {
      state.subTabs[tab] = subTab;
    }
    document.querySelectorAll(".tab-btn").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === tab);
    });
    document.querySelectorAll(".tab-panel").forEach((panel) => {
      panel.classList.toggle("active", panel.id === `tab-${tab}`);
    });
    if (tab === "content" || tab === "worldmap" || tab === "management") {
      const activeSub = state.subTabs[tab];
      document.querySelectorAll(`.m3-segmented-btn[data-parent-tab="${tab}"]`).forEach((btn) => {
        btn.classList.toggle("active", btn.dataset.subtab === activeSub);
      });
      const parentEl = $(`tab-${tab}`);
      if (parentEl) {
        parentEl.querySelectorAll(".subtab-panel").forEach((sp) => {
          sp.classList.toggle("active", sp.id === `subtab-${activeSub}`);
        });
      }
    }
    loadActiveTabData();
  }

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

  function openDialog(id) {
    const dlg = $(id);
    if (!dlg) return;
    if (typeof dlg.showModal === "function") {
      if (!dlg.open) dlg.showModal();
    } else {
      dlg.setAttribute("open", "");
    }
  }

  function closeDialog(id) {
    const dlg = $(id);
    if (!dlg) return;
    if (typeof dlg.close === "function") {
      if (dlg.open) dlg.close();
    } else {
      dlg.removeAttribute("open");
    }
  }

  function showConfirmDialog({ title = "Confirm Action", message = "", confirmText = "Confirm", destructive = false } = {}) {
    return new Promise((resolve) => {
      const dlg = $("m3-confirm-dialog");
      if (!dlg) { resolve(false); return; }
      $("m3-confirm-title").textContent = title;
      $("m3-confirm-message").textContent = message;
      const okBtn = $("m3-confirm-ok-btn");
      okBtn.textContent = confirmText;
      okBtn.className = destructive ? "btn btn-danger" : "btn btn-primary";

      let settled = false;
      const finish = (val) => {
        if (settled) return;
        settled = true;
        cleanup();
        closeDialog("m3-confirm-dialog");
        resolve(val);
      };
      const onOk = () => finish(true);
      const onCancel = () => finish(false);
      const onClose = () => finish(false);
      const cleanup = () => {
        okBtn.removeEventListener("click", onOk);
        $("m3-confirm-cancel-btn")?.removeEventListener("click", onCancel);
        $("m3-confirm-close-btn")?.removeEventListener("click", onClose);
        dlg.removeEventListener("cancel", onClose);
      };
      okBtn.addEventListener("click", onOk);
      $("m3-confirm-cancel-btn")?.addEventListener("click", onCancel);
      $("m3-confirm-close-btn")?.addEventListener("click", onClose);
      dlg.addEventListener("cancel", onClose);
      openDialog("m3-confirm-dialog");
    });
  }

  function showPromptDialog({ title = "Enter Value", message = "", placeholder = "", defaultValue = "", inputType = "text", confirmText = "Save" } = {}) {
    return new Promise((resolve) => {
      const dlg = $("m3-prompt-dialog");
      const form = $("m3-prompt-form");
      const input = $("m3-prompt-input");
      if (!dlg || !form || !input) { resolve(null); return; }
      $("m3-prompt-title").textContent = title;
      $("m3-prompt-message").textContent = message;
      $("m3-prompt-ok-btn").textContent = confirmText;
      input.type = inputType;
      input.placeholder = placeholder;
      input.value = defaultValue;

      let settled = false;
      const finish = (val) => {
        if (settled) return;
        settled = true;
        cleanup();
        closeDialog("m3-prompt-dialog");
        resolve(val);
      };
      const onSubmit = (e) => {
        e.preventDefault();
        finish(input.value);
      };
      const onCancel = () => finish(null);
      const cleanup = () => {
        form.removeEventListener("submit", onSubmit);
        $("m3-prompt-cancel-btn")?.removeEventListener("click", onCancel);
        $("m3-prompt-close-btn")?.removeEventListener("click", onCancel);
        dlg.removeEventListener("cancel", onCancel);
      };
      form.addEventListener("submit", onSubmit);
      $("m3-prompt-cancel-btn")?.addEventListener("click", onCancel);
      $("m3-prompt-close-btn")?.addEventListener("click", onCancel);
      dlg.addEventListener("cancel", onCancel);
      openDialog("m3-prompt-dialog");
      setTimeout(() => input.focus(), 40);
    });
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
      if (state.wasAuthenticated) {
        openDialog("session-expired-modal");
      } else {
        showLogin();
      }
      throw new Error(data.error || "Session expired");
    }
    if (!res.ok) {
      throw new Error(data.error || `Request failed (${res.status})`);
    }
    return data;
  }

  function disconnectServerStream() {
    if (state.eventSource) {
      try { state.eventSource.close(); } catch (_) {}
      state.eventSource = null;
      state.sseConnected = false;
    }
  }

  function applyServerMetricsPayload(data) {
    if (!data || data.id !== state.selectedServerId) return;
    const idx = state.servers.findIndex((s) => s.id === data.id);
    if (idx >= 0) state.servers[idx] = { ...state.servers[idx], ...data };
    refreshSelectedServerHeader();
    renderServerList();

    const m = data.metrics || {};
    const isRunning = data.status === "RUNNING" || data.status === "STARTING";
    const cpu = isRunning ? Number(m.cpuPercentage || 0) : 0;
    const ramUsed = isRunning ? Number(m.ramUsedMb || 0) : 0;
    const ramMax = Number(m.ramMaxMb || data.allocatedRamMb || 1);
    const tpsNum = typeof m.tps === "number" ? m.tps : 20.0;
    const msptNum = typeof m.mspt === "number" ? m.mspt : 0.0;
    const onlinePlayers = Number(m.onlinePlayerCount || 0);

    $("metric-cpu").textContent = `${cpu.toFixed(1)}%`;
    $("metric-ram").textContent = `${ramUsed} / ${data.allocatedRamMb || ramMax} MB`;
    const tps = tpsNum.toFixed(1);
    if (data.status === "RUNNING") {
      recordPerfSample(data.id, cpu, ramUsed, ramMax, tpsNum, msptNum);
    }
    const mspt = msptNum.toFixed(1);
    $("metric-tps").textContent = isRunning && typeof m.tps === "number" ? `${tps} / ${mspt}ms` : "-- / --";
    $("metric-players").textContent = `${onlinePlayers} Online`;

    const setBar = (id, pct, warnAt, errAt) => {
      const bar = $(id);
      if (!bar) return;
      const clamped = Math.max(0, Math.min(100, pct));
      bar.style.width = `${clamped}%`;
      bar.classList.toggle("error", clamped >= errAt);
      bar.classList.toggle("warning", clamped >= warnAt && clamped < errAt);
    };
    setBar("metric-cpu-bar", cpu, 70, 90);
    setBar("metric-ram-bar", ramMax > 0 ? (ramUsed / ramMax) * 100 : 0, 75, 92);
    setBar("metric-tps-bar", isRunning ? (msptNum / 50) * 100 : 0, 70, 95);
    setBar("metric-players-bar", Math.min(100, (onlinePlayers / 20) * 100), 85, 100);
  }

  function connectServerStream(serverId) {
    disconnectServerStream();
    if (!serverId || typeof EventSource === "undefined") return;
    try {
      const es = new EventSource(`/api/servers/${encodeURIComponent(serverId)}/stream`);
      state.eventSource = es;
      es.addEventListener("open", () => {
        state.sseConnected = true;
      });
      es.addEventListener("console", (evt) => {
        if (state.selectedServerId !== serverId) return;
        try {
          const data = JSON.parse(evt.data);
          updateConsoleOutput(data.lines || []);
        } catch (_) {}
      });
      es.addEventListener("status", (evt) => {
        if (state.selectedServerId !== serverId) return;
        try {
          const data = JSON.parse(evt.data);
          applyServerMetricsPayload(data);
        } catch (_) {}
      });
      es.onerror = () => {
        state.sseConnected = false;
      };
    } catch (_) {
      state.sseConnected = false;
    }
  }

  function showLogin() {
    disconnectServerStream();
    if (state.pollTimer) {
      clearInterval(state.pollTimer);
      state.pollTimer = null;
    }
    state.wasAuthenticated = false;
    state.whatsNewChecked = false;
    state.cloudLinked = false;
    closeDialog("session-expired-modal");
    $("app-view").classList.add("hidden");
    $("login-view").classList.remove("hidden");
  }

  function showApp() {
    state.wasAuthenticated = true;
    $("login-view").classList.add("hidden");
    $("app-view").classList.remove("hidden");
    loadSystemInfo();
    loadServers();
    if (hasPerm("MANAGE_HOST")) {
      loadCloudAccount();
    }
    if (!state.whatsNewChecked) {
      state.whatsNewChecked = true;
      setTimeout(() => loadWhatsNewData(true, false), 900);
    }
    if (!state.pollTimer) {
      state.pollTimer = setInterval(pollActiveData, 2500);
    }
  }

  function hasPerm(perm) {
    const perms = state.currentUser?.permissions || [];
    return perms.includes(perm);
  }

  function applyUserPermissionsUi() {
    const u = state.currentUser;
    const pill = $("current-user-pill");
    if (pill && u) {
      pill.classList.remove("hidden");
      const uname = u.username || "admin";
      $("current-user-name").textContent = uname;
      if ($("current-user-avatar")) {
        $("current-user-avatar").textContent = uname.charAt(0).toUpperCase();
      }
      const roleEl = $("current-user-role");
      roleEl.textContent = u.roleLabel || u.role || "User";
      roleEl.className = `role-badge ${u.role || "VIEWER"}`;
    }
    const canManageHost = hasPerm("MANAGE_HOST");
    const canManageUsers = hasPerm("MANAGE_USERS");
    const canViewAudit = hasPerm("VIEW_AUDIT_LOG");
    const canControl = hasPerm("SERVER_CONTROL");
    const canConsoleWrite = hasPerm("SERVER_CONSOLE_WRITE");
    const canPlayers = hasPerm("SERVER_PLAYERS");
    const canBackups = hasPerm("SERVER_BACKUPS_CREATE");
    const canBackupsManage = hasPerm("SERVER_BACKUPS_MANAGE");
    const canFiles = hasPerm("SERVER_FILES_WRITE");
    const canSettings = hasPerm("SERVER_SETTINGS_WRITE");

    $("open-create-modal-btn")?.classList.toggle("hidden", !canManageHost);
    $("open-import-server-modal-btn")?.classList.toggle("hidden", !canManageHost);
    $("open-cloud-modal-btn")?.classList.toggle("hidden", !canManageHost || !!state.isPlayStore);
    $("empty-create-btn")?.classList.toggle("hidden", !canManageHost);
    $("open-host-tools-btn")?.classList.toggle("hidden", false);
    document.querySelectorAll('[data-host-tab="java"], [data-host-tab="diagnostics"], [data-host-tab="updates"]').forEach((btn) => {
      btn.classList.toggle("hidden", !canManageHost);
    });
    const isDesktop = !!(state.systemInfo && (state.systemInfo.isDesktopHost || ["linux", "macos", "windows"].includes(state.systemInfo.platform)));
    $("host-dirs-group")?.classList.toggle("hidden", !isDesktop || !canManageHost);
    $("safeguards-card")?.classList.toggle("hidden", isDesktop || !canManageHost);
    $("open-users-modal-btn")?.classList.toggle("hidden", !canManageUsers && !canViewAudit);
    $("btn-start")?.classList.toggle("hidden", !canControl);
    $("btn-restart")?.classList.toggle("hidden", !canControl);
    $("btn-stop")?.classList.toggle("hidden", !canControl);
    $("console-form")?.classList.toggle("hidden", !canConsoleWrite);
    $("console-quick-chips")?.classList.toggle("hidden", !canConsoleWrite);
    $("btn-open-network-tab")?.classList.toggle("hidden", !canSettings);
    $("apply-aikar-btn")?.classList.toggle("hidden", !canSettings);
    $("delete-server-btn")?.classList.toggle("hidden", !canManageHost);
    $("cloud-backups-refresh-btn")?.closest(".panel-card")?.classList.toggle("hidden", !canBackupsManage);

    const subTabPerms = {
      plugins: canSettings,
      files: canFiles,
      world: canSettings,
      livemap: true,
      settings: canSettings,
      backups: canBackups,
      automation: canSettings
    };
    document.querySelectorAll(".m3-segmented-btn[data-subtab]").forEach((btn) => {
      const st = btn.dataset.subtab;
      btn.classList.toggle("hidden", !subTabPerms[st]);
    });

    const tabPerms = {
      console: true,
      performance: true,
      players: canPlayers,
      content: subTabPerms.plugins || subTabPerms.files,
      worldmap: subTabPerms.world || subTabPerms.livemap,
      management: subTabPerms.settings || subTabPerms.backups || subTabPerms.automation
    };
    document.querySelectorAll(".tab-btn").forEach((btn) => {
      const tab = btn.dataset.tab;
      btn.classList.toggle("hidden", !tabPerms[tab]);
    });

    if (state.activeTab === "content" && !subTabPerms[state.subTabs.content]) {
      state.subTabs.content = subTabPerms.plugins ? "plugins" : "files";
    }
    if (state.activeTab === "worldmap" && !subTabPerms[state.subTabs.worldmap]) {
      state.subTabs.worldmap = subTabPerms.world ? "world" : "livemap";
    }
    if (state.activeTab === "management" && !subTabPerms[state.subTabs.management]) {
      state.subTabs.management = subTabPerms.settings ? "settings" : (subTabPerms.backups ? "backups" : "automation");
    }
    if (!tabPerms[state.activeTab]) {
      setActiveTab("console");
    }
  }

  async function checkAuth() {
    try {
      const me = await apiFetch("/api/me");
      state.currentUser = me;
      applyUserPermissionsUi();
      showApp();
    } catch (_) {
      state.currentUser = null;
      showLogin();
    }
  }

  function formatRamValue(mb) {
    const val = Number(mb) || 512;
    if (val >= 1024) {
      return `${(val / 1024).toFixed(1)} GB (${val} MB)`;
    }
    return `${val} MB`;
  }

  function formatRamShort(mb) {
    const val = Number(mb) || 512;
    if (val >= 1024) {
      const gb = Math.round((val / 1024) * 10) / 10;
      return `${gb % 1 === 0 ? gb.toFixed(0) : gb.toFixed(1)} GB`;
    }
    return `${val} MB`;
  }

  function getDeviceRamBounds() {
    return state.deviceRam || {
      totalMb: 4096,
      maxAllocatableMb: 3072,
      defaultMb: 2048,
      minMb: 512,
      stepMb: 256
    };
  }

  function configureRamSlider(prefix, targetMb) {
    const slider = $(prefix);
    const display = $(`${prefix}-display`);
    const minEl = $(`${prefix}-min`);
    const maxEl = $(`${prefix}-max`);
    const hintEl = $(`${prefix}-device-hint`);
    if (!slider) return;

    const bounds = getDeviceRamBounds();
    const step = bounds.stepMb || 256;
    const min = bounds.minMb || 512;
    const rawVal = Number(targetMb || slider.value || bounds.defaultMb);
    const snapped = Math.max(min, Math.round(rawVal / step) * step);
    const max = Math.max(bounds.maxAllocatableMb || 3072, snapped);

    slider.min = String(min);
    slider.max = String(max);
    slider.step = String(step);
    slider.value = String(snapped);

    if (display) display.textContent = formatRamValue(snapped);
    if (minEl) minEl.textContent = formatRamShort(min);
    if (maxEl) maxEl.textContent = `${formatRamShort(max)} max`;
    if (hintEl) {
      hintEl.textContent = bounds.totalMb
        ? `Device Total: ${formatRamShort(bounds.totalMb)}`
        : "Device-aware allocation";
    }
  }

  async function loadSystemInfo() {
    try {
      const info = await apiFetch("/api/system");
      state.systemInfo = info;
      state.isPlayStore = !!info.isPlayStore;
      if (info.version && $("sidebar-version-pill")) {
        $("sidebar-version-pill").textContent = `v${info.version}`;
      }
      $("system-info").textContent = (info.isDesktopHost || ["linux", "macos", "windows"].includes(info.platform))
        ? info.device
        : `${info.device} (Android ${info.android})`;
      if (info.serversDir && $("host-dir-servers")) $("host-dir-servers").value = info.serversDir;
      if (info.runtimesDir && $("host-dir-runtimes")) $("host-dir-runtimes").value = info.runtimesDir;
      applyUserPermissionsUi();
      syncCloudUnlinkedCallouts();
      if (info.maxAllocatableRamMb) {
        state.deviceRam = {
          totalMb: Number(info.deviceTotalRamMb) || 4096,
          maxAllocatableMb: Number(info.maxAllocatableRamMb) || 3072,
          defaultMb: Number(info.defaultRamMb) || 2048,
          minMb: Number(info.minRamMb) || 512,
          stepMb: Number(info.ramStepMb) || 256
        };
        configureRamSlider("create-ram", state.deviceRam.defaultMb);
        configureRamSlider("import-srv-ram", state.deviceRam.defaultMb);
        configureRamSlider("set-ram", parseInt($("set-ram")?.value, 10) || state.deviceRam.defaultMb);
      }
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
        <span class="dot ${escapeHtml(s.status)}"></span>
        <div class="server-item-text">
          <div class="server-item-name">${escapeHtml(s.name)}</div>
          <div class="server-item-sub">${escapeHtml(s.type)} ${escapeHtml(s.version)} :${s.port}</div>
        </div>
      </div>
    `).join("");

    container.querySelectorAll(".server-item").forEach((el) => {
      el.addEventListener("click", () => {
        selectServer(el.dataset.id);
        $("sidebar")?.classList.remove("open");
        $("sidebar-backdrop")?.classList.remove("open");
      });
    });
  }

  function refreshSelectedServerHeader() {
    const server = state.servers.find((s) => s.id === state.selectedServerId);
    if (!server) return;
    $("active-server-name").textContent = server.name;
    $("active-server-meta").textContent = `${server.type} ${server.version} / Port ${server.port} / ${server.allocatedRamMb} MB`;
    const badge = $("active-server-badge");
    badge.classList.remove("hidden");
    badge.textContent = server.status;
    badge.className = `status-badge ${server.status}`;

    const running = server.status === "RUNNING" || server.status === "STARTING";
    $("btn-start").disabled = running;
    $("btn-stop").disabled = server.status === "STOPPED";
    $("btn-restart").disabled = server.status === "STOPPED";
    renderConnectionStrip(server.network || {});
  }

  function copyTextToClipboard(text, label) {
    if (!text) return;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(() => {
        showToast(`Copied ${label}: ${text}`);
      }).catch(() => {
        fallbackCopyText(text, label);
      });
    } else {
      fallbackCopyText(text, label);
    }
  }

  function fallbackCopyText(text, label) {
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.style.position = "fixed";
    ta.style.opacity = "0";
    document.body.appendChild(ta);
    ta.select();
    try {
      document.execCommand("copy");
      showToast(`Copied ${label}: ${text}`);
    } catch (_) {
      showToast("Unable to copy to clipboard", "error");
    }
    document.body.removeChild(ta);
  }

  function renderConnectionStrip(net) {
    const strip = $("connection-strip");
    const badge = $("net-public-badge");
    const pubBtn = $("btn-copy-public-addr");
    const pubAddr = $("net-public-addr");
    const note = $("net-public-note");
    const localLabel = $("net-local-label");
    const localAddr = $("net-local-addr");
    const copyLocalBtn = $("btn-copy-local-addr");
    if (!badge || !pubBtn) return;

    strip?.classList.remove("hidden");

    const publicAddress = net.publicAddress || "";
    const tunnelStatus = net.tunnelStatus || "disconnected";
    const pfStatus = net.portForwardStatus || "idle";
    const activeMethod = net.activeMethod || net.tunnelProviderLabel || "";

    pubAddr.textContent = publicAddress;
    pubAddr.classList.toggle("hidden", !publicAddress);
    pubBtn.dataset.addr = publicAddress;

    if (publicAddress) {
      pubBtn.className = "m3-chip m3-chip--tertiary net-chip-btn";
      badge.textContent = `${activeMethod || "Tunnel"}:`;
      if (net.tunnelAddress && net.directAddress && net.tunnelAddress !== net.directAddress) {
        note.textContent = `UPnP also mapped: ${net.directAddress}`;
      } else {
        note.textContent = "";
      }
    } else if (tunnelStatus === "connecting") {
      pubBtn.className = "m3-chip m3-chip--warning net-chip-btn";
      badge.textContent = `${net.tunnelProviderLabel || "Tunnel"}: Connecting`;
      note.textContent = net.tunnelMessage || "Establishing public tunnel...";
    } else if (pfStatus === "discovering") {
      pubBtn.className = "m3-chip m3-chip--warning net-chip-btn";
      badge.textContent = "UPnP / NAT-PMP: Mapping";
      note.textContent = net.portForwardMessage || "Mapping router port...";
    } else if (tunnelStatus === "error") {
      pubBtn.className = "m3-chip m3-chip--error net-chip-btn";
      badge.textContent = `${net.tunnelProviderLabel || "Tunnel"}: Error`;
      note.textContent = net.tunnelMessage || "Tunnel connection failed";
    } else if (pfStatus === "failed") {
      pubBtn.className = "m3-chip m3-chip--error net-chip-btn";
      badge.textContent = "Port Forward: Failed";
      note.textContent = net.portForwardMessage || "Router mapping failed";
    } else {
      pubBtn.className = "m3-chip m3-chip--neutral net-chip-btn";
      badge.textContent = "Tunnel Off";
      if (net.firewallWarning) {
        note.textContent = `Firewall warning: ${net.firewallWarning}`;
      } else {
        note.textContent = "";
      }
    }

    const lan = net.localAddress || "";
    if (localLabel) localLabel.classList.toggle("hidden", !lan);
    if (localAddr) {
      localAddr.textContent = lan;
      localAddr.classList.toggle("hidden", !lan);
    }
    if (copyLocalBtn) {
      copyLocalBtn.classList.toggle("hidden", !lan);
      copyLocalBtn.dataset.addr = lan;
    }
  }

  function selectServer(serverId) {
    state.selectedServerId = serverId;
    state.currentPath = "";
    closeFileEditor();
    renderServerList();

    if (!serverId) {
      disconnectServerStream();
      $("active-server-name").textContent = "Select a Server";
      $("active-server-meta").textContent = "";
      $("active-server-badge")?.classList.add("hidden");
      $("connection-strip")?.classList.add("hidden");
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
    connectServerStream(serverId);
  }

  async function loadServerDetails() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}`);
      applyServerMetricsPayload(data);
    } catch (_) {}
  }

  function loadActiveTabData() {
    if (!state.selectedServerId) return;
    const eff = getEffectiveTab();
    if (eff === "console") loadConsole();
    else if (eff === "performance") loadPerformance();
    else if (eff === "automation") { loadAutomation(); loadDiscordWebhook(); }
    else if (eff === "files") loadFiles(state.currentPath);
    else if (eff === "world") { loadWorld(); loadSettings(); }
    else if (eff === "livemap") loadLiveMap();
    else if (eff === "settings") { loadSettings(); loadUpgradeVersions(); loadNetwork(); loadProxySettings(); }
    else if (eff === "players") loadPlayers();
    else if (eff === "backups") loadBackups();
    else if (eff === "plugins") loadPlugins();
  }

  async function pollActiveData() {
    if (!state.selectedServerId) {
      await loadServers();
      return;
    }
    const eff = getEffectiveTab();
    if (!state.sseConnected) {
      await loadServerDetails();
      if (eff === "console") {
        await loadConsole();
      }
    }
    if (eff === "performance") {
      await loadPerformance();
    } else if (eff === "settings") {
      await loadNetwork(false);
    } else if (eff === "players") {
      await loadPlayers();
    } else if (eff === "world") {
      await loadPregenStatus();
    }
  }

  // Console
  function updateConsoleOutput(lines) {
    const out = $("console-output");
    if (!out) return;
    const countEl = $("console-line-count");
    if (countEl) {
      countEl.textContent = `Live Server Console (${lines.length} lines)`;
    }
    let nextText = lines.join("\n");
    if (!nextText.trim()) {
      const srv = state.servers.find((s) => s.id === state.selectedServerId);
      if (srv && srv.status === "STOPPED") {
        nextText = "Server is ready. Click Start above to launch it.";
      }
    }
    if (out.textContent === nextText) return;
    const prevTop = out.scrollTop;
    const wasAtBottom = out.scrollHeight - out.scrollTop - out.clientHeight < 40;
    out.textContent = nextText;
    if (wasAtBottom) {
      out.scrollTop = out.scrollHeight;
    } else {
      out.scrollTop = prevTop;
    }
  }

  async function loadConsole() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/console?limit=2000`);
      updateConsoleOutput(data.lines || []);
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
        list.innerHTML = `<div class="player-sub" style="padding: 16px;">Empty directory - drag &amp; drop files here to upload</div>`;
        return;
      }
      const iconFolder = `<svg class="ms-icon ms-icon--sm ms-icon--primary" viewBox="0 0 24 24" fill="currentColor"><path d="M4 20q-.825 0-1.412-.587T2 18V6q0-.825.588-1.412T4 4h6l2 2h8q.825 0 1.413.588T22 8v10q0 .825-.587 1.413T20 20z"/></svg>`;
      const iconZip = `<svg class="ms-icon ms-icon--sm ms-icon--tertiary" viewBox="0 0 24 24" fill="currentColor"><path d="M14 14v-2h-2v2zm2-2v-2h-2v2zm-2-2V8h-2v2zm-10 10q-.825 0-1.412-.587T2 18V6q0-.825.588-1.412T4 4h6l2 2h8q.825 0 1.413.588T22 8v10q0 .825-.587 1.413T20 20z"/></svg>`;
      const iconFile = `<svg class="ms-icon ms-icon--sm" viewBox="0 0 24 24" fill="currentColor"><path d="M6 22q-.825 0-1.412-.587T4 20V4q0-.825.588-1.412T6 2h8l6 6v12q0 .825-.587 1.413T18 22zm7-13h5l-5-5z"/></svg>`;
      const iconDownload = `<svg class="ms-icon ms-icon--sm" viewBox="0 0 24 24" fill="currentColor"><path d="m12 16l-5-5l1.4-1.45l2.6 2.6V4h2v8.15l2.6-2.6L17 11zm-6 4q-.825 0-1.412-.587T4 18v-3h2v3h12v-3h2v3q0 .825-.587 1.413T18 20z"/></svg>`;
      const iconExtract = `<svg class="ms-icon ms-icon--sm" viewBox="0 0 24 24" fill="currentColor"><path d="M11 16V7.85l-2.6 2.6L7 9l5-5l5 5l-1.4 1.45l-2.6-2.6V16zm-5 4q-.825 0-1.412-.587T4 18v-3h2v3h12v-3h2v3q0 .825-.587 1.413T18 20z"/></svg>`;
      const iconEdit = `<svg class="ms-icon ms-icon--sm" viewBox="0 0 24 24" fill="currentColor"><path d="M3 21v-4.25L16.2 3.575q.3-.275.663-.425t.762-.15t.775.15t.65.45L20.425 5q.3.275.438.65T21 6.4q0 .4-.137.763t-.438.662L7.25 21zM17.6 7.8L19 6.4L17.6 5l-1.4 1.4z"/></svg>`;
      const iconDelete = `<svg class="ms-icon ms-icon--sm ms-icon--error" viewBox="0 0 24 24" fill="currentColor"><path d="M7 21q-.825 0-1.412-.587T5 19V6H4V4h5V3h6v1h5v2h-1v13q0 .825-.587 1.413T17 21zm2-4h2V8H9zm4 0h2V8h-2z"/></svg>`;

      list.innerHTML = files.map((f) => {
        const isZip = !f.isDirectory && f.name.toLowerCase().endsWith(".zip");
        return `
        <div class="file-row">
          <button class="file-name-btn" data-path="${escapeHtml(f.relativePath)}" data-dir="${f.isDirectory}" data-editable="${f.isEditable}">
            ${f.isDirectory ? iconFolder : (isZip ? iconZip : iconFile)}
            <span>${escapeHtml(f.name)}</span>
          </button>
          <div class="file-meta">
            <span>${f.isDirectory ? "Folder" : formatSize(f.sizeBytes)}</span>
            ${!f.isDirectory ? `<a class="btn-icon" href="/api/servers/${encodeURIComponent(state.selectedServerId)}/files/download?path=${encodeURIComponent(f.relativePath)}" download title="Download">${iconDownload}</a>` : ""}
            ${isZip ? `<button type="button" class="btn-icon file-unzip-btn" data-path="${escapeHtml(f.relativePath)}" title="Unzip Archive">${iconExtract}</button>` : ""}
            <button type="button" class="btn-icon file-rename-btn" data-path="${escapeHtml(f.relativePath)}" data-name="${escapeHtml(f.name)}" title="Rename">${iconEdit}</button>
            <button type="button" class="btn-icon file-del-btn" data-path="${escapeHtml(f.relativePath)}" title="Delete">${iconDelete}</button>
          </div>
        </div>
      `;
      }).join("");

      list.querySelectorAll(".file-name-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
          const rel = btn.dataset.path;
          if (btn.dataset.dir === "true") {
            loadFiles(rel);
          } else if (btn.dataset.editable === "true") {
            openFileEditor(rel);
          } else {
            showToast("Binary or large file cannot be edited in browser (use Download)", "error");
          }
        });
      });

      list.querySelectorAll(".file-rename-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const rel = btn.dataset.path;
          const oldName = btn.dataset.name;
          const newName = await showPromptDialog({
            title: "Rename Item",
            message: `Rename "${oldName}" to:`,
            defaultValue: oldName,
            confirmText: "Rename"
          });
          if (!newName || newName.trim() === oldName) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/files/rename`, {
              method: "POST",
              body: JSON.stringify({ path: rel, newName: newName.trim() })
            });
            showToast("Renamed");
            loadFiles(state.currentPath);
          } catch (e) {
            showToast(e.message, "error");
          }
        });
      });

      list.querySelectorAll(".file-unzip-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const rel = btn.dataset.path;
          const ok = await showConfirmDialog({
            title: "Extract Archive",
            message: `Extract "${rel}" into the current folder?`,
            confirmText: "Extract"
          });
          if (!ok) return;
          try {
            const res = await apiFetch(`/api/servers/${state.selectedServerId}/files/unzip`, {
              method: "POST",
              body: JSON.stringify({ path: rel, deleteZipAfter: false })
            });
            showToast(`Extracted ${res.extractedCount || 0} file(s)`);
            loadFiles(state.currentPath);
          } catch (e) {
            showToast(e.message, "error");
          }
        });
      });

      list.querySelectorAll(".file-del-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const rel = btn.dataset.path;
          const ok = await showConfirmDialog({
            title: "Delete File",
            message: `Permanently delete "${rel}"?`,
            confirmText: "Delete",
            destructive: true
          });
          if (!ok) return;
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
    $("file-editor-pane")?.classList.add("hidden");
    document.querySelector(".files-layout")?.classList.remove("editing");
  }

  // Settings & Properties
  async function loadSettings() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/settings`);
      state.cachedSettings = data;
      const srv = data.server || {};
      const props = data.properties || {};

      $("set-name").value = srv.name || "";
      configureRamSlider("set-ram", srv.allocatedRamMb || getDeviceRamBounds().defaultMb);
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

      if ($("world-seed-input")) $("world-seed-input").value = props.levelSeed || "";
      if ($("world-difficulty")) $("world-difficulty").value = props.difficulty || "normal";
      if ($("world-gamemode")) $("world-gamemode").value = props.gamemode || "survival";
      if ($("world-allow-nether")) $("world-allow-nether").checked = !!props.allowNether;
      if ($("world-spawn-monsters")) $("world-spawn-monsters").checked = !!props.spawnMonsters;
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  // Players
  async function loadPlayers() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/players`);
      state.playersData = data;
      renderPlayersRoster();
    } catch (_) {}
  }

  function renderPlayersRoster() {
    const table = $("players-roster-table");
    const data = state.playersData || {};
    const online = data.onlinePlayers || [];
    const ops = data.ops || [];
    const whitelist = data.whitelist || [];
    const banned = data.bannedPlayers || [];
    const bannedIps = data.bannedIps || [];

    if ($("players-count-online")) $("players-count-online").textContent = String(online.length);
    if ($("players-count-ops")) $("players-count-ops").textContent = String(ops.length);
    if ($("players-count-whitelist")) $("players-count-whitelist").textContent = String(whitelist.length);
    if ($("players-count-banned")) $("players-count-banned").textContent = String(banned.length);
    if ($("players-count-bannedips")) $("players-count-bannedips").textContent = String(bannedIps.length);

    if (!table) return;
    const filter = state.playersFilter || "online";
    const q = (state.playersSearch || "").trim().toLowerCase();
    const opSet = new Set(ops.map((o) => String(o.name || "").toLowerCase()));

    let rows = [];
    if (filter === "online") {
      rows = online.map((name) => ({
        title: name,
        sub: opSet.has(String(name).toLowerCase()) ? "Online - Operator" : "Online Player",
        isOp: opSet.has(String(name).toLowerCase()),
        kind: "online"
      }));
    } else if (filter === "ops") {
      rows = ops.map((op) => ({
        title: op.name,
        sub: `OP Level ${op.level || 4}${op.uuid ? ` | ${op.uuid}` : ""}`,
        isOp: true,
        kind: "ops"
      }));
    } else if (filter === "whitelist") {
      rows = whitelist.map((w) => ({
        title: w.name,
        sub: w.uuid || "Whitelisted",
        isOp: opSet.has(String(w.name || "").toLowerCase()),
        kind: "whitelist"
      }));
    } else if (filter === "banned") {
      rows = banned.map((b) => ({
        title: b.name,
        sub: `${b.reason || "Banned"}${b.uuid ? ` | ${b.uuid}` : ""}`,
        isOp: false,
        kind: "banned"
      }));
    } else if (filter === "bannedIps") {
      rows = bannedIps.map((b) => ({
        title: b.ip,
        sub: b.reason || "Banned IP",
        isOp: false,
        kind: "bannedIps"
      }));
    }

    if (q) {
      rows = rows.filter((r) => `${r.title} ${r.sub}`.toLowerCase().includes(q));
    }

    if (rows.length === 0) {
      table.innerHTML = `<div class="empty-state" style="padding:28px 16px;"><h3>No Matching Entries</h3><p style="margin-bottom:0;">Players in this category will appear here.</p></div>`;
      return;
    }

    table.innerHTML = rows.map((r) => {
      const initial = escapeHtml(String(r.title || "?").charAt(0).toUpperCase());
      let actionsHtml = "";
      if (r.kind === "online") {
        actionsHtml = `
          ${r.isOp
            ? `<button type="button" class="btn btn-secondary btn-sm roster-deop-btn" data-name="${escapeHtml(r.title)}">De-OP</button>`
            : `<button type="button" class="btn btn-secondary btn-sm roster-op-btn" data-name="${escapeHtml(r.title)}">OP</button>`}
          <button type="button" class="btn btn-ghost btn-sm roster-kick-btn" data-name="${escapeHtml(r.title)}">Kick</button>
          <button type="button" class="btn btn-danger btn-sm roster-ban-btn" data-name="${escapeHtml(r.title)}">Ban</button>
        `;
      } else if (r.kind === "ops") {
        actionsHtml = `<button type="button" class="btn btn-ghost btn-sm roster-deop-btn" data-name="${escapeHtml(r.title)}">De-OP</button>`;
      } else if (r.kind === "whitelist") {
        actionsHtml = `<button type="button" class="btn btn-ghost btn-sm roster-wl-rm-btn" data-name="${escapeHtml(r.title)}">Remove</button>`;
      } else if (r.kind === "banned") {
        actionsHtml = `<button type="button" class="btn btn-secondary btn-sm roster-pardon-btn" data-name="${escapeHtml(r.title)}">Pardon</button>`;
      } else if (r.kind === "bannedIps") {
        actionsHtml = `<button type="button" class="btn btn-secondary btn-sm roster-pardonip-btn" data-ip="${escapeHtml(r.title)}">Pardon IP</button>`;
      }
      return `
        <div class="m3-table-row">
          <div style="display:flex;align-items:center;gap:12px;min-width:0;flex:1;">
            <span class="player-avatar-badge ${r.isOp ? "player-avatar-badge--op" : ""}">${initial}</span>
            <div style="min-width:0;">
              <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                <strong class="${r.kind === "bannedIps" ? "mono" : ""}">${escapeHtml(r.title)}</strong>
                ${r.isOp ? `<span class="m3-chip m3-chip--warning">OP</span>` : ""}
              </div>
              <div class="player-sub mono" style="word-break:break-all;">${escapeHtml(r.sub)}</div>
            </div>
          </div>
          <div class="player-actions">${actionsHtml}</div>
        </div>
      `;
    }).join("");

    table.querySelectorAll(".roster-op-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ops", { action: "add", name: b.dataset.name, level: 4 }));
    });
    table.querySelectorAll(".roster-deop-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        const ok = await showConfirmDialog({ title: "Remove Operator", message: `Remove OP status from ${b.dataset.name}?`, confirmText: "De-OP" });
        if (ok) playerListAction("ops", { action: "remove", name: b.dataset.name });
      });
    });
    table.querySelectorAll(".roster-wl-rm-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("whitelist", { action: "remove", name: b.dataset.name }));
    });
    table.querySelectorAll(".roster-kick-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        const ok = await showConfirmDialog({ title: "Kick Player", message: `Kick ${b.dataset.name} from the server?`, confirmText: "Kick", destructive: true });
        if (ok) playerListAction("kick", { name: b.dataset.name, reason: "Kicked via Web Admin" });
      });
    });
    table.querySelectorAll(".roster-ban-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        const ok = await showConfirmDialog({ title: "Ban Player", message: `Permanently ban ${b.dataset.name} from this server?`, confirmText: "Ban", destructive: true });
        if (ok) playerListAction("ban", { action: "add", name: b.dataset.name, reason: "Banned via Web Admin" });
      });
    });
    table.querySelectorAll(".roster-pardon-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ban", { action: "remove", name: b.dataset.name }));
    });
    table.querySelectorAll(".roster-pardonip-btn").forEach((b) => {
      b.addEventListener("click", () => playerListAction("ban-ip", { action: "remove", ip: b.dataset.ip }));
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

  function drawPerfCanvas(canvas, hist, emptyMsg, seriesList) {
    if (!canvas) return;
    const rect = canvas.parentElement.getBoundingClientRect();
    const width = Math.max(280, Math.floor(rect.width || 520));
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

    const gridColor = getCssToken("--ms-chart-grid");
    const mutedColor = getCssToken("--md-sys-color-on-surface-variant");
    const monoFont = getCssToken("--md-sys-typescale-font-mono");
    const sansFont = getCssToken("--md-sys-typescale-font-sans");

    ctx.font = `11px ${monoFont}`;
    ctx.textAlign = "right";
    ctx.textBaseline = "middle";
    for (let pct = 0; pct <= 100; pct += 25) {
      const y = padT + plotH - (pct / 100) * plotH;
      ctx.strokeStyle = gridColor;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(padL, y);
      ctx.lineTo(padL + plotW, y);
      ctx.stroke();

      ctx.fillStyle = mutedColor;
      ctx.fillText(`${pct}%`, padL - 6, y);
    }

    if (hist.length === 0) {
      ctx.fillStyle = mutedColor;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.font = `13px ${sansFont}`;
      ctx.fillText(emptyMsg, width / 2, height / 2);
      return;
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

      if (fillAlpha && values.length > 1) {
        const lastX = padL + plotW;
        ctx.lineTo(lastX, padT + plotH);
        ctx.lineTo(padL, padT + plotH);
        ctx.closePath();
        ctx.fillStyle = fillAlpha;
        ctx.fill();
      }
      ctx.restore();
    };

    seriesList.forEach((s) => drawSeries(s.values, s.color, s.fill));

    const fmtTime = (ts) => new Date(ts).toLocaleTimeString([], { hour12: false, minute: "2-digit", second: "2-digit" });
    const latest = hist[hist.length - 1];
    ctx.fillStyle = mutedColor;
    ctx.font = `11px ${monoFont}`;
    ctx.textBaseline = "top";
    ctx.textAlign = "left";
    ctx.fillText(fmtTime(hist[0].ts), padL, padT + plotH + 6);
    ctx.textAlign = "right";
    ctx.fillText(fmtTime(latest.ts), padL + plotW, padT + plotH + 6);
  }

  function renderPerfChart() {
    if (!state.selectedServerId) return;
    const hist = state.perfHistory[state.selectedServerId] || [];
    const summaryEl = $("perf-chart-summary");
    const memSummaryEl = $("perf-mem-summary");

    const primaryColor = getCssToken("--md-sys-color-primary");
    const primaryFill = getCssToken("--ms-chart-primary-fill");
    const tertiaryColor = getCssToken("--md-sys-color-tertiary");
    const tertiaryFill = getCssToken("--ms-chart-tertiary-fill");
    const warningColor = getCssToken("--md-sys-color-warning");

    if (hist.length === 0) {
      if (summaryEl) summaryEl.textContent = "Waiting for running server samples...";
      if (memSummaryEl) memSummaryEl.textContent = "Waiting for running server samples...";
    } else {
      const latest = hist[hist.length - 1];
      if (summaryEl) {
        summaryEl.textContent = `${hist.length} samples | CPU ${latest.cpu.toFixed(1)}% | TPS ${latest.tps.toFixed(1)} | MSPT ${latest.mspt.toFixed(1)} ms`;
      }
      if (memSummaryEl) {
        memSummaryEl.textContent = `${latest.ramUsed} / ${latest.ramMax} MB (${latest.ramPct.toFixed(0)}%)`;
      }
    }

    drawPerfCanvas($("perf-chart-cpu-canvas"), hist, "Start the server to record CPU and TPS.", [
      { values: hist.map((h) => h.cpu), color: primaryColor, fill: primaryFill },
      { values: hist.map((h) => (h.tps / 20) * 100), color: warningColor, fill: null }
    ]);

    drawPerfCanvas($("perf-chart-mem-canvas"), hist, "Start the server to record Memory usage.", [
      { values: hist.map((h) => h.ramPct), color: tertiaryColor, fill: tertiaryFill }
    ]);
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
        list.innerHTML = `<div class="player-sub" style="padding:14px;">No scheduled tasks configured.</div>`;
        return;
      }
      list.innerHTML = tasks.map((t) => `
        <div class="m3-table-row">
          <div>
            <div><strong>${escapeHtml(t.name)}</strong> <span class="m3-chip m3-chip--neutral">${escapeHtml(t.taskTypeDisplay)}</span></div>
            <div class="player-sub mono" style="margin-top:2px;">${escapeHtml(t.frequencyDescription)}</div>
          </div>
          <div class="player-actions">
            <button class="btn btn-secondary btn-sm task-toggle-btn" data-id="${escapeHtml(t.id)}">${t.enabled ? "Disable" : "Enable"}</button>
            <button class="btn btn-danger btn-sm task-del-btn" data-id="${escapeHtml(t.id)}">Delete</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".task-toggle-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/automation/tasks/toggle`, {
              method: "POST", body: JSON.stringify({ taskId: b.dataset.id })
            });
            loadAutomation();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
      list.querySelectorAll(".task-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/automation/tasks/delete`, {
              method: "POST", body: JSON.stringify({ taskId: b.dataset.id })
            });
            loadAutomation();
          } catch (err) {
            showToast(err.message, "error");
          }
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
        <div class="m3-card" style="background:var(--md-sys-color-surface-container-low);display:flex;flex-direction:column;justify-content:space-between;gap:10px;">
          <div>
            <div style="font-size:15px;font-weight:700;">${escapeHtml(d.displayName)}</div>
            <div class="dimension-size">${d.exists ? formatSize(d.sizeBytes) : "0 B"}</div>
            <div class="player-sub mono">${d.exists ? `${d.regionFileCount} regions | ${d.estimatedChunks} chunks` : "Not generated yet"}</div>
          </div>
          <button class="btn btn-danger btn-sm btn-full dim-reset-btn" data-dim="${escapeHtml(d.type)}" ${d.exists ? "" : "disabled"}>Reset ${escapeHtml(d.displayName)}</button>
        </div>
      `).join("");

      list.querySelectorAll(".dim-reset-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          const ok = await showConfirmDialog({
            title: "Reset Dimension",
            message: `Reset ${b.dataset.dim}? A world backup will be created automatically first.`,
            confirmText: "Reset Dimension",
            destructive: true
          });
          if (!ok) return;
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
    loadPregenStatus();
    loadRegions();
  }

  async function loadPregenStatus() {
    if (!state.selectedServerId) return;
    try {
      const p = await apiFetch(`/api/servers/${state.selectedServerId}/world/pregen`);
      $("pregen-status-text").textContent = p.isRunning ? `${p.statusMessage} (${p.percent}%)` : (p.statusMessage || "Idle");
      $("pregen-start-btn").classList.toggle("hidden", !!p.isRunning);
      $("pregen-stop-btn").classList.toggle("hidden", !p.isRunning);
    } catch (_) {}
  }

  async function loadRegions() {
    if (!state.selectedServerId) return;
    const list = $("regions-list");
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/world/regions`);
      const regions = data.regions || [];
      if (regions.length === 0) {
        list.innerHTML = `<div class="player-sub" style="padding:14px;">No .mca region files found.</div>`;
        return;
      }
      list.innerHTML = regions.map((r) => `
        <div class="m3-table-row">
          <div>
            <div class="mono"><strong>${escapeHtml(r.fileName)}</strong></div>
            <div class="player-sub mono">${escapeHtml(r.dimensionName)} | ${r.chunkCount}/1024 chunks | ${formatSize(r.sizeBytes)}</div>
          </div>
          <button class="btn btn-danger btn-sm region-del-btn" data-dim="${escapeHtml(r.dimension)}" data-file="${escapeHtml(r.fileName)}">Delete</button>
        </div>
      `).join("");
      list.querySelectorAll(".region-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          const ok = await showConfirmDialog({
            title: "Delete Region File",
            message: `Delete ${b.dataset.file}? Stop the server first.`,
            confirmText: "Delete",
            destructive: true
          });
          if (!ok) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/world/regions/delete`, {
              method: "POST", body: JSON.stringify({ dimension: b.dataset.dim, fileName: b.dataset.file })
            });
            showToast(`Deleted ${b.dataset.file}`);
            loadWorld();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
    } catch (err) {
      list.innerHTML = `<div class="player-sub">${escapeHtml(err.message)}</div>`;
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

      const canEditMap = hasPerm("SERVER_SETTINGS_WRITE");
      installBtn.classList.toggle("hidden", !canEditMap || m.installed || !m.supportsPluginsOrMods);
      uninstallBtn.classList.toggle("hidden", !canEditMap || !m.installed);
      portRow.classList.toggle("hidden", !canEditMap || !m.installed);
      if (m.installed && document.activeElement !== portInput) {
        portInput.value = m.port;
      }

      const showMap = m.installed && m.isServerRunning && !m.portConflict;
      openBtn.classList.toggle("hidden", !showMap);
      const emptyBox = $("livemap-empty-state");
      const emptyDesc = $("livemap-empty-desc");

      if (!m.installed) {
        const msg = m.supportsPluginsOrMods
          ? "Squaremap is not installed on this server."
          : "This server software does not support Squaremap.";
        $("livemap-status-text").textContent = msg;
        if (emptyDesc) emptyDesc.textContent = msg;
        emptyBox?.classList.remove("hidden");
        conflictBanner.classList.add("hidden");
        frame.classList.add("hidden");
        frame.src = "about:blank";
      } else if (m.portConflict) {
        const suggested = m.suggestedPort || 8081;
        $("livemap-status-text").textContent = `Port conflict: Squaremap is on port ${m.port}, which is used by Web Admin.`;
        emptyBox?.classList.add("hidden");
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
        const msg = `${m.pluginName} is installed (port ${m.port}). Start the server to view the live map.`;
        $("livemap-status-text").textContent = msg;
        if (emptyDesc) emptyDesc.textContent = msg;
        emptyBox?.classList.remove("hidden");
        frame.classList.add("hidden");
        frame.src = "about:blank";
      } else {
        conflictBanner.classList.add("hidden");
        emptyBox?.classList.add("hidden");
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
  function syncCloudUnlinkedCallouts() {
    if (state.isPlayStore) {
      $("tunnel-cloud-unlinked-callout")?.classList.add("hidden");
      $("backups-cloud-unlinked-callout")?.classList.add("hidden");
      $("open-cloud-modal-btn")?.classList.add("hidden");
      return;
    }
    const isCloudProvider = $("net-tunnel-provider")?.value === "MINESERVE_CLOUD";
    $("tunnel-cloud-unlinked-callout")?.classList.toggle("hidden", !isCloudProvider || !!state.cloudLinked);
    $("backups-cloud-unlinked-callout")?.classList.toggle("hidden", !!state.cloudLinked);
  }

  function updateTunnelProviderFields() {
    const provider = $("net-tunnel-provider").value;
    document.querySelectorAll("[data-tunnel-provider]").forEach((el) => {
      el.classList.toggle("hidden", el.dataset.tunnelProvider !== provider);
    });
    syncCloudUnlinkedCallouts();
  }

  async function loadNetwork(syncForm = true) {
    if (!state.selectedServerId) return;
    try {
      const n = await apiFetch(`/api/servers/${state.selectedServerId}/network`);
      const tc = n.tunnelConfig || {};
      const ts = n.tunnelState || {};
      if (syncForm) {
        $("net-tunnel-provider").value = tc.provider || "BORE";
        $("net-bore-host").value = tc.customRelayHost || "bore.pub";
        $("net-bore-port").value = tc.customRelayPort || 7835;
        $("net-cloud-subdomain").value = tc.cloudSubdomain || "";
        $("net-cloud-region").value = tc.cloudRegion || "us-east";
        $("net-cloud-domain").value = tc.cloudDomain || "play.mineserve.app";
        $("net-tunnel-autostart").checked = !!tc.autoStart;
        updateTunnelProviderFields();
      }

      if (ts.status === "connected") {
        $("tunnel-status-text").textContent = ts.panelUrl
          ? `Connected: ${ts.fullAddress} | Panel: ${ts.panelUrl}`
          : `Connected: ${ts.fullAddress}`;
      } else if (ts.status === "connecting") {
        $("tunnel-status-text").textContent = ts.claimUrl ? `Connecting (Claim: ${ts.claimUrl})` : (ts.message || "Connecting...");
      } else if (ts.status === "error") {
        $("tunnel-status-text").textContent = `Error: ${ts.errorMessage}`;
      } else {
        $("tunnel-status-text").textContent = "Disconnected";
      }

      const pc = n.portForwardConfig || {};
      const ps = n.portForwardState || {};
      if (syncForm) {
        $("net-pf-protocol").value = pc.protocol || "BOTH";
        $("net-pf-lease").value = pc.leaseDurationSeconds || 0;
        $("net-pf-auto").checked = !!pc.autoForward;
      }

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
        list.innerHTML = `<div class="player-sub" style="padding:14px;">No backups yet.</div>`;
      } else {
        const canManageBackups = hasPerm("SERVER_BACKUPS_MANAGE");
        list.innerHTML = backups.map((b) => `
        <div class="m3-table-row">
          <div>
            <div class="mono"><strong>${escapeHtml(b.fileName)}</strong></div>
            <div class="player-sub mono">${b.isWorldOnly ? "World Only" : "Full Server"} | ${formatSize(b.sizeBytes)} | ${new Date(b.timestamp).toLocaleString()}</div>
          </div>
          <div class="player-actions">
            <a class="btn btn-secondary btn-sm" href="/api/servers/${state.selectedServerId}/backups/download?file=${encodeURIComponent(b.fileName)}" download>Download</a>
            ${canManageBackups ? `
              <button class="btn btn-secondary btn-sm backup-cloud-upload-btn" data-file="${escapeHtml(b.fileName)}">Cloud Sync</button>
              <button class="btn btn-ghost btn-sm backup-restore-btn" data-file="${escapeHtml(b.fileName)}">Restore</button>
              <button class="btn btn-danger btn-sm backup-del-btn" data-file="${escapeHtml(b.fileName)}">Delete</button>
            ` : ""}
          </div>
        </div>
      `).join("");

        list.querySelectorAll(".backup-cloud-upload-btn").forEach((b) => {
          b.addEventListener("click", async () => {
            b.disabled = true;
            b.textContent = "Uploading...";
            try {
              await apiFetch(`/api/servers/${state.selectedServerId}/backups/cloud/upload`, {
                method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
              });
              showToast("Uploaded to MineServe Cloud");
              loadCloudBackups();
            } catch (err) {
              showToast(err.message, "error");
            } finally {
              b.disabled = false;
              b.textContent = "Cloud Sync";
            }
          });
        });

        list.querySelectorAll(".backup-restore-btn").forEach((b) => {
          b.addEventListener("click", async () => {
            const ok = await showConfirmDialog({
              title: "Restore Backup",
              message: `Restore "${b.dataset.file}"? Stop the server first.`,
              confirmText: "Restore"
            });
            if (!ok) return;
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
            const ok = await showConfirmDialog({
              title: "Delete Backup",
              message: `Delete backup "${b.dataset.file}"?`,
              confirmText: "Delete",
              destructive: true
            });
            if (!ok) return;
            try {
              await apiFetch(`/api/servers/${state.selectedServerId}/backups/delete`, {
                method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
              });
              showToast("Backup deleted");
              loadBackups();
            } catch (err) {
              showToast(err.message, "error");
            }
          });
        });
      }
    } catch (e) {
      showToast(e.message, "error");
    } finally {
      if (hasPerm("SERVER_BACKUPS_MANAGE")) {
        loadCloudBackups();
      }
    }
  }

  async function loadCloudBackups() {
    if (!state.selectedServerId || !hasPerm("SERVER_BACKUPS_MANAGE")) return;
    const list = $("cloud-backups-list");
    const statusEl = $("cloud-backups-status");
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/backups/cloud`);
      const items = data.backups || [];
      statusEl.textContent = "Remote encrypted snapshots stored in MineServe Cloud (R2).";
      if (items.length === 0) {
        list.innerHTML = `<div class="player-sub" style="padding:14px;">No cloud snapshots for this server yet.</div>`;
        return;
      }
      list.innerHTML = items.map((b) => `
        <div class="m3-table-row">
          <div>
            <div class="mono"><strong>${escapeHtml(b.fileName)}</strong></div>
            <div class="player-sub mono">${formatSize(b.sizeBytes)} | ${b.createdAtEpochMs ? new Date(b.createdAtEpochMs).toLocaleString() : b.status}</div>
          </div>
          <div class="player-actions">
            <button class="btn btn-secondary btn-sm cloud-restore-btn" data-id="${escapeHtml(b.backupId)}" data-file="${escapeHtml(b.fileName)}">Restore</button>
            <button class="btn btn-danger btn-sm cloud-del-btn" data-id="${escapeHtml(b.backupId)}">Delete</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll(".cloud-restore-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          const ok = await showConfirmDialog({
            title: "Restore Cloud Snapshot",
            message: `Download and restore cloud snapshot "${b.dataset.file}"? Stop the server first.`,
            confirmText: "Restore"
          });
          if (!ok) return;
          b.disabled = true;
          b.textContent = "Restoring...";
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/backups/cloud/restore`, {
              method: "POST", body: JSON.stringify({ backupId: b.dataset.id, fileName: b.dataset.file })
            });
            showToast("Cloud snapshot restored");
          } catch (err) {
            showToast(err.message, "error");
          } finally {
            b.disabled = false;
            b.textContent = "Restore";
          }
        });
      });

      list.querySelectorAll(".cloud-del-btn").forEach((b) => {
        b.addEventListener("click", async () => {
          const ok = await showConfirmDialog({
            title: "Delete Cloud Snapshot",
            message: "Delete this cloud snapshot from MineServe Cloud?",
            confirmText: "Delete",
            destructive: true
          });
          if (!ok) return;
          try {
            await apiFetch(`/api/servers/${state.selectedServerId}/backups/cloud/delete`, {
              method: "POST", body: JSON.stringify({ backupId: b.dataset.id })
            });
            showToast("Cloud snapshot deleted");
            loadCloudBackups();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
    } catch (err) {
      statusEl.textContent = err.message || "Sign in to MineServe Cloud in the Android app to view remote snapshots.";
      list.innerHTML = `<div class="player-sub">MineServe Cloud snapshots unavailable.</div>`;
    }
  }

  // Plugins / Mods
  async function loadPlugins() {
    if (!state.selectedServerId) return;
    try {
      const data = await apiFetch(`/api/servers/${state.selectedServerId}/plugins`);
      state.installedPlugins = data.items || [];
      renderInstalledPlugins();
    } catch (e) {
      showToast(e.message, "error");
    }
  }

  function renderInstalledPlugins() {
    const list = $("plugins-installed-list");
    if (!list) return;
    const q = (state.pluginsFilter || "").trim().toLowerCase();
    const items = (state.installedPlugins || []).filter((p) => {
      if (!q) return true;
      return `${p.name} ${p.fileName}`.toLowerCase().includes(q);
    });
    if (items.length === 0) {
      list.innerHTML = `<div class="player-sub" style="padding:14px;">${q ? "No matching plugins or mods." : "No plugins or mods installed."}</div>`;
      return;
    }
    list.innerHTML = items.map((p) => `
      <div class="m3-table-row">
        <div style="min-width:0;flex:1;">
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
            <strong>${escapeHtml(p.name)}</strong>
            <span class="m3-chip ${p.enabled ? "m3-chip--primary" : "m3-chip--neutral"}">${p.enabled ? "Enabled" : "Disabled"}</span>
          </div>
          <div class="player-sub mono" style="margin-top:2px;word-break:break-all;">${escapeHtml(p.fileName)} | ${formatSize(p.sizeBytes)}</div>
        </div>
        <div class="player-actions">
          <button class="btn btn-secondary btn-sm plug-toggle-btn" data-file="${escapeHtml(p.fileName)}">${p.enabled ? "Disable" : "Enable"}</button>
          <button class="btn btn-danger btn-sm plug-del-btn" data-file="${escapeHtml(p.fileName)}">Delete</button>
        </div>
      </div>
    `).join("");

    list.querySelectorAll(".plug-toggle-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        try {
          await apiFetch(`/api/servers/${state.selectedServerId}/plugins/toggle`, {
            method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
          });
          loadPlugins();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
    list.querySelectorAll(".plug-del-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        const ok = await showConfirmDialog({
          title: "Delete Add-on",
          message: `Delete "${b.dataset.file}" from this server?`,
          confirmText: "Delete",
          destructive: true
        });
        if (!ok) return;
        try {
          await apiFetch(`/api/servers/${state.selectedServerId}/plugins/delete`, {
            method: "POST", body: JSON.stringify({ fileName: b.dataset.file })
          });
          loadPlugins();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
  }

  // Create Server 2-Step Wizard Modal
  function setCreateStep(step) {
    const isStep2 = step === 2;
    $("create-step-1")?.classList.toggle("hidden", isStep2);
    $("create-step-2")?.classList.toggle("hidden", !isStep2);
    $("create-step-ind-1")?.classList.toggle("active", !isStep2);
    $("create-step-ind-2")?.classList.toggle("active", isStep2);
  }

  function renderSoftwareCards(selectedType) {
    const grid = $("create-software-grid");
    if (!grid) return;
    grid.innerHTML = (state.softwareTypes || []).map((t) => `
      <button type="button" class="software-tile ${t.name === selectedType ? "active" : ""}" data-sw-type="${escapeHtml(t.name)}">
        <span class="software-tile-title">${escapeHtml(t.displayName)}</span>
        <span class="software-tile-desc">${escapeHtml(t.description || t.category || "Server Software")}</span>
      </button>
    `).join("");
    grid.querySelectorAll(".software-tile").forEach((tile) => {
      tile.addEventListener("click", () => {
        const type = tile.dataset.swType;
        $("create-type").value = type;
        grid.querySelectorAll(".software-tile").forEach((el) => el.classList.toggle("active", el === tile));
        loadVersionsForType(type);
      });
    });
  }

  async function openCreateModal() {
    $("create-error").classList.add("hidden");
    $("create-progress-box").classList.add("hidden");
    $("submit-create-btn").disabled = false;
    $("create-eula").checked = false;
    setCreateStep(1);

    const usedPorts = new Set(state.servers.map((s) => s.port));
    let nextPort = 25565;
    while (usedPorts.has(nextPort)) nextPort++;
    $("create-port").value = nextPort;

    openDialog("create-modal");

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
    const currentType = $("create-type").value || state.softwareTypes[0]?.name || "PAPER";
    $("create-type").value = currentType;
    renderSoftwareCards(currentType);
    await loadVersionsForType(currentType);
  }

  async function loadVersionsForType(type) {
    const sel = $("create-version");
    const quick = $("create-quick-versions");
    sel.innerHTML = `<option value="">Loading versions...</option>`;
    if (quick) quick.innerHTML = "";
    sel.disabled = true;
    try {
      const data = await apiFetch(`/api/software/versions?type=${encodeURIComponent(type)}`);
      const versions = data.versions || [];
      sel.innerHTML = versions.map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join("");
      if (quick && versions.length > 0) {
        const top = versions.slice(0, 5);
        quick.innerHTML = top.map((v, idx) =>
          `<button type="button" class="quick-version-chip ${idx === 0 ? "active" : ""}" data-ver="${escapeHtml(v)}">${escapeHtml(v)}</button>`
        ).join("");
        quick.querySelectorAll(".quick-version-chip").forEach((chip) => {
          chip.addEventListener("click", () => {
            sel.value = chip.dataset.ver;
            quick.querySelectorAll(".quick-version-chip").forEach((c) => c.classList.toggle("active", c === chip));
          });
        });
      }
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
        closeDialog("create-modal");
        showToast("Server created! Click the green Start button above to launch it.");
        await loadServers();
        if (job.serverId) selectServer(job.serverId);
        const startBtn = $("btn-start");
        if (startBtn) {
          startBtn.classList.remove("btn-pulse-highlight");
          void startBtn.offsetWidth;
          startBtn.classList.add("btn-pulse-highlight");
          setTimeout(() => startBtn.classList.remove("btn-pulse-highlight"), 6000);
        }
        return;
      }
      if (job.status === "error") {
        throw new Error(job.message || "Server creation failed");
      }
    }
  }

  async function loadCloudAccount() {
    try {
      const c = await apiFetch("/api/cloud/status");
      if (typeof c.isPlayStore === "boolean") {
        state.isPlayStore = c.isPlayStore;
        applyUserPermissionsUi();
      }
      state.cloudLinked = !!c.isLinked;
      const signinBox = $("cloud-signin-box");
      const logoutBtn = $("cloud-logout-btn");
      const badge = $("cloud-tier-badge");
      const sidebarBadge = $("sidebar-cloud-badge");
      if (c.isLinked) {
        const usedMb = ((c.storageUsedBytes || 0) / (1024 * 1024)).toFixed(1);
        const quotaGb = ((c.storageQuotaBytes || 0) / (1024 * 1024 * 1024)).toFixed(0);
        $("cloud-status-summary").textContent = `Signed in as ${c.email || "user"} · Tier: ${c.tier} · Cloud Storage: ${usedMb} MB / ${quotaGb} GB · Active Subdomains: ${(c.activeSubdomains || []).join(", ") || "none"}`;
        if (badge) badge.textContent = `${c.tier || "FREE"} PLAN`;
        if (sidebarBadge) {
          sidebarBadge.textContent = String(c.tier || "FREE").toUpperCase();
          sidebarBadge.classList.remove("hidden");
        }
        signinBox?.classList.add("hidden");
        logoutBtn?.classList.remove("hidden");
      } else {
        $("cloud-status-summary").textContent = "Your host runs Minecraft servers 100% free forever (with Bore, Playit.gg, UPnP, and local/WebDAV backups). MineServe Cloud is an optional add-on that adds custom *.play.mineserve.app addresses, remote browser Web Admin from anywhere, and encrypted Cloudflare R2 backups.";
        if (badge) badge.textContent = "OPTIONAL ADD-ON";
        sidebarBadge?.classList.add("hidden");
        signinBox?.classList.remove("hidden");
        logoutBtn?.classList.add("hidden");
      }
      syncCloudUnlinkedCallouts();
    } catch (err) {
      $("cloud-status-summary").textContent = err.message || "Unable to load cloud status.";
    }
  }

  function renderWhatsNewModal(data) {
    if (!data) return;
    if ($("whatsnew-title")) $("whatsnew-title").textContent = data.headline || "What's New in MineServe";
    if ($("whatsnew-version-badge")) $("whatsnew-version-badge").textContent = data.displayVersion || "v1.8";
    if ($("whatsnew-subtitle")) $("whatsnew-subtitle").textContent = data.subtitle || "";
    if ($("open-whatsnew-from-settings-btn")) {
      $("open-whatsnew-from-settings-btn").textContent = `What's New in ${data.displayVersion || "v1.8"}`;
    }
    const showEnabled = data.showOnUpdateEnabled !== false;
    if ($("settings-whatsnew-autoshow")) $("settings-whatsnew-autoshow").checked = showEnabled;
    if ($("whatsnew-never-show-chk")) $("whatsnew-never-show-chk").checked = !showEnabled;

    const listEl = $("whatsnew-features-list");
    if (listEl) {
      const features = data.features || [];
      listEl.innerHTML = features.map((f) => `
          <div class="m3-card" style="background:var(--md-sys-color-surface-container-low);padding:14px;">
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:6px;">
              <span class="status-badge RUNNING">${escapeHtml(f.badge || "NEW")}</span>
              <strong style="font-size:15px;color:var(--md-sys-color-on-surface);">${escapeHtml(f.title || "")}</strong>
            </div>
            <div class="player-sub" style="line-height:1.55;margin-bottom:10px;">${escapeHtml(f.summary || "")}</div>
            <div class="m3-card" style="background:var(--md-sys-color-surface-container);padding:10px 12px;">
              <div style="font-size:11px;font-weight:700;color:var(--md-sys-color-primary);text-transform:uppercase;margin-bottom:6px;">Where to find it</div>
              <ol style="margin:0;padding-left:18px;font-size:12px;color:var(--md-sys-color-on-surface);line-height:1.6;">
                ${(f.stepsToFind || []).map((s) => `<li>${escapeHtml(s)}</li>`).join("")}
              </ol>
            </div>
          </div>
        `).join("");
    }
  }

  async function loadWhatsNewData(openModalIfAutoOrForced = false, forceOpen = false) {
    try {
      const data = await apiFetch("/api/whats-new");
      renderWhatsNewModal(data);
      if (forceOpen || (openModalIfAutoOrForced && data.shouldAutoShow)) {
        openDialog("whatsnew-modal");
        // Immediately mark seen on the host so neither Desktop/Android nor Web UI auto-pops it again
        await apiFetch("/api/whats-new", {
          method: "POST",
          body: JSON.stringify({ markSeen: true })
        });
      }
    } catch (_) {}
  }

  // Event Listeners
  function bindEvents() {
    $("login-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      $("login-error").classList.add("hidden");
      $("login-btn").disabled = true;
      try {
        const loginRes = await apiFetch("/api/login", {
          method: "POST",
          body: JSON.stringify({
            username: $("login-user").value.trim(),
            password: $("login-pass").value
          })
        });
        $("login-pass").value = "";
        if (loginRes.user) {
          state.currentUser = { ...loginRes.user, permissions: loginRes.permissions || [] };
          applyUserPermissionsUi();
          showApp();
        } else {
          await checkAuth();
        }
      } catch (err) {
        $("login-error").textContent = err.message;
        $("login-error").classList.remove("hidden");
      } finally {
        $("login-btn").disabled = false;
      }
    });

    const closeAccountPopover = () => {
      $("account-popover-menu")?.classList.add("hidden");
      $("current-user-pill")?.setAttribute("aria-expanded", "false");
    };

    $("current-user-pill")?.addEventListener("click", (e) => {
      e.stopPropagation();
      const menu = $("account-popover-menu");
      if (!menu) return;
      const isHidden = menu.classList.toggle("hidden");
      $("current-user-pill")?.setAttribute("aria-expanded", isHidden ? "false" : "true");
    });

    document.addEventListener("click", (e) => {
      const wrap = document.querySelector(".account-profile-wrap");
      if (wrap && !wrap.contains(e.target)) {
        closeAccountPopover();
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeAccountPopover();
    });

    document.querySelectorAll("[data-theme-choice]").forEach((btn) => {
      btn.addEventListener("click", () => {
        applyTheme(btn.dataset.themeChoice);
      });
    });

    $("logout-btn").addEventListener("click", async () => {
      closeAccountPopover();
      try { await apiFetch("/api/logout", { method: "POST" }); } catch (_) {}
      showLogin();
    });

    const openDrawer = () => {
      $("sidebar")?.classList.add("open");
      $("sidebar-backdrop")?.classList.add("open");
    };
    const closeDrawer = () => {
      $("sidebar")?.classList.remove("open");
      $("sidebar-backdrop")?.classList.remove("open");
    };
    $("mobile-menu-btn")?.addEventListener("click", openDrawer);
    $("mobile-close-sidebar")?.addEventListener("click", closeDrawer);
    $("sidebar-backdrop")?.addEventListener("click", closeDrawer);

    document.querySelectorAll(".tab-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        setActiveTab(btn.dataset.tab);
      });
    });

    document.querySelectorAll(".m3-segmented-btn[data-subtab]").forEach((btn) => {
      btn.addEventListener("click", () => {
        setActiveTab(btn.dataset.parentTab, btn.dataset.subtab);
      });
    });

    document.querySelectorAll(".console-quick-chip").forEach((chip) => {
      chip.addEventListener("click", async () => {
        const cmd = chip.dataset.cmd;
        if (!cmd || !state.selectedServerId) return;
        try {
          await apiFetch(`/api/servers/${state.selectedServerId}/console`, {
            method: "POST",
            body: JSON.stringify({ command: cmd })
          });
          showToast(`Sent: ${cmd}`);
          setTimeout(loadConsole, 200);
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });

    // Server lifecycle controls
    $("btn-start").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/start`, { method: "POST" });
        showToast("Start requested");
        loadServerDetails();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("btn-stop").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/stop`, { method: "POST" });
        showToast("Stop requested");
        loadServerDetails();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("btn-restart").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/restart`, { method: "POST" });
        showToast("Restart requested");
        loadServerDetails();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("btn-copy-public-addr")?.addEventListener("click", () => {
      const addr = $("btn-copy-public-addr")?.dataset?.addr || "";
      if (addr) {
        copyTextToClipboard(addr, "public address");
      } else {
        setActiveTab("management", "settings");
      }
    });
    $("btn-copy-local-addr")?.addEventListener("click", () => {
      const addr = $("btn-copy-local-addr")?.dataset?.addr || "";
      copyTextToClipboard(addr, "LAN address");
    });
    $("btn-open-network-tab")?.addEventListener("click", () => {
      setActiveTab("management", "settings");
    });
    $("set-ram")?.addEventListener("input", (e) => {
      const disp = $("set-ram-display");
      if (disp) disp.textContent = formatRamValue(e.target.value);
    });
    $("create-ram")?.addEventListener("input", (e) => {
      const disp = $("create-ram-display");
      if (disp) disp.textContent = formatRamValue(e.target.value);
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
      const ok = await showConfirmDialog({
        title: "Delete Server",
        message: `Delete server "${srv?.name || state.selectedServerId}"? This removes all world data permanently.`,
        confirmText: "Delete Server",
        destructive: true
      });
      if (!ok) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/delete`, { method: "POST" });
        showToast("Server deleted");
        selectServer(null);
        await loadServers();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Password visibility toggles
    document.querySelectorAll(".password-toggle-btn[data-toggle-password]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const input = $(btn.dataset.togglePassword);
        if (!input) return;
        const show = input.type === "password";
        input.type = show ? "text" : "password";
        btn.setAttribute("aria-label", show ? "Hide password" : "Show password");
      });
    });

    // Native <dialog> backdrop click-to-close (except session-expired-modal)
    document.querySelectorAll("dialog.m3-dialog-native").forEach((dlg) => {
      if (dlg.id === "session-expired-modal") return;
      dlg.addEventListener("click", (e) => {
        if (e.target === dlg) dlg.close();
      });
    });

    $("session-expired-signin-btn")?.addEventListener("click", () => {
      showLogin();
    });

    // Players Unified Filter & Quick Actions
    document.querySelectorAll("#player-filter-chips [data-player-filter]").forEach((chip) => {
      chip.addEventListener("click", () => {
        state.playersFilter = chip.dataset.playerFilter || "online";
        document.querySelectorAll("#player-filter-chips [data-player-filter]").forEach((c) => {
          c.classList.toggle("active", c === chip);
        });
        renderPlayersRoster();
      });
    });

    $("players-roster-search")?.addEventListener("input", (e) => {
      state.playersSearch = e.target.value || "";
      renderPlayersRoster();
    });

    document.querySelectorAll("[data-player-quick]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const target = ($("player-quick-target")?.value || "").trim();
        if (!target) {
          showToast("Enter a player username or IP address first", "error");
          return;
        }
        const act = btn.dataset.playerQuick;
        if (act === "op") await playerListAction("ops", { action: "add", name: target, level: 4 });
        else if (act === "deop") await playerListAction("ops", { action: "remove", name: target });
        else if (act === "whitelist") await playerListAction("whitelist", { action: "add", name: target });
        else if (act === "kick") {
          const ok = await showConfirmDialog({ title: "Kick Player", message: `Kick ${target} from the server?`, confirmText: "Kick", destructive: true });
          if (ok) await playerListAction("kick", { name: target, reason: "Kicked via Web Admin" });
        } else if (act === "ban") {
          const ok = await showConfirmDialog({ title: "Ban Player", message: `Permanently ban ${target}?`, confirmText: "Ban", destructive: true });
          if (ok) await playerListAction("ban", { action: "add", name: target, reason: "Banned via Web Admin" });
        } else if (act === "pardon") {
          await playerListAction("ban", { action: "remove", name: target });
        } else if (act === "banip") {
          const ok = await showConfirmDialog({ title: "Ban IP Address", message: `Ban IP address ${target}?`, confirmText: "Ban IP", destructive: true });
          if (ok) await playerListAction("ban-ip", { action: "add", ip: target, reason: "Banned IP via Web Admin" });
        }
        $("player-quick-target").value = "";
      });
    });

    $("plugins-filter-input")?.addEventListener("input", (e) => {
      state.pluginsFilter = e.target.value || "";
      renderInstalledPlugins();
    });

    $("world-seed-copy-btn")?.addEventListener("click", () => {
      const seed = ($("world-seed-input")?.value || "").trim();
      if (!seed) {
        showToast("World seed is random (empty)", "error");
        return;
      }
      copyTextToClipboard(seed, "world seed");
    });

    $("world-rules-save-btn")?.addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        if ($("prop-level-seed")) $("prop-level-seed").value = $("world-seed-input")?.value || "";
        if ($("prop-difficulty")) $("prop-difficulty").value = $("world-difficulty")?.value || "normal";
        if ($("prop-gamemode")) $("prop-gamemode").value = $("world-gamemode")?.value || "survival";
        if ($("prop-allow-nether")) $("prop-allow-nether").checked = !!$("world-allow-nether")?.checked;
        if ($("prop-spawn-monsters")) $("prop-spawn-monsters").checked = !!$("world-spawn-monsters")?.checked;
        $("settings-form")?.requestSubmit();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Performance, Automation, World, Live Map, Network, Backups, Plugins
    $("apply-aikar-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/performance/aikar`, { method: "POST" });
        showToast("Aikar's G1GC flags applied");
        loadPerformance();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("automation-power-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/automation`, {
          method: "POST",
          body: JSON.stringify({
            idleSleepEnabled: $("auto-idle-sleep").checked,
            idleSleepTimeoutMinutes: parseInt($("auto-idle-mins").value, 10) || 10,
            autoWakeOnPing: $("auto-wake-ping").checked
          })
        });
        showToast("Automation settings saved");
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    $("automation-task-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      try {
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
      } catch (err) {
        showToast(err.message, "error");
      }
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
      const ok = await showConfirmDialog({
        title: "Run Chunk Pruner",
        message: "Delete uninhabited chunks using the selected threshold? Stop the server first.",
        confirmText: "Run Pruner",
        destructive: true
      });
      if (!ok) return;
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
      if (!state.selectedServerId) return;
      const ok = await showConfirmDialog({
        title: "Uninstall Squaremap",
        message: "Remove Squaremap plugin/mod from this server?",
        confirmText: "Uninstall",
        destructive: true
      });
      if (!ok) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/livemap/uninstall`, { method: "POST" });
        showToast("Squaremap removed");
        loadLiveMap();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    const sendTunnelAction = async (action) => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/network/tunnel`, {
          method: "POST",
          body: JSON.stringify({
            action,
            provider: $("net-tunnel-provider").value,
            autoStart: $("net-tunnel-autostart").checked,
            customRelayHost: $("net-bore-host").value.trim(),
            customRelayPort: parseInt($("net-bore-port").value, 10) || 7835,
            playitSecret: $("net-playit-secret").value.trim(),
            cloudSubdomain: $("net-cloud-subdomain").value.trim().toLowerCase(),
            cloudRegion: $("net-cloud-region").value,
            cloudDomain: $("net-cloud-domain").value
          })
        });
        showToast(action === "start" ? "Tunnel starting..." : (action === "stop" ? "Tunnel stopped" : "Tunnel settings saved"));
        setTimeout(loadNetwork, 400);
      } catch (err) {
        showToast(err.message, "error");
      }
    };
    $("net-tunnel-provider").addEventListener("change", updateTunnelProviderFields);
    $("tunnel-start-btn").addEventListener("click", () => sendTunnelAction("start"));
    $("tunnel-stop-btn").addEventListener("click", () => sendTunnelAction("stop"));
    $("tunnel-save-btn").addEventListener("click", () => sendTunnelAction("save"));

    const sendPfAction = async (action) => {
      if (!state.selectedServerId) return;
      try {
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
      } catch (err) {
        showToast(err.message, "error");
      }
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

    $("plugin-upload-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      const input = $("plugin-upload-file");
      const file = input.files && input.files[0];
      if (!file) return;
      const btn = $("plugin-upload-btn");
      btn.disabled = true;
      btn.textContent = "Uploading...";
      try {
        const res = await fetch(`/api/servers/${state.selectedServerId}/plugins/upload?name=${encodeURIComponent(file.name)}`, {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/octet-stream" },
          body: file
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.error || `Upload failed (${res.status})`);
        const targetFolder = data.folder ? ` to ${data.folder}/` : "";
        showToast(`Uploaded ${data.fileName || file.name}${targetFolder}`);
        input.value = "";
        loadPlugins();
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Upload .jar";
      }
    });

    $("modpack-import-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!state.selectedServerId) return;
      const input = $("modpack-import-file");
      const file = input.files && input.files[0];
      if (!file) return;
      const btn = $("modpack-import-btn");
      const statusEl = $("modpack-import-status");
      btn.disabled = true;
      btn.textContent = "Uploading...";
      statusEl.textContent = "Uploading .mrpack archive...";
      try {
        const serverId = state.selectedServerId;
        const res = await fetch(`/api/servers/${serverId}/modpack/import`, {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/octet-stream" },
          body: file
        });
        const startData = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(startData.error || `Import failed (${res.status})`);
        if (!startData.jobId) throw new Error(startData.error || "Modpack import is not available for this server.");

        const jobId = startData.jobId;
        btn.textContent = "Importing...";
        const pollJob = async () => {
          try {
            const job = await apiFetch(`/api/servers/${serverId}/modpack/jobs/${jobId}`);
            statusEl.textContent = `${job.message || "Importing..."} ${job.currentFile ? "(" + job.currentFile + ")" : ""} - ${job.progressPercent || 0}%`;
            if (job.status === "DONE") {
              showToast(job.message || "Modpack imported!");
              input.value = "";
              loadPlugins();
              btn.disabled = false;
              btn.textContent = "Import .mrpack";
            } else if (job.status === "ERROR") {
              showToast(job.error || "Modpack import failed", "error");
              btn.disabled = false;
              btn.textContent = "Import .mrpack";
            } else {
              setTimeout(pollJob, 1000);
            }
          } catch (err) {
            statusEl.textContent = err.message;
            showToast(err.message, "error");
            btn.disabled = false;
            btn.textContent = "Import .mrpack";
          }
        };
        setTimeout(pollJob, 800);
      } catch (err) {
        statusEl.textContent = err.message;
        showToast(err.message, "error");
        btn.disabled = false;
        btn.textContent = "Import .mrpack";
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
          <div class="marketplace-card">
            <div style="min-width:0;flex:1;">
              <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                <strong>${escapeHtml(r.name)}</strong>
                <span class="m3-chip m3-chip--neutral">by ${escapeHtml(r.author)}</span>
              </div>
              <div class="player-sub" style="margin-top:4px;line-height:1.45;">${escapeHtml(r.description)}</div>
            </div>
            <button class="btn btn-secondary btn-sm modrinth-install-btn" data-id="${escapeHtml(r.id)}">Install</button>
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

    // Create Server 2-Step Wizard Modal
    $("open-create-modal-btn").addEventListener("click", openCreateModal);
    $("empty-create-btn").addEventListener("click", openCreateModal);
    $("close-create-modal").addEventListener("click", () => closeDialog("create-modal"));
    $("cancel-create-btn").addEventListener("click", () => closeDialog("create-modal"));
    $("create-next-btn")?.addEventListener("click", () => setCreateStep(2));
    $("create-back-btn")?.addEventListener("click", () => setCreateStep(1));
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

    // Host Tools (Cloud, Safeguards, Diagnostics)
    const loadHostTools = async () => {
      await loadCloudAccount();
      if (state.systemInfo && (state.systemInfo.isDesktopHost || ["linux", "macos", "windows"].includes(state.systemInfo.platform))) return;
      try {
        const sg = await apiFetch("/api/safeguards");
        $("sg-battery-enabled").checked = !!sg.batteryAutoStopEnabled;
        $("sg-battery-pct").value = String(sg.minBatteryPercent || 15);
        $("sg-thermal-enabled").checked = !!sg.thermalAutoStopEnabled;
        $("sg-thermal-c").value = String(sg.maxBatteryTempC || 45);
      } catch (_) {}
    };

    const openCloudModal = () => {
      if (state.isPlayStore) return;
      openDialog("cloud-modal");
      loadCloudAccount();
    };
    $("open-cloud-modal-btn")?.addEventListener("click", openCloudModal);
    $("tunnel-open-cloud-modal-btn")?.addEventListener("click", openCloudModal);
    $("backups-open-cloud-modal-btn")?.addEventListener("click", openCloudModal);
    $("close-cloud-modal")?.addEventListener("click", () => closeDialog("cloud-modal"));

    const switchHostTab = (tabName) => {
      document.querySelectorAll("[data-host-tab]").forEach((b) => {
        b.classList.toggle("active", b.dataset.hostTab === tabName);
      });
      ["appearance", "java", "diagnostics", "updates"].forEach((name) => {
        $(`host-pane-${name}`)?.classList.toggle("active", name === tabName);
      });
    };

    document.querySelectorAll("[data-host-tab]").forEach((btn) => {
      btn.addEventListener("click", () => switchHostTab(btn.dataset.hostTab));
    });

    const loadHostDirectories = async () => {
      try {
        const dirs = await apiFetch("/api/host/directories");
        if ($("host-dir-servers") && dirs.serversDir) $("host-dir-servers").value = dirs.serversDir;
        if ($("host-dir-runtimes") && dirs.runtimesDir) $("host-dir-runtimes").value = dirs.runtimesDir;
      } catch (_) {}
    };

    const saveHostDirectories = async (serversDir, runtimesDir) => {
      try {
        const res = await apiFetch("/api/host/directories", {
          method: "POST",
          body: JSON.stringify({ serversDir, runtimesDir })
        });
        if ($("host-dir-servers") && res.serversDir) $("host-dir-servers").value = res.serversDir;
        if ($("host-dir-runtimes") && res.runtimesDir) $("host-dir-runtimes").value = res.runtimesDir;
        if (state.systemInfo) {
          state.systemInfo.serversDir = res.serversDir;
          state.systemInfo.runtimesDir = res.runtimesDir;
        }
        showToast("Updated host directories");
        loadJavaRuntimes();
      } catch (err) {
        showToast(err.message, "error");
      }
    };

    $("host-dirs-save-btn")?.addEventListener("click", () => {
      saveHostDirectories(
        ($("host-dir-servers")?.value || "").trim(),
        ($("host-dir-runtimes")?.value || "").trim()
      );
    });
    $("host-dirs-reset-btn")?.addEventListener("click", () => {
      saveHostDirectories("", "");
    });

    const openHostToolsModal = () => {
      applyUserPermissionsUi();
      switchHostTab("appearance");
      openDialog("host-tools-modal");
      const isPlay = !!state.isPlayStore;
      $("web-update-check-btn")?.classList.toggle("hidden", isPlay);
      $("web-update-link-btn")?.classList.toggle("hidden", isPlay);
      if ($("web-update-status")) {
        $("web-update-status").textContent = isPlay
          ? "App updates are managed via Google Play Store."
          : "Check GitHub Releases for new MineServe versions.";
      }
      if (hasPerm("MANAGE_HOST")) {
        const isDesktop = !!(state.systemInfo && (state.systemInfo.isDesktopHost || ["linux", "macos", "windows"].includes(state.systemInfo.platform)));
        if (isDesktop) loadHostDirectories();
        loadHostTools();
        loadJavaRuntimes();
        loadWhatsNewData(false);
      }
    };
    $("open-host-tools-btn")?.addEventListener("click", openHostToolsModal);

    // What's New Feature Guide Modal (Synced with Desktop & Android UI)
    const closeWhatsNewModal = async () => {
      closeDialog("whatsnew-modal");
      const neverShow = !!$("whatsnew-never-show-chk")?.checked;
      try {
        await apiFetch("/api/whats-new", {
          method: "POST",
          body: JSON.stringify({
            markSeen: true,
            showOnUpdateEnabled: !neverShow
          })
        });
        if ($("settings-whatsnew-autoshow")) $("settings-whatsnew-autoshow").checked = !neverShow;
      } catch (_) {}
    };

    $("close-whatsnew-modal")?.addEventListener("click", closeWhatsNewModal);
    $("whatsnew-got-it-btn")?.addEventListener("click", closeWhatsNewModal);
    $("whatsnew-never-show-chk")?.addEventListener("change", async (e) => {
      const showOnUpdateEnabled = !e.target.checked;
      if ($("settings-whatsnew-autoshow")) $("settings-whatsnew-autoshow").checked = showOnUpdateEnabled;
      try {
        await apiFetch("/api/whats-new", {
          method: "POST",
          body: JSON.stringify({ markSeen: true, showOnUpdateEnabled })
        });
      } catch (_) {}
    });
    $("settings-whatsnew-autoshow")?.addEventListener("change", async (e) => {
      const showOnUpdateEnabled = !!e.target.checked;
      if ($("whatsnew-never-show-chk")) $("whatsnew-never-show-chk").checked = !showOnUpdateEnabled;
      try {
        await apiFetch("/api/whats-new", {
          method: "POST",
          body: JSON.stringify({ showOnUpdateEnabled })
        });
        showToast(showOnUpdateEnabled ? "What's New guide will show after major updates" : "What's New auto-popup disabled");
      } catch (_) {}
    });
    $("open-whatsnew-from-settings-btn")?.addEventListener("click", () => {
      loadWhatsNewData(false, true);
    });
    $("open-whatsnew-from-about-btn")?.addEventListener("click", () => {
      closeDialog("about-modal");
      loadWhatsNewData(false, true);
    });

    // App Updates Checker in App Settings
    $("web-update-check-btn")?.addEventListener("click", async () => {
      if (state.isPlayStore) return;
      const btn = $("web-update-check-btn");
      const statusEl = $("web-update-status");
      const notesEl = $("web-update-notes");
      btn.disabled = true;
      btn.textContent = "Checking...";
      statusEl.textContent = "Contacting GitHub Releases API...";
      notesEl?.classList.add("hidden");
      try {
        const res = await apiFetch("/api/updates/check");
        if (res.status === "MANAGED_BY_PLAY_STORE") {
          statusEl.textContent = res.message || "App updates are managed via Google Play Store.";
        } else if (res.status === "UPDATE_AVAILABLE") {
          statusEl.textContent = `Update Available: ${res.latestTag} (Current: v${res.currentVersion})`;
          if (res.htmlUrl && $("web-update-link-btn")) {
            $("web-update-link-btn").href = res.htmlUrl;
          }
          if (res.releaseNotes && notesEl) {
            notesEl.textContent = res.releaseNotes;
            notesEl.classList.remove("hidden");
          }
          showToast(`New update available: ${res.latestTag}`);
        } else if (res.status === "UP_TO_DATE") {
          statusEl.textContent = `You are running the latest version (v${res.currentVersion}).`;
          showToast("MineServe is up to date!");
        } else {
          statusEl.textContent = res.error || "Unable to check for updates.";
        }
      } catch (err) {
        statusEl.textContent = err.message;
      } finally {
        btn.disabled = false;
        btn.textContent = "Check for Updates";
      }
    });

    // About, Platform Changelog & Open-Source Credits Modal
    let cachedAboutData = null;
    let selectedCreditCategory = "ALL";

    const renderAboutCredits = () => {
      const listEl = $("about-credits-list");
      if (!listEl || !cachedAboutData) return;
      const credits = (cachedAboutData.credits || []).filter((c) =>
        selectedCreditCategory === "ALL" || c.category === selectedCreditCategory
      );
      listEl.innerHTML = credits.map((c) => `
        <div class="m3-table-row" style="align-items:flex-start;">
          <div style="flex:1;min-width:0;">
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
              <strong>${escapeHtml(c.name)}</strong>
              <span class="m3-chip m3-chip--primary">${escapeHtml(c.tag || c.categoryLabel || "")}</span>
              <span class="m3-chip m3-chip--neutral mono">${escapeHtml(c.license || "")}</span>
            </div>
            <div class="player-sub" style="margin-top:4px;">${escapeHtml(c.description || "")}</div>
          </div>
          <a href="${escapeHtml(c.url || "#")}" target="_blank" rel="noopener" class="btn btn-secondary btn-sm" style="white-space:nowrap;">Open Project ↗</a>
        </div>
      `).join("");
    };

    const switchAboutTab = (tabName) => {
      document.querySelectorAll("[data-about-tab]").forEach((b) => {
        b.classList.toggle("active", b.dataset.aboutTab === tabName);
      });
      $("about-changelog-section")?.classList.toggle("hidden", tabName !== "changelog");
      $("about-supporters-section")?.classList.toggle("hidden", tabName !== "supporters");
      $("about-credits-section")?.classList.toggle("hidden", tabName !== "credits");
    };

    const loadAboutModal = async () => {
      try {
        const data = await apiFetch("/api/about");
        cachedAboutData = data;
        if ($("about-version-badge")) $("about-version-badge").textContent = `v${data.version || "1.8.1"}`;
        if ($("about-platform-badge")) $("about-platform-badge").textContent = String(data.platform || "linux").toUpperCase();

        const links = data.links || {};
        if (links.website && $("about-link-website")) $("about-link-website").href = links.website;
        if (links.githubRepo && $("about-link-github")) $("about-link-github").href = links.githubRepo;
        if (links.githubIssues && $("about-link-issues")) $("about-link-issues").href = links.githubIssues;
        if (links.discord && $("about-link-discord")) $("about-link-discord").href = links.discord;
        if (links.patreon && $("about-link-patreon")) $("about-link-patreon").href = links.patreon;
        if (links.buyMeACoffee && $("about-link-bmac")) $("about-link-bmac").href = links.buyMeACoffee;

        const isPlay = !!data.isPlayStore;
        $("about-link-patreon")?.classList.toggle("hidden", isPlay);
        $("about-link-bmac")?.classList.toggle("hidden", isPlay);
        $("about-tab-supporters-btn")?.classList.toggle("hidden", isPlay);

        const supporters = data.supporters || [];
        if ($("about-tab-supporters-btn")) {
          $("about-tab-supporters-btn").textContent = `Supporters (${supporters.length})`;
        }
        if ($("about-tab-credits-btn")) {
          $("about-tab-credits-btn").textContent = `Open-Source Credits (${(data.credits || []).length})`;
        }
        if ($("about-supporters-list")) {
          if (supporters.length === 0) {
            $("about-supporters-list").innerHTML = `<div class="player-sub" style="padding:14px;">Supporters list will appear here.</div>`;
          } else {
            $("about-supporters-list").innerHTML = supporters.map((s) => `
              <div class="m3-table-row">
                <div style="display:flex;align-items:center;gap:12px;min-width:0;flex:1;">
                  <span class="supporter-heart-badge">
                    <svg class="ms-icon ms-icon--sm" viewBox="0 0 24 24" fill="currentColor"><path d="m12 21l-1.45-1.3q-2.525-2.275-4.175-3.925T3.75 12.812T2.388 10.4T2 8.15Q2 5.8 3.575 4.225T7.5 2.65q1.3 0 2.475.55T12 4.75q.85-1 2.025-1.55t2.475-.55q2.35 0 3.925 1.575T22 8.15q0 1.15-.387 2.25t-1.363 2.412t-2.625 2.963T13.45 19.7z"/></svg>
                  </span>
                  <div style="min-width:0;">
                    <div><strong>${escapeHtml(s.name)}</strong></div>
                    <div style="font-size:11px;font-weight:600;color:var(--md-sys-color-primary);">${escapeHtml(s.tier || "")}</div>
                  </div>
                </div>
                <span class="m3-chip m3-chip--error">Supporter</span>
              </div>
            `).join("");
          }
        }

        const changelog = data.changelog || [];
        if ($("about-changelog-list")) {
          $("about-changelog-list").innerHTML = changelog.map((rel) => `
            <div class="m3-card" style="background:var(--md-sys-color-surface-container-low);padding:12px 14px;">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
                <strong style="color:var(--md-sys-color-primary);font-size:14px;">${escapeHtml(rel.version)}</strong>
                <span class="player-sub mono">${escapeHtml(rel.date)}</span>
              </div>
              <ul style="margin:0;padding-left:18px;font-size:12px;color:var(--md-sys-color-on-surface);line-height:1.65;">
                ${(rel.highlights || []).map((h) => `<li>${escapeHtml(h)}</li>`).join("")}
              </ul>
            </div>
          `).join("");
        }
        renderAboutCredits();
      } catch (err) {
        showToast(err.message, "error");
      }
    };

    $("open-about-modal-btn")?.addEventListener("click", () => {
      switchAboutTab("changelog");
      openDialog("about-modal");
      loadAboutModal();
    });
    $("close-about-modal")?.addEventListener("click", () => closeDialog("about-modal"));

    document.querySelectorAll("[data-about-tab]").forEach((btn) => {
      btn.addEventListener("click", () => switchAboutTab(btn.dataset.aboutTab));
    });

    document.querySelectorAll("#about-credits-filters [data-credit-cat]").forEach((btn) => {
      btn.addEventListener("click", () => {
        selectedCreditCategory = btn.dataset.creditCat || "ALL";
        document.querySelectorAll("#about-credits-filters [data-credit-cat]").forEach((b) => {
          b.classList.toggle("active", b === btn);
        });
        renderAboutCredits();
      });
    });
    $("btn-copy-console")?.addEventListener("click", async () => {
      const text = ($("console-output")?.textContent || "").trim();
      if (!text) {
        showToast("Console is empty", "error");
        return;
      }
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
        } else {
          const ta = document.createElement("textarea");
          ta.value = text;
          document.body.appendChild(ta);
          ta.select();
          document.execCommand("copy");
          ta.remove();
        }
        showToast("Console output copied to clipboard!");
      } catch (_) {
        showToast("Failed to copy console output", "error");
      }
    });
    $("close-host-tools-modal").addEventListener("click", () => closeDialog("host-tools-modal"));
    $("cloud-refresh-btn").addEventListener("click", async () => {
      try {
        await apiFetch("/api/cloud/refresh", { method: "POST" });
        await loadHostTools();
        showToast("Cloud profile refreshed");
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("cloud-otp-request-btn")?.addEventListener("click", async () => {
      const email = ($("cloud-otp-email")?.value || "").trim();
      if (!email || !email.includes("@")) {
        showToast("Enter a valid email address.", "error");
        return;
      }
      const btn = $("cloud-otp-request-btn");
      btn.disabled = true;
      try {
        await apiFetch("/api/cloud/otp/request", { method: "POST", body: JSON.stringify({ email }) });
        $("cloud-otp-verify-row")?.classList.remove("hidden");
        showToast("6-digit verification code sent! Check your email.");
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
      }
    });
    $("cloud-otp-verify-btn")?.addEventListener("click", async () => {
      const email = ($("cloud-otp-email")?.value || "").trim();
      const code = ($("cloud-otp-code")?.value || "").trim();
      if (code.length < 6) {
        showToast("Enter the 6-digit code from your email.", "error");
        return;
      }
      const btn = $("cloud-otp-verify-btn");
      btn.disabled = true;
      try {
        await apiFetch("/api/cloud/otp/verify", { method: "POST", body: JSON.stringify({ email, code }) });
        $("cloud-otp-code").value = "";
        $("cloud-otp-verify-row")?.classList.add("hidden");
        await loadHostTools();
        showToast("Signed in to MineServe Cloud!");
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
      }
    });
    $("cloud-logout-btn")?.addEventListener("click", async () => {
      const ok = await showConfirmDialog({
        title: "Sign Out of MineServe Cloud",
        message: "Sign out of MineServe Cloud on this host?",
        confirmText: "Sign Out",
        destructive: true
      });
      if (!ok) return;
      try {
        await apiFetch("/api/cloud/logout", { method: "POST" });
        await loadHostTools();
        showToast("Signed out of MineServe Cloud");
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("sg-save-btn").addEventListener("click", async () => {
      try {
        await apiFetch("/api/safeguards", {
          method: "POST",
          body: JSON.stringify({
            batteryAutoStopEnabled: $("sg-battery-enabled").checked,
            minBatteryPercent: parseInt($("sg-battery-pct").value, 10) || 15,
            thermalAutoStopEnabled: $("sg-thermal-enabled").checked,
            maxBatteryTempC: parseInt($("sg-thermal-c").value, 10) || 45
          })
        });
        showToast("Safeguards saved");
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("diag-logcat-btn").addEventListener("click", async () => {
      const mode = $("diag-filter-mode").value;
      $("diag-logcat-output").textContent = "Capturing redacted logcat...";
      try {
        const res = await apiFetch(`/api/diagnostics/logcat?filter=${encodeURIComponent(mode)}`);
        $("diag-logcat-output").textContent = res.logs || "(No matching log lines)";
      } catch (err) {
        $("diag-logcat-output").textContent = err.message;
      }
    });
    $("diag-selftest-btn").addEventListener("click", async () => {
      const list = $("diag-selftest-list");
      list.innerHTML = `<div class="player-sub">Running network & tunnel self-tests...</div>`;
      try {
        const res = await apiFetch("/api/diagnostics/selftest");
        const checks = res.checks || [];
        list.innerHTML = checks.map((c) => `
          <div class="m3-table-row">
            <div>
              <div style="display:flex;align-items:center;gap:8px;">
                <span class="m3-chip ${c.passed ? "m3-chip--primary" : "m3-chip--error"}">${c.passed ? "PASS" : "FAIL"}</span>
                <strong>${escapeHtml(c.name)}</strong>
              </div>
              <div class="player-sub">${escapeHtml(c.detail)}</div>
            </div>
          </div>
        `).join("");
      } catch (err) {
        list.innerHTML = `<div class="player-sub">${escapeHtml(err.message)}</div>`;
      }
    });
    $("diag-bundle-btn").addEventListener("click", () => {
      const mode = $("diag-filter-mode").value;
      window.location.href = `/api/diagnostics/bundle?filter=${encodeURIComponent(mode)}`;
    });
    $("cloud-backups-refresh-btn").addEventListener("click", () => loadCloudBackups());
    $("regions-refresh-btn").addEventListener("click", () => loadRegions());
    $("pregen-start-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/world/pregen`, {
          method: "POST",
          body: JSON.stringify({
            action: "start",
            dimension: $("pregen-dim").value,
            radiusBlocks: parseInt($("pregen-radius").value, 10) || 500
          })
        });
        showToast("Chunk pre-generation started");
        loadPregenStatus();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("pregen-stop-btn").addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/world/pregen`, {
          method: "POST",
          body: JSON.stringify({ action: "stop" })
        });
        showToast("Chunk pre-generation stopped");
        loadPregenStatus();
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Multi-User, Sessions & Audit Log Modal
    const uploadFilesToCurrentDir = async (fileList) => {
      if (!state.selectedServerId || !fileList || !fileList.length) return;
      for (const file of Array.from(fileList)) {
        try {
          showToast(`Uploading ${file.name}...`);
          const url = `/api/servers/${encodeURIComponent(state.selectedServerId)}/files/upload?path=${encodeURIComponent(state.currentPath || "")}&name=${encodeURIComponent(file.name)}`;
          const res = await fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/octet-stream" },
            body: file
          });
          const data = await res.json().catch(() => ({}));
          if (!res.ok) throw new Error(data.error || `Upload failed (${res.status})`);
          showToast(`Uploaded ${file.name}`);
        } catch (err) {
          showToast(err.message, "error");
        }
      }
      loadFiles(state.currentPath);
    };
    $("files-new-file-btn")?.addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const name = await showPromptDialog({
        title: "Create New File",
        message: "Enter a file name (e.g. config.yml):",
        placeholder: "config.yml",
        confirmText: "Create"
      });
      if (!name || !name.trim()) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/files/create`, {
          method: "POST",
          body: JSON.stringify({ path: state.currentPath || "", name: name.trim(), content: "" })
        });
        showToast(`Created ${name.trim()}`);
        await loadFiles(state.currentPath);
        const rel = state.currentPath ? `${state.currentPath}/${name.trim()}` : name.trim();
        openFileEditor(rel);
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("files-new-dir-btn")?.addEventListener("click", async () => {
      if (!state.selectedServerId) return;
      const name = await showPromptDialog({
        title: "Create New Folder",
        message: "Enter a folder name:",
        placeholder: "plugins",
        confirmText: "Create"
      });
      if (!name || !name.trim()) return;
      try {
        await apiFetch(`/api/servers/${state.selectedServerId}/files/mkdir`, {
          method: "POST",
          body: JSON.stringify({ path: state.currentPath || "", name: name.trim() })
        });
        showToast(`Created folder ${name.trim()}`);
        loadFiles(state.currentPath);
      } catch (err) {
        showToast(err.message, "error");
      }
    });
    $("files-upload-btn")?.addEventListener("click", () => {
      const inp = $("files-upload-input");
      if (inp) {
        inp.value = "";
        inp.click();
      }
    });
    $("files-upload-input")?.addEventListener("change", (e) => {
      uploadFilesToCurrentDir(e.target.files);
    });
    const filesDropEl = $("files-list");
    if (filesDropEl) {
      filesDropEl.addEventListener("dragover", (e) => {
        e.preventDefault();
        filesDropEl.classList.add("drag-over");
      });
      filesDropEl.addEventListener("dragleave", () => {
        filesDropEl.classList.remove("drag-over");
      });
      filesDropEl.addEventListener("drop", (e) => {
        e.preventDefault();
        filesDropEl.classList.remove("drag-over");
        if (e.dataTransfer?.files?.length) {
          uploadFilesToCurrentDir(e.dataTransfer.files);
        }
      });
    }

    const switchUsersModalTab = async (tabName) => {
      const showAudit = tabName === "audit";
      $("users-subtab-btn")?.classList.toggle("active", !showAudit);
      $("audit-subtab-btn")?.classList.toggle("active", showAudit);
      $("users-tab-users")?.classList.toggle("hidden", showAudit);
      $("users-tab-audit")?.classList.toggle("hidden", !showAudit);
      if (showAudit) {
        await loadAuditLog();
      } else {
        await Promise.all([loadPanelUsers(), loadActiveSessions()]);
      }
    };

    const openUsersModal = async () => {
      const canManageUsers = hasPerm("MANAGE_USERS");
      const canViewAudit = hasPerm("VIEW_AUDIT_LOG");
      $("users-subtab-btn")?.classList.toggle("hidden", !canManageUsers);
      $("audit-subtab-btn")?.classList.toggle("hidden", !canViewAudit);
      openDialog("users-modal");
      renderUserServerCheckboxes();
      await switchUsersModalTab(canManageUsers ? "users" : "audit");
    };
    $("open-users-modal-btn")?.addEventListener("click", openUsersModal);
    $("users-subtab-btn")?.addEventListener("click", () => switchUsersModalTab("users"));
    $("audit-subtab-btn")?.addEventListener("click", () => switchUsersModalTab("audit"));
    $("close-users-modal-btn")?.addEventListener("click", () => closeDialog("users-modal"));
    $("users-refresh-btn")?.addEventListener("click", () => loadPanelUsers());
    $("sessions-refresh-btn")?.addEventListener("click", () => loadActiveSessions());

    // Audit Log Controls
    $("audit-refresh-btn")?.addEventListener("click", () => loadAuditLog());
    $("audit-limit-select")?.addEventListener("change", () => loadAuditLog());
    $("audit-search-input")?.addEventListener("input", () => renderFilteredAuditLog());
    $("audit-category-filter")?.addEventListener("change", () => renderFilteredAuditLog());
    $("audit-user-filter")?.addEventListener("change", () => renderFilteredAuditLog());
    $("audit-export-csv-btn")?.addEventListener("click", () => exportAuditCsv());

    // Java Runtime Manager
    $("java-refresh-btn")?.addEventListener("click", () => loadJavaRuntimes());

    // Server Version & Latest Build Upgrader
    const runServerUpgradeJob = async (newVersion, confirmText) => {
      if (!state.selectedServerId) return;
      const createBackup = !!$("upgrade-create-backup")?.checked;
      if (!newVersion) {
        showToast("Select a target version first", "error");
        return;
      }
      const ok = await showConfirmDialog({
        title: "Upgrade Server Software",
        message: confirmText,
        confirmText: "Upgrade"
      });
      if (!ok) return;
      const btn = $("upgrade-server-btn");
      const buildBtn = $("upgrade-latest-build-btn");
      const box = $("upgrade-progress-box");
      if (btn) btn.disabled = true;
      if (buildBtn) buildBtn.disabled = true;
      box.classList.remove("hidden");
      $("upgrade-progress-msg").textContent = "Starting update...";
      $("upgrade-progress-pct").textContent = "2%";
      $("upgrade-progress-fill").style.width = "2%";
      try {
        const res = await apiFetch(`/api/servers/${state.selectedServerId}/upgrade`, {
          method: "POST",
          body: JSON.stringify({ newVersion, createBackup })
        });
        const jobId = res.jobId;
        const pollUpgrade = setInterval(async () => {
          try {
            const job = await apiFetch(`/api/servers/jobs/${jobId}`);
            $("upgrade-progress-msg").textContent = job.message || "Updating...";
            $("upgrade-progress-pct").textContent = `${job.progress || 0}%`;
            $("upgrade-progress-fill").style.width = `${job.progress || 0}%`;
            if (job.status === "done") {
              clearInterval(pollUpgrade);
              if (btn) btn.disabled = false;
              if (buildBtn) buildBtn.disabled = false;
              showToast(job.message || "Update complete!");
              await loadServers();
              loadSettings();
              loadUpgradeVersions();
            } else if (job.status === "error") {
              clearInterval(pollUpgrade);
              if (btn) btn.disabled = false;
              if (buildBtn) buildBtn.disabled = false;
              showToast(job.message || "Update failed", "error");
            }
          } catch (err) {
            clearInterval(pollUpgrade);
            if (btn) btn.disabled = false;
            if (buildBtn) buildBtn.disabled = false;
            showToast(err.message, "error");
          }
        }, 1000);
      } catch (err) {
        if (btn) btn.disabled = false;
        if (buildBtn) buildBtn.disabled = false;
        showToast(err.message, "error");
      }
    };
    $("upgrade-server-btn")?.addEventListener("click", () => {
      const newVersion = $("upgrade-version-select")?.value;
      runServerUpgradeJob(
        newVersion,
        `Upgrade this server to ${newVersion}? If running, the server will be stopped during the upgrade.`
      );
    });
    $("upgrade-latest-build-btn")?.addEventListener("click", () => {
      const srv = state.servers.find((s) => s.id === state.selectedServerId);
      if (!srv) return;
      runServerUpgradeJob(
        srv.version,
        `Update ${srv.type} ${srv.version} to the latest available build? If running, the server will be stopped briefly.`
      );
    });

    // Import Existing Server (.zip) Modal
    $("open-import-server-modal-btn")?.addEventListener("click", () => {
      configureRamSlider("import-srv-ram", getDeviceRamBounds().defaultMb);
      $("import-srv-name").value = "";
      $("import-srv-file").value = "";
      $("import-srv-eula").checked = false;
      $("import-progress-box")?.classList.add("hidden");
      openDialog("import-server-modal");
    });
    $("close-import-server-modal")?.addEventListener("click", () => closeDialog("import-server-modal"));
    $("cancel-import-server-btn")?.addEventListener("click", () => closeDialog("import-server-modal"));
    $("import-srv-ram")?.addEventListener("input", (e) => {
      const disp = $("import-srv-ram-display");
      if (disp) disp.textContent = formatRamValue(e.target.value);
    });
    $("import-server-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const file = $("import-srv-file").files?.[0];
      if (!file) {
        showToast("Please select a .zip archive", "error");
        return;
      }
      if (!$("import-srv-eula").checked) {
        showToast("You must accept the Minecraft EULA", "error");
        return;
      }
      const btn = $("import-srv-submit-btn");
      const progBox = $("import-progress-box");
      const progMsg = $("import-progress-msg");
      const progPct = $("import-progress-pct");
      const progFill = $("import-progress-fill");
      btn.disabled = true;
      btn.textContent = "Uploading & Extracting...";
      progBox?.classList.remove("hidden");
      if (progMsg) progMsg.textContent = "Uploading server archive...";
      if (progPct) progPct.textContent = "0%";
      if (progFill) progFill.style.width = "0%";

      try {
        const params = new URLSearchParams({
          name: $("import-srv-name").value.trim(),
          type: $("import-srv-type").value,
          version: $("import-srv-version").value.trim() || "Custom",
          port: $("import-srv-port").value,
          ramMb: $("import-srv-ram").value,
          eulaAccepted: "true"
        });
        const data = await new Promise((resolve, reject) => {
          const xhr = new XMLHttpRequest();
          xhr.open("POST", `/api/servers/import?${params.toString()}`, true);
          xhr.withCredentials = true;
          xhr.setRequestHeader("Content-Type", "application/octet-stream");
          xhr.upload.onprogress = (evt) => {
            if (evt.lengthComputable) {
              const pct = Math.min(95, Math.round((evt.loaded / evt.total) * 95));
              if (progPct) progPct.textContent = `${pct}%`;
              if (progFill) progFill.style.width = `${pct}%`;
              if (pct >= 95 && progMsg) progMsg.textContent = "Extracting archive on host...";
            }
          };
          xhr.onload = () => {
            let parsed = {};
            try { parsed = JSON.parse(xhr.responseText || "{}"); } catch (_) {}
            if (xhr.status >= 200 && xhr.status < 300) resolve(parsed);
            else reject(new Error(parsed.error || `Import failed (${xhr.status})`));
          };
          xhr.onerror = () => reject(new Error("Network error during upload"));
          xhr.send(file);
        });
        if (progPct) progPct.textContent = "100%";
        if (progFill) progFill.style.width = "100%";
        closeDialog("import-server-modal");
        showToast(`Imported '${data.name}' (${data.extractedCount || 0} files, ${data.jarFileName})`);
        await loadServers();
        if (data.serverId) selectServer(data.serverId);
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        btn.disabled = false;
        btn.textContent = "Upload & Import";
      }
    });
    $("new-user-role")?.addEventListener("change", (e) => {
      const isOwner = e.target.value === "OWNER";
      $("new-user-restrict-wrap")?.classList.toggle("hidden", isOwner);
      if (isOwner) {
        if ($("new-user-restrict")) $("new-user-restrict").checked = false;
        $("new-user-servers-box")?.classList.add("hidden");
      }
    });
    $("create-version")?.addEventListener("change", (e) => {
      const selected = e.target.value;
      document.querySelectorAll("#create-quick-versions .quick-version-chip").forEach((c) => {
        c.classList.toggle("active", c.dataset.ver === selected);
      });
    });
    $("new-user-restrict")?.addEventListener("change", (e) => {
      $("new-user-servers-box").classList.toggle("hidden", !e.target.checked);
      if (e.target.checked) renderUserServerCheckboxes();
    });
    $("create-user-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const restrict = $("new-user-restrict").checked;
      const allowedServerIds = restrict
        ? Array.from(document.querySelectorAll('input[name="new-user-srv"]:checked')).map((el) => el.value)
        : [];
      try {
        await apiFetch("/api/users", {
          method: "POST",
          body: JSON.stringify({
            username: $("new-user-name").value.trim(),
            password: $("new-user-pass").value,
            role: $("new-user-role").value,
            restrictServers: restrict,
            allowedServerIds
          })
        });
        $("new-user-name").value = "";
        $("new-user-pass").value = "";
        $("new-user-restrict").checked = false;
        $("new-user-servers-box").classList.add("hidden");
        showToast("User account created");
        await Promise.all([loadPanelUsers(), loadAuditLog()]);
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    // Change Own Password Modal
    $("open-password-modal-btn")?.addEventListener("click", () => {
      $("account-popover-menu")?.classList.add("hidden");
      $("current-user-pill")?.setAttribute("aria-expanded", "false");
      $("pw-current").value = "";
      $("pw-new").value = "";
      openDialog("password-modal");
    });
    $("close-password-modal-btn")?.addEventListener("click", () => closeDialog("password-modal"));
    $("cancel-password-modal-btn")?.addEventListener("click", () => closeDialog("password-modal"));
    $("change-password-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      try {
        await apiFetch("/api/me/password", {
          method: "POST",
          body: JSON.stringify({
            currentPassword: $("pw-current").value,
            newPassword: $("pw-new").value
          })
        });
        closeDialog("password-modal");
        showToast("Password updated successfully");
      } catch (err) {
        showToast(err.message, "error");
      }
    });
  }

  function renderUserServerCheckboxes() {
    const box = $("new-user-servers-box");
    if (!box) return;
    if (!state.servers.length) {
      box.innerHTML = `<div class="player-sub">No servers created yet.</div>`;
      return;
    }
    box.innerHTML = state.servers.map((s) => `
      <label class="checkbox-label">
        <input type="checkbox" name="new-user-srv" value="${escapeHtml(s.id)}">
        <span>${escapeHtml(s.name)}</span>
      </label>
    `).join("");
  }

  async function loadPanelUsers() {
    const list = $("users-list");
    if (!list) return;
    try {
      const data = await apiFetch("/api/users");
      const users = data.users || [];
      list.innerHTML = users.map((u) => {
        const scope = u.restrictServers
          ? `Scoped (${(u.allowedServerIds || []).length} server(s))`
          : "All servers";
        const lastLogin = u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : "Never";
        return `
          <div class="m3-table-row">
            <div>
              <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                <strong>${escapeHtml(u.username)}</strong>
                <span class="role-badge ${escapeHtml(u.role)}">${escapeHtml(u.roleLabel || u.role)}</span>
                ${!u.enabled ? `<span class="status-badge ERROR">DISABLED</span>` : ""}
              </div>
              <div class="player-sub mono">${escapeHtml(scope)} | Last login: ${escapeHtml(lastLogin)}</div>
            </div>
            <div class="player-actions">
              <button class="btn btn-secondary btn-sm" data-user-toggle="${escapeHtml(u.id)}" data-enabled="${u.enabled ? "1" : "0"}">${u.enabled ? "Disable" : "Enable"}</button>
              <button class="btn btn-secondary btn-sm" data-user-reset="${escapeHtml(u.id)}" data-username="${escapeHtml(u.username)}">Reset PW</button>
              <button class="btn btn-danger btn-sm" data-user-del="${escapeHtml(u.id)}" data-username="${escapeHtml(u.username)}">Delete</button>
            </div>
          </div>
        `;
      }).join("");

      list.querySelectorAll("[data-user-toggle]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const id = btn.dataset.userToggle;
          const currentlyEnabled = btn.dataset.enabled === "1";
          try {
            await apiFetch(`/api/users/${id}`, {
              method: "PUT",
              body: JSON.stringify({ enabled: !currentlyEnabled })
            });
            showToast(currentlyEnabled ? "User disabled" : "User enabled");
            await Promise.all([loadPanelUsers(), loadActiveSessions(), loadAuditLog()]);
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });

      list.querySelectorAll("[data-user-reset]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const id = btn.dataset.userReset;
          const uname = btn.dataset.username;
          const newPw = await showPromptDialog({
            title: "Reset User Password",
            message: `Enter a new password for ${uname} (min 6 chars):`,
            inputType: "password",
            placeholder: "New password",
            confirmText: "Reset Password"
          });
          if (!newPw) return;
          try {
            await apiFetch(`/api/users/${id}`, {
              method: "PUT",
              body: JSON.stringify({ password: newPw })
            });
            showToast(`Password reset for ${uname}`);
            loadAuditLog();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });

      list.querySelectorAll("[data-user-del]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const id = btn.dataset.userDel;
          const uname = btn.dataset.username;
          const ok = await showConfirmDialog({
            title: "Delete User Account",
            message: `Delete user '${uname}'? Active sessions for this user will be invalidated.`,
            confirmText: "Delete User",
            destructive: true
          });
          if (!ok) return;
          try {
            await apiFetch(`/api/users/${id}`, { method: "DELETE" });
            showToast(`Deleted user ${uname}`);
            await Promise.all([loadPanelUsers(), loadActiveSessions(), loadAuditLog()]);
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
    } catch (err) {
      list.innerHTML = `<div class="player-sub">${escapeHtml(err.message)}</div>`;
    }
  }

  async function loadActiveSessions() {
    const list = $("sessions-list");
    if (!list) return;
    try {
      const data = await apiFetch("/api/sessions");
      const sessions = data.sessions || [];
      if (!sessions.length) {
        list.innerHTML = `<div class="player-sub">No active sessions.</div>`;
        return;
      }
      list.innerHTML = sessions.map((s) => `
        <div class="m3-table-row">
          <div>
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
              <strong>${escapeHtml(s.username)}</strong>
              <span class="role-badge ${escapeHtml(s.role)}">${escapeHtml(s.role)}</span>
              ${s.isCurrent ? `<span class="status-badge RUNNING">THIS SESSION</span>` : ""}
            </div>
            <div class="player-sub mono">IP: ${escapeHtml(s.clientIp || "unknown")} | Signed in ${escapeHtml(new Date(s.createdAt).toLocaleString())}</div>
          </div>
          <div class="player-actions">
            <button class="btn btn-danger btn-sm" data-revoke-session="${escapeHtml(s.id)}">Revoke</button>
          </div>
        </div>
      `).join("");

      list.querySelectorAll("[data-revoke-session]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          try {
            await apiFetch(`/api/sessions/${btn.dataset.revokeSession}`, { method: "DELETE" });
            showToast("Session revoked");
            await Promise.all([loadActiveSessions(), loadAuditLog()]);
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });
    } catch (err) {
      list.innerHTML = `<div class="player-sub">${escapeHtml(err.message)}</div>`;
    }
  }

  function auditCategoryFor(action) {
    const a = String(action || "").toUpperCase();
    if (a.includes("LOGIN") || a.includes("LOGOUT") || a.includes("PASSWORD")) return "AUTH";
    if (a.startsWith("SERVER_")) return "SERVER";
    if (a === "CONSOLE_CMD") return "CONSOLE";
    if (a.startsWith("FILE_") || a.startsWith("DIR_") || a.includes("UPLOAD") || a.includes("IMPORT")) return "FILES";
    if (a.startsWith("PLAYER")) return "PLAYERS";
    if (a.startsWith("USER_") || a.startsWith("SESSION_")) return "USERS";
    return "SETTINGS";
  }

  function getFilteredAuditEntries() {
    const all = state.auditEntries || [];
    const q = ($("audit-search-input")?.value || "").trim().toLowerCase();
    const cat = $("audit-category-filter")?.value || "ALL";
    const usr = $("audit-user-filter")?.value || "ALL";
    return all.filter((e) => {
      if (cat !== "ALL" && auditCategoryFor(e.action) !== cat) return false;
      if (usr !== "ALL" && e.username !== usr) return false;
      if (q) {
        const hay = `${e.username} ${e.role} ${e.action} ${e.target} ${e.details} ${e.clientIp}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }

  function renderFilteredAuditLog() {
    const tbody = $("audit-table-body");
    const summary = $("audit-summary-line");
    if (!tbody) return;
    const filtered = getFilteredAuditEntries();
    const total = (state.auditEntries || []).length;
    if (summary) {
      summary.textContent = `Showing ${filtered.length} of ${total} audit event(s)`;
    }
    if (!filtered.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="player-sub" style="text-align:center;padding:24px;">No matching audit events found.</td></tr>`;
      return;
    }
    tbody.innerHTML = filtered.map((e) => `
      <tr>
        <td class="mono" style="white-space:nowrap;font-size:12px;">${escapeHtml(new Date(e.timestamp).toLocaleString())}</td>
        <td style="white-space:nowrap;">
          <strong>${escapeHtml(e.username)}</strong>
          <span class="role-badge ${escapeHtml(e.role || "VIEWER")}" style="margin-left:4px;">${escapeHtml(e.role || "")}</span>
        </td>
        <td style="white-space:nowrap;"><span class="role-badge">${escapeHtml(e.action)}</span></td>
        <td>${escapeHtml(e.target || "-")}</td>
        <td class="mono" style="font-size:12px;word-break:break-word;">${escapeHtml(e.details || "")}</td>
        <td class="mono" style="white-space:nowrap;font-size:12px;">${escapeHtml(e.clientIp || "")}</td>
      </tr>
    `).join("");
  }

  function exportAuditCsv() {
    const rows = getFilteredAuditEntries();
    const csvEscape = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
    const header = ["Timestamp", "Username", "Role", "Action", "Target", "Details", "IP"].join(",");
    const lines = rows.map((e) => [
      new Date(e.timestamp).toISOString(),
      e.username,
      e.role,
      e.action,
      e.target,
      e.details,
      e.clientIp
    ].map(csvEscape).join(","));
    const blob = new Blob([[header, ...lines].join("\n")], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `mineserve-audit-log-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  async function loadJavaRuntimes() {
    const list = $("java-runtimes-list");
    const statusEl = $("java-manager-status");
    if (!list) return;
    try {
      const data = await apiFetch("/api/java");
      const runtimes = data.runtimes || [];
      const installing = data.installingVersion;
      if (data.error) {
        statusEl.textContent = `Error: ${data.error}`;
      } else if (installing) {
        statusEl.textContent = `Installing Java ${installing}: ${data.statusMessage || "Working..."}`;
      } else if (data.statusMessage) {
        statusEl.textContent = data.statusMessage;
      } else {
        statusEl.textContent = "Manage OpenJDK runtimes installed on this host.";
      }

      list.innerHTML = runtimes.map((r) => `
        <div class="m3-table-row">
          <div>
            <strong>OpenJDK ${r.version}</strong>
            <span class="status-badge ${r.installed ? "RUNNING" : ""}" style="margin-left:8px;">
              ${r.installed ? "INSTALLED" : (installing === r.version ? "INSTALLING..." : "NOT INSTALLED")}
            </span>
          </div>
          <div class="player-actions">
            ${!r.installed ? `<button type="button" class="btn btn-primary btn-sm" data-install-java="${r.version}" ${installing ? "disabled" : ""}>Install Java ${r.version}</button>` : ""}
          </div>
        </div>
      `).join("");

      list.querySelectorAll("[data-install-java]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const ver = parseInt(btn.dataset.installJava, 10);
          try {
            await apiFetch("/api/java/install", {
              method: "POST",
              body: JSON.stringify({ version: ver })
            });
            showToast(`Started installing OpenJDK ${ver}`);
            loadJavaRuntimes();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });

      if (installing && $("host-tools-modal")?.open) {
        setTimeout(() => {
          if ($("host-tools-modal")?.open) loadJavaRuntimes();
        }, 2500);
      }
    } catch (err) {
      if (statusEl) statusEl.textContent = err.message;
    }
  }

  async function loadUpgradeVersions() {
    const sel = $("upgrade-version-select");
    const lbl = $("upgrade-current-label");
    const buildStatusEl = $("upgrade-build-status");
    const srv = state.servers.find((s) => s.id === state.selectedServerId);
    if (!sel || !srv) return;
    if (lbl) lbl.textContent = `Current: ${srv.type} ${srv.version}`;
    if (buildStatusEl) buildStatusEl.textContent = "Checking latest server build...";
    sel.innerHTML = `<option value="">Loading versions...</option>`;

    apiFetch(`/api/servers/${state.selectedServerId}/build-status`).then((b) => {
      if (!buildStatusEl) return;
      const cur = b.currentBuild ? `Build #${b.currentBuild}` : "Unknown build";
      if (b.latestBuild) {
        if (b.updateAvailable) {
          buildStatusEl.textContent = `Installed: ${cur} · Latest build available: #${b.latestBuild} (click "Update Latest Build" to update without changing Minecraft version)`;
        } else {
          buildStatusEl.textContent = `Installed: ${cur} · You are on the latest build (#${b.latestBuild}) for ${b.currentVersion}.`;
        }
      } else {
        buildStatusEl.textContent = `Upgrade ${b.currentVersion} to another version or reinstall the latest build in place.`;
      }
    }).catch(() => {
      if (buildStatusEl) buildStatusEl.textContent = "Upgrade Minecraft version or update to the latest server build in place.";
    });

    try {
      const data = await apiFetch(`/api/software/versions?type=${encodeURIComponent(srv.type)}`);
      const versions = data.versions || [];
      if (!versions.length) {
        sel.innerHTML = `<option value="">No versions available</option>`;
        return;
      }
      sel.innerHTML = versions.map((v) => `<option value="${escapeHtml(v)}" ${v === srv.version ? "selected" : ""}>${escapeHtml(v)}${v === srv.version ? " (Current)" : ""}</option>`).join("");
    } catch (_) {
      sel.innerHTML = `<option value="">Unable to fetch versions</option>`;
    }
  }

  async function loadAuditLog() {
    const tbody = $("audit-table-body");
    if (!tbody) return;
    const limit = parseInt($("audit-limit-select")?.value, 10) || 250;
    try {
      const data = await apiFetch(`/api/audit?limit=${limit}`);
      state.auditEntries = data.entries || [];
      const userSel = $("audit-user-filter");
      if (userSel) {
        const prev = userSel.value || "ALL";
        const users = Array.from(new Set(state.auditEntries.map((e) => e.username).filter(Boolean))).sort();
        userSel.innerHTML = `<option value="ALL">All Users</option>` + users.map((u) => `<option value="${escapeHtml(u)}">${escapeHtml(u)}</option>`).join("");
        userSel.value = users.includes(prev) ? prev : "ALL";
      }
      renderFilteredAuditLog();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" class="player-sub" style="text-align:center;padding:24px;">${escapeHtml(err.message)}</td></tr>`;
    }
  }

  initTheme();
  bindEvents();
  checkAuth();
})();

