const BASE_URL = "/api";
let dashboardConfig = { settings: {}, controls: {} };
let dashboardState = {
  donor: null,
  approvedVisits: [],
  focus: {
    eligibility: "Loading...",
    priority: "Loading...",
    milestone: "Loading..."
  }
};

document.addEventListener("DOMContentLoaded", async () => {
  initRevealAnimations();
  await loadPublicConfig();
  applyDashboardControls();
  checkSession();
});

async function loadPublicConfig() {
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) return;
    dashboardConfig = await res.json();
  } catch (e) {
    dashboardConfig = { settings: {}, controls: {} };
  }
}

function initRevealAnimations() {
  const items = Array.from(document.querySelectorAll("[data-reveal]"));
  if (!items.length) return;

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion || !("IntersectionObserver" in window)) {
    items.forEach((item) => item.classList.add("is-visible"));
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
    { threshold: 0.15, rootMargin: "0px 0px -48px 0px" }
  );

  items.forEach((item) => observer.observe(item));
}

function notify(message, type = "info") {
  if (window.showToast) {
    window.showToast(message, type);
    return;
  }
  alert(message);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatDate(value) {
  if (!value) return "Not available";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Not available" : date.toLocaleDateString();
}

function renderList(items) {
  return `
    <ul class="detail-list">
      ${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
    </ul>
  `;
}

function animateCount(id, value) {
  const el = document.getElementById(id);
  if (!el) return;

  const target = Number(value || 0);
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion) {
    el.textContent = String(target);
    return;
  }

  const startedAt = performance.now();
  const duration = 900;

  const tick = (now) => {
    const progress = Math.min((now - startedAt) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = String(Math.round(target * eased));
    if (progress < 1) {
      requestAnimationFrame(tick);
    } else {
      el.textContent = String(target);
    }
  };

  requestAnimationFrame(tick);
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function setHtml(id, value) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = value;
}

function setRingProgress(percentage) {
  const ring = document.getElementById("readinessRing");
  const value = Math.max(0, Math.min(Number(percentage || 0), 100));
  if (ring) {
    ring.style.setProperty("--ring-progress", `${value / 100}`);
  }
  setText("eligibilityPercent", `${value}%`);
}

function updateFocusSnapshot(overrides) {
  dashboardState.focus = { ...dashboardState.focus, ...overrides };
  setText("focusEligibility", dashboardState.focus.eligibility);
  setText("focusPriority", dashboardState.focus.priority);
  setText("focusMilestone", dashboardState.focus.milestone);
}

function applyDashboardControls() {
  const controls = dashboardConfig.controls || {};

  if (controls.maintenanceMode === true) {
    const main = document.querySelector(".dashboard-main");
    if (main) {
      main.innerHTML = '<section class="info-section"><h2>Service temporarily unavailable due to maintenance.</h2></section>';
    }
    return;
  }

  if (controls.showLeaderboard === false) {
    document.querySelectorAll('a[href="/leaderboard"]').forEach((el) => (el.style.display = "none"));
  }

  if (controls.showMedicine === false) {
    document.querySelectorAll('a[href="/medicine"]').forEach((el) => (el.style.display = "none"));
  }

  if (controls.showBloodRequest === false || controls.enableRequests === false) {
    document.querySelectorAll('a[href="/blood-request"], a[href="/receiver-form"]').forEach((el) => (el.style.display = "none"));
    const matchingSection = document.getElementById("matchingRequestsSection");
    if (matchingSection) matchingSection.style.display = "none";
  }

  if (controls.showCertificates === false) {
    const certSection = document.querySelector(".certificate-section");
    if (certSection) certSection.style.display = "none";
  }

  if (controls.showRewards === false) {
    document.querySelectorAll(".info-section h2, .info-section h3").forEach((heading) => {
      if ((heading.textContent || "").toLowerCase().includes("reward")) {
        const section = heading.closest(".info-section");
        if (section) section.style.display = "none";
      }
    });
  }
}

function typeText(text) {
  const el = document.getElementById("welcomeText");
  if (el) el.textContent = text;
}

function checkSession() {
  fetch(`${BASE_URL}/auth/check-session`, { credentials: "include" })
    .then((res) => {
      if (!res.ok) {
        window.location.href = "/login";
        return null;
      }
      return res.json();
    })
    .then((user) => {
      if (!user) return;
      typeText(`Welcome back, ${user.name || "Donor"}`);
      loadDonorData();
    })
    .catch(() => {
      window.location.href = "/login";
    });
}

function loadDonorData() {
  const controls = dashboardConfig.controls || {};
  loadDonorStats();
  loadPriorityScore();
  loadSmartEligibility();
  if (controls.showBloodRequest !== false && controls.enableRequests !== false) {
    loadMatchingRequests();
  }
  loadBadges();
  loadRecentActivity();
  if (controls.showCertificates !== false) loadCertificates();
  if (controls.showRewards !== false) loadRewards();
}

function loadDonorStats() {
  Promise.all([
    fetch(`${BASE_URL}/donor/me`, { credentials: "include" }).then((r) => (r.ok ? r.json() : null)),
    fetch(`${BASE_URL}/visit-request/my`, { credentials: "include" }).then((r) => (r.ok ? r.json() : []))
  ])
    .then(([donor, visits]) => {
      if (!donor) {
        setNoDonorState();
        return;
      }

      dashboardState.donor = donor;
      window.DONOR_ID = donor.id;

      const approvedVisits = (visits || [])
        .filter((visit) => (visit.status || "").toUpperCase() === "APPROVED")
        .sort((a, b) => new Date(b.requestDate) - new Date(a.requestDate));

      dashboardState.approvedVisits = approvedVisits;
      renderDonationMomentum(donor, approvedVisits);
    })
    .catch((err) => {
      console.error("Error loading donor stats:", err);
      setNoDonorState();
    });
}

function renderDonationMomentum(donor, approvedVisits) {
  const totalDonations = approvedVisits.length;
  const totalUnits = approvedVisits.reduce((sum, visit) => sum + Number(visit.units || 0), 0);
  const lastDonation = approvedVisits.length ? approvedVisits[0].requestDate : null;

  animateCount("totalDonations", totalDonations);
  animateCount("totalUnits", totalUnits);
  setText("lastDonation", approvedVisits.length ? formatDate(lastDonation) : "Not yet");

  const gap = donor.gender === "FEMALE" ? 120 : 90;
  const lastDonationDate = donor.lastDonationDate ? new Date(donor.lastDonationDate) : null;
  const diffDays = lastDonationDate
    ? Math.floor((Date.now() - lastDonationDate.getTime()) / (1000 * 60 * 60 * 24))
    : gap;
  const daysRemaining = Math.max(gap - diffDays, 0);
  const eligibleNow = !lastDonationDate || diffDays >= gap;
  const readinessPercent = Math.max(8, Math.min(100, Math.round((Math.min(diffDays, gap) / gap) * 100)));

  const streak = calculateDonationStreak(approvedVisits);
  const level = resolveDonorLevel(totalDonations);

  setRingProgress(eligibleNow ? 100 : readinessPercent);
  setText("readinessState", eligibleNow ? "Ready now" : `${daysRemaining} days left`);
  setText("status", eligibleNow ? "Eligible" : "Not Eligible");
  setText("nextEligible", eligibleNow ? "You are currently eligible to donate blood." : `You can donate again after ${daysRemaining} day(s).`);
  setText("eligibilitySummary", eligibleNow
    ? "You have cleared the cooldown window and can schedule your next visit."
    : "Recovery window is active after your last donation. Keep your profile updated while you wait.");
  setText("eligibilityCountdown", eligibleNow ? "Donation window open now" : `Next donation window in ${daysRemaining} day(s)`);
  animateCount("donationStreak", streak);
  setText("donorLevel", level.label);
  setText("donorLevelLabel", level.label);
  setText("donorMilestoneText", level.nextLabel
    ? `${level.remaining} more approved donation(s) to reach ${level.nextLabel}.`
    : "You have already reached the highest donor level available right now.");

  const progressBar = document.getElementById("donorLevelProgressBar");
  if (progressBar) progressBar.style.width = `${level.progress}%`;

  updateFocusSnapshot({
    eligibility: eligibleNow ? "Eligible now" : `${daysRemaining} day(s) remaining`,
    milestone: level.nextLabel ? `${level.remaining} donation(s) to ${level.nextLabel}` : "Top level achieved"
  });

  const statusEl = document.getElementById("status");
  if (statusEl) statusEl.style.color = eligibleNow ? "var(--success)" : "#8a2431";
}

function calculateDonationStreak(approvedVisits) {
  if (!Array.isArray(approvedVisits) || approvedVisits.length === 0) return 0;
  const sortedDates = approvedVisits
    .map((visit) => new Date(visit.requestDate))
    .filter((date) => !Number.isNaN(date.getTime()))
    .sort((a, b) => b - a);

  if (!sortedDates.length) return 0;

  let streak = 1;
  for (let index = 0; index < sortedDates.length - 1; index += 1) {
    const diffDays = Math.floor((sortedDates[index].getTime() - sortedDates[index + 1].getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays <= 160) {
      streak += 1;
    } else {
      break;
    }
  }
  return streak;
}

function resolveDonorLevel(totalDonations) {
  const levels = [
    { label: "Starter", target: 0 },
    { label: "Ready Donor", target: 1 },
    { label: "Lifeline", target: 3 },
    { label: "Guardian", target: 5 },
    { label: "Hero", target: 8 },
    { label: "Legend", target: 12 }
  ];

  let current = levels[0];
  let next = null;
  for (let index = 0; index < levels.length; index += 1) {
    if (totalDonations >= levels[index].target) {
      current = levels[index];
      next = levels[index + 1] || null;
    }
  }

  if (!next) {
    return {
      label: current.label,
      nextLabel: null,
      remaining: 0,
      progress: 100
    };
  }

  const span = Math.max(next.target - current.target, 1);
  const progress = Math.max(0, Math.min(100, Math.round(((totalDonations - current.target) / span) * 100)));

  return {
    label: current.label,
    nextLabel: next.label,
    remaining: Math.max(next.target - totalDonations, 0),
    progress
  };
}

function loadPriorityScore() {
  fetch(`${BASE_URL}/features/my-priority`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : null))
    .then((data) => {
      if (!data) return;

      const priorityScore = data.priorityScore || 0;
      animateCount("priorityScore", priorityScore);

      let level = "Building priority";
      if (priorityScore >= 80) level = "Top priority";
      else if (priorityScore >= 60) level = "High priority";
      else if (priorityScore >= 40) level = "Good priority";

      setText("priorityLevel", level);
      updateFocusSnapshot({ priority: `${level} (${priorityScore})` });
    })
    .catch((err) => console.log("Error loading priority score:", err));
}

function loadSmartEligibility() {
  fetch(`${BASE_URL}/features/my-eligibility`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : null))
    .then((eligibility) => {
      if (!eligibility) return;

      const cards = [];
      const eligibleNow = eligibility.isEligible === true;

      cards.push(`
        <article class="detail-card ${eligibleNow ? "detail-card--success" : "detail-card--danger"}">
          <p><strong>${eligibleNow ? "Eligible now" : "Waiting period active"}</strong></p>
          <p>Score: ${escapeHtml(eligibility.eligibilityScore || 0)}/100</p>
        </article>
      `);

      if (Array.isArray(eligibility.blockers) && eligibility.blockers.length > 0) {
        cards.push(`
          <article class="detail-card detail-card--danger">
            <p><strong>Issues to resolve</strong></p>
            ${renderList(eligibility.blockers)}
          </article>
        `);
      }

      if (Array.isArray(eligibility.warnings) && eligibility.warnings.length > 0) {
        cards.push(`
          <article class="detail-card detail-card--warning">
            <p><strong>Notes</strong></p>
            ${renderList(eligibility.warnings)}
          </article>
        `);
      }

      if (eligibility.daysUntilEligible && eligibility.daysUntilEligible > 0) {
        cards.push(`
          <article class="detail-card detail-card--neutral">
            <p><strong>Next eligible date</strong></p>
            <p>${escapeHtml(eligibility.nextEligibleDate || "Soon")}</p>
            <p>Days remaining: ${escapeHtml(eligibility.daysUntilEligible)}</p>
          </article>
        `);
      }

      setHtml("eligibilityDetails", cards.join(""));

      if (!eligibleNow && Array.isArray(eligibility.blockers) && eligibility.blockers.length > 0) {
        setText("eligibilitySummary", eligibility.blockers[0]);
      }
    })
    .catch((err) => console.log("Error loading eligibility:", err));
}

