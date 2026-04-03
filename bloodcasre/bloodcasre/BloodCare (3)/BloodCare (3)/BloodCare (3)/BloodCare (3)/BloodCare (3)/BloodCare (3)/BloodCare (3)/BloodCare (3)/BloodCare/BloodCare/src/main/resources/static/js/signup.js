const BASE_URL = "/api/auth";

const form = document.getElementById("signupForm");

function localShow(msg, success) {
  if (window.showToast) return window.showToast(msg, success ? 'success' : 'error');
  // fallback simple toast
  const c = document.getElementById('toastContainer') || (() => { const d=document.createElement('div'); d.id='toastContainer'; document.body.appendChild(d); return d; })();
  const t = document.createElement('div'); t.className='toast ' + (success ? '' : 'error'); t.innerText = msg; c.appendChild(t); setTimeout(()=>{ t.style.opacity='0'; setTimeout(()=>t.remove(),300); }, 3000);
}

function togglePassword(id) {
  const input = document.getElementById(id);
  if (!input) return;
  input.type = input.type === 'password' ? 'text' : 'password';
  // switch icon if present
  const span = input.parentElement.querySelector('.show-hide');
  if (span) span.textContent = input.type === 'password' ? '👁️' : '🙈';
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

const passwordInput = document.getElementById('password');
const strengthDiv = document.getElementById('passwordStrength');
if (passwordInput) {
  passwordInput.addEventListener('input', () => {
    const val = passwordInput.value;
    const score = passwordScore(val);
    let msg = '';
    // map score to strength class
    const cls = score <= 2 ? 'weak' : score === 3 ? 'medium' : 'strong';
    if (strengthDiv) {
      if (!val) { strengthDiv.style.display = 'none'; strengthDiv.className = 'strength'; }
      else { strengthDiv.style.display = 'block'; strengthDiv.className = 'strength ' + cls; }
    }
  });
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const name = document.getElementById("name").value.trim();
  const email = document.getElementById("email").value.trim();
  const mobile = document.getElementById("mobile").value.trim();
  const password = document.getElementById("password").value;
  const confirmPassword = document.getElementById("confirmPassword").value;

  // Basic Validation
  if (!name || !email || !mobile || !password || !confirmPassword) {
    return localShow("All fields are required", false);
  }

  if (!/^[0-9]{10}$/.test(mobile)) {
    return localShow("Mobile number must be exactly 10 digits", false);
  }

  if (password !== confirmPassword) {
    return localShow("Passwords do not match", false);
  }

  // enforce reasonable strength
  const score = passwordScore(password);
  if (score < 3) {
    return localShow("Password too weak — use min 8 chars, include numbers and letters", false);
  }

  try {
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
      localShow("Account created successfully!", true);

      setTimeout(() => {
        window.location.href = "/login";
      }, 1200);

    } else {
      const errorText = await response.text();
      localShow(errorText || "Signup failed");
    }

  } catch (error) {
    localShow("Server error. Please try again.");
  }
});
