// ======= Modal Confirm Utility =======
function showConfirm(message, onYes, onNo) {
  let modal = document.getElementById('confirmModal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'confirmModal';
    modal.innerHTML = `
      <div class="custom-modal-overlay">
        <div class="custom-modal-box">
          <div class="custom-modal-title" id="confirmModalMsg"></div>
          <div class="custom-modal-actions">
            <button id="confirmModalYes" class="custom-modal-btn custom-modal-btn-yes">Yes</button>
            <button id="confirmModalNo" class="custom-modal-btn custom-modal-btn-no">No</button>
          </div>
        </div>
      </div>
      <style>
        .custom-modal-overlay {
          position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
          background: rgba(30, 41, 59, 0.45); z-index: 99999;
          display: flex; align-items: center; justify-content: center;
        }
        .custom-modal-box {
          background: linear-gradient(135deg, #fff 70%, #f3f4f6 100%);
          padding: 36px 32px 24px 32px; border-radius: 18px; min-width: 340px;
          box-shadow: 0 8px 32px rgba(0,0,0,0.18);
          text-align: center; position: relative;
          border: 2px solid #e0e7ef;
        }
        .custom-modal-title {
          font-size: 20px; font-weight: 700; color: #1e293b; margin-bottom: 22px;
        }
        .custom-modal-actions {
          display: flex; gap: 22px; justify-content: center;
        }
        .custom-modal-btn {
          padding: 10px 32px; border: none; border-radius: 8px;
          font-size: 16px; font-weight: 600; cursor: pointer; transition: 0.18s;
        }
        .custom-modal-btn-yes {
          background: linear-gradient(90deg, #ef4444 60%, #f87171 100%);
          color: #fff;
          box-shadow: 0 2px 8px rgba(239,68,68,0.08);
        }
        .custom-modal-btn-yes:hover {
          background: linear-gradient(90deg, #dc2626 60%, #f87171 100%);
        }
        .custom-modal-btn-no {
          background: #e0e7ef; color: #334155;
        }
        .custom-modal-btn-no:hover {
          background: #cbd5e1;
        }
      </style>
    `;
    document.body.appendChild(modal);
  }
  modal.style.display = 'flex';
  modal.querySelector('#confirmModalMsg').innerText = message;
  modal.querySelector('#confirmModalYes').onclick = () => { modal.style.display = 'none'; if(onYes) onYes(); };
  modal.querySelector('#confirmModalNo').onclick = () => { modal.style.display = 'none'; if(onNo) onNo(); };
}
document.addEventListener("DOMContentLoaded", () => {
  checkAdminSession();
  setupLogin();
  setupNavigation();
  setupLogout();
});

let lastCriticalAlertCount = -1;
const metricAnimationFrames = {};

