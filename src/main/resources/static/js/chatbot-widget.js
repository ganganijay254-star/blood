(function () {
  const QUICK_ACTIONS = [
    { label: "Eligibility", type: "prompt", value: "Am I eligible to donate blood right now?" },
    { label: "Dashboard", type: "nav", value: "/donor-dashboard", botText: "Opening Dashboard..." },
    { label: "Request Blood", type: "nav", value: "/receiver-form", botText: "Opening Blood Request..." },
    { label: "Track Request", type: "nav", value: "/receiver-track", botText: "Opening Receiver Track..." },
    { label: "Leaderboard", type: "nav", value: "/leaderboard", botText: "Opening Leaderboard..." }
  ];

  const html = `
    <div id="chatbot-widget-container" class="chatbot-widget">
      <button id="chatbot-toggle-btn" class="chatbot-toggle-btn" aria-label="Open BloodCare Assistant">
        <i class="fas fa-comments"></i>
      </button>

      <div id="chatbot-window" class="chatbot-window">
        <div class="chatbot-header">
          <div>
            <h5>BloodCare Assistant</h5>
            <p>Fast help for donation flow, requests, and eligibility.</p>
          </div>
          <button id="chatbot-close-btn" class="chatbot-close-btn" aria-label="Close assistant">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div id="chatbot-messages" class="chatbot-messages"></div>

        <div id="chatbot-quick-actions" class="chatbot-quick-actions"></div>

        <div class="chatbot-input-row">
          <input id="chatbot-input" type="text" placeholder="Ask about donations..." />
          <button id="chatbot-voice-btn" title="Voice input" class="chatbot-icon-btn chatbot-icon-btn--soft">Mic</button>
          <button id="chatbot-send-btn" class="chatbot-icon-btn chatbot-icon-btn--solid">
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </div>
  `;

  async function shouldShowWidget() {
    try {
      const res = await fetch("/api/admin/public-config");
      if (!res.ok) return true;
      const config = await res.json();
      return !(config && config.controls && config.controls.showChatbot === false);
    } catch (e) {
      return true;
    }
  }

  document.addEventListener("DOMContentLoaded", async function () {
    if (!(await shouldShowWidget())) return;

    if (!window.fontAwesomeLoaded) {
      const link = document.createElement("link");
      link.rel = "stylesheet";
      link.href = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css";
      document.head.appendChild(link);
      window.fontAwesomeLoaded = true;
    }

    const wrap = document.createElement("div");
    wrap.innerHTML = html;
    document.body.appendChild(wrap);
    injectStyles();
    setup();
  });

  function setup() {
    const toggle = document.getElementById("chatbot-toggle-btn");
    const close = document.getElementById("chatbot-close-btn");
    const win = document.getElementById("chatbot-window");
    const msgs = document.getElementById("chatbot-messages");
    const input = document.getElementById("chatbot-input");
    const voice = document.getElementById("chatbot-voice-btn");
    const send = document.getElementById("chatbot-send-btn");
    const quickActions = document.getElementById("chatbot-quick-actions");
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = SpeechRecognition ? new SpeechRecognition() : null;

    let isListening = false;
    let voiceTranscript = "";
    let voiceSent = false;

    const NAV_PAGES = [
      { path: "/donor-dashboard", label: "Dashboard", keys: ["dashboard", "donor dashboard"] },
      { path: "/donor-profile", label: "Donor Profile", keys: ["profile", "donor profile", "my profile"] },
      { path: "/donor-form", label: "Donor Form", keys: ["donor form", "register donor"] },
      { path: "/receiver-form", label: "Blood Request", keys: ["blood request", "request blood"] },
      { path: "/receiver-track", label: "Receiver Track", keys: ["receiver track", "track request"] },
      { path: "/request-visit", label: "Request Visit", keys: ["request visit", "book visit"] },
      { path: "/visit-history", label: "Visit History", keys: ["visit history", "history"] },
      { path: "/leaderboard", label: "Leaderboard", keys: ["leaderboard", "rank"] },
      { path: "/medicine", label: "Medicine", keys: ["medicine"] },
      { path: "/about", label: "About", keys: ["about"] },
      { path: "/login", label: "Login", keys: ["login", "sign in"] },
      { path: "/signup", label: "Signup", keys: ["signup", "register", "sign up"] },
      { path: "/admin", label: "Admin", keys: ["admin"] },
      { path: "/", label: "Home", keys: ["home", "main page"] }
    ];

    msgs.innerHTML = `
      <div class="chatbot-row chatbot-row--bot">
        <div class="chatbot-bubble chatbot-bubble--bot">
          Hello, I can help with donation eligibility, blood requests, certificates, and navigation.
        </div>
      </div>
    `;

    if (quickActions) {
      quickActions.innerHTML = QUICK_ACTIONS
        .map(
          (action, index) => `
            <button class="chatbot-chip" data-action-index="${index}">
              ${action.label}
            </button>
          `
        )
        .join("");

      quickActions.addEventListener("click", function (event) {
        const button = event.target.closest("[data-action-index]");
        if (!button) return;
        const action = QUICK_ACTIONS[Number(button.getAttribute("data-action-index"))];
        handleQuickAction(action);
      });
    }

    toggle.addEventListener("click", function () {
      const open = win.classList.contains("is-open");
      win.classList.toggle("is-open", !open);
      if (!open) input.focus();
    });

    close.addEventListener("click", function () {
      win.classList.remove("is-open");
    });

    send.addEventListener("click", sendMessage);
    voice.addEventListener("click", toggleVoiceInput);
    input.addEventListener("keypress", function (e) {
      if (e.key === "Enter") sendMessage();
    });

    if (recognition) {
      recognition.lang = "hi-IN";
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onresult = function (event) {
        let transcript = "";
        for (let i = event.resultIndex; i < event.results.length; i += 1) {
          transcript += event.results[i][0].transcript;
        }
        voiceTranscript = transcript.trim();
        input.value = voiceTranscript;

        const finalResult = event.results[event.results.length - 1];
        if (finalResult && finalResult.isFinal && voiceTranscript) {
          isListening = false;
          updateVoiceButton(false);
          voiceSent = true;
          sendMessage();
        }
      };

      recognition.onerror = function () {
        isListening = false;
        updateVoiceButton(false);
        add("Voice input not available. Please type your message.", "bot");
      };

      recognition.onend = function () {
        if (isListening) {
          isListening = false;
          updateVoiceButton(false);
        }
        if (!voiceSent && voiceTranscript) {
          sendMessage();
        } else if (!voiceSent && !voiceTranscript) {
          add("Voice not detected. Please speak again or type your message.", "bot");
        }
        voiceSent = false;
        voiceTranscript = "";
      };
    } else {
      voice.disabled = true;
      voice.title = "Voice input not supported in this browser";
      voice.style.opacity = "0.6";
      voice.style.cursor = "not-allowed";
    }

    function handleQuickAction(action) {
      if (!action) return;

      if (action.type === "nav") {
        add(action.botText || `Opening ${action.label}...`, "bot");
        setTimeout(function () {
          window.location.href = action.value;
        }, 220);
        return;
      }

      input.value = action.value;
      sendMessage();
    }

    async function sendMessage() {
      const message = input.value.trim();
      if (!message) return;

      add(message, "user");
      input.value = "";

      if (handleLocalCommand(message)) return;

      send.disabled = true;

      try {
        const res = await fetch("/api/chatbot/message", {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ message: message })
        });

        if (!res.ok) {
          let errorMessage = "unable to fetch response.";
          try {
            const err = await res.json();
            if (err && err.error) errorMessage = err.error;
          } catch (e) {
          }
          add(`Error: ${errorMessage}`, "bot");
        } else {
          const data = await res.json();
          add(data.response || "No response", "bot");
        }
      } catch (e) {
        add("Network error. Please try again.", "bot");
      } finally {
        send.disabled = false;
      }
    }

    function handleLocalCommand(rawMessage) {
      const query = normalize(rawMessage);
      if (!query) return false;

      if (containsAny(query, "bye", "goodbye", "see you", "tata", "alvida")) {
        add("Goodbye. Take care.", "bot");
        return true;
      }

      const target = resolveNavigationCommand(query);
      if (target) {
        add(`Opening ${target.label}...`, "bot");
        setTimeout(function () {
          window.location.href = target.path;
        }, 250);
        return true;
      }

      return false;
    }

    function resolveNavigationCommand(query) {
      const wantsOpen = containsAny(query, "open", "go to", "goto", "take me to", "show", "khol", "kholo", "chalo");
      for (let i = 0; i < NAV_PAGES.length; i += 1) {
        const page = NAV_PAGES[i];
        for (let j = 0; j < page.keys.length; j += 1) {
          const key = page.keys[j];
          if (query.includes(key) && (wantsOpen || query === key)) {
            return page;
          }
        }
      }
      return null;
    }

    function normalize(text) {
      return (text || "")
        .toLowerCase()
        .replace(/[^a-z0-9\s]/g, " ")
        .replace(/\s+/g, " ")
        .trim();
    }

    function containsAny(source) {
      for (let i = 1; i < arguments.length; i += 1) {
        if (source.includes(arguments[i])) return true;
      }
      return false;
    }

    function toggleVoiceInput() {
      if (!recognition) return;

      if (isListening) {
        recognition.stop();
        isListening = false;
        updateVoiceButton(false);
        return;
      }

      try {
        recognition.lang = /[^\x00-\x7F]/.test(input.value) ? "hi-IN" : "en-IN";
        voiceTranscript = "";
        voiceSent = false;
        recognition.start();
        isListening = true;
        updateVoiceButton(true);
      } catch (e) {
        isListening = false;
        updateVoiceButton(false);
      }
    }

    function updateVoiceButton(listening) {
      voice.style.background = listening ? "#991b1b" : "#fee2e2";
      voice.style.color = listening ? "#ffffff" : "#991b1b";
    }

    function add(text, sender) {
      const row = document.createElement("div");
      row.className = `chatbot-row chatbot-row--${sender}`;

      const bubble = document.createElement("div");
      bubble.className = `chatbot-bubble chatbot-bubble--${sender}`;
      bubble.innerHTML = (text || "")
        .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
        .replace(/\n/g, "<br>");

      row.appendChild(bubble);
      msgs.appendChild(row);
      msgs.scrollTop = msgs.scrollHeight;
    }
  }

  function injectStyles() {
    const style = document.createElement("style");
    style.textContent = `
      .chatbot-widget {
        position: fixed;
        right: 20px;
        bottom: 20px;
        z-index: 9999;
        font-family: "Poppins", "Segoe UI", sans-serif;
      }

      .chatbot-toggle-btn {
        width: 62px;
        height: 62px;
        border: none;
        border-radius: 50%;
        background: linear-gradient(135deg, #ef4444, #7f1d1d);
        color: #fff;
        cursor: pointer;
        box-shadow: 0 12px 30px rgba(127, 29, 29, .45);
        transition: transform .25s ease, box-shadow .25s ease;
      }

      .chatbot-toggle-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 16px 34px rgba(127, 29, 29, .5);
      }

      .chatbot-window {
        position: absolute;
        right: 0;
        bottom: 82px;
        width: 390px;
        height: 540px;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        border-radius: 20px;
        border: 1px solid #fecaca;
        background: #fff;
        box-shadow: 0 25px 60px rgba(2, 6, 23, .35);
        transform-origin: bottom right;
        transform: translateY(10px) scale(.96);
        opacity: 0;
        pointer-events: none;
        transition: transform .22s ease, opacity .22s ease;
      }

      .chatbot-window.is-open {
        transform: translateY(0) scale(1);
        opacity: 1;
        pointer-events: auto;
      }

      .chatbot-header {
        padding: 14px 16px;
        background: linear-gradient(135deg, #991b1b, #7f1d1d);
        color: #fff;
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 10px;
      }

      .chatbot-header h5 {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
      }

      .chatbot-header p {
        margin: 4px 0 0 0;
        font-size: 12px;
        color: rgba(255,255,255,.82);
      }

      .chatbot-close-btn {
        border: none;
        background: transparent;
        color: #fff;
        font-size: 18px;
        cursor: pointer;
      }

      .chatbot-messages {
        flex: 1;
        overflow-y: auto;
        padding: 15px;
        background: linear-gradient(180deg, #fff, #fef2f2);
      }

      .chatbot-row {
        display: flex;
        margin-bottom: 10px;
      }

      .chatbot-row--user {
        justify-content: flex-end;
      }

      .chatbot-row--bot {
        justify-content: flex-start;
      }

      .chatbot-bubble {
        max-width: 78%;
        padding: 10px 12px;
        border-radius: 12px;
        font-size: 13px;
        line-height: 1.45;
        word-wrap: break-word;
        animation: chatbotFadeIn .2s ease;
      }

      .chatbot-bubble--user {
        background: linear-gradient(135deg, #ef4444, #991b1b);
        color: #fff;
        border-radius: 12px 12px 4px 12px;
      }

      .chatbot-bubble--bot {
        background: #f3f4f6;
        color: #1f2937;
        border: 1px solid #e5e7eb;
        border-radius: 12px 12px 12px 4px;
      }

      .chatbot-quick-actions {
        display: flex;
        gap: 8px;
        overflow-x: auto;
        padding: 10px 12px;
        background: #fff7f7;
        border-top: 1px solid #fecaca;
        border-bottom: 1px solid #fee2e2;
      }

      .chatbot-chip {
        border: 1px solid #fecaca;
        background: #fff;
        color: #991b1b;
        border-radius: 999px;
        padding: 8px 12px;
        font-size: 12px;
        font-weight: 700;
        cursor: pointer;
        white-space: nowrap;
      }

      .chatbot-chip:hover {
        background: #fff1f2;
      }

      .chatbot-input-row {
        padding: 12px;
        border-top: 1px solid #fecaca;
        display: flex;
        gap: 8px;
        background: #fff;
      }

      .chatbot-input-row input {
        flex: 1;
        padding: 10px 13px;
        border: 1px solid #fca5a5;
        border-radius: 999px;
        outline: none;
        font-size: 13px;
        color: #1f2937;
      }

      .chatbot-icon-btn {
        width: 38px;
        height: 38px;
        border: none;
        border-radius: 50%;
        cursor: pointer;
      }

      .chatbot-icon-btn--soft {
        background: #fee2e2;
        color: #991b1b;
        font-size: 11px;
        font-weight: 700;
      }

      .chatbot-icon-btn--solid {
        background: linear-gradient(135deg, #ef4444, #991b1b);
        color: #fff;
      }

      @keyframes chatbotFadeIn {
        from { opacity: 0; transform: translateY(6px); }
        to { opacity: 1; transform: translateY(0); }
      }

      @media (max-width: 520px) {
        .chatbot-window {
          width: calc(100vw - 22px) !important;
          right: -5px !important;
          height: 72vh !important;
        }
      }
    `;
    document.head.appendChild(style);
  }
})();
