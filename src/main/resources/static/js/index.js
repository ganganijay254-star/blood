const BASE_URL = "/api/auth";
let APP_CONFIG = null;
let HOME_OVERVIEW = null;
let tickerRotationTimer = null;

const FALLBACK_OVERVIEW = {
  stats: {
    donorCount: 0,
    approvedDonations: 0,
    donatedUnits: 0,
    availableUnits: 0,
    activeRequests: 0
  },
  criticalRequests: [],
  stockGroups: [],
  cityPulse: []
};

function checkSession() {
  return fetch(`${BASE_URL}/check-session`, { credentials: "include" });
}

async function loadPublicConfig() {
  if (APP_CONFIG) return APP_CONFIG;
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) throw new Error("config");
    APP_CONFIG = await res.json();
  } catch (e) {
    APP_CONFIG = { settings: {}, controls: {} };
  }
  return APP_CONFIG;
}

async function loadHomeOverview() {
  if (HOME_OVERVIEW) return HOME_OVERVIEW;
  try {
    const res = await fetch("/api/admin/public-overview");
    if (!res.ok) throw new Error("overview");
    HOME_OVERVIEW = await res.json();
  } catch (e) {
    HOME_OVERVIEW = FALLBACK_OVERVIEW;
  }
  return HOME_OVERVIEW;
}

function isControlOn(flag, fallback = true) {
  const controls = APP_CONFIG && APP_CONFIG.controls ? APP_CONFIG.controls : {};
  if (controls[flag] === undefined || controls[flag] === null) return fallback;
  return controls[flag] !== false;
}

function isMaintenanceOn() {
  const controls = APP_CONFIG && APP_CONFIG.controls ? APP_CONFIG.controls : {};
  return controls.maintenanceMode === true;
}

function goToDonor() {
  Promise.all([checkSession(), loadPublicConfig()])
    .then(([res]) => {
      if (isMaintenanceOn() || !isControlOn("enableDonations", true) || !isControlOn("showDonorProfile", true)) {
        if (typeof localShow === "function") {
          localShow("Donor feature is currently disabled by admin.", "error");
        } else {
          alert("Donor feature is currently disabled by admin.");
        }
        return;
      }
      window.location.href = res.status === 200 ? "/donor-form" : "/login";
    })
    .catch(() => {
      window.location.href = "/login";
    });
}

function goToReceiver() {
  Promise.all([checkSession(), loadPublicConfig()])
    .then(([res]) => {
      if (isMaintenanceOn() || !isControlOn("showBloodRequest", true) || !isControlOn("enableRequests", true)) {
        if (typeof localShow === "function") {
          localShow("Blood request feature is currently disabled by admin.", "error");
        } else {
          alert("Blood request feature is currently disabled by admin.");
        }
        return;
      }
      window.location.href = res.status === 200 ? "/receiver-form" : "/login";
    })
    .catch(() => {
      window.location.href = "/login";
    });
}

function goToProfile() {
  checkSession()
    .then((res) => {
      window.location.href = res.status === 200 ? "/donor-profile" : "/login";
    })
    .catch(() => {
      window.location.href = "/login";
    });
}

function logout() {
  const doLogout = () => {
    fetch(`${BASE_URL}/logout`, { method: "POST", credentials: "include" }).then(() => {
      window.location.href = "/";
    });
  };

  if (typeof showConfirm === "function") {
    showConfirm("Are you sure you want to logout?", doLogout);
  } else {
    if (!confirm("Are you sure you want to logout?")) return;
    doLogout();
  }
}

function playHeroTypewriter() {
  const lines = Array.from(document.querySelectorAll(".hero-title-line"));
  if (!lines.length) return;

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  lines.forEach((line) => {
    const text = line.getAttribute("data-text") || "";
    line.innerHTML = '<span class="hero-title-text"></span><span class="hero-title-cursor is-hidden" aria-hidden="true"></span>';
    line.querySelector(".hero-title-text").textContent = prefersReducedMotion ? text : "";
  });
  if (prefersReducedMotion) return;

  let lineIndex = 0;

  const typeNextLine = () => {
    if (lineIndex >= lines.length) return;

    const line = lines[lineIndex];
    const text = line.getAttribute("data-text") || "";
    const textNode = line.querySelector(".hero-title-text");
    const cursor = line.querySelector(".hero-title-cursor");
    let charIndex = 0;

    if (cursor) {
      cursor.classList.remove("is-hidden");
      cursor.style.opacity = "1";
    }

    const tick = () => {
      textNode.textContent = text.slice(0, charIndex + 1);
      charIndex += 1;

      if (charIndex < text.length) {
        window.setTimeout(tick, 38);
        return;
      }

      if (cursor) cursor.classList.add("is-hidden");
      lineIndex += 1;
      window.setTimeout(typeNextLine, 180);
    };

    window.setTimeout(tick, 160);
  };

  typeNextLine();
}

function initRevealAnimations() {
  const revealItems = Array.from(document.querySelectorAll("[data-reveal]"));
  if (!revealItems.length) return;

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion || !("IntersectionObserver" in window)) {
    revealItems.forEach((item) => item.classList.add("is-visible"));
    return;
  }

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      });
    },
    { threshold: 0.16, rootMargin: "0px 0px -48px 0px" }
  );

  revealItems.forEach((item) => observer.observe(item));
}