function loadBadges() {
  fetch(`${BASE_URL}/features/my-badges`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : null))
    .then((data) => {
      if (!data) return;

      const badgesContainer = document.getElementById("badgesContainer");
      const nextBadgeContainer = document.getElementById("nextBadgeContainer");
      const latestBadgeHighlight = document.getElementById("latestBadgeHighlight");

      if (badgesContainer) {
        if (Array.isArray(data.badges) && data.badges.length > 0) {
          badgesContainer.innerHTML = data.badges
            .map(
              (badge) => `
                <article class="badge-card" title="${escapeHtml(badge.description || "")}">
                  <div class="badge-card__icon">${escapeHtml(badge.icon || "*")}</div>
                  <div class="badge-card__name">${escapeHtml(badge.name || "Badge")}</div>
                  <div class="badge-card__desc">${escapeHtml(badge.description || "Achievement unlocked")}</div>
                </article>
              `
            )
            .join("");
        } else {
          badgesContainer.innerHTML = '<div class="empty-state">No badges yet. Keep donating.</div>';
        }
      }

      if (latestBadgeHighlight) {
        if (Array.isArray(data.badges) && data.badges.length > 0) {
          const badge = data.badges[0];
          latestBadgeHighlight.innerHTML = `
            <article class="badge-highlight__card">
              <div class="badge-highlight__title">${escapeHtml(badge.icon || "*")} ${escapeHtml(badge.name || "Badge unlocked")}</div>
              <div class="badge-highlight__meta">${escapeHtml(badge.description || "Your latest recognition is now visible here.")}</div>
            </article>
          `;
        } else {
          latestBadgeHighlight.innerHTML = '<div class="empty-state">Your newest badge highlight will appear here.</div>';
        }
      }

      if (nextBadgeContainer) {
        if (data.next) {
          const progress = Number(data.next.percentage || 0);
          nextBadgeContainer.innerHTML = `
            <p><strong>Next badge:</strong> ${escapeHtml(data.next.icon || "")} ${escapeHtml(data.next.name || "Upcoming badge")}</p>
            <p>Progress: ${escapeHtml(data.next.progress || 0)}/${escapeHtml(data.next.target || 0)}</p>
            <div class="progress-track"><span style="width:${Math.max(0, Math.min(progress, 100))}%"></span></div>
          `;
        } else {
          nextBadgeContainer.innerHTML = "<p><strong>All available badges have been earned.</strong></p>";
        }
      }
    })
    .catch((err) => console.log("Error loading badges:", err));
}

