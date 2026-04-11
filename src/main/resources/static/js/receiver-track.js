const trackContainer = document.getElementById('trackContainer');
const proofContainer = document.getElementById('proofContainer');
const stockContainer = document.getElementById('stockContainer');
let trackingPollId = null;
let successRedirectScheduled = false;

function showToast(text, type = 'success') {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.innerText = text;
  toast.className = `toast show ${type}`;
  setTimeout(() => toast.classList.remove('show'), 4000);
}

function getQueryParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

function getStatusMessage(data) {
  if (data.status === 'DONOR_ASSIGNED') {
    return 'A donor has been assigned to your request.';
  }
  if (data.status === 'OPEN' || data.status === 'IN_PROGRESS') {
    return 'System is waiting for donor response. If no donor is available, blood bank fallback will be checked after 15 seconds.';
  }
  if (data.status === 'BLOOD_BANK_AVAILABLE') {
    return 'Donor unavailable, but blood stock is available from hospitals/blood banks.';
  }
  if (data.status === 'FAILED') {
    return 'No donor or blood stock is available right now.';
  }
  return 'Tracking your request in real time.';
}

async function loadTracking() {
  const id = getQueryParam('requestId');
  if (!id) {
    trackContainer.innerHTML = '<p class="track-error">Missing request id</p>';
    return;
  }

  try {
    const res = await fetch(`/api/blood-request/track/${id}`, { credentials: 'include' });
    if (!res.ok) throw new Error('Unable to load');
    const data = await res.json();

    if ((data.status === 'DONOR_ASSIGNED' || data.status === 'COMPLETED' || data.status === 'BLOOD_BANK_AVAILABLE') && !successRedirectScheduled) {
      successRedirectScheduled = true;
      showToast('Request processed successfully. Redirecting to home page in 5 seconds...', 'success');
      window.setTimeout(() => {
        window.location.href = '/';
      }, 5000);
      if (trackingPollId) {
        window.clearInterval(trackingPollId);
        trackingPollId = null;
      }
    }

    trackContainer.innerHTML = `
      <div class="track-grid ${data.urgency === 'CRITICAL' || data.urgency === 'HIGH' ? 'priority-track-card' : ''}">
        <p><strong>Request:</strong> ${data.publicId || '-'}</p>
        <p><strong>Status:</strong> ${data.status || '-'}</p>
        <p><strong>Urgency:</strong> ${data.urgency || '-'}</p>
        <p><strong>Blood Group:</strong> ${data.bloodGroup || '-'}</p>
        <p><strong>Matched Donors:</strong> ${data.matchedDonors ?? 0}</p>
        <p><strong>Donors Responded:</strong> ${data.donorsResponded ?? 0}</p>
        <p><strong>Update:</strong> ${getStatusMessage(data)}</p>
      </div>
      <div class="auth-neo" style="margin-top:16px;">
        <div class="card-head">
          <span class="badge">Assigned Donor</span>
          <h3>Live Donor Assignment</h3>
          <p>Receiver can see the currently assigned donor here</p>
        </div>
        ${data.assignedDonor ? `
          <div class="responses-list">
            <div class="response-item">
              <strong>Name: ${data.assignedDonor.name || 'Donor'}</strong>
              <span class="response-status">${data.assignedDonor.bloodGroup || '-'}</span>
              <span class="response-date">Phone: ${data.assignedDonor.phone || 'Not available'}</span>
            </div>
            <div class="response-item">
              <strong>Location</strong>
              <span class="response-status">${data.assignedDonor.location || 'Not available'}</span>
              <span class="response-date">Status: ${data.status || '-'}</span>
            </div>
          </div>
        ` : '<p class="track-muted">No donor assigned yet.</p>'}
      </div>
      <h4 class="responses-title">Responses</h4>
      <div id="responsesList" class="responses-list"></div>
    `;

    const list = document.getElementById('responsesList');
    if (Array.isArray(data.responses) && data.responses.length > 0) {
      data.responses.forEach((r) => {
        const el = document.createElement('div');
        el.className = 'response-item';
        el.innerHTML = `
          <strong>${r.name || 'Donor'}</strong>
          <span class="response-status">${r.status || 'Pending'}</span>
          <span class="response-date">${r.responseDate || ''}</span>
        `;
        list.appendChild(el);
      });
    } else {
      list.innerHTML = '<p class="track-muted">No donor responses yet.</p>';
    }

    if (data.medicalProofPath) {
      proofContainer.innerHTML = `
        <p><strong>Medical Proof:</strong>
          <a href="${data.medicalProofPath}" target="_blank" rel="noopener noreferrer">View uploaded proof</a>
        </p>
      `;
    } else {
      proofContainer.innerHTML = '';
    }

    renderStockSnapshot(data);
  } catch (e) {
    showToast('Failed to load tracking', 'error');
    trackContainer.innerHTML = '<p class="track-error">Error loading tracking info</p>';
    if (stockContainer) stockContainer.innerHTML = '';
  }
}

function renderStockSnapshot(data) {
  if (!stockContainer) return;
  const compatibleGroups = Array.isArray(data.compatibleBloodGroups) ? data.compatibleBloodGroups : [];
  const snapshot = data.stockSnapshot || {};
  const stockRows = Array.isArray(snapshot.stock) ? snapshot.stock : [];
  const multiSource = data.multiSourceAvailability || {};
  const bloodBanks = Array.isArray(multiSource.bloodBanks) ? multiSource.bloodBanks : [];

  stockContainer.innerHTML = `
    <div class="auth-neo" style="margin-top:18px;">
      <div class="card-head">
        <span class="badge">Live Stock</span>
        <h3>Compatible Blood Availability</h3>
        <p>Auto-refresh every 15 seconds</p>
      </div>
      <p><strong>Receiver Compatible:</strong> ${compatibleGroups.join(', ') || '-'}</p>
      <p><strong>Hospital:</strong> ${snapshot.hospitalName || data.hospital || '-'}</p>
      <p><strong>Total Compatible Units:</strong> ${snapshot.availableUnits ?? 0}</p>
      <div class="responses-list">
        ${stockRows.length ? stockRows.map((row) => `
          <div class="response-item">
            <strong>${row.bloodGroup}</strong>
            <span class="response-status">${row.unitsAvailable} units</span>
            <span class="response-date">Reorder at ${row.reorderLevel ?? 0}</span>
          </div>
        `).join('') : '<p class="track-muted">Hospital stock snapshot unavailable.</p>'}
      </div>
      <div class="card-head" style="margin-top:18px;">
        <span class="badge">Multi Source</span>
        <h3>Blood Banks & Nearby Sources</h3>
        <p>System checks hospitals and blood banks together</p>
      </div>
      <p><strong>Total Alternate Units:</strong> ${multiSource.totalAvailableUnits ?? 0}</p>
      <div class="responses-list">
        ${bloodBanks.length ? bloodBanks.map((bank) => `
          <div class="response-item">
            <strong>${bank.name || 'Blood Bank'}</strong>
            <span class="response-status">${bank.bloodGroup || '-'} • ${bank.quantity ?? 0} units</span>
            <span class="response-date">${bank.location || '-'}</span>
          </div>
        `).join('') : '<p class="track-muted">No blood bank source found yet.</p>'}
      </div>
    </div>
  `;
}

window.addEventListener('DOMContentLoaded', () => {
  loadTracking();
  trackingPollId = window.setInterval(loadTracking, 15000);
});

window.addEventListener('beforeunload', () => {
  if (trackingPollId) {
    window.clearInterval(trackingPollId);
  }
});