function showCriticalRequestAlert(count) {
  if (!count || count <= 0) return;
  if (lastCriticalAlertCount === count) return;
  lastCriticalAlertCount = count;

  let modal = document.getElementById("criticalAlertModal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "criticalAlertModal";
    modal.innerHTML = `
      <div class="critical-alert-overlay">
        <div class="critical-alert-card">
          <div class="critical-alert-badge">Emergency Alert</div>
          <h3 id="criticalAlertTitle">Critical Requests Pending</h3>
          <p id="criticalAlertText"></p>
          <div class="critical-alert-actions">
            <button id="criticalAlertView" class="critical-alert-btn critical-alert-btn-primary">View Requests</button>
            <button id="criticalAlertClose" class="critical-alert-btn critical-alert-btn-secondary">Close</button>
          </div>
        </div>
      </div>
      <style>
        .critical-alert-overlay {
          position: fixed;
          inset: 0;
          background: rgba(2, 6, 23, 0.64);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 100000;
          animation: criticalOverlayFadeIn .25s ease-out;
        }
        .critical-alert-card {
          width: min(92vw, 460px);
          background: linear-gradient(180deg, #fff5f5 0%, #fee2e2 100%);
          border: 2px solid #ef4444;
          border-radius: 18px;
          box-shadow: 0 24px 80px rgba(127, 29, 29, 0.4);
          padding: 22px 20px 18px;
          text-align: center;
          animation: criticalCardIn .45s cubic-bezier(.2,.8,.2,1), criticalPulse 1.8s ease-in-out infinite;
        }
        .critical-alert-badge {
          display: inline-block;
          background: #b91c1c;
          color: #fff;
          border-radius: 999px;
          padding: 6px 12px;
          font-size: 12px;
          font-weight: 700;
          margin-bottom: 10px;
          letter-spacing: 0.3px;
        }
        .critical-alert-card h3 {
          margin: 0 0 8px 0;
          color: #7f1d1d;
          font-size: 24px;
          font-weight: 800;
        }
        .critical-alert-card p {
          margin: 0;
          color: #1f2937;
          font-size: 15px;
          line-height: 1.5;
        }
        .critical-alert-actions {
          margin-top: 18px;
          display: flex;
          gap: 10px;
          justify-content: center;
          flex-wrap: wrap;
        }
        .critical-alert-btn {
          border: none;
          border-radius: 10px;
          padding: 10px 14px;
          font-size: 14px;
          font-weight: 700;
          cursor: pointer;
          transition: transform .2s ease, box-shadow .2s ease;
        }
        .critical-alert-btn:hover {
          transform: translateY(-1px);
        }
        .critical-alert-btn-primary {
          background: linear-gradient(135deg, #dc2626, #991b1b);
          color: #fff;
          box-shadow: 0 8px 20px rgba(153, 27, 27, 0.35);
        }
        .critical-alert-btn-secondary {
          background: #e2e8f0;
          color: #1e293b;
        }
        @keyframes criticalOverlayFadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes criticalCardIn {
          from { transform: translateY(24px) scale(.94); opacity: 0; }
          to { transform: translateY(0) scale(1); opacity: 1; }
        }
        @keyframes criticalPulse {
          0%, 100% { box-shadow: 0 24px 80px rgba(127, 29, 29, 0.4); }
          50% { box-shadow: 0 18px 60px rgba(220, 38, 38, 0.55); }
        }
      </style>
    `;
    document.body.appendChild(modal);
  }

  const text = modal.querySelector("#criticalAlertText");
  const closeBtn = modal.querySelector("#criticalAlertClose");
  const viewBtn = modal.querySelector("#criticalAlertView");

  if (text) {
    text.textContent = `${count} critical blood request(s) are awaiting admin approval. Please review immediately.`;
  }

  modal.style.display = "block";
  if (closeBtn) closeBtn.onclick = () => { modal.style.display = "none"; };
  if (viewBtn) {
    viewBtn.onclick = () => {
      modal.style.display = "none";
      const receiverLink = document.querySelector('.sidebar a[data-page="receivers"]');
      if (receiverLink) receiverLink.click();
      else loadReceiverRequests();
    };
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function setMetricValue(id, value, suffix = "") {
  const el = document.getElementById(id);
  if (!el) return;

  const target = Number(value || 0);
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion) {
    el.innerText = `${target}${suffix}`;
    return;
  }

  const startedAt = performance.now();
  const duration = 850;

  const tick = (now) => {
    const progress = Math.min((now - startedAt) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.innerText = `${Math.round(target * eased)}${suffix}`;
    if (progress < 1) {
      metricAnimationFrames[id] = requestAnimationFrame(tick);
    } else {
      el.innerText = `${target}${suffix}`;
    }
  };

  if (metricAnimationFrames[id]) cancelAnimationFrame(metricAnimationFrames[id]);
  metricAnimationFrames[id] = requestAnimationFrame(tick);
}

function openAdminPage(pageId) {
  const link = document.querySelector(`.sidebar a[data-page="${pageId}"]`);
  if (link) link.click();
}

// Provide loader and toast helpers if not available yet
if (typeof window.showLoader === 'undefined') {
  window.showLoader = function(show) {
    const l = document.getElementById('globalLoader');
    if (!l) return;
    l.style.display = show ? 'flex' : 'none';
  }
}

if (typeof window.showToast === 'undefined') {
  window.showToast = function(message, type='success', timeout=3500) {
    const c = document.getElementById('toastContainer');
    if (!c) {
        if (typeof localShow === 'function') localShow(message, type === 'success' ? 'success' : 'error');
        else alert(message);
      return;
    }
    const t = document.createElement('div');
    t.className = 'toast ' + (type === 'error' ? 'error' : 'success');
    t.innerText = message;
    c.appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; setTimeout(()=>t.remove(),300); }, timeout);
  }
}

/* ================= SESSION CHECK ================= */
function checkAdminSession() {
  showLoader(true);
  fetch("/api/admin/check-session", { credentials: "include" })
    .then(res => res.ok ? res.json() : { authenticated: false })
    .then(data => {
      showLoader(false);
      if (!data || data.authenticated === false) {
        showLogin();
      } else {
        showDashboard();
      }
    })
    .catch(() => { showLoader(false); showLogin(); });
}

function showLogin() {
  document.getElementById("loginPage").classList.remove("hidden");
  document.getElementById("adminWrapper").classList.remove("logged-in");
}

function showDashboard() {
  document.getElementById("loginPage").classList.add("hidden");
  document.getElementById("adminWrapper").classList.add("logged-in");
  updatePageHeader("dashboard");

  // Load dashboard data only once
  loadUsers();
  loadDonors();
  loadAllRequests();
  loadReceiverRequests();
  loadBloodStockOverview();
  loadAdminInsights();
  loadHospitalsAdmin();
  loadBloodBanksAdmin();
  loadBloodStocksAdmin();
  loadAdminSettings();
}

