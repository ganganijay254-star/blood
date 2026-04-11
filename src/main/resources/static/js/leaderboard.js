let allDonors = [];
let currentFilter = "all";

function getAvatarEmoji(bloodGroup) {
  const avatars = {
    "A+": "A", "A-": "A",
    "B+": "B", "B-": "B",
    "AB+": "AB", "AB-": "AB",
    "O+": "O", "O-": "O"
  };
  return avatars[bloodGroup] || "D";
}

function toTitle(text) {
  return (text || "").charAt(0).toUpperCase() + (text || "").slice(1);
}

function createDonorCard(donor, rank, isTop3 = false) {
  const avatar = getAvatarEmoji(donor.bloodGroup);
  const tierClass = `tier-${donor.tier}`;

  if (isTop3) {
    const medalEmoji = ["🥇", "🥈", "🥉"][rank - 1] || "🏅";
    const medalClass = ["gold-medal", "silver-medal", "bronze-medal"][rank - 1] || "";

    return `
      <div class="top-donor-card ${medalClass}">
        <div class="medal-emoji">${medalEmoji}</div>
        <div class="donor-top-avatar">${avatar}</div>
        <div class="donor-rank-top">#${rank}</div>
        <div class="donor-name-top">${donor.name}</div>
        <div class="tier-badge ${tierClass}">${toTitle(donor.tier)} Tier</div>
        <div class="donor-details">
          <div class="detail-item"><span class="detail-label">Blood</span><span class="detail-value">${donor.bloodGroup}</span></div>
          <div class="detail-item"><span class="detail-label">Gender</span><span class="detail-value">${donor.gender || "--"}</span></div>
          <div class="detail-item"><span class="detail-label">Age</span><span class="detail-value">${donor.age || "--"}</span></div>
          <div class="detail-item"><span class="detail-label">City</span><span class="detail-value">${donor.city}</span></div>
          <div class="detail-item"><span class="detail-label">Units Donated</span><span class="detail-value">${donor.totalUnitsDonated || 0}</span></div>
          <div class="detail-item"><span class="detail-label">Donations</span><span class="detail-value">${donor.donationCount || 0}</span></div>
          <div class="detail-item"><span class="detail-label">Points</span><span class="detail-value">${donor.points}</span></div>
        </div>
        <div class="coupon-section">
          <div class="coupon-label">🎁 Earned Reward</div>
          <div class="coupon-code" style="background:#fff;border:1px solid #e5e7eb;line-height:1.6;font-size:15px;padding:12px;color:#333;">
            ${donor.reward}
          </div>
        </div>
      </div>
    `;
  }

  return `
    <div class="donor-card">
      <div class="card-rank">#${rank}</div>
      <div class="card-avatar">${avatar}</div>
      <div class="card-name">${donor.name}</div>
      <div class="card-blood">${donor.bloodGroup}</div>
      <div class="card-city">${donor.city}</div>
      <div class="card-city">Gender: ${donor.gender || "--"} | Age: ${donor.age || "--"}</div>
      <div class="tier-badge ${tierClass}">${toTitle(donor.tier)} Tier</div>
      <div class="card-stats">
        <div class="stat">
          <span class="stat-label">Units</span>
          <span class="stat-value">${donor.totalUnitsDonated || 0}</span>
        </div>
        <div class="stat">
          <span class="stat-label">Donations</span>
          <span class="stat-value">${donor.donationCount || 0}</span>
        </div>
        <div class="stat">
          <span class="stat-label">Points</span>
          <span class="stat-value">${donor.points}</span>
        </div>
      </div>
      <div class="coupon-box">
        <div class="coupon-title">🎁 Reward</div>
        <div class="coupon-display" style="font-size:14px;line-height:1.5;color:#333;font-weight:600;">${donor.reward}</div>
      </div>
    </div>
  `;
}

function filteredDonors() {
  if (currentFilter === "all") return allDonors;
  return allDonors.filter((d) => d.tier === currentFilter);
}

function renderLeaderboard() {
  const donors = filteredDonors();
  const gridContainer = document.getElementById("donorsGrid");
  const top3Container = document.getElementById("top3Container");
  const emptyState = document.getElementById("emptyState");
  const totalDonors = document.getElementById("totalDonors");
  const totalDonations = document.getElementById("totalDonations");
  const totalPoints = document.getElementById("totalPoints");

  if (!gridContainer || !top3Container || !emptyState) return;

  if (donors.length === 0) {
    emptyState.style.display = "block";
    gridContainer.innerHTML = "";
    top3Container.innerHTML = "";
    if (totalDonors) totalDonors.textContent = "0";
    if (totalDonations) totalDonations.textContent = "0";
    if (totalPoints) totalPoints.textContent = "0";
    return;
  }

  emptyState.style.display = "none";

  top3Container.innerHTML = donors
    .slice(0, 3)
    .map((d, i) => createDonorCard(d, i + 1, true))
    .join("");

  gridContainer.innerHTML = donors
    .map((d, i) => createDonorCard(d, i + 1))
    .join("");

  if (totalDonors) totalDonors.textContent = String(donors.length);
  if (totalDonations) {
    totalDonations.textContent = String(
      donors.reduce((sum, d) => sum + Number(d.totalUnitsDonated || 0), 0)
    );
  }
  if (totalPoints) {
    totalPoints.textContent = String(
      donors.reduce((sum, d) => sum + Number(d.points || 0), 0)
    );
  }
}

function setupFilters() {
  document.querySelectorAll(".filter-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".filter-btn").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      currentFilter = btn.dataset.filter || "all";
      renderLeaderboard();
    });
  });
}

function loadLeaderboard() {
  fetch("/api/donor/leaderboard")
    .then((res) => {
      if (!res.ok) throw new Error("Failed to load leaderboard");
      return res.json();
    })
    .then((data) => {
      allDonors = Array.isArray(data) ? data : [];
      renderLeaderboard();
    })
    .catch(() => {
      allDonors = [];
      renderLeaderboard();
    });
}

document.addEventListener("DOMContentLoaded", () => {
  setupFilters();
  loadLeaderboard();
});
