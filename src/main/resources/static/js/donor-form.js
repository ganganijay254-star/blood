const BASE_URL = "/api";
const ALLOWED_BLOOD_GROUPS = new Set(["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"]);
const ALLOWED_GENDERS = new Set(["MALE", "FEMALE"]);
const ALLOWED_SMOKING_STATUS = new Set(["YES", "NO"]);

let donorConfig = {
  minAge: 18,
  maxAge: 65,
  minWeight: 50,
  controls: {}
};

async function loadPublicConfig() {
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) return;

    const data = await res.json();
    donorConfig.minAge = (data.settings && data.settings.minAge) || 18;
    donorConfig.maxAge = (data.settings && data.settings.maxAge) || 65;
    donorConfig.minWeight = (data.settings && data.settings.minWeight) || 50;
    donorConfig.controls = data.controls || {};
  } catch (error) {
    // Keep defaults when config is unavailable.
  }
}

function bindFloatingLabel(field) {
  const group = field.closest(".input-group");
  if (!group) return;

  const syncState = () => {
    const value = typeof field.value === "string" ? field.value.trim() : field.value;
    group.classList.toggle("has-value", Boolean(value));
  };

  ["input", "change", "blur"].forEach(eventName => {
    field.addEventListener(eventName, syncState);
  });

  syncState();
}

async function readResponsePayload(response) {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    try {
      return await response.json();
    } catch (error) {
      return {};
    }
  }

  const text = await response.text();
  return text ? { message: text } : {};
}

