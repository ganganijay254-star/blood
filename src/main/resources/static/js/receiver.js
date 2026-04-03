const form = document.getElementById("visitForm");
const msg = document.getElementById("eligibilityMsg");
const toast = document.getElementById("toast");
const hospitalList = document.getElementById("hospitalList");
const locationStep = document.getElementById("locationStep");
const locationStatus = document.getElementById("locationStatus");
const locationDisplay = document.getElementById("locationDisplay");
const requestFormSection = document.getElementById("requestFormSection");
const selectedHospitalNameInput = document.getElementById("selectedHospitalName");
const requestUnitsInput = document.getElementById("requestUnits");
const formStep = document.getElementById("formStep");
const visitStep = document.getElementById("visitStep");
const requestSummary = document.getElementById("requestSummary");
const bloodGroupInput = document.getElementById("bloodGroup");
const emergencyLevelInput = document.getElementById("emergencyLevel");
const compatibilityText = document.getElementById("compatibilityText");
const compatibilityPills = document.getElementById("compatibilityPills");
const urgentNeededBtn = document.getElementById("urgentNeededBtn");
const emergencyBanner = document.getElementById("emergencyBanner");
let receiverConfig = { settings: {}, controls: {} };
let emergencyMode = false;

let selectedHospital = "";

const compatibility = {
  "A+": ["A+", "A-", "O+", "O-"],
  "A-": ["A-", "O-"],
  "B+": ["B+", "B-", "O+", "O-"],
  "B-": ["B-", "O-"],
  "AB+": ["AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"],
  "AB-": ["AB-", "A-", "B-", "O-"],
  "O+": ["O+", "O-"],
  "O-": ["O-"]
};

function showToast(text, type = "success") {
  if (!toast) return;
  toast.innerText = text;
  toast.className = `toast show ${type}`;
  setTimeout(() => toast.classList.remove("show"), 3000);
}

function showEligibility(status = "eligible", message = "") {
  if (!msg) return;
  msg.innerText = message || "Eligibility will be verified by the system.";
  msg.className = "eligibility " + (status === "eligible" ? "eligible" : "not-eligible");
}

function updateCompatibilityPanel() {
  if (!compatibilityText || !compatibilityPills) return;
  const selectedGroup = (bloodGroupInput && bloodGroupInput.value) || "";
  const compatibleGroups = compatibility[selectedGroup] || [];

  if (!selectedGroup) {
    compatibilityText.innerText = "Select blood group to see what can be used for this receiver.";
    compatibilityPills.innerHTML = "";
    return;
  }

  compatibilityText.innerText = `${selectedGroup} receiver can receive from:`;
  compatibilityPills.innerHTML = compatibleGroups.map((group) => `<span class="compat-pill">${group}</span>`).join("");
}

function syncEmergencyMode(forceCritical = false) {
  const selectedUrgency = (emergencyLevelInput && emergencyLevelInput.value) || "LOW";
  emergencyMode = forceCritical || selectedUrgency === "CRITICAL";

  if (emergencyLevelInput && forceCritical) {
    emergencyLevelInput.value = "CRITICAL";
  }
  if (urgentNeededBtn) {
    urgentNeededBtn.classList.toggle("active", emergencyMode);
    urgentNeededBtn.innerText = emergencyMode ? "Emergency Mode Enabled" : "Urgent Needed";
  }
  if (emergencyBanner) {
    emergencyBanner.classList.toggle("hidden", !emergencyMode);
  }
}

async function loadPublicConfig() {
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) return;
    receiverConfig = await res.json();
  } catch (e) {
    receiverConfig = { settings: {}, controls: {} };
  }
}

function ensureHospitalInput() {
  if (!form) return null;
  let hospitalInput = document.getElementById("hospital");
  if (!hospitalInput) {
    hospitalInput = document.createElement("input");
    hospitalInput.type = "hidden";
    hospitalInput.id = "hospital";
    hospitalInput.name = "hospital";
    form.appendChild(hospitalInput);
  }
  return hospitalInput;
}