function updatePageHeader(pageId) {
  const page = document.getElementById(pageId);
  if (!page) return;

  const titleEl = document.getElementById("adminPageTitle");
  const hintEl = document.getElementById("adminPageHint");
  if (!titleEl || !hintEl) return;

  titleEl.textContent = page.dataset.title || "Blood Admin";
  hintEl.textContent = page.dataset.hint || "Manage your platform data.";
}

/* ================= LOGIN ================= */
function setupLogin() {
  const form = document.getElementById("loginForm");
  const error = document.getElementById("errorMsg");

  form.addEventListener("submit", e => {
    e.preventDefault();

    showLoader(true);
    fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({
        email: document.getElementById("adminEmail").value,
        password: document.getElementById("adminPass").value
      })
    })
      .then(res => {
        showLoader(false);
        if (res.status === 401) {
          error.innerText = "Invalid credentials";
          return;
        }
        error.innerText = "";
        showToast('Logged in successfully', 'success');
        showDashboard();
      })
      .catch(() => {
        showLoader(false);
        error.innerText = "Server error";
        showToast('Server error during login', 'error');
      });
  });
}

/* ================= NAVIGATION ================= */
function setupNavigation() {
  const links = document.querySelectorAll(".sidebar a[data-page]");
  const pages = document.querySelectorAll(".page");

  links.forEach(link => {
    link.addEventListener("click", () => {

      links.forEach(l => l.classList.remove("active"));
      link.classList.add("active");

      pages.forEach(p => p.classList.remove("active"));
      document.getElementById(link.dataset.page).classList.add("active");
      updatePageHeader(link.dataset.page);

      if (link.dataset.page === "users") loadUsers();
      if (link.dataset.page === "donors") loadDonors();
      if (link.dataset.page === "hospitals") loadHospitalsAdmin();
      if (link.dataset.page === "bloodBanks") loadBloodBanksAdmin();
      if (link.dataset.page === "bloodStocks") loadBloodStocksAdmin();
      if (link.dataset.page === "requests") loadAllRequests();
      if (link.dataset.page === "receivers") loadReceiverRequests();
      if (link.dataset.page === "dashboard") {
        loadBloodStockOverview();
        loadAdminInsights();
      }
    });
  });
}

/* ================= HOSPITALS ================= */
function loadHospitalsAdmin() {
  fetch("/api/admin/hospitals", { credentials: "include" })
    .then(res => res.ok ? res.json() : [])
    .then(data => {
      const tbody = document.getElementById("hospitalTableBody");
      const bloodBankHospitalSelect = document.getElementById("bloodBankHospitalSelect");
      if (tbody) {
        tbody.innerHTML = "";
        data.forEach((h, idx) => {
          tbody.innerHTML += `
            <tr>
              <td title="Hospital ID: ${h.id}">${idx + 1}</td>
              <td>${h.name || "-"}</td>
              <td>${h.address || "-"}</td>
              <td>${h.contact || "-"}</td>
            </tr>`;
        });
      }

      if (bloodBankHospitalSelect) {
        bloodBankHospitalSelect.innerHTML = `<option value="">Select Hospital</option>` +
          data.map(h => `<option value="${h.id}">${h.name}</option>`).join("");
      }
    })
    .catch(() => showToast("Failed to load hospitals", "error"));
}

function saveHospital() {
  const payload = {
    name: document.getElementById("hospitalNameInput").value,
    address: document.getElementById("hospitalLocationInput").value,
    contact: document.getElementById("hospitalContactInput").value,
    latitude: 0,
    longitude: 0
  };

  fetch("/api/admin/hospital", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to save hospital");
      return res.json();
    })
    .then(() => {
      document.getElementById("hospitalNameInput").value = "";
      document.getElementById("hospitalLocationInput").value = "";
      document.getElementById("hospitalContactInput").value = "";
      loadHospitalsAdmin();
      showToast("Hospital saved", "success");
    })
    .catch(err => showToast(err.message || "Failed to save hospital", "error"));
}

/* ================= BLOOD BANKS ================= */
function loadBloodBanksAdmin() {
  fetch("/api/admin/blood-banks", { credentials: "include" })
    .then(res => res.ok ? res.json() : [])
    .then(data => {
      const tbody = document.getElementById("bloodBankTableBody");
      const stockSelect = document.getElementById("bloodStockBankSelect");
      if (tbody) {
        tbody.innerHTML = "";
        data.forEach((bank, idx) => {
          tbody.innerHTML += `
            <tr>
              <td title="Blood Bank ID: ${bank.bloodBankID}">${idx + 1}</td>
              <td>${bank.name || "-"}</td>
              <td>${bank.hospital?.name || "-"}</td>
              <td>${bank.location || "-"}</td>
            </tr>`;
        });
      }
      if (stockSelect) {
        stockSelect.innerHTML = `<option value="">Select Blood Bank</option>` +
          data.map(bank => `<option value="${bank.bloodBankID}">${bank.name} (${bank.hospital?.name || "No Hospital"})</option>`).join("");
      }
    })
    .catch(() => showToast("Failed to load blood banks", "error"));
}

