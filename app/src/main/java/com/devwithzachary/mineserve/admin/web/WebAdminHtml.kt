package com.devwithzachary.mineserve.admin.web

object WebAdminHtml {
    const val HTML: String = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MineServe Remote Admin</title>
    <style>
        :root {
            --bg-base: #0B1120;
            --bg-card: #1E293B;
            --bg-card-hover: #26334D;
            --border: #334155;
            --emerald-primary: #10B981;
            --emerald-light: #34D399;
            --emerald-dark: #065F46;
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
            --red: #EF4444;
            --yellow: #F59E0B;
            --blue: #3B82F6;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: var(--bg-base);
            color: var(--text-main);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        header {
            background: #111827;
            border-bottom: 1px solid var(--border);
            padding: 12px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
        }
        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 700;
            font-size: 1.15rem;
            color: var(--emerald-light);
        }
        .brand-icon {
            width: 28px;
            height: 28px;
            background: var(--emerald-primary);
            color: #000;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 900;
            font-size: 16px;
        }
        .header-controls {
            display: flex;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }
        select, input, button, textarea {
            font-family: inherit;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: #0F172A;
            color: var(--text-main);
            padding: 8px 12px;
            font-size: 0.9rem;
            outline: none;
        }
        select:focus, input:focus, textarea:focus {
            border-color: var(--emerald-primary);
        }
        button {
            cursor: pointer;
            font-weight: 600;
            transition: background 0.15s, border-color 0.15s;
        }
        .btn-primary {
            background: var(--emerald-primary);
            color: #064E3B;
            border: none;
        }
        .btn-primary:hover { background: var(--emerald-light); }
        .btn-danger {
            background: rgba(239, 68, 68, 0.2);
            color: #FCA5A5;
            border-color: rgba(239, 68, 68, 0.4);
        }
        .btn-danger:hover { background: rgba(239, 68, 68, 0.35); }
        .btn-secondary {
            background: var(--bg-card);
            color: var(--text-main);
        }
        .btn-secondary:hover { background: var(--bg-card-hover); }
        .badge {
            font-size: 0.75rem;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 6px;
            text-transform: uppercase;
            display: inline-block;
        }
        .badge-running { background: rgba(16, 185, 129, 0.2); color: var(--emerald-light); border: 1px solid rgba(16, 185, 129, 0.4); }
        .badge-stopped { background: rgba(148, 163, 184, 0.2); color: var(--text-muted); border: 1px solid rgba(148, 163, 184, 0.4); }
        .badge-starting { background: rgba(245, 158, 11, 0.2); color: var(--yellow); border: 1px solid rgba(245, 158, 11, 0.4); }
        .badge-error { background: rgba(239, 68, 68, 0.2); color: var(--red); border: 1px solid rgba(239, 68, 68, 0.4); }
        
        nav {
            background: #0F172A;
            border-bottom: 1px solid var(--border);
            display: flex;
            padding: 0 24px;
            overflow-x: auto;
        }
        .nav-item {
            padding: 12px 18px;
            font-size: 0.95rem;
            font-weight: 600;
            color: var(--text-muted);
            cursor: pointer;
            border-bottom: 2px solid transparent;
            transition: color 0.15s, border-color 0.15s;
            white-space: nowrap;
        }
        .nav-item:hover { color: var(--text-main); }
        .nav-item.active {
            color: var(--emerald-light);
            border-bottom-color: var(--emerald-primary);
        }

        main {
            flex: 1;
            padding: 24px;
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
        }
        .view-panel { display: none; }
        .view-panel.active { display: block; }