function openReceiverForm(hospitalName, hospitalCard) {
  selectedHospital = hospitalName || "";
  const hospitalInput = ensureHospitalInput();
  if (hospitalInput) hospitalInput.value = selectedHospital;
  if (selectedHospitalNameInput) selectedHospitalNameInput.value = selectedHospital;

  document.querySelectorAll(".hospital-card").forEach((card) => {
    card.style.borderColor = "#e5e7eb";
  });
  if (hospitalCard) hospitalCard.style.borderColor = "#dc2626";

  if (requestFormSection) requestFormSection.classList.remove("hidden");
  if (requestFormSection) requestFormSection.scrollIntoView({ behavior: "smooth", block: "center" });

  showToast(`Selected: ${selectedHospital}`);
}

function renderHospitals(data) {
  if (!hospitalList) return;

  if (!Array.isArray(data) || data.length === 0) {
    hospitalList.innerHTML = "<p>No nearby hospital found in 5 km range.</p>";
    showToast("No nearby hospitals found", "error");
    return;
  }

  hospitalList.innerHTML = "";
  const hospitalInput = ensureHospitalInput();

  data.forEach((h, index) => {
    const card = document.createElement("div");
    card.className = "hospital-card";
    const safeName = (h.name || "").replace(/"/g, "&quot;");
    card.innerHTML = `
      <h4>${h.name || "Hospital"}</h4>
      <p>${h.address || "-"}</p>
      <span>${h.distance ?? "-"} km away</span>
      <div style="margin-top:10px;">
        <button type="button" class="btn btn-main select-hospital-btn" data-name="${safeName}">Select</button>
      </div>
    `;
    hospitalList.appendChild(card);

    if (index === 0 && !selectedHospital) {
      openReceiverForm(h.name || "", card);
      if (hospitalInput) hospitalInput.value = h.name || "";
    }
  });

  document.querySelectorAll(".select-hospital-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const card = btn.closest(".hospital-card");
      openReceiverForm(btn.dataset.name || "", card);
    });
  });

  showToast("Nearby hospitals loaded");
}

function fetchNearbyHospitals(lat, lng) {
  fetch(`/api/hospitals/nearby?lat=${lat}&lng=${lng}`)
    .then((res) => {
      if (!res.ok) throw new Error("Failed to fetch hospitals");
      return res.json();
    })
    .then((data) => {
      renderHospitals(data);
      if (locationStep) locationStep.classList.add("hidden");
    })
    .catch(() => {
      if (locationStatus) {
        locationStatus.innerText = "Unable to load nearby hospitals.";
      }
      showToast("Hospital service error", "error");
    });
}