function loadRecentActivity() {
  const tbody = document.getElementById("activityBody");
  if (!tbody) return;

  fetch(`${BASE_URL}/visit-request/recent`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : []))
    .then((data) => {
      if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">No recent activity</td></tr>';
        return;
      }

      tbody.innerHTML = data
        .map(
          (visit) => `
            <tr>
              <td>${escapeHtml(formatDate(visit.requestDate))}</td>
              <td>${escapeHtml(visit.hospitalName || "-")}</td>
              <td>${escapeHtml(visit.units || 0)}</td>
              <td>${escapeHtml(visit.status || "-")}</td>
            </tr>
          `
        )
        .join("");
    })
    .catch(() => {
      tbody.innerHTML = '<tr><td colspan="4">Unable to load activity</td></tr>';
    });
}

function loadCertificates() {
  const certificatesBody = document.getElementById("certificatesBody");
  const certificateHighlight = document.getElementById("certificateHighlight");
  if (!certificatesBody) return;

  fetch(`${BASE_URL}/certificate/my-certificates`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : []))
    .then((data) => {
      if (!data || data.length === 0) {
        certificatesBody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:20px;color:#999;">No certificates yet. Complete a donation to earn a certificate.</td></tr>';
        if (certificateHighlight) {
          certificateHighlight.innerHTML = '<div class="empty-state">Complete a donation to unlock a shareable certificate snapshot.</div>';
        }
        return;
      }

      certificatesBody.innerHTML = data
        .map(
          (cert) => `
            <tr>
              <td><strong>${escapeHtml(cert.certificateNumber || "-")}</strong></td>
              <td>${escapeHtml(cert.hospitalName || "-")}</td>
              <td>${escapeHtml(formatDate(cert.createdDate))}</td>
              <td>${escapeHtml(cert.units || 0)} units</td>
              <td><a href="/certificate?id=${encodeURIComponent(cert.id)}" class="table-link">View and Download</a></td>
            </tr>
          `
        )
        .join("");

      if (certificateHighlight) {
        const latest = data[0];
        certificateHighlight.innerHTML = `
          <article class="certificate-highlight__card">
            <div class="certificate-highlight__title">${escapeHtml(data.length)} certificate(s) earned</div>
            <div class="certificate-highlight__meta">Latest: ${escapeHtml(latest.certificateNumber || "-")} from ${escapeHtml(latest.hospitalName || "your last hospital visit")}.</div>
          </article>
        `;
      }
    })
    .catch(() => {
      certificatesBody.innerHTML = '<tr><td colspan="5">Unable to load certificates</td></tr>';
    });
}