document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("donorForm");
  const toast = document.getElementById("toast");
  const submitButton = document.getElementById("saveDonorButton");
  const statusCard = document.getElementById("donorStatusCard");
  const statusBadge = document.getElementById("statusBadge");
  const statusTitle = document.getElementById("statusTitle");
  const statusMessage = document.getElementById("statusMessage");
  const statusSummary = document.getElementById("donorStatusSummary");
  const statusBloodGroup = document.getElementById("statusBloodGroup");
  const statusCity = document.getElementById("statusCity");
  const statusLastDonation = document.getElementById("statusLastDonation");
  const statusPrimaryLink = document.getElementById("statusPrimaryLink");
  const statusSecondaryLink = document.getElementById("statusSecondaryLink");
  const lastDonationField = document.getElementById("lastDonation");
  const trackedFields = Array.from(document.querySelectorAll("#donorForm .input-group input, #donorForm .input-group select"));

  if (!form) return;

  if (lastDonationField) {
    lastDonationField.max = new Date().toISOString().split("T")[0];
  }

  trackedFields.forEach(field => {
    bindFloatingLabel(field);
    ["input", "change"].forEach(eventName => {
      field.addEventListener(eventName, () => clearFieldError(field.id));
    });
  });

  function showToast(message, type = "success") {
    if (!toast) return;

    toast.innerText = message;
    toast.className = `toast show ${type}`;
    setTimeout(() => toast.classList.remove("show"), 3000);
  }

  function setSubmitState(isLoading) {
    if (!submitButton) return;

    submitButton.disabled = isLoading;
    submitButton.textContent = isLoading
      ? "Saving..."
      : (submitButton.dataset.defaultLabel || "Save & Continue");
  }

  function clearFieldError(fieldId) {
    const field = document.getElementById(fieldId);
    const group = field ? field.closest(".input-group") : null;
    const errorEl = document.getElementById(`${fieldId}Error`);

    if (group) {
      group.classList.remove("is-invalid");
    }

    if (errorEl) {
      errorEl.textContent = "";
    }
  }

  function clearFieldErrors() {
    trackedFields.forEach(field => clearFieldError(field.id));
  }

  function setFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const group = field ? field.closest(".input-group") : null;
    const errorEl = document.getElementById(`${fieldId}Error`);

    if (group) {
      group.classList.add("is-invalid");
    }

    if (errorEl) {
      errorEl.textContent = message;
    }
  }

  function applyFieldErrors(fieldErrors = {}) {
    Object.entries(fieldErrors).forEach(([fieldId, message]) => {
      if (fieldId === "form") return;
      setFieldError(fieldId, message);
    });
  }

  function formatDate(value) {
    if (!value) return "Not provided";

    const dateObj = new Date(`${value}T00:00:00`);
    if (Number.isNaN(dateObj.getTime())) {
      return value;
    }

    return dateObj.toLocaleDateString("en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric"
    });
  }

  function showForm() {
    statusCard.hidden = true;
    form.hidden = false;
  }

  function showStatusCard({
    badge,
    title,
    message,
    primaryHref,
    primaryLabel,
    secondaryHref,
    secondaryLabel,
    summary
  }) {
    form.hidden = true;
    statusCard.hidden = false;

    statusBadge.textContent = badge;
    statusTitle.textContent = title;
    statusMessage.textContent = message;
    statusPrimaryLink.href = primaryHref;
    statusPrimaryLink.textContent = primaryLabel;
    statusSecondaryLink.href = secondaryHref;
    statusSecondaryLink.textContent = secondaryLabel;

    if (summary) {
      statusSummary.hidden = false;
      statusBloodGroup.textContent = summary.bloodGroup || "-";
      statusCity.textContent = summary.city || "-";
      statusLastDonation.textContent = summary.lastDonation || "Not provided";
    } else {
      statusSummary.hidden = true;
    }
  }

  function getFormValues() {
    return {
      bloodGroup: document.getElementById("bloodGroup").value,
      gender: document.getElementById("gender").value,
      age: document.getElementById("age").value,
      weight: document.getElementById("weight").value,
      city: document.getElementById("city").value.trim(),
      smoking: document.getElementById("smoking").value,
      lastDonationDate: document.getElementById("lastDonation").value
    };
  }

  function validateForm(values) {
    const fieldErrors = {};
    const age = Number(values.age);
    const weight = Number(values.weight);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (!ALLOWED_BLOOD_GROUPS.has(values.bloodGroup)) {
      fieldErrors.bloodGroup = "Select a valid blood group.";
    }

    if (!ALLOWED_GENDERS.has(values.gender)) {
      fieldErrors.gender = "Select a valid gender.";
    }

    if (values.age === "") {
      fieldErrors.age = "Enter your age.";
    } else if (!Number.isFinite(age)) {
      fieldErrors.age = "Enter a valid age.";
    } else if (age < donorConfig.minAge || age > donorConfig.maxAge) {
      fieldErrors.age = `Age must be between ${donorConfig.minAge} and ${donorConfig.maxAge}.`;
    }

    if (values.weight === "") {
      fieldErrors.weight = "Enter your weight.";
    } else if (!Number.isFinite(weight)) {
      fieldErrors.weight = "Enter a valid weight.";
    } else if (weight < donorConfig.minWeight) {
      fieldErrors.weight = `Minimum weight must be ${donorConfig.minWeight} kg.`;
    }

    if (!values.city) {
      fieldErrors.city = "Select your city.";
    }

    if (!ALLOWED_SMOKING_STATUS.has(values.smoking)) {
      fieldErrors.smoking = "Select your smoking status.";
    } else if (values.smoking === "YES") {
      fieldErrors.smoking = "Smokers are not eligible to register as donors.";
    }

    if (values.lastDonationDate) {
      const lastDonation = new Date(`${values.lastDonationDate}T00:00:00`);

      if (Number.isNaN(lastDonation.getTime())) {
        fieldErrors.lastDonation = "Enter a valid donation date.";
      } else if (lastDonation > today) {
        fieldErrors.lastDonation = "Last donation date cannot be in the future.";
      } else {
        const diffDays = Math.floor((today - lastDonation) / (1000 * 60 * 60 * 24));

        if (values.gender === "MALE" && diffDays < 90) {
          fieldErrors.lastDonation = "Male donors need a minimum 90 day gap.";
        }

        if (values.gender === "FEMALE" && diffDays < 120) {
          fieldErrors.lastDonation = "Female donors need a minimum 120 day gap.";
        }
      }
    }

    return fieldErrors;
  }

  async function showExistingDonorStatus(customMessage) {
    try {
      const donorRes = await fetch(`${BASE_URL}/donor/me`, {
        credentials: "include"
      });

      if (donorRes.status === 401) {
        window.location.href = "/login";
        return;
      }

      const donor = donorRes.ok ? await readResponsePayload(donorRes) : null;

      showStatusCard({
        badge: "Profile Ready",
        title: "Donor profile already exists",
        message: customMessage || "Your donor profile is already complete. You can continue from the dashboard or review your saved profile.",
        primaryHref: "/donor-dashboard",
        primaryLabel: "Open Dashboard",
        secondaryHref: "/donor-profile",
        secondaryLabel: "View Profile",
        summary: donor && donor.id ? {
          bloodGroup: donor.bloodGroup || "-",
          city: donor.city || "-",
          lastDonation: donor.lastDonationDate ? formatDate(donor.lastDonationDate) : "Not provided"
        } : null
      });
    } catch (error) {
      showStatusCard({
        badge: "Profile Ready",
        title: "Donor profile already exists",
        message: customMessage || "Your donor profile is already complete. Open your dashboard to continue.",
        primaryHref: "/donor-dashboard",
        primaryLabel: "Open Dashboard",
        secondaryHref: "/donor-profile",
        secondaryLabel: "View Profile"
      });
    }
  }

  async function initializeForm() {
    const sessionRes = await fetch(`${BASE_URL}/auth/check-session`, {
      credentials: "include"
    });

    if (sessionRes.status === 401) {
      window.location.href = "/login";
      return;
    }

    await loadPublicConfig();

    if (donorConfig.controls.maintenanceMode === true || donorConfig.controls.enableDonations === false) {
      showStatusCard({
        badge: "Unavailable",
        title: "Donor form is currently unavailable",
        message: "The admin has temporarily paused donor registration. Please try again later or return to the dashboard.",
        primaryHref: "/donor-dashboard",
        primaryLabel: "Open Dashboard",
        secondaryHref: "/",
        secondaryLabel: "Back to Home"
      });
      return;
    }

    const donorRes = await fetch(`${BASE_URL}/donor/me`, {
      credentials: "include"
    });

    if (donorRes.status === 401) {
      window.location.href = "/login";
      return;
    }

    if (donorRes.ok) {
      const donor = await readResponsePayload(donorRes);
      if (donor && donor.id) {
        await showExistingDonorStatus();
        return;
      }
    }

    showForm();
  }

  initializeForm().catch(() => {
    showForm();
    showToast("Unable to load donor form details right now.", "warn");
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    clearFieldErrors();

    const values = getFormValues();
    const fieldErrors = validateForm(values);

    if (Object.keys(fieldErrors).length > 0) {
      applyFieldErrors(fieldErrors);
      showToast("Please fix the highlighted fields.", "error");
      return;
    }

    const donorData = {
      bloodGroup: values.bloodGroup,
      gender: values.gender,
      age: Number(values.age),
      weight: Number(values.weight),
      city: values.city,
      smoking: values.smoking,
      lastDonationDate: values.lastDonationDate || null
    };

    setSubmitState(true);

    try {
      const res = await fetch(`${BASE_URL}/donor/save`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(donorData)
      });

      if (res.status === 401) {
        window.location.href = "/login";
        return;
      }

      const payload = await readResponsePayload(res);

      if (res.status === 409 && payload.code === "DONOR_ALREADY_EXISTS") {
        await showExistingDonorStatus(payload.message);
        showToast(payload.message || "Donor profile already exists.", "warn");
        return;
      }

      if (!res.ok) {
        if (payload.fieldErrors) {
          applyFieldErrors(payload.fieldErrors);
        }

        showToast(payload.message || "Server error. Please try again.", "error");
        return;
      }

      showToast("Donor profile saved successfully");

      setTimeout(() => {
        window.location.href = "/visit";
      }, 1200);
    } catch (error) {
      showToast("Server error. Please try again.", "error");
    } finally {
      setSubmitState(false);
    }
  });
});