        /* Cards & Grid */
        .grid-cards {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: 20px;
        }
        .card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            transition: border-color 0.15s;
        }
        .card:hover { border-color: rgba(16, 185, 129, 0.5); }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }
        .card-title {
            font-size: 1.15rem;
            font-weight: 700;
            color: #FFF;
        }
        .card-meta {
            font-size: 0.85rem;
            color: var(--text-muted);
            margin-top: 4px;
        }
        .card-stats {
            display: flex;
            gap: 16px;
            padding: 10px 0;
            border-top: 1px solid var(--border);
            border-bottom: 1px solid var(--border);
            font-size: 0.85rem;
        }
        .stat-item {
            flex: 1;
        }
        .stat-label { color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; font-weight: 700; }
        .stat-value { font-weight: 700; margin-top: 2px; font-size: 0.95rem; }
        .card-actions {
            display: flex;
            gap: 8px;
            margin-top: auto;
        }

        /* Terminal Console */
        .console-container {
            display: flex;
            flex-direction: column;
            background: #050811;
            border: 1px solid var(--border);
            border-radius: 12px;
            height: calc(100vh - 200px);
            min-height: 480px;
            overflow: hidden;
        }
        .console-toolbar {
            background: #0F172A;
            border-bottom: 1px solid var(--border);
            padding: 8px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
        }
        .console-quick-cmds {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
        }
        .cmd-pill {
            background: var(--bg-card);
            border: 1px solid var(--border);
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 0.8rem;
            cursor: pointer;
            color: var(--text-muted);
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        }
        .cmd-pill:hover { color: var(--emerald-light); border-color: var(--emerald-primary); }
        .terminal-output {
            flex: 1;
            padding: 16px;
            overflow-y: auto;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 0.85rem;
            line-height: 1.45;
            color: #E2E8F0;
            white-space: pre-wrap;
            word-break: break-all;
        }
        .log-info { color: #60A5FA; }
        .log-warn { color: var(--yellow); }
        .log-error { color: #F87171; }
        .log-emerald { color: var(--emerald-light); }
        .console-input-row {
            background: #0F172A;
            border-top: 1px solid var(--border);
            padding: 12px 16px;
            display: flex;
            gap: 10px;
        }
        .console-input-row input {
            flex: 1;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        }

        /* File Explorer */
        .file-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 10px 10px 0 0;
            padding: 12px 16px;
            gap: 12px;
            flex-wrap: wrap;
        }
        .breadcrumbs {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 0.9rem;
            color: var(--text-muted);
            overflow-x: auto;
        }
        .crumb {
            color: var(--emerald-light);
            cursor: pointer;
            text-decoration: underline;
        }
        .file-table {
            width: 100%;
            border-collapse: collapse;
            background: #0F172A;
            border: 1px solid var(--border);
            border-top: none;
            border-radius: 0 0 10px 10px;
            overflow: hidden;
        }
        .file-table th, .file-table td {
            padding: 12px 16px;
            text-align: left;
            border-bottom: 1px solid var(--border);
            font-size: 0.9rem;
        }
        .file-table th {
            background: #111827;
            color: var(--text-muted);
            font-size: 0.8rem;
            text-transform: uppercase;
        }
        .file-table tr:hover { background: #1E293B; }
        .file-name {
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: 500;
            cursor: pointer;
        }
        .file-name.is-dir { color: var(--emerald-light); font-weight: 600; }

        /* Telemetry */
        .telemetry-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 20px;
            margin-bottom: 24px;
        }
        .gauge-card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 20px;
            text-align: center;
        }
        .gauge-title { font-size: 0.85rem; color: var(--text-muted); text-transform: uppercase; font-weight: 700; margin-bottom: 8px; }
        .gauge-num { font-size: 2.5rem; font-weight: 900; }
        .gauge-desc { font-size: 0.85rem; color: var(--text-muted); margin-top: 4px; }
        .progress-bar-bg {
            background: #0F172A;
            border-radius: 6px;
            height: 10px;
            width: 100%;
            overflow: hidden;
            margin-top: 10px;
        }
        .progress-bar-fill {
            background: var(--emerald-primary);
            height: 100%;
            transition: width 0.3s;
        }

        /* Modal */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.75);
            display: none;
            align-items: center;
            justify-content: center;
            padding: 20px;
            z-index: 1000;
        }
        .modal-overlay.active { display: flex; }
        .modal {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 14px;
            max-width: 900px;
            width: 100%;
            max-height: 90vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        .modal-header {
            padding: 16px 20px;
            border-bottom: 1px solid var(--border);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .modal-body {
            padding: 20px;
            flex: 1;
            overflow-y: auto;
        }
        .modal-footer {
            padding: 16px 20px;
            border-top: 1px solid var(--border);
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }

        /* Auth Screen */
        .auth-container {
            max-width: 420px;
            margin: 80px auto;
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 32px;
            display: flex;
            flex-direction: column;
            gap: 18px;
            text-align: center;
        }
        .auth-container h2 { font-size: 1.4rem; color: #FFF; }
        .auth-container p { color: var(--text-muted); font-size: 0.9rem; }
        .auth-error {
            background: rgba(239, 68, 68, 0.2);
            border: 1px solid rgba(239, 68, 68, 0.4);
            color: #FCA5A5;
            padding: 10px;
            border-radius: 8px;
            font-size: 0.85rem;
            display: none;
        }
    </style>
</head>
<body>

    <!-- Authentication View -->
    <div id="authView" class="auth-container">
        <div style="display: flex; justify-content: center; margin-bottom: 6px;">
            <div class="brand-icon" style="width: 48px; height: 48px; font-size: 24px;">M</div>
        </div>
        <h2>MineServe Remote Admin</h2>
        <p>Enter your administrator credentials configured in the MineServe app.</p>
        <div id="authError" class="auth-error"></div>
        <form id="authForm" style="display: flex; flex-direction: column; gap: 14px; text-align: left;">
            <div>
                <label style="font-size: 0.8rem; color: var(--text-muted); font-weight: 700; text-transform: uppercase;">Username</label>
                <input type="text" id="authUsername" value="admin" style="width: 100%; margin-top: 4px;" required autocomplete="username">
            </div>
            <div>
                <label style="font-size: 0.8rem; color: var(--text-muted); font-weight: 700; text-transform: uppercase;">Password</label>
                <input type="password" id="authPassword" style="width: 100%; margin-top: 4px;" required autocomplete="current-password" placeholder="Enter password">
            </div>
            <button type="submit" class="btn-primary" style="padding: 12px; margin-top: 6px;">Sign In to Dashboard</button>
        </form>
    </div>

    <!-- Main Admin Shell -->
    <div id="adminShell" style="display: none; flex-direction: column; min-height: 100vh;">
        <header>
            <div class="brand">
                <div class="brand-icon">M</div>
                <span>MineServe Admin</span>
            </div>
            <div class="header-controls">
                <div style="font-size: 0.85rem; color: var(--text-muted);" id="deviceInfoLabel">Device: Android</div>
                <select id="serverSelect" style="min-width: 180px;"></select>
                <button id="logoutBtn" class="btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;">Logout</button>
            </div>
        </header>

        <nav>
            <div class="nav-item active" data-tab="dashboard">Dashboard</div>
            <div class="nav-item" data-tab="console">Console</div>
            <div class="nav-item" data-tab="files">File Manager</div>
            <div class="nav-item" data-tab="telemetry">Live Telemetry</div>
        </nav>

        <main>
            <!-- 1. Dashboard View -->
            <div id="tab-dashboard" class="view-panel active">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <h2 style="font-size: 1.4rem;">Server Management</h2>
                        <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 2px;">Overview of all hosted Minecraft servers.</p>
                    </div>
                    <button id="refreshServersBtn" class="btn-secondary">Refresh</button>
                </div>
                <div id="serversGrid" class="grid-cards"></div>
            </div>

            <!-- 2. Console View -->
            <div id="tab-console" class="view-panel">
                <div class="console-container">
                    <div class="console-toolbar">
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <span id="consoleServerTitle" style="font-weight: 700;">Terminal</span>
                            <span id="consoleStatusBadge" class="badge badge-stopped">Offline</span>
                        </div>
                        <div class="console-quick-cmds">
                            <span class="cmd-pill" onclick="sendQuickCmd('list')">list</span>
                            <span class="cmd-pill" onclick="sendQuickCmd('tps')">tps</span>
                            <span class="cmd-pill" onclick="sendQuickCmd('say Hello!')">say Hello</span>
                            <span class="cmd-pill" onclick="sendQuickCmd('save-all')">save-all</span>
                            <span class="cmd-pill" onclick="sendQuickCmd('help')">help</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <label style="font-size: 0.8rem; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px;">
                                <input type="checkbox" id="autoScrollCheck" checked> Auto-scroll
                            </label>
                            <button id="clearConsoleBtn" class="btn-secondary" style="padding: 4px 10px; font-size: 0.75rem;">Clear</button>
                        </div>
                    </div>
                    <div id="terminalOutput" class="terminal-output">Connecting to server console...</div>
                    <form id="consoleForm" class="console-input-row">
                        <input type="text" id="consoleInput" placeholder="Type a server command (e.g. /gamemode creative Steve)..." autocomplete="off">
                        <button type="submit" class="btn-primary" style="padding: 8px 18px;">Send</button>
                    </form>
                </div>
            </div>

            <!-- 3. Files View -->
            <div id="tab-files" class="view-panel">
                <div class="file-toolbar">
                    <div class="breadcrumbs" id="fileBreadcrumbs">
                        <span class="crumb" onclick="loadFiles('')">root</span>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button id="refreshFilesBtn" class="btn-secondary" style="font-size: 0.8rem;">Refresh</button>
                    </div>
                </div>
                <table class="file-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Size</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="filesTableBody"></tbody>
                </table>
            </div>

            <!-- 4. Telemetry View -->
            <div id="tab-telemetry" class="view-panel">
                <div class="telemetry-grid">
                    <div class="gauge-card">
                        <div class="gauge-title">Server Tick Rate (TPS)</div>
                        <div class="gauge-num" id="telemetryTps" style="color: var(--emerald-light);">20.0</div>
                        <div class="gauge-desc" id="telemetryTpsStatus">Optimal (Target 20.0)</div>
                    </div>
                    <div class="gauge-card">
                        <div class="gauge-title">Tick Duration (MSPT)</div>
                        <div class="gauge-num" id="telemetryMspt" style="color: var(--emerald-light);">15.0 ms</div>
                        <div class="gauge-desc" id="telemetryMsptStatus">Budget: 50.0 ms max</div>
                    </div>
                    <div class="gauge-card">
                        <div class="gauge-title">Server Memory</div>
                        <div class="gauge-num" id="telemetryRam" style="color: #60A5FA;">0 MB</div>
                        <div class="progress-bar-bg">
                            <div id="telemetryRamFill" class="progress-bar-fill" style="width: 0%;"></div>
                        </div>
                        <div class="gauge-desc" id="telemetryRamDesc">Allocated: 2048 MB</div>
                    </div>
                    <div class="gauge-card">
                        <div class="gauge-title">Online Players</div>
                        <div class="gauge-num" id="telemetryPlayersCount" style="color: #FFF;">0</div>
                        <div class="gauge-desc" id="telemetryPlayersList">No players online</div>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <!-- Edit File Modal -->
    <div id="editorModal" class="modal-overlay">
        <div class="modal">
            <div class="modal-header">
                <div>
                    <h3 id="editorModalTitle" style="font-size: 1.1rem; color: #FFF;">Edit File</h3>
                    <div id="editorModalPath" style="font-size: 0.8rem; color: var(--text-muted); font-family: monospace;"></div>
                </div>
                <button onclick="closeEditorModal()" class="btn-secondary" style="padding: 4px 10px;">X</button>
            </div>
            <div class="modal-body" style="padding: 0;">
                <textarea id="editorTextarea" style="width: 100%; height: 500px; border: none; border-radius: 0; background: #050811; font-family: monospace; font-size: 0.85rem; padding: 16px; resize: none;"></textarea>
            </div>
            <div class="modal-footer">
                <button onclick="closeEditorModal()" class="btn-secondary">Cancel</button>
                <button id="saveFileBtn" class="btn-primary">Save Changes</button>
            </div>
        </div>
    </div>

    <script>
        let sessionToken = localStorage.getItem("mineserve_admin_token") || "";
        let currentServers = [];
        let selectedServerId = "";
        let currentPath = "";
        let pollTimer = null;
        let commandHistory = [];
        let historyIndex = -1;

        async function api(path, options) {
            options = options || {};
            options.headers = options.headers || {};
            if (sessionToken) {
                options.headers["Authorization"] = "Bearer " + sessionToken;
            }
            if (options.body && typeof options.body === "object") {
                options.headers["Content-Type"] = "application/json";
                options.body = JSON.stringify(options.body);
            }
            const res = await fetch(path, options);
            if (res.status === 401) {
                handleUnauthorized();
                throw new Error("Unauthorized");
            }
            if (!res.ok) {
                let errText = res.statusText;
                try {
                    const j = await res.json();
                    if (j.error) errText = j.error;
                } catch (_) {}
                throw new Error(errText);
            }
            return res.json();
        }

        function handleUnauthorized() {
            sessionToken = "";
            localStorage.removeItem("mineserve_admin_token");
            document.getElementById("authView").style.display = "flex";
            document.getElementById("adminShell").style.display = "none";
            clearInterval(pollTimer);
        }

        // Auth Form
        document.getElementById("authForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const errBox = document.getElementById("authError");
            errBox.style.display = "none";
            const u = document.getElementById("authUsername").value.trim();
            const p = document.getElementById("authPassword").value;
            try {
                const res = await fetch("/api/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ username: u, password: p })
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.error || "Authentication failed");
                }
                sessionToken = data.token;
                localStorage.setItem("mineserve_admin_token", sessionToken);
                showDashboard();
            } catch (err) {
                errBox.innerText = err.message;
                errBox.style.display = "block";
            }
        });

        document.getElementById("logoutBtn").addEventListener("click", async () => {
            try { await api("/api/logout", { method: "POST" }); } catch (_) {}
            handleUnauthorized();
        });

        async function initApp() {
            if (!sessionToken) {
                handleUnauthorized();
                return;
            }
            try {
                const me = await api("/api/me");
                if (me.authenticated) {
                    showDashboard();
                } else {
                    handleUnauthorized();
                }
            } catch (_) {
                handleUnauthorized();
            }
        }

        async function showDashboard() {
            document.getElementById("authView").style.display = "none";
            document.getElementById("adminShell").style.display = "flex";
            await loadSystemInfo();
            await loadServers();
            startPolling();
        }

        async function loadSystemInfo() {
            try {
                const sys = await api("/api/system");
                document.getElementById("deviceInfoLabel").innerText = sys.device + " (Android " + sys.android + ") • Free RAM: " + sys.freeRamMb + " MB";
            } catch (_) {}
        }

        async function loadServers() {
            try {
                currentServers = await api("/api/servers");
                renderServerSelect();
                renderServersGrid();
                if (currentServers.length > 0 && !selectedServerId) {
                    selectedServerId = currentServers[0].id;
                    document.getElementById("serverSelect").value = selectedServerId;
                }
            } catch (err) {
                console.error("Failed loading servers", err);
            }
        }

        function renderServerSelect() {
            const select = document.getElementById("serverSelect");
            select.innerHTML = "";
            currentServers.forEach(s => {
                const opt = document.createElement("option");
                opt.value = s.id;
                opt.innerText = s.name + " (" + s.type + " " + s.version + ")";
                select.appendChild(opt);
            });
            if (selectedServerId) select.value = selectedServerId;
            select.onchange = (e) => {
                selectedServerId = e.target.value;
                onServerChanged();
            };
        }

        function onServerChanged() {
            const s = currentServers.find(x => x.id === selectedServerId);
            if (s) {
                document.getElementById("consoleServerTitle").innerText = s.name;
            }
            refreshConsole();
            loadFiles(currentPath);
            loadTelemetry();
        }

        function renderServersGrid() {
            const grid = document.getElementById("serversGrid");
            grid.innerHTML = "";
            if (currentServers.length === 0) {
                grid.innerHTML = "<div style='color: var(--text-muted);'>No Minecraft servers found on this device.</div>";
                return;
            }
            currentServers.forEach(s => {
                const isRunning = s.status === "RUNNING";
                const isStarting = s.status === "STARTING";
                const badgeClass = isRunning ? "badge-running" : (isStarting ? "badge-starting" : "badge-stopped");
                const card = document.createElement("div");
                card.className = "card";
                let actionButtons = "";
                if (isRunning) {
                    actionButtons = "<button class='btn-danger' style='flex: 1;' onclick='stopServer(\"" + s.id + "\")'>Stop</button>" +
                                   "<button class='btn-secondary' onclick='restartServer(\"" + s.id + "\")'>Restart</button>";
                } else {
                    actionButtons = "<button class='btn-primary' style='flex: 1;' onclick='startServer(\"" + s.id + "\")'>Start Server</button>";
                }
                card.innerHTML = 
                    "<div class='card-header'>" +
                        "<div>" +
                            "<div class='card-title'>" + escapeHtml(s.name) + "</div>" +
                            "<div class='card-meta'>" + s.type + " • Version " + s.version + " • Port " + s.port + "</div>" +
                        "</div>" +
                        "<span class='badge " + badgeClass + "'>" + s.status + "</span>" +
                    "</div>" +
                    "<div class='card-stats'>" +
                        "<div class='stat-item'>" +
                            "<div class='stat-label'>RAM</div>" +
                            "<div class='stat-value'>" + s.allocatedRamMb + " MB</div>" +
                        "</div>" +
                        "<div class='stat-item'>" +
                            "<div class='stat-label'>Actions</div>" +
                            "<div class='stat-value' style='font-size: 0.85rem; font-weight: 500;'>" +
                                "<a href='javascript:void(0)' onclick='selectAndNav(\"" + s.id + "\", \"console\")' style='color: var(--emerald-light); text-decoration: none;'>Console</a> • " +
                                "<a href='javascript:void(0)' onclick='selectAndNav(\"" + s.id + "\", \"files\")' style='color: var(--emerald-light); text-decoration: none;'>Files</a>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "<div class='card-actions'>" +
                        actionButtons +
                    "</div>";
                grid.appendChild(card);
            });
        }

        function selectAndNav(serverId, tabName) {
            selectedServerId = serverId;
            document.getElementById("serverSelect").value = serverId;
            onServerChanged();
            switchTab(tabName);
        }

        async function startServer(id) {
            try {
                await api("/api/servers/" + id + "/start", { method: "POST" });
                await loadServers();
            } catch (e) { alert("Failed to start server: " + e.message); }
        }

        async function stopServer(id) {
            try {
                await api("/api/servers/" + id + "/stop", { method: "POST" });
                await loadServers();
            } catch (e) { alert("Failed to stop server: " + e.message); }
        }

        async function restartServer(id) {
            try {
                await api("/api/servers/" + id + "/restart", { method: "POST" });
                await loadServers();
            } catch (e) { alert("Failed to restart server: " + e.message); }
        }

        // Navigation Tabs
        document.querySelectorAll(".nav-item").forEach(item => {
            item.addEventListener("click", () => {
                switchTab(item.getAttribute("data-tab"));
            });
        });

        function switchTab(tab) {
            document.querySelectorAll(".nav-item").forEach(x => x.classList.remove("active"));
            document.querySelectorAll(".view-panel").forEach(x => x.classList.remove("active"));
            const nav = document.querySelector(".nav-item[data-tab='" + tab + "']");
            if (nav) nav.classList.add("active");
            const panel = document.getElementById("tab-" + tab);
            if (panel) panel.classList.add("active");
            if (tab === "console") refreshConsole();
            if (tab === "files") loadFiles(currentPath);
            if (tab === "telemetry") loadTelemetry();
        }

        // Console Tab
        async function refreshConsole() {
            if (!selectedServerId) return;
            try {
                const res = await api("/api/servers/" + selectedServerId + "/console");
                const out = document.getElementById("terminalOutput");
                const s = currentServers.find(x => x.id === selectedServerId);
                if (s) {
                    const badge = document.getElementById("consoleStatusBadge");
                    badge.className = "badge " + (s.status === "RUNNING" ? "badge-running" : (s.status === "STARTING" ? "badge-starting" : "badge-stopped"));
                    badge.innerText = s.status;
                }
                if (res.lines && res.lines.length > 0) {
                    out.innerHTML = res.lines.map(formatLogLine).join("\n");
                } else {
                    out.innerHTML = "<span style='color: var(--text-muted);'>No output logged.</span>";
                }
                if (document.getElementById("autoScrollCheck").checked) {
                    out.scrollTop = out.scrollHeight;
                }
            } catch (_) {}
        }

        function formatLogLine(line) {
            const escaped = escapeHtml(line);
            if (escaped.includes("[INFO]") || escaped.includes("INFO]:")) {
                return "<span class='log-info'>" + escaped + "</span>";
            } else if (escaped.includes("[WARN]") || escaped.includes("WARN]:")) {
                return "<span class='log-warn'>" + escaped + "</span>";
            } else if (escaped.includes("[ERROR]") || escaped.includes("ERROR]:") || escaped.includes("Exception")) {
                return "<span class='log-error'>" + escaped + "</span>";
            }
            return escaped;
        }

        document.getElementById("consoleForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const input = document.getElementById("consoleInput");
            const cmd = input.value.trim();
            if (!cmd || !selectedServerId) return;
            commandHistory.push(cmd);
            historyIndex = commandHistory.length;
            input.value = "";
            try {
                await api("/api/servers/" + selectedServerId + "/console", {
                    method: "POST",
                    body: { command: cmd }
                });
                setTimeout(refreshConsole, 300);
            } catch (err) {
                alert("Failed to send command: " + err.message);
            }
        });

        function sendQuickCmd(cmd) {
            document.getElementById("consoleInput").value = cmd;
            document.getElementById("consoleForm").dispatchEvent(new Event("submit"));
        }

        document.getElementById("consoleInput").addEventListener("keydown", (e) => {
            if (e.key === "ArrowUp") {
                if (historyIndex > 0) {
                    historyIndex--;
                    e.target.value = commandHistory[historyIndex];
                }
                e.preventDefault();
            } else if (e.key === "ArrowDown") {
                if (historyIndex < commandHistory.length - 1) {
                    historyIndex++;
                    e.target.value = commandHistory[historyIndex];
                } else {
                    historyIndex = commandHistory.length;
                    e.target.value = "";
                }
                e.preventDefault();
            }
        });

        document.getElementById("clearConsoleBtn").addEventListener("click", () => {
            document.getElementById("terminalOutput").innerText = "";
        });

        // Files Tab
        async function loadFiles(path) {
            if (!selectedServerId) return;
            currentPath = path;
            renderBreadcrumbs(path);
            try {
                const res = await api("/api/servers/" + selectedServerId + "/files?path=" + encodeURIComponent(path));
                renderFilesTable(res.files || []);
            } catch (err) {
                console.error("Failed to load files", err);
            }
        }

        function renderBreadcrumbs(path) {
            const bc = document.getElementById("fileBreadcrumbs");
            bc.innerHTML = "<span class='crumb' onclick='loadFiles(\"\")'>root</span>";
            if (!path) return;
            const segments = path.split("/").filter(Boolean);
            let accum = "";
            segments.forEach(seg => {
                accum += (accum ? "/" : "") + seg;
                const segPath = accum;
                bc.innerHTML += " / <span class='crumb' onclick='loadFiles(\"" + escapeHtml(segPath) + "\")'>" + escapeHtml(seg) + "</span>";
            });
        }

        function renderFilesTable(files) {
            const tbody = document.getElementById("filesTableBody");
            tbody.innerHTML = "";
            if (currentPath) {
                const parentPath = currentPath.includes("/") ? currentPath.substring(0, currentPath.lastIndexOf("/")) : "";
                const tr = document.createElement("tr");
                tr.innerHTML = 
                    "<td colspan='3'>" +
                        "<div class='file-name is-dir' onclick='loadFiles(\"" + escapeHtml(parentPath) + "\")'>📁 .. (Parent Directory)</div>" +
                    "</td>";
                tbody.appendChild(tr);
            }
            if (files.length === 0) {
                const tr = document.createElement("tr");
                tr.innerHTML = "<td colspan='3' style='color: var(--text-muted); text-align: center; padding: 24px;'>This directory is empty.</td>";
                tbody.appendChild(tr);
                return;
            }
            files.forEach(f => {
                const tr = document.createElement("tr");
                const icon = f.isDirectory ? "📁" : (f.isEditable ? "📄" : "📦");
                let clickAction = "";
                if (f.isDirectory) {
                    clickAction = "loadFiles(\"" + escapeHtml(f.relativePath) + "\")";
                } else if (f.isEditable) {
                    clickAction = "editFile(\"" + escapeHtml(f.relativePath) + "\")";
                }

                let actionsHtml = "";
                if (f.isEditable) {
                    actionsHtml += "<button class='btn-secondary' style='padding: 4px 10px; font-size: 0.8rem;' onclick='editFile(\"" + escapeHtml(f.relativePath) + "\")'>Edit</button> ";
                }
                actionsHtml += "<button class='btn-danger' style='padding: 4px 10px; font-size: 0.8rem;' onclick='deleteFile(\"" + escapeHtml(f.relativePath) + "\")'>Delete</button>";

                tr.innerHTML = 
                    "<td>" +
                        "<div class='file-name " + (f.isDirectory ? "is-dir" : "") + "' onclick='" + clickAction + "'>" +
                            "<span>" + icon + "</span> " +
                            "<span>" + escapeHtml(f.name) + "</span>" +
                        "</div>" +
                    "</td>" +
                    "<td style='color: var(--text-muted); font-size: 0.85rem;'>" +
                        (f.isDirectory ? "-" : formatBytes(f.sizeBytes)) +
                    "</td>" +
                    "<td>" +
                        "<div style='display: flex; gap: 8px;'>" +
                            actionsHtml +
                        "</div>" +
                    "</td>";
                tbody.appendChild(tr);
            });
        }

        async function editFile(path) {
            try {
                const data = await api("/api/servers/" + selectedServerId + "/files/read?path=" + encodeURIComponent(path));
                document.getElementById("editorModalTitle").innerText = path.split("/").pop();
                document.getElementById("editorModalPath").innerText = path;
                document.getElementById("editorTextarea").value = data.content;
                document.getElementById("editorModal").classList.add("active");
                document.getElementById("saveFileBtn").onclick = async () => {
                    const newContent = document.getElementById("editorTextarea").value;
                    try {
                        await api("/api/servers/" + selectedServerId + "/files/write", {
                            method: "POST",
                            body: { path: path, content: newContent }
                        });
                        closeEditorModal();
                        loadFiles(currentPath);
                    } catch (e) {
                        alert("Failed to save: " + e.message);
                    }
                };
            } catch (err) {
                alert("Failed to read file: " + err.message);
            }
        }

        function closeEditorModal() {
            document.getElementById("editorModal").classList.remove("active");
        }

        async function deleteFile(path) {
            if (!confirm("Are you sure you want to delete " + path + "? This cannot be undone.")) return;
            try {
                await api("/api/servers/" + selectedServerId + "/files/delete", {
                    method: "POST",
                    body: { path: path }
                });
                loadFiles(currentPath);
            } catch (err) {
                alert("Failed to delete: " + err.message);
            }
        }

        // Telemetry Tab
        async function loadTelemetry() {
            if (!selectedServerId) return;
            try {
                const data = await api("/api/servers/" + selectedServerId);
                const m = data.metrics || {};
                const tps = m.tps !== undefined ? m.tps.toFixed(1) : "20.0";
                const mspt = m.mspt !== undefined ? m.mspt.toFixed(1) : "15.0";
                const tpsNum = parseFloat(tps);
                const msptNum = parseFloat(mspt);

                document.getElementById("telemetryTps").innerText = tps;
                document.getElementById("telemetryTps").style.color = tpsNum >= 19.5 ? "var(--emerald-light)" : (tpsNum >= 15.0 ? "var(--yellow)" : "var(--red)");
                document.getElementById("telemetryTpsStatus").innerText = tpsNum >= 19.5 ? "Optimal" : (tpsNum >= 15.0 ? "Moderate Load" : "Lag Detected");

                document.getElementById("telemetryMspt").innerText = mspt + " ms";
                document.getElementById("telemetryMspt").style.color = msptNum <= 50.0 ? "var(--emerald-light)" : "var(--yellow)";
                document.getElementById("telemetryMsptStatus").innerText = msptNum <= 50.0 ? "Within 50.0 ms budget" : "Exceeding 50.0 ms budget";

                const ramUsed = m.ramUsedMb || 0;
                const ramMax = data.allocatedRamMb || 2048;
                document.getElementById("telemetryRam").innerText = ramUsed + " MB";
                const pct = Math.min(100, Math.round((ramUsed / ramMax) * 100));
                document.getElementById("telemetryRamFill").style.width = pct + "%";
                document.getElementById("telemetryRamDesc").innerText = "Allocated: " + ramMax + " MB (" + pct + "% used)";

                const pCount = m.onlinePlayerCount || 0;
                document.getElementById("telemetryPlayersCount").innerText = pCount;
                if (m.onlinePlayers && m.onlinePlayers.length > 0) {
                    document.getElementById("telemetryPlayersList").innerText = m.onlinePlayers.join(", ");
                } else {
                    document.getElementById("telemetryPlayersList").innerText = "No players online";
                }
            } catch (_) {}
        }

        function startPolling() {
            if (pollTimer) clearInterval(pollTimer);
            pollTimer = setInterval(() => {
                const activeTab = document.querySelector(".nav-item.active")?.getAttribute("data-tab");
                if (activeTab === "console") refreshConsole();
                else if (activeTab === "telemetry") loadTelemetry();
                else if (activeTab === "dashboard") loadServers();
            }, 1800);
        }

        function formatBytes(bytes) {
            if (bytes === 0) return "0 B";
            const k = 1024;
            const sizes = ["B", "KB", "MB", "GB"];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
        }

        function escapeHtml(str) {
            if (!str) return "";
            return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
        }

        document.getElementById("refreshServersBtn").onclick = loadServers;
        document.getElementById("refreshFilesBtn").onclick = () => loadFiles(currentPath);

        window.onload = initApp;
    </script>
</body>
</html>"""
}