function setNoDonorState() {
  setText("totalDonations", "0");
  setText("totalUnits", "0");
  setText("lastDonation", "Not yet");
  setText("status", "Incomplete Profile");
  setText("nextEligible", "Please complete your donor profile first.");
  setText("eligibilitySummary", "Complete your donor profile to unlock readiness, streak, and milestone tracking.");
  setText("eligibilityCountdown", "Profile completion needed");
  setText("readinessState", "Profile needed");
  setText("donorLevel", "Starter");
  setText("donorLevelLabel", "Starter");
  setText("donorMilestoneText", "Complete your first donation to unlock your next level.");
  setRingProgress(8);
  updateFocusSnapshot({
    eligibility: "Profile incomplete",
    priority: "Waiting for donor profile",
    milestone: "Complete profile to begin"
  });

  const statusEl = document.getElementById("status");
  if (statusEl) statusEl.style.color = "#b77720";
}

function logout() {
  const doLogout = () => {
    fetch(`${BASE_URL}/auth/logout`, {
      method: "POST",
      credentials: "include"
    }).then(() => {
      window.location.href = "/login";
    });
  };

  if (typeof showConfirm === "function") {
    showConfirm("Are you sure you want to logout?", doLogout);
  } else {
    if (!confirm("Are you sure you want to logout?")) return;
    doLogout();
  }
}

