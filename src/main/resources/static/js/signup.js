const BASE_URL = "/api/auth";

const form = document.getElementById("signupForm");
const signupSubmitBtn = document.getElementById("signupSubmitBtn");
const signupStatus = document.getElementById("signupStatus");

function localShow(msg, success) {
  if (window.showToast) return window.showToast(msg, success ? "success" : "error");
  const c = document.getElementById("toastContainer") || (() => {
    const d = document.createElement("div");
    d.id = "toastContainer";
    document.body.appendChild(d);
    return d;
  })();
  const t = document.createElement("div");
  t.className = "toast " + (success ? "" : "error");
  t.innerText = msg;
  c.appendChild(t);
  setTimeout(() => {
    t.style.opacity = "0";
    setTimeout(() => t.remove(), 300);
  }, 3000);
}

function togglePassword(id) {
  const input = document.getElementById(id);
  if (!input) return;
  input.type = input.type === "password" ? "text" : "password";
  const span = input.parentElement.querySelector(".show-hide");
  if (span) span.textContent = input.type === "password" ? "Show" : "Hide";
}

function setSignupSubmitting(isSubmitting) {
  if (!signupSubmitBtn) return;
  signupSubmitBtn.disabled = isSubmitting;
  signupSubmitBtn.setAttribute("aria-busy", isSubmitting ? "true" : "false");
  signupSubmitBtn.classList.toggle("is-loading", isSubmitting);
  signupSubmitBtn.textContent = isSubmitting ? "Creating Account..." : "Create Account";
}

function setStatus(message, type) {
  if (!signupStatus) return;
  signupStatus.className = "form-status" + (message ? " is-visible is-" + type : "");
  signupStatus.textContent = message || "";
}

function setFieldState(groupId, errorId, message) {
  const group = document.getElementById(groupId);
  const error = document.getElementById(errorId);
  if (group) group.classList.toggle("has-error", Boolean(message));
  if (error) error.textContent = message || "";
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function passwordScore(val) {
  let score = 0;
  if (!val) return 0;
  if (val.length >= 8) score++;
  if (/[A-Z]/.test(val)) score++;
  if (/[a-z]/.test(val)) score++;
  if (/[0-9]/.test(val)) score++;
  if (/[^A-Za-z0-9]/.test(val)) score++;
  return score;
}

const passwordInput = document.getElementById("password");
const confirmPasswordInput = document.getElementById("confirmPassword");
const strengthDiv = document.getElementById("passwordStrength");

function updateStrength() {
  const val = passwordInput ? passwordInput.value : "";
  const score = passwordScore(val);
  const cls = score <= 2 ? "weak" : score === 3 ? "medium" : "strong";
  if (strengthDiv) {
    if (!val) {
      strengthDiv.style.display = "none";
      strengthDiv.className = "strength";
    } else {
      strengthDiv.style.display = "block";
      strengthDiv.className = "strength " + cls;
    }
  }
}

function validateSignupForm() {
  if (!form) return true;

  const name = document.getElementById("name").value.trim();
  const email = document.getElementById("email").value.trim();
  const mobile = document.getElementById("mobile").value.trim();
  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;
  let valid = true;

  if (!name || name.length < 3) {
    setFieldState("nameGroup", "nameError", "Enter your full name.");
    valid = false;
  } else {
    setFieldState("nameGroup", "nameError", "");
  }

  if (!email) {
    setFieldState("emailGroup", "emailError", "Email is required.");
    valid = false;
  } else if (!isValidEmail(email)) {
    setFieldState("emailGroup", "emailError", "Enter a valid email address.");
    valid = false;
  } else {
    setFieldState("emailGroup", "emailError", "");
  }

  if (!/^[0-9]{10}$/.test(mobile)) {
    setFieldState("mobileGroup", "mobileError", "Mobile number must be exactly 10 digits.");
    valid = false;
  } else {
    setFieldState("mobileGroup", "mobileError", "");
  }

  if (!password) {
    setFieldState("passwordGroup", "passwordError", "Password is required.");
    valid = false;
  } else if (passwordScore(password) < 3) {
    setFieldState("passwordGroup", "passwordError", "Use 8+ characters with letters and numbers.");
    valid = false;
  } else {
    setFieldState("passwordGroup", "passwordError", "");
  }

  if (!confirmPassword) {
    setFieldState("confirmPasswordGroup", "confirmPasswordError", "Please confirm your password.");
    valid = false;
  } else if (password !== confirmPassword) {
    setFieldState("confirmPasswordGroup", "confirmPasswordError", "Passwords do not match.");
    valid = false;
  } else {
    setFieldState("confirmPasswordGroup", "confirmPasswordError", "");
  }

  if (valid) {
    setStatus("", "error");
  }

  return valid;
}

if (passwordInput) {
  passwordInput.addEventListener("input", () => {
    updateStrength();
    validateSignupForm();
  });
  passwordInput.addEventListener("keyup", (event) => {
    const capsHint = document.getElementById("passwordCapsHint");
    if (capsHint) {
      capsHint.textContent = event.getModifierState("CapsLock") ? "Caps Lock is on" : "";
    }
  });
}

if (confirmPasswordInput) {
  confirmPasswordInput.addEventListener("input", validateSignupForm);
}

["name", "email", "mobile"].forEach((id) => {
  const field = document.getElementById(id);
  if (field) field.addEventListener("input", validateSignupForm);
});

if (form) {
  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const mobile = document.getElementById("mobile").value.trim();
    const password = passwordInput.value;

    if (!validateSignupForm()) {
      setStatus("Please fix the highlighted fields.", "error");
      return;
    }

    try {
      setSignupSubmitting(true);
      setStatus("", "error");
      const response = await fetch(`${BASE_URL}/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          name,
          email,
          mobile,
          password
        })
      });

      if (response.ok) {
        setStatus("Account created successfully. Redirecting to login...", "success");
        localShow("Account created successfully!", true);

        setTimeout(() => {
          window.location.href = "/login";
        }, 1200);
      } else {
        const errorText = await response.text();
        setStatus(errorText || "Signup failed. Please try again.", "error");
        localShow(errorText || "Signup failed");
      }
    } catch (error) {
      setStatus("Server error. Please try again.", "error");
      localShow("Server error. Please try again.");
    } finally {
      setSignupSubmitting(false);
    }
  });
}
