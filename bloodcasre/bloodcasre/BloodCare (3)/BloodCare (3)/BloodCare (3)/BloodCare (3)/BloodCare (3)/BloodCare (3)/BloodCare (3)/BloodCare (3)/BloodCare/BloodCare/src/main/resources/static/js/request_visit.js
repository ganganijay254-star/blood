const BASE_URL = "/api";
const form = document.getElementById("visitForm");
const params = new URLSearchParams(window.location.search);
const hospitalFromUrl = params.get("hospital");
let visitConfig = { settings: {}, controls: {} };

function syncFloatingLabelState(field) {
  if (!field || !field.parentElement) return;
  const hasValue = String(field.value || "").trim() !== "";
  field.parentElement.classList.toggle("has-value", hasValue);
}

function bindFloatingLabel(field) {
  if (!field) return;
  syncFloatingLabelState(field);
  field.addEventListener("input", () => syncFloatingLabelState(field));
  field.addEventListener("change", () => syncFloatingLabelState(field));
}

function showToast(msg, type = "success") {
  if (typeof window.localShow === "function") {
    window.localShow(msg, type);
    return;
  }
  if (typeof window.showToast === "function") {
    window.showToast(msg, type);
    return;
  }
  alert(msg);
}

async function loadPublicConfig() {
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) return;
    visitConfig = await res.json();
  } catch (e) {
    visitConfig = { settings: {}, controls: {} };
  }
}

function loadDonorInfo() {
  fetch(`${BASE_URL}/donor/me`, { credentials: "include" })
    .then((res) => {
      if (res.status === 401) {
        window.location.href = "/login";
        return null;
      }
      return res.json();
    })
    .then((donor) => {
      if (!donor) return;
      document.getElementById("donorId").innerText = donor.id || "-";
      document.getElementById("donorBlood").innerText = donor.bloodGroup || "-";
      document.getElementById("donorUnits").innerText = donor.units || "-";
    })
    .catch(() => {});
}

if (hospitalFromUrl) {
  const hospitalNameField = document.getElementById("hospitalName");
  hospitalNameField.value = decodeURIComponent(hospitalFromUrl);
  syncFloatingLabelState(hospitalNameField);
}

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("#visitForm .input-group input, #visitForm .input-group select").forEach(bindFloatingLabel);

  loadPublicConfig().then(() => {
    const controls = visitConfig.controls || {};
    if (controls.maintenanceMode === true || controls.enableDonations === false) {
      showToast("Donation visit requests are currently disabled", "error");
      if (form) form.style.display = "none";
      return;
    }
  });

  loadDonorInfo();

  const today = new Date().toISOString().split("T")[0];
  const visitDateField = document.getElementById("visitDate");
  visitDateField.min = today;
  syncFloatingLabelState(visitDateField);
});

if (form) {
  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const visitDate = document.getElementById("visitDate").value;
    const timeSlot = document.getElementById("timeSlot").value;
    const donationType = document.getElementById("donationType").value;
    const units = parseInt(document.getElementById("units").value, 10);
    const location = document.getElementById("location").value;
    const remark = document.getElementById("remark").value.trim();

    if (!visitDate || !timeSlot || !donationType || !units || !location) {
      showToast("Please fill all required fields", "error");
      return;
    }

    const controls = visitConfig.controls || {};
    if (controls.maintenanceMode === true || controls.enableDonations === false) {
      showToast("Donation visit requests are currently disabled", "error");
      return;
    }

    const maxUnits = (visitConfig.settings && visitConfig.settings.maxUnitsPerDonation) || 2;
    if (units > maxUnits) {
      showToast(`Maximum units per donation is ${maxUnits}`, "error");
      return;
    }

    const selectedDate = new Date(visitDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (selectedDate < today) {
      showToast("Please select a future date", "error");
      return;
    }

    fetch(`${BASE_URL}/visit-request`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        hospitalName: decodeURIComponent(hospitalFromUrl),
        visitDate,
        timeSlot,
        donationType,
        units,
        location,
        remark: remark || null,
        requestType: "DONOR"
      })
    })
      .then((res) => {
        if (res.status === 401) {
          window.location.href = "/login";
          return null;
        }
        if (!res.ok) throw new Error("Failed to submit");
        return res.json();
      })
      .then((data) => {
        if (!data) return;
        showToast("Visit request submitted successfully", "success");
        setTimeout(() => {
          window.location.href = "/donor-dashboard";
        }, 2200);
      })
      .catch(() => {
        showToast("Failed to submit request. Please try again.", "error");
      });
  });
}