function loadRewards() {
  fetch(`${BASE_URL}/rewards/my-rewards`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : null))
    .then((data) => {
      if (!data) return;

      const activeGrid = document.getElementById("activeRewardsGrid");
      const redeemedGrid = document.getElementById("redeemedRewardsGrid");
      const redeemedContainer = document.getElementById("redeemedRewardsContainer");

      if (activeGrid) {
        if (Array.isArray(data.active) && data.active.length > 0) {
          activeGrid.innerHTML = data.active
            .map(
              (reward) => `
                <article class="reward-card">
                  <div class="reward-card__icon">${escapeHtml(reward.icon || "*")}</div>
                  <div>
                    <h4>${escapeHtml(reward.title || "Reward")}</h4>
                    <p>${escapeHtml(reward.description || "Reward unlocked through donation activity.")}</p>
                  </div>
                  <div class="reward-code">${escapeHtml(reward.rewardCode || "-")}</div>
                  <div class="reward-card__actions">
                    <button class="btn btn-main btn-small" onclick="redeemReward(${reward.id})">Redeem</button>
                  </div>
                </article>
              `
            )
            .join("");
        } else {
          activeGrid.innerHTML = '<div class="empty-state">No active rewards yet. Donate blood to earn rewards.</div>';
        }
      }

      if (Array.isArray(data.redeemed) && data.redeemed.length > 0) {
        if (redeemedContainer) redeemedContainer.style.display = "block";
        if (redeemedGrid) {
          redeemedGrid.innerHTML = data.redeemed
            .map(
              (reward) => `
                <article class="reward-card reward-card--redeemed">
                  <div class="reward-card__icon">${escapeHtml(reward.icon || "*")}</div>
                  <div>
                    <h4>${escapeHtml(reward.title || "Reward")}</h4>
                    <p>Redeemed on ${escapeHtml(formatDate(reward.redeemedDate))}</p>
                  </div>
                  <div class="reward-status">Redeemed</div>
                </article>
              `
            )
            .join("");
        }
      }
    })
    .catch((err) => console.log("Error loading rewards:", err));
}

function flashRewardArea() {
  const rewardSection = document.getElementById("activeRewardsContainer");
  if (!rewardSection) return;
  rewardSection.style.transform = "translateY(-3px)";
  rewardSection.style.transition = "transform 180ms ease";
  window.setTimeout(() => {
    rewardSection.style.transform = "translateY(0)";
  }, 220);
}

