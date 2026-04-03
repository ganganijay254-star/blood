window.addEventListener("DOMContentLoaded", () => {

  // 🔐 SESSION CHECK
  fetch("/api/auth/check-session", {
    credentials: "include"
  })
    .then(res => {
      if (res.status === 401) {
        document.getElementById("notLoggedIn").style.display = "block";
        document.getElementById("profileContent").style.display = "none";
        return null;
      }
      return res.json();
    })
    .then(user => {
      if (!user) return;

      // Load basic user info
      loadUserProfile(user);

      // Load donor profile
      loadDonorProfile();
    })
    .catch(() => {
      document.getElementById("notLoggedIn").style.display = "block";
      document.getElementById("profileContent").style.display = "none";
    });
});

/* =========================
   LOAD USER PROFILE (BASIC)
========================= */
function loadUserProfile(user) {
  document.getElementById("userEmail").textContent = user.email || "--";
  document.getElementById("profileName").textContent = user.name || "--";
  document.getElementById("profileEmail").textContent = user.email || "--";
  document.getElementById("profileMobile").textContent = user.mobile || "--";
}

/* =========================
   LOAD DONOR PROFILE (DB)
========================= */
function loadDonorProfile() {
  fetch("/api/donor/me", {
    credentials: "include"
  })
    .then(res => res.ok ? res.json() : null)
    .then(donor => {
      if (!donor) return;

      setValue("profileBloodGroup", donor.bloodGroup);
      setValue(
        "profileGender",
        donor.gender === "MALE" ? "Male" :
        donor.gender === "FEMALE" ? "Female" : null
      );
      setValue("profileAge", donor.age ? donor.age + " years" : null);
      setValue("profileWeight", donor.weight ? donor.weight + " kg" : null);
      setValue("profileCity", donor.city);
      setValue(
        "profileSmoking",
        donor.smoking === "YES" ? "Yes" :
        donor.smoking === "NO" ? "No" : null
      );
      setValue("profileUnits", donor.units ? donor.units + " Unit(s)" : null);

      if (donor.lastDonationDate) {
        const dateObj = new Date(donor.lastDonationDate);
        setValue(
          "profileLastDonation",
          dateObj.toLocaleDateString("en-IN", {
            year: "numeric",
            month: "long",
            day: "numeric"
          })
        );
      }
    });
}

/* =========================
   HELPER
========================= */
function setValue(id, value) {
  const el = document.getElementById(id);
  if (!el) return;

  if (value !== null && value !== undefined) {
    el.textContent = value;
    el.classList.remove("empty");
  }
}

/* =========================
   LOGOUT (SESSION)
========================= */
function logout() {
  const doLogout = () => {
    fetch("/api/auth/logout", {
      method: "POST",
      credentials: "include"
    }).then(() => {
      window.location.href = "/login";
    });
  };

  if (typeof showConfirm === 'function') {
    showConfirm('Are you sure you want to logout?', doLogout);
  } else {
    if (!confirm('Are you sure you want to logout?')) return;
    doLogout();
  }
}
