const BASE_URL = "/api/auth";

function localShow(msg, type) {
  if (window.showToast) {
    return window.showToast(msg, type === "success" ? "success" : "error");
  }

  let c = document.getElementById("toastContainer");
  if (!c) {
    c = document.createElement("div");
    c.id = "toastContainer";
    c.style.position = "fixed";
    c.style.top = "20px";
    c.style.right = "20px";
    c.style.zIndex = "9999";
    document.body.appendChild(c);
  }

  const t = document.createElement("div");
  t.className = "toast " + (type === "success" ? "success" : "error");
  t.innerText = msg;
  t.style.background = type === "success" ? "#2b7d58" : "#8a2431";
  t.style.color = "#fff";
  t.style.padding = "12px 24px";
  t.style.marginTop = "10px";
  t.style.borderRadius = "16px";
  t.style.boxShadow = "0 12px 28px rgba(29,37,53,0.18)";
  t.style.fontWeight = "bold";
  c.appendChild(t);
  setTimeout(() => {
    t.style.opacity = "0";
    setTimeout(() => t.remove(), 300);
  }, 3500);
}

function togglePassword(id) {
  const input = document.getElementById(id);
  if (!input) return;

  input.type = input.type === "password" ? "text" : "password";
  const toggle = input.parentElement ? input.parentElement.querySelector(".show-hide") : null;
  if (toggle) {
    toggle.textContent = input.type === "password" ? "Show" : "Hide";
  }
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function setFieldState(groupId, errorId, message) {
  const group = groupId ? document.getElementById(groupId) : null;
  const error = document.getElementById(errorId);
  if (group) {
    group.classList.toggle("has-error", Boolean(message));
  }
  if (error) {
    error.textContent = message || "";
  }
}

function setStatusMessage(id, message, type) {
  const status = document.getElementById(id);
  if (!status) return;
  status.className = "form-status" + (message ? " is-visible is-" + type : "");
  status.textContent = message || "";
}

document.addEventListener("DOMContentLoaded", () => {
  const params = new URLSearchParams(window.location.search);
  const googleStatus = params.get("google");
  if (googleStatus === "failed") {
    localShow("Google login failed. Please try again.", "error");
  } else if (googleStatus === "blocked") {
    localShow("Your account has been blocked by admin", "error");
  }
  if (googleStatus) {
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  const loginForm = document.getElementById("loginForm");
  const loginSubmitBtn = document.getElementById("loginSubmitBtn");
  const loginEmail = document.getElementById("loginEmail");
  const loginPassword = document.getElementById("loginPassword");

  function setLoginSubmitting(isSubmitting) {
    if (!loginSubmitBtn) return;
    loginSubmitBtn.disabled = isSubmitting;
    loginSubmitBtn.setAttribute("aria-busy", isSubmitting ? "true" : "false");
    loginSubmitBtn.classList.toggle("is-loading", isSubmitting);
    loginSubmitBtn.textContent = isSubmitting ? "Signing In..." : "Login";
  }

  function validateLoginForm() {
    if (!loginForm) return true;

    const email = loginEmail ? loginEmail.value.trim() : "";
    const password = loginPassword ? loginPassword.value : "";
    let valid = true;

    if (!email) {
      setFieldState("loginEmailGroup", "loginEmailError", "Email is required.");
      valid = false;
    } else if (!isValidEmail(email)) {
      setFieldState("loginEmailGroup", "loginEmailError", "Enter a valid email address.");
      valid = false;
    } else {
      setFieldState("loginEmailGroup", "loginEmailError", "");
    }

    if (!password) {
      setFieldState("loginPasswordGroup", "loginPasswordError", "Password is required.");
      valid = false;
    } else {
      setFieldState("loginPasswordGroup", "loginPasswordError", "");
    }

    if (valid) {
      setStatusMessage("loginStatus", "", "error");
    }

    return valid;
  }

  if (loginEmail) {
    loginEmail.addEventListener("input", validateLoginForm);
  }

  if (loginPassword) {
    loginPassword.addEventListener("input", validateLoginForm);
    loginPassword.addEventListener("keyup", (event) => {
      const capsHint = document.getElementById("loginCapsHint");
      if (capsHint) {
        capsHint.textContent = event.getModifierState("CapsLock") ? "Caps Lock is on" : "";
      }
    });
  }

  if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const email = loginEmail.value.trim();
      const password = loginPassword.value;

      if (!validateLoginForm()) {
        setStatusMessage("loginStatus", "Please fix the highlighted fields.", "error");
        return;
      }

      try {
        setLoginSubmitting(true);
        setStatusMessage("loginStatus", "", "error");
        const res = await fetch(`${BASE_URL}/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ email, password })
        });

        if (res.status === 403) {
          setStatusMessage("loginStatus", "Your account has been blocked by admin.", "error");
          return;
        }

        if (!res.ok) {
          setStatusMessage("loginStatus", "Invalid email or password.", "error");
          return;
        }

        setStatusMessage("loginStatus", "Login successful. Redirecting...", "success");
        localShow("Login successful", "success");

        setTimeout(() => {
          window.location.href = "/";
        }, 1000);
      } catch (err) {
        setStatusMessage("loginStatus", "Server error. Please try again.", "error");
        localShow("Server error. Please try again.", "error");
      } finally {
        setLoginSubmitting(false);
      }
    });
  }
});

function openForgot() {
  document.getElementById("forgotModal").classList.add("show");
}

function closeForgot() {
  document.getElementById("forgotModal").classList.remove("show");
  setFieldState("", "forgotEmailError", "");
}

async function sendReset() {
  const email = document.getElementById("forgotEmail").value.trim();

  if (!email) {
    setFieldState("", "forgotEmailError", "Email is required.");
    localShow("Please enter email", "error");
    return;
  }

  if (!isValidEmail(email)) {
    setFieldState("", "forgotEmailError", "Enter a valid email address.");
    return;
  }

  setFieldState("", "forgotEmailError", "");

  try {
    const res = await fetch(`/api/auth/forgot-password?email=${email}`, {
      method: "POST"
    });

    if (!res.ok) {
      localShow("Email not registered", "error");
      return;
    }

    localShow("Reset link sent to your email", "success");
    closeForgot();
  } catch (err) {
    localShow("Server error", "error");
  }
}