function generateNewReward() {
  const btn = typeof event !== "undefined" ? event.target : null;
  if (btn) {
    btn.disabled = true;
    btn.textContent = "Generating...";
  }

  fetch(`${BASE_URL}/rewards/generate`, {
    method: "POST",
    credentials: "include"
  })
    .then((res) => res.json())
    .then((data) => {
      if (data.success) {
        notify(data.message, "success");
        flashRewardArea();
        loadRewards();
      } else {
        notify(data.reward || "Error generating reward", "error");
      }
    })
    .catch((err) => {
      notify(`Error: ${err.message}`, "error");
    })
    .finally(() => {
      if (btn) {
        btn.disabled = false;
        btn.textContent = "Get Random Reward";
      }
    });
}

function redeemReward(rewardId) {
  const doRedeem = () => {
    fetch(`${BASE_URL}/rewards/redeem/${rewardId}`, {
      method: "POST",
      credentials: "include"
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          notify(data.message, "success");
          loadRewards();
        } else {
          notify(`Error: ${data.message}`, "error");
        }
      })
      .catch((err) => {
        notify(`Error: ${err.message}`, "error");
      });
  };

  if (typeof showConfirm === "function") {
    showConfirm("Redeem this reward now?", doRedeem);
  } else {
    if (!confirm("Redeem this reward now?")) return;
    doRedeem();
  }
}

function loadMatchingRequests() {
  const container = document.getElementById("matchingRequests");
  if (!container) return;

  fetch(`${BASE_URL}/blood-request/matching-for-donor`, { credentials: "include" })
    .then((res) => (res.ok ? res.json() : []))
    .then((data) => {
      if (!data || data.length === 0) {
        container.innerHTML = '<div class="empty-state">No nearby requests at the moment.</div>';
        return;
      }

      container.innerHTML = data
        .map((req) => {
          const urgency = String(req.urgency || "NORMAL").toUpperCase();
          const urgent = urgency === "CRITICAL" || urgency === "HIGH";
          const title = `${req.patientName || "Patient"}${req.publicId ? ` | ${req.publicId}` : ""}`;
          const meta = [
            req.hospital || "",
            req.city || "",
            `${req.unitsRequired || 0} unit(s)`,
            req.status || ""
          ]
            .filter(Boolean)
            .map(escapeHtml)
            .join(" | ");

          const sub = `Matched donors: ${req.matchedDonors || 0} | Responded: ${req.donorsResponded || 0}`;

          return `
            <article class="request-card ${urgent ? "request-card--urgent" : ""}">
              <div>
                <div class="request-card__title">${escapeHtml(title)}</div>
                <div class="request-card__meta">${meta}</div>
                <div class="request-card__badges">
                  <span class="request-badge ${urgent ? "request-badge--critical" : ""}">${escapeHtml(urgency)}</span>
                  <span class="request-badge">${req.sameCity ? "Same city match" : "Regional match"}</span>
                </div>
                <div class="request-card__sub">${escapeHtml(sub)}</div>
              </div>
              <div class="request-card__actions">
                ${
                  req.responded
                    ? '<span class="request-pill">Responded</span>'
                    : `
                      <button class="btn btn-main btn-small" onclick="respondToRequest(${req.id}, 'accept', this)">Accept</button>
                      <button class="btn-outline btn-small" onclick="respondToRequest(${req.id}, 'decline', this)">Decline</button>
                    `
                }
              </div>
            </article>
          `;
        })
        .join("");
    })
    .catch(() => {
      container.innerHTML = '<div class="empty-state">Unable to load requests.</div>';
    });
}

function respondToRequest(requestId, action, btn) {
  if (btn) btn.disabled = true;

  const url = window.DONOR_ID
    ? `${BASE_URL}/blood-request/respond/${requestId}/${window.DONOR_ID}?action=${action}`
    : `${BASE_URL}/blood-request/respond/${requestId}?action=${action}`;

  fetch(url, { method: "POST", credentials: "include" })
    .then((res) => {
      if (!res.ok) throw new Error("Response not ok");
      return res.json();
    })
    .then(() => {
      notify(action === "accept" ? "Accepted request" : "Declined request", action === "accept" ? "success" : "info");
      loadMatchingRequests();
    })
    .catch((err) => {
      console.error("Error responding to request:", err);
      notify("Unable to respond", "error");
      if (btn) btn.disabled = false;
    });
}
