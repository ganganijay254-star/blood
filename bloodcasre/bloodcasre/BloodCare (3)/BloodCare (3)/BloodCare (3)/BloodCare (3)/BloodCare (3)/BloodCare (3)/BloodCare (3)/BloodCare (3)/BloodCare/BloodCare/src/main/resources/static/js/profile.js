window.addEventListener("DOMContentLoaded", () => {

  fetch("/api/auth/check-session", {
    credentials: "include"
  })
    .then(res => {
      if (res.status === 401) {
        // ❌ not logged in
        window.location.href = "/login";
        return null;
      }
      return res.json();
    })
    .then(user => {
      if (!user) return;

      // ✅ show profile
      document.getElementById("profileContent").style.display = "block";

      // ✅ fill user data
      document.getElementById("profileName").textContent = user.name || "—";
      document.getElementById("profileEmail").textContent = user.email || "—";
      document.getElementById("userEmail").textContent = user.email || "—";
      document.getElementById("profileMobile").textContent = user.mobile || "—";
      document.getElementById("profileRole").textContent = user.role || "USER";
    })
    .catch(() => {
      window.location.href = "/login";
    });
});

/* =====================
   LOGOUT (SESSION)
===================== */
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
