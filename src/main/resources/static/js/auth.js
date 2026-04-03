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

document.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");

  if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const email = document.getElementById("loginEmail").value.trim();
      const password = document.getElementById("loginPassword").value;

      if (!email || !password) {
        return localShow("All fields are required", "error");
      }

      try {
        const res = await fetch(`${BASE_URL}/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({ email, password })
        });

        if (res.status === 403) {
          return localShow("Your account is blocked by admin", "error");
        }

        if (!res.ok) {
          return localShow("Invalid email or password", "error");
        }

        localShow("Login successful", "success");

        setTimeout(() => {
          window.location.href = "/";
        }, 1000);
      } catch (err) {
        localShow("Server error. Please try again.", "error");
      }
    });
  }
});

function openForgot() {
  document.getElementById("forgotModal").classList.add("show");
}

function closeForgot() {
  document.getElementById("forgotModal").classList.remove("show");
}

async function sendReset() {
  const email = document.getElementById("forgotEmail").value.trim();

  if (!email) {
    localShow("Please enter email", "error");
    return;
  }

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