function animateCount(elementId, value) {
  const el = document.getElementById(elementId);
  if (!el) return;

  const target = Number(value || 0);
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion) {
    el.textContent = String(target);
    return;
  }

  const duration = 900;
  const start = performance.now();

  const tick = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = String(Math.round(target * eased));
    if (progress < 1) {
      window.requestAnimationFrame(tick);
    } else {
      el.textContent = String(target);
    }
  };

  window.requestAnimationFrame(tick);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderHomeOverview(overview) {
  const payload = overview || FALLBACK_OVERVIEW;
  const stats = payload.stats || {};

  animateCount("publicDonorsCount", stats.donorCount || 0);
  animateCount("publicDonationCount", stats.approvedDonations || 0);
  animateCount("publicUnitsCount", stats.availableUnits || 0);
  animateCount("publicActiveRequestsCount", stats.activeRequests || 0);

  renderRequestTicker(payload.criticalRequests || []);
  renderStockGroups(payload.stockGroups || []);
  renderCityPulse(payload.cityPulse || []);
}

function renderRequestTicker(requests) {
  const container = document.getElementById("criticalRequestsFeed");
  if (!container) return;

  if (!Array.isArray(requests) || requests.length === 0) {
    container.innerHTML = '<div class="pulse-empty">No urgent requests are active right now. The network is calm.</div>';
    stopTickerRotation();
    return;
  }

  container.innerHTML = requests
    .map((request, index) => {
      const urgency = escapeHtml(request.urgency || "NORMAL");
      const toneClass = urgency === "CRITICAL" || urgency === "HIGH" ? "ticker-card--critical" : "";
      return `
        <article class="ticker-card ${toneClass} ${index === 0 ? "is-active" : ""}">
          <div class="ticker-card__top">
            <div class="ticker-card__id">${escapeHtml(request.publicId || "Request")}</div>
            <span class="ticker-card__badge">${urgency}</span>
          </div>
          <div class="ticker-card__title">${escapeHtml(request.bloodGroup || "--")} blood needed at ${escapeHtml(request.hospital || "Unknown hospital")}</div>
          <div class="ticker-card__meta">
            <span>${escapeHtml(request.city || "Unknown city")} | ${escapeHtml(request.unitsRequired || 0)} unit(s)</span>
            <span class="ticker-card__wait">${escapeHtml(request.waitLabel || "Live now")}</span>
          </div>
        </article>
      `;
    })
    .join("");

  startTickerRotation();
}

function renderStockGroups(groups) {
  const container = document.getElementById("bloodGroupStockStrip");
  if (!container) return;

  if (!Array.isArray(groups) || groups.length === 0) {
    container.innerHTML = '<div class="pulse-empty">Blood group stock summary is not available yet.</div>';
    return;
  }

  container.innerHTML = groups
    .map((group) => {
      const tone = escapeHtml(group.tone || "stable");
      return `
        <article class="stock-chip stock-chip--${tone}">
          <strong>${escapeHtml(group.bloodGroup || "--")}</strong>
          <span>${escapeHtml(group.units || 0)} visible unit(s)</span>
        </article>
      `;
    })
    .join("");
}

function renderCityPulse(cities) {
  const container = document.getElementById("cityPulseBoard");
  if (!container) return;

  if (!Array.isArray(cities) || cities.length === 0) {
    container.innerHTML = '<div class="pulse-empty">City demand pulse will appear once live requests are available.</div>';
    return;
  }

  container.innerHTML = cities
    .map((city) => {
      const isHot = city.tone === "hot";
      return `
        <article class="city-card ${isHot ? "city-card--hot" : ""}">
          <div>
            <div class="city-card__name">${escapeHtml(city.city || "Unspecified")}</div>
            <div class="city-card__meta">${escapeHtml(city.requestCount || 0)} live request(s)</div>
          </div>
          <span class="city-card__badge">${escapeHtml(city.criticalCount || 0)} urgent</span>
        </article>
      `;
    })
    .join("");
}

function stopTickerRotation() {
  if (tickerRotationTimer) {
    window.clearInterval(tickerRotationTimer);
    tickerRotationTimer = null;
  }
}

function startTickerRotation() {
  stopTickerRotation();
  const cards = Array.from(document.querySelectorAll(".ticker-card"));
  if (cards.length <= 1 || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

  let activeIndex = 0;
  tickerRotationTimer = window.setInterval(() => {
    cards[activeIndex].classList.remove("is-active");
    activeIndex = (activeIndex + 1) % cards.length;
    cards[activeIndex].classList.add("is-active");
  }, 2600);
}

function renderNavigation(sessionResponse) {
  const mainNav = document.getElementById("mainNav");
  if (!mainNav) return;

  if (sessionResponse && sessionResponse.status === 200) {
    const links = [
      '<a href="/">Home</a>',
      '<a href="/donor-dashboard">Dashboard</a>',
      '<a href="/about">About</a>'
    ];
    if (isControlOn("showLeaderboard", true)) links.push('<a href="/leaderboard">Leaderboard</a>');
    if (isControlOn("showMedicine", true)) links.push('<a href="/medicine">Medicines</a>');
    if (isControlOn("showDonorProfile", true)) links.push('<a href="/donor-profile">My Profile</a>');
    links.push('<button class="btn btn-small" onclick="logout()">Logout</button>');
    mainNav.innerHTML = links.join("");
    return;
  }

  mainNav.innerHTML = `
    <a href="/">Home</a>
    <a href="/about">About</a>
    <a href="/login">Login</a>
  `;
}

window.addEventListener("DOMContentLoaded", async () => {
  playHeroTypewriter();
  initRevealAnimations();

  let sessionResponse = null;

  try {
    const [sessionRes] = await Promise.all([checkSession(), loadPublicConfig(), loadHomeOverview()]);
    sessionResponse = sessionRes;
  } catch (e) {
    sessionResponse = null;
  }

  renderNavigation(sessionResponse);
  renderHomeOverview(HOME_OVERVIEW || FALLBACK_OVERVIEW);
});
