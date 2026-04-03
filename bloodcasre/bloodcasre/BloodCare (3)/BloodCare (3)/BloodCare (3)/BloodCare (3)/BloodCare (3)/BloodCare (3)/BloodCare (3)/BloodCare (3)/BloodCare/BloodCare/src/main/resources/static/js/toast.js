(function(){
  // Create a singleton toast element and expose showToast globally
  function ensureToast() {
    let t = document.getElementById('global-toast');
    if (t) return t;

    t = document.createElement('div');
    t.id = 'global-toast';
    t.className = 'toast';

    const close = document.createElement('button');
    close.className = 'close';
    close.innerHTML = '✕';
    close.onclick = () => hideToast();
    t.appendChild(close);

    const msg = document.createElement('div');
    msg.className = 'message';
    t.appendChild(msg);

    document.body.appendChild(t);
    return t;
  }

  function showToast(message, type='info', ttl=3000) {
    const t = ensureToast();
    t.className = `toast ${type} show`;
    const msg = t.querySelector('.message');
    msg.innerText = message;

    clearTimeout(t._timeout);
    t._timeout = setTimeout(()=> hideToast(), ttl);
    return t;
  }

  function hideToast() {
    const t = document.getElementById('global-toast');
    if (!t) return;
    t.classList.remove('show');
    clearTimeout(t._timeout);
  }

  // Backwards-compatible aliases used across templates
  window.showToast = showToast;
  window.showSimpleToast = function(text, type){ showToast(text, type || 'info'); };
  window.localShow = function(text, type){ showToast(text, type || 'info'); };

  function ensureConfirmModal() {
    let modal = document.getElementById('global-confirm');
    if (modal) return modal;

    modal = document.createElement('div');
    modal.id = 'global-confirm';
    modal.className = 'confirm-modal-overlay';
    modal.innerHTML = `
      <div class="confirm-modal-box" role="dialog" aria-modal="true" aria-labelledby="global-confirm-title">
        <h3 id="global-confirm-title" class="confirm-modal-title">Please Confirm</h3>
        <p class="confirm-modal-message" id="global-confirm-message"></p>
        <div class="confirm-modal-actions">
          <button type="button" id="global-confirm-cancel" class="confirm-modal-btn cancel">Cancel</button>
          <button type="button" id="global-confirm-ok" class="confirm-modal-btn ok">OK</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
    return modal;
  }

  function showConfirm(message, onYes, onNo) {
    const modal = ensureConfirmModal();
    const msg = modal.querySelector('#global-confirm-message');
    const okBtn = modal.querySelector('#global-confirm-ok');
    const cancelBtn = modal.querySelector('#global-confirm-cancel');

    msg.innerText = message || 'Are you sure?';
    modal.classList.add('show');

    const close = () => modal.classList.remove('show');

    okBtn.onclick = () => {
      close();
      if (typeof onYes === 'function') onYes();
    };
    cancelBtn.onclick = () => {
      close();
      if (typeof onNo === 'function') onNo();
    };
  }

  if (typeof window.showConfirm === 'undefined') {
    window.showConfirm = showConfirm;
  }

  // Provide programmatic control if needed
  window.__toast = { show: showToast, hide: hideToast };
})();