function saveBloodBank() {
  const payload = {
    name: document.getElementById("bloodBankNameInput").value,
    hospitalId: document.getElementById("bloodBankHospitalSelect").value
  };

  fetch("/api/admin/blood-bank", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(async res => {
      const text = await res.text();
      if (!res.ok) throw new Error(text || "Failed to save blood bank");
      return text ? JSON.parse(text) : {};
    })
    .then(() => {
      document.getElementById("bloodBankNameInput").value = "";
      document.getElementById("bloodBankHospitalSelect").value = "";
      loadBloodBanksAdmin();
      showToast("Blood bank saved", "success");
    })
    .catch(err => showToast(err.message || "Failed to save blood bank", "error"));
}

/* ================= BLOOD STOCKS ================= */
function loadBloodStocksAdmin() {
  fetch("/api/admin/blood-stocks", { credentials: "include" })
    .then(res => res.ok ? res.json() : [])
    .then(data => {
      const tbody = document.getElementById("bloodStockTableBody");
      if (!tbody) return;
      tbody.innerHTML = "";
      data.forEach((stock, idx) => {
        const available = (stock.unitsAvailable || 0) > 0;
        tbody.innerHTML += `
          <tr>
            <td>${idx + 1}</td>
            <td>${stock.hospital?.name || "-"}</td>
            <td>${stock.bloodBank?.name || "-"}</td>
            <td>${stock.bloodGroup || "-"}</td>
            <td>${stock.unitsAvailable ?? 0}</td>
            <td>
              <span class="status ${available ? "ACTIVE" : "REJECTED"}">
                ${available ? "Available" : "Not Available"}
              </span>
            </td>
          </tr>`;
      });
    })
    .catch(() => showToast("Failed to load blood stock", "error"));
}

function saveBloodStock() {
  const payload = {
    bloodBankId: document.getElementById("bloodStockBankSelect").value,
    bloodGroup: document.getElementById("bloodGroupSelect").value,
    units: document.getElementById("bloodStockUnitsInput").value
  };

  fetch("/api/admin/blood-stock", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(async res => {
      const text = await res.text();
      if (!res.ok) throw new Error(text || "Failed to save stock");
      return text ? JSON.parse(text) : {};
    })
    .then(() => {
      document.getElementById("bloodStockBankSelect").value = "";
      document.getElementById("bloodGroupSelect").value = "A+";
      document.getElementById("bloodStockUnitsInput").value = 0;
      loadBloodStocksAdmin();
      showToast("Stock saved", "success");
    })
    .catch(err => showToast(err.message || "Failed to save stock", "error"));
}

/* ================= USERS ================= */
function loadUsers() {
  showLoader(true);
  fetch("/api/admin/users", { credentials: "include" })
    .then(res => res.json())
    .then(data => {
      showLoader(false);
      setMetricValue("totalUsers", data.length);

      const tbody = document.getElementById("usersTableBody");
      tbody.innerHTML = "";

      data.forEach((u, idx) => {
        tbody.innerHTML += `
          <tr>
            <td>${idx + 1}</td>
            <td>${u.name}</td>
            <td>${u.email}</td>
            <td>${u.mobile || "-"}</td>
            <td>
              <span class="status ${u.blocked ? "BLOCKED" : "ACTIVE"}">
                ${u.blocked ? "BLOCKED" : "ACTIVE"}
              </span>
            </td>
            <td>
              <button
                class="action-btn ${u.blocked ? "unblock" : "block"}"
                onclick="toggleUserBlock(${u.id}, ${!u.blocked})">
                ${u.blocked ? "Unblock" : "Block"}
              </button>
              <button class="action-btn delete" style="margin-left:8px;" onclick="deleteUser(${u.id})">Delete</button>
            </td>
          </tr>`;
      });
    })
    .catch(err => { showLoader(false); showToast('Failed to load users', 'error'); });
}

function toggleUserBlock(userId, blocked) {
  showLoader(true);
  fetch(`/api/admin/users/${userId}/block?blocked=${blocked}`, {
    method: "PUT",
    credentials: "include"
  })
    .then(res => res.text())
    .then(() => {
      showLoader(false);
      loadUsers();
      showToast('User updated', 'success');
    })
    .catch(err => { showLoader(false); showToast('Failed to update user', 'error'); });
}

function deleteUser(userId) {
  showConfirm('Delete this user? This action cannot be undone.', () => {
    showLoader(true);
    fetch(`/api/admin/users/${userId}`, { method: 'DELETE', credentials: 'include' })
      .then(async res => {
        const message = await res.text();
        if (!res.ok) {
          throw new Error(message || 'Failed to delete user');
        }
        return message;
      })
      .then(() => { showLoader(false); loadUsers(); showToast('User deleted', 'success'); })
      .catch((err) => { showLoader(false); showToast(err.message || 'Failed to delete user', 'error'); });
  });
}

/* ================= DONORS ================= */
function loadDonors() {
  showLoader(true);
  fetch("/api/admin/donors", { credentials: "include" })
    .then(res => res.json())
    .then(data => {
      showLoader(false);
      setMetricValue("totalDonors", data.length);

      const tbody = document.getElementById("donorTableBody");
      tbody.innerHTML = "";

      data.forEach((d, idx) => {
        tbody.innerHTML += `
          <tr>
            <td title="Donor ID: ${d.id}">${idx + 1}</td>
            <td>${d.user?.name || "-"}</td>
            <td>${d.bloodGroup}</td>
            <td>${d.city}</td>
          </tr>`;
      });
    })
    .catch(err => { showLoader(false); showToast('Failed to load donors', 'error'); });
}

/* ================= VISITS ================= */
function loadAllRequests() {
  showLoader(true);
  fetch("/api/admin/visits", { credentials: "include" })
    .then(res => res.json())
    .then(data => {
      showLoader(false);

      setMetricValue("totalRequests", data.length);

      const visitBody = document.getElementById("visitBody");      // Dashboard
      const allBody = document.getElementById("allRequestsBody");  // Donor Visit page

      visitBody.innerHTML = "";
      allBody.innerHTML = "";
      let donorVisitIndex = 0;

      data.forEach(r => {
        // Only show DONOR requests (or requests without requestType for backward compatibility)
        if (r.requestType === "RECEIVER") return;
        donorVisitIndex++;

        const isPending =
          r.status && r.status.toUpperCase() === "PENDING";

        // Dashboard (NO button)
        visitBody.innerHTML += `
          <tr>
            <td title="Visit ID: ${r.id}">${donorVisitIndex}</td>
            <td>${r.user?.name || "-"}</td>
            <td>${r.hospitalName}</td>
            <td>${r.units}</td>
            <td>
              <span class="status ${r.status}">
                ${r.status}
              </span>
            </td>
          </tr>`;

        // Donation Visits Page (WITH button)
        allBody.innerHTML += `
          <tr>
            <td title="Visit ID: ${r.id}">${donorVisitIndex}</td>
            <td>${r.user?.name || "-"}</td>
            <td>${r.hospitalName}</td>
            <td>${r.units}</td>
            <td>
              <span class="status ${r.status}">
                ${r.status}
              </span>
            </td>
            <td>
              ${isPending ? `
                <button class="action-btn approve" onclick="updateStatus(${r.id},'APPROVED')">
                  Approve
                </button>
                <button class="action-btn reject" onclick="updateStatus(${r.id},'REJECTED')">
                  Reject
                </button>
              ` : ""}
            </td>
          </tr>`;
      });
    })
    .catch(err => { showLoader(false); showToast('Failed to load visits', 'error'); });
}

/* ================= BLOOD STOCK OVERVIEW ================= */
function loadBloodStockOverview() {
  fetch("/api/admin/blood-stock-overview", { credentials: "include" })
    .then(res => {
      if (!res.ok) return null;
      return res.json();
    })
    .then(data => {
      if (!data) return;
      setMetricValue("lowStockCount", data.lowStockCount ?? 0);
    })
    .catch(() => {
      setMetricValue("lowStockCount", 0);
    });
}

function loadAdminInsights() {
  fetch("/api/admin/dashboard-insights", { credentials: "include" })
    .then(res => (res.ok ? res.json() : null))
    .then(data => {
      if (!data) return;

      const stats = data.commandStats || {};
      setMetricValue("dashboardCriticalCount", stats.criticalPendingCount || 0);
      setMetricValue("dashboardReadyDonors", stats.readyDonors || 0);
      setMetricValue("dashboardAvailableUnits", stats.availableUnits || 0);
      setMetricValue("dashboardResponseRate", stats.responseRate || 0, "%");

      const hint = document.getElementById("dashboardCriticalHint");
      if (hint) {
        hint.innerText = stats.criticalPendingCount > 0
          ? `${stats.liveRequestCount || 0} live request(s) need active monitoring.`
          : "No critical approvals are waiting right now.";
      }

      renderUrgentQueue(data.urgentQueue || []);
      renderStockPressure(data.stockPressure || []);
      renderCityDemand(data.cityDemand || []);
      renderShortageList(data.shortageMap || []);
    })
    .catch(() => {
      renderUrgentQueue([]);
      renderStockPressure([]);
      renderCityDemand([]);
      renderShortageList([]);
    });
}

function renderUrgentQueue(items) {
  const container = document.getElementById("urgentQueueList");
  if (!container) return;

  if (!items.length) {
    container.innerHTML = '<div class="command-empty">No live urgent receiver requests right now.</div>';
    return;
  }

  container.innerHTML = items.map(item => {
    const urgency = String(item.urgency || "NORMAL").toUpperCase();
    const urgentClass = urgency === "CRITICAL" || urgency === "HIGH" ? "urgent-queue__item--critical" : "";
    const badgeClass = urgency === "CRITICAL" || urgency === "HIGH" ? "queue-badge" : "queue-badge queue-badge--watch";
    return `
      <article class="urgent-queue__item ${urgentClass}">
        <div class="urgent-queue__top">
          <span class="urgent-queue__id">${escapeHtml(item.publicId || "Request")}</span>
          <span class="${badgeClass}">${escapeHtml(urgency)}</span>
        </div>
        <div class="urgent-queue__title">${escapeHtml(item.bloodGroup || "--")} needed at ${escapeHtml(item.hospital || "Unknown hospital")}</div>
        <div class="urgent-queue__meta">${escapeHtml(item.city || "Unknown city")} | ${escapeHtml(item.unitsRequired || 0)} unit(s) | ${escapeHtml(item.waitLabel || "Live")}</div>
        <div class="urgent-queue__meta">Matched donors: ${escapeHtml(item.matchedDonors || 0)} | Responded: ${escapeHtml(item.donorsResponded || 0)} | ${item.needsAction ? "Needs admin action" : "In active flow"}</div>
      </article>
    `;
  }).join("");
}

function renderStockPressure(items) {
  const container = document.getElementById("stockPressureGrid");
  if (!container) return;

  if (!items.length) {
    container.innerHTML = '<div class="command-empty">Blood group pressure heatmap is not available.</div>';
    return;
  }

  container.innerHTML = items.map(item => `
    <article class="pressure-card pressure-card--${escapeHtml(item.tone || "stable")}">
      <span>${escapeHtml(item.bloodGroup || "--")}</span>
      <strong>${escapeHtml(item.availableUnits || 0)}</strong>
      <small>${escapeHtml(item.demandUnits || 0)} unit(s) in live demand</small>
    </article>
  `).join("");
}

function renderCityDemand(items) {
  const container = document.getElementById("cityDemandList");
  if (!container) return;

  if (!items.length) {
    container.innerHTML = '<div class="command-empty">City demand pulse will appear when requests are active.</div>';
    return;
  }

  container.innerHTML = items.map(item => {
    const critical = Number(item.critical || 0);
    return `
      <article class="city-demand__item">
        <div class="city-demand__top">
          <span class="city-demand__city">${escapeHtml(item.city || "Unspecified")}</span>
          <span class="city-demand__badge ${critical > 0 ? "city-demand__badge--critical" : ""}">${escapeHtml(item.requests || 0)} request(s)</span>
        </div>
        <div class="city-demand__meta">${critical > 0 ? `${critical} high-priority request(s)` : "No critical load right now"}</div>
      </article>
    `;
  }).join("");
}

function renderShortageList(items) {
  const container = document.getElementById("shortageList");
  if (!container) return;

  if (!items.length) {
    container.innerHTML = '<div class="command-empty">No low stock locations are currently flagged.</div>';
    return;
  }

  container.innerHTML = items.map(item => `
    <article class="shortage-item">
      <div class="shortage-item__top">
        <span class="shortage-item__group">${escapeHtml(item.bloodGroup || "--")}</span>
        <span class="shortage-pill ${item.tone === "watch" ? "shortage-pill--watch" : ""}">${escapeHtml(item.unitsAvailable || 0)} unit(s)</span>
      </div>
      <div class="shortage-item__location">${escapeHtml(item.hospital || "Unknown hospital")}</div>
      <div class="shortage-item__meta">Reorder level: ${escapeHtml(item.reorderLevel || 0)}</div>
    </article>
  `).join("");
}

/* ================= RECEIVER REQUESTS ================= */
function loadReceiverRequests() {
  showLoader(true);
  fetch("/api/admin/receiver-requests", { credentials: "include" })
    .then(res => res.json())
    .then(data => {
      showLoader(false);

      const receiversBody = document.getElementById("receiversTableBody");
      receiversBody.innerHTML = "";

      let receiverCount = 0;
      let pendingCount = 0;
      let fulfilledCount = 0;
      let criticalCount = 0;
      let pendingCriticalCount = 0;
      let totalMatched = 0;
      let totalResponded = 0;

      const sortedReceivers = [...data].sort((a, b) => {
        const urgencyRank = { CRITICAL: 3, HIGH: 2, MEDIUM: 1, LOW: 0 };
        const urgencyDiff = (urgencyRank[(b.bloodUrgency || '').toUpperCase()] || 0) - (urgencyRank[(a.bloodUrgency || '').toUpperCase()] || 0);
        if (urgencyDiff !== 0) return urgencyDiff;
        const aId = Number(a.id || 0);
        const bId = Number(b.id || 0);
        return bId - aId;
      });

      sortedReceivers.forEach((r, index) => {
        // defensive mapping: support legacy and new field names
        const requestType = r.requestType || r.type || 'RECEIVER';
        if (requestType !== 'RECEIVER') return;

        receiverCount++;

        const status = (r.status || r.requestStatus || '').toUpperCase();
        const isPending = status === 'PENDING' || status === 'SUBMITTED' || status === 'PENDING_APPROVAL';

        const bloodUrgency = r.bloodUrgency || r.bloodUrgency || r.urgency || r.bloodSituation || 'NORMAL';
        if (bloodUrgency === 'CRITICAL') criticalCount++;
        if (bloodUrgency === 'CRITICAL' && isPending) pendingCriticalCount++;
        if (status === 'FULFILLED' || status === 'RESERVED' || status === 'COMPLETED' || status === 'DONOR_ASSIGNED') fulfilledCount++;
        if (isPending) pendingCount++;

        const matched = r.matchedDonors || r.matchedDonorsCount || 0;
        const responded = r.donorsResponded || r.donorsRespondedCount || 0;
        totalMatched += matched;
        totalResponded += responded;

        const hospital = r.hospitalName || r.hospital || '-';
        const units = r.units || r.unitsRequired || r.unitsRequired || '-';

        receiversBody.innerHTML += `
          <tr class="${bloodUrgency === 'CRITICAL' || bloodUrgency === 'HIGH' ? 'top-priority-row' : ''}">
            <td title="Request ID: ${r.id}">${receiverCount}</td>
            <td>${(r.user && r.user.name) || r.userName || '-'}</td>
            <td>${hospital}</td>
            <td>${units}</td>
            <td>
              <span class="status ${bloodUrgency}">
                ${bloodUrgency}
              </span>
            </td>
            <td>
              <span class="status ${status}">
                ${status}
              </span>
            </td>
            <td>
              ${isPending ? `
                <button class="action-btn approve" onclick="approveReceiverRequest(${r.id}, '${String(hospital).replace(/'/g, "\\'")}')">
                  Approve
                </button>
                <button class="action-btn reject" onclick="rejectReceiverRequest(${r.id})">
                  Reject
                </button>
              ` : ""}
            </td>
          </tr>`;
      });

      if (receiverCount === 0) {
        receiversBody.innerHTML = "<tr><td colspan='7' style='text-align:center;color:#999;'>No receiver requests found</td></tr>";
      }

      // Update summary stats
      document.getElementById('totalRequestsCount').innerText = receiverCount;
      document.getElementById('pendingRequestsCount').innerText = pendingCount;
      document.getElementById('fulfilledRequestsCount').innerText = fulfilledCount;
      document.getElementById('criticalRequestsCount').innerText = criticalCount;
      const responseRate = totalMatched > 0 ? Math.round((totalResponded * 100) / totalMatched) : 0;
      document.getElementById('responseRatePercent').innerText = responseRate + '%';

      showCriticalRequestAlert(pendingCriticalCount);
      loadAdminInsights();
    })
    .catch(err => { showLoader(false); showToast('Failed to load receiver requests', 'error'); });
}

function approveReceiverRequest(id, hospitalName) {
  const expectedDeliveryTime = window.prompt("Expected delivery time", "Within 2-3 hours");
  if (expectedDeliveryTime === null) return;
  const deliveryLocation = window.prompt("Delivery location / counter", hospitalName || "");
  if (deliveryLocation === null) return;
  const deliveryInstructions = window.prompt("Special instructions (optional)", "") ?? "";

  showLoader(true);
  fetch(`/api/blood-request/approve/${id}?expectedDeliveryTime=${encodeURIComponent(expectedDeliveryTime)}&deliveryLocation=${encodeURIComponent(deliveryLocation)}&deliveryInstructions=${encodeURIComponent(deliveryInstructions)}`, {
    method: "POST",
    credentials: "include"
  })
    .then(async res => {
      const text = await res.text();
      let payload = {};
      try {
        payload = text ? JSON.parse(text) : {};
      } catch (e) {
        payload = { message: text };
      }
      if (!res.ok) {
        throw new Error(payload.message || "Failed to approve request");
      }
      return payload;
    })
    .then(payload => {
      showLoader(false);
      loadReceiverRequests();
      loadBloodStockOverview();
      loadAdminInsights();
      showToast(payload.message || "Receiver request approved", "success");
    })
    .catch(err => {
      showLoader(false);
      showToast(err.message || "Failed to approve request", "error");
    });
}

function rejectReceiverRequest(id) {
  showConfirm('Reject this receiver request?', () => {
    showLoader(true);
    fetch(`/api/blood-request/reject/${id}`, {
      method: "POST",
      credentials: "include"
    })
      .then(async res => {
        const text = await res.text();
        let payload = {};
        try {
          payload = text ? JSON.parse(text) : {};
        } catch (e) {
          payload = { message: text };
        }
        if (!res.ok) {
          throw new Error(payload.message || "Failed to reject request");
        }
        return payload;
      })
      .then(payload => {
        showLoader(false);
        loadReceiverRequests();
        loadAdminInsights();
        showToast(payload.message || "Receiver request rejected", "success");
      })
      .catch(err => {
        showLoader(false);
        showToast(err.message || "Failed to reject request", "error");
      });
  });
}

/* ================= UPDATE STATUS ================= */
function updateStatus(id, status) {
  showLoader(true);
  fetch(`/api/admin/visit/${id}?status=${status}`, {
    method: "PUT",
    credentials: "include"
  })
  .then(res => res.text())
  .then(msg => {
    showLoader(false);
    loadAllRequests();
    loadReceiverRequests();
    loadAdminInsights();
    showToast('Request updated', 'success');
  })
  .catch(err => { showLoader(false); showToast('Failed to update request', 'error'); });
}

/* ================= LOGOUT ================= */
function setupLogout() {
  document.querySelector(".logout").addEventListener("click", () => {
    showLoader(true);
    fetch("/api/admin/logout", {
      method: "POST",
      credentials: "include"
    }).then(() => {
      showLoader(false);
      showLogin();
      showToast('Logged out', 'success');
    }).catch(() => { showLoader(false); showToast('Logout failed', 'error'); });
  });
}

/* ================= ADMIN SETTINGS ================= */
function loadAdminSettings() {
  fetch("/api/admin/settings", { credentials: "include" })
    .then(res => res.json())
    .then(settings => {
      document.getElementById("pointsPerUnit").value = settings.pointsPerUnit || 100;
      document.getElementById("bonusPerDonation").value = settings.bonusPerDonation || 200;
      document.getElementById("agePointsMultiplier").value = settings.agePointsMultiplier || 5;
      document.getElementById("minAge").value = settings.minAge || 18;
      document.getElementById("maxAge").value = settings.maxAge || 65;
      document.getElementById("minWeight").value = settings.minWeight || 50;
      
      console.log("Settings loaded successfully");
    })
    .catch(err => {
      console.log("Error loading settings:", err);
      showToast('Settings loaded with default values', 'error');
    });
  
  // Load user panel controls
  fetch("/api/admin/controls", { credentials: "include" })
    .then(res => res.json())
    .then(controls => {
      document.getElementById("showDonorProfile").checked = controls.showDonorProfile !== false;
      document.getElementById("showBloodRequest").checked = controls.showBloodRequest !== false;
      document.getElementById("showLeaderboard").checked = controls.showLeaderboard !== false;
      document.getElementById("showMedicine").checked = controls.showMedicine !== false;
      document.getElementById("showCertificates").checked = controls.showCertificates !== false;
      document.getElementById("showRewards").checked = controls.showRewards !== false;
      document.getElementById("enableDonations").checked = controls.enableDonations !== false;
      document.getElementById("enableRequests").checked = controls.enableRequests !== false;
      document.getElementById("showChatbot").checked = controls.showChatbot !== false;
      document.getElementById("maintenanceMode").checked = controls.maintenanceMode === true;
      
      console.log("User panel controls loaded");
    })
    .catch(err => {
      console.log("Error loading controls:", err);
    });
}

function saveAdminSettings() {
  const settings = {
    pointsPerUnit: parseInt(document.getElementById("pointsPerUnit").value),
    bonusPerDonation: parseInt(document.getElementById("bonusPerDonation").value),
    agePointsMultiplier: parseInt(document.getElementById("agePointsMultiplier").value),
    minAge: parseInt(document.getElementById("minAge").value),
    maxAge: parseInt(document.getElementById("maxAge").value),
    minWeight: parseInt(document.getElementById("minWeight").value)
  };
  
  const controls = {
    showDonorProfile: document.getElementById("showDonorProfile").checked,
    showBloodRequest: document.getElementById("showBloodRequest").checked,
    showLeaderboard: document.getElementById("showLeaderboard").checked,
    showMedicine: document.getElementById("showMedicine").checked,
    showCertificates: document.getElementById("showCertificates").checked,
    showRewards: document.getElementById("showRewards").checked,
    enableDonations: document.getElementById("enableDonations").checked,
    enableRequests: document.getElementById("enableRequests").checked,
    showChatbot: document.getElementById("showChatbot").checked,
    maintenanceMode: document.getElementById("maintenanceMode").checked
  };
  
  // Save settings
  fetch("/api/admin/settings", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(settings)
  })
  .then(res => res.json())
  .then(data => {
    console.log("Settings saved:", data);
  })
  .catch(err => console.log("Error saving settings:", err));
  
  // Save controls
  fetch("/api/admin/controls", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(controls)
  })
  .then(res => res.json())
  .then(data => {
    console.log("Controls saved:", data);
    showToast('All settings saved successfully!', 'success');
  })
  .catch(err => {
    console.log("Error saving controls:", err);
    showToast('Settings saved (partial)', 'error');
  });
}