function getLocation() {
  if (locationStep) locationStep.classList.remove("hidden");
  if (locationStatus) locationStatus.innerText = "Detecting your location...";

  if (!navigator.geolocation) {
    if (locationStatus) locationStatus.innerText = "Geolocation is not supported.";
    showToast("Geolocation not supported", "error");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;

      if (locationStatus) locationStatus.innerText = "Location detected.";
      if (locationDisplay) locationDisplay.innerText = `Latitude: ${lat.toFixed(5)}, Longitude: ${lng.toFixed(5)}`;

      fetchNearbyHospitals(lat, lng);
    },
    () => {
      if (locationStatus) locationStatus.innerText = "Location permission denied.";
      showToast("Please allow location permission", "error");
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

window.getLocation = getLocation;

showEligibility();
ensureHospitalInput();
loadPublicConfig().then(() => {
  const controls = receiverConfig.controls || {};
  if (controls.maintenanceMode === true || controls.showBloodRequest === false || controls.enableRequests === false) {
    showEligibility("not-eligible", "Blood request feature is currently disabled by admin.");
    if (form) form.style.display = "none";
    if (locationStep) locationStep.classList.add("hidden");
    return;
  }
  getLocation();
});

if (form) {
  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const hospital = (document.getElementById("hospital") || {}).value || selectedHospital;
    const units = Number((requestUnitsInput && requestUnitsInput.value) || 0);
    const emergencyLevel = (document.getElementById("emergencyLevel") || {}).value || 'LOW';

    if (!hospital) {
      showToast("Please select hospital", "error");
      return;
    }

    if (!units || units <= 0) {
      showToast("Please enter valid units", "error");
      return;
    }

    // emergencyLevel is optional but defaults to NORMAL

    const controls = receiverConfig.controls || {};
    if (controls.maintenanceMode === true || controls.showBloodRequest === false || controls.enableRequests === false) {
      showToast("Blood request feature is currently disabled by admin", "error");
      return;
    }

    // Prepare form data and submit blood request
    const payload = {
      patientName: (document.getElementById("patientName") || {}).value || "",
      hospital: hospital,
      unitsRequired: units,
      bloodGroup: (document.getElementById("bloodGroup") || {}).value || "",
      city: (document.getElementById("city") || {}).value || "",
      contactNumber: (document.getElementById("contactNumber") || {}).value || "",
      urgency: emergencyMode ? "CRITICAL" : ((document.getElementById("emergencyLevel") || {}).value || emergencyLevel || "LOW"),
      description: "",
      // medical proof upload not implemented server-side; skip for now
    };

    const endpoint = payload.urgency === "CRITICAL" ? "/api/blood-request/create-emergency" : "/api/blood-request/create";

    fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(payload)
    })
      .then(async (res) => {
        if (res.status === 401) {
          showToast("Please login first", "error");
          setTimeout(() => {
            window.location.href = "/login";
          }, 1200);
          return null;
        }
        if (!res.ok) throw new Error("Submit failed");
        return res.json();
      })
      .then((saved) => {
        if (!saved) return;

        if (requestSummary) {
          requestSummary.innerHTML = `
            <p><strong>Hospital:</strong> ${saved.request.hospital}</p>
            <p><strong>Units:</strong> ${saved.request.unitsRequired}</p>
            <p><strong>Request ID:</strong> ${saved.request.publicId || ''}</p>
            <p><strong>Status:</strong> ${saved.request.status}</p>
            <p><strong>Compatible:</strong> ${(saved.compatibleBloodGroups || []).join(', ') || '-'}</p>
          `;
        }

        if (formStep) formStep.classList.add("hidden");
        if (visitStep) visitStep.classList.remove("hidden");
        showToast("Request submitted successfully");

        // Redirect to tracking page for receiver
        // If medical proof file is selected, upload it, then redirect to tracking
        const fileInput = document.getElementById('medicalProof');
        const reqId = saved.request.id;
        if (fileInput && fileInput.files && fileInput.files.length > 0 && reqId) {
          const fd = new FormData();
          fd.append('file', fileInput.files[0]);
          fetch(`/api/blood-request/upload-proof/${reqId}`, { method: 'POST', body: fd, credentials: 'include' })
            .then(res => res.ok ? res.json() : null)
            .then(() => {
              window.location.href = `/receiver-track?requestId=${reqId}`;
            })
            .catch(() => {
              // still redirect even if upload fails
              window.location.href = `/receiver-track?requestId=${reqId}`;
            });
        } else {
          setTimeout(() => window.location.href = `/receiver-track?requestId=${reqId}` , 1000);
        }
      })
      .catch(() => {
        showToast("Unable to submit request", "error");
      });
  });
}

if (bloodGroupInput) {
  bloodGroupInput.addEventListener("change", updateCompatibilityPanel);
  updateCompatibilityPanel();
}

if (emergencyLevelInput) {
  emergencyLevelInput.addEventListener("change", () => syncEmergencyMode(false));
  syncEmergencyMode(emergencyLevelInput.value === "CRITICAL");
}

if (urgentNeededBtn) {
  urgentNeededBtn.addEventListener("click", () => syncEmergencyMode(true));
}
