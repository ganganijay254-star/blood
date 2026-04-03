let bloodGroupChartInstance = null;
let monthlyDonationsChartInstance = null;

function canRenderCharts() {
  const wrapper = document.getElementById('adminWrapper');
  const bloodCanvas = document.getElementById('bloodGroupChart');
  const monthlyCanvas = document.getElementById('monthlyDonationsChart');
  return !!(wrapper && wrapper.classList.contains('logged-in') && bloodCanvas && monthlyCanvas);
}

function initAdminCharts() {
  if (canRenderCharts()) {
    renderBloodGroupChart();
    renderMonthlyDonationsChart();
  }

  const wrapper = document.getElementById('adminWrapper');
  if (wrapper) {
    const observer = new MutationObserver(() => {
      if (wrapper.classList.contains('logged-in')) {
        renderBloodGroupChart();
        renderMonthlyDonationsChart();
      }
    });
    observer.observe(wrapper, { attributes: true, attributeFilter: ['class'] });
  }

  document.addEventListener('click', (e) => {
    const link = e.target.closest('[data-page]');
    if (link && link.dataset.page === 'dashboard') {
      setTimeout(() => {
        if (canRenderCharts()) {
          renderBloodGroupChart();
          renderMonthlyDonationsChart();
        }
      }, 80);
    }
  });
}

document.addEventListener('DOMContentLoaded', initAdminCharts);

function showLoader(show) {
  const l = document.getElementById('globalLoader');
  if (!l) return;
  l.style.display = show ? 'flex' : 'none';
}

function showToast(message, type='success', timeout=3500) {
  const c = document.getElementById('toastContainer');
  if (!c) return;
  const t = document.createElement('div');
  t.className = 'toast ' + (type === 'error' ? 'error' : 'success');
  t.innerText = message;
  c.appendChild(t);
  setTimeout(() => { t.style.opacity = '0'; setTimeout(()=>t.remove(),300); }, timeout);
}

async function renderBloodGroupChart() {
  if (!canRenderCharts()) return;
  try {
    showLoader(true);
    const res = await fetch('/api/admin/donors', { credentials: 'include' });
    if (!res.ok) {
      showLoader(false);
      return;
    }
    const donors = await res.json();

    const counts = {};
    donors.forEach(d => {
      const g = (d.bloodGroup || 'Unknown').toUpperCase();
      counts[g] = (counts[g] || 0) + 1;
    });

    const labels = Object.keys(counts);
    const data = labels.map(l => counts[l]);

    const el = document.getElementById('bloodGroupChart');
    if (!el) { showLoader(false); return; }
    const ctx = el.getContext && el.getContext('2d');
    if (!ctx || labels.length === 0) {
      el.parentElement.innerHTML = '<div style="padding:40px;text-align:center;color:#666">No donor data available for chart</div>';
      showLoader(false);
      return;
    }
    if (bloodGroupChartInstance) {
      bloodGroupChartInstance.destroy();
    }

    bloodGroupChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          borderRadius: 6,
          borderWidth: 2,
          borderColor: '#ffffff',
          hoverOffset: 10,
          backgroundColor: ['#EF4444','#F59E0B','#10B981','#3B82F6','#8B5CF6','#EC4899','#06B6D4','#F97316']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '56%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 10,
              color: '#334155',
              font: { size: 12, weight: '600' }
            }
          },
          tooltip: {
            backgroundColor: '#0f172a',
            titleColor: '#e2e8f0',
            bodyColor: '#e2e8f0',
            padding: 10,
            callbacks: {
              label: (context) => `${context.label}: ${context.raw}`
            }
          }
        }
      }
    });

    showLoader(false);
  } catch (e) {
    showLoader(false);
    if (canRenderCharts()) showToast('Failed to load blood group chart', 'error');
    console.error(e);
  }
}

async function renderMonthlyDonationsChart() {
  if (!canRenderCharts()) return;
  try {
    showLoader(true);
    const res = await fetch('/api/admin/visits', { credentials: 'include' });
    if (!res.ok) {
      showLoader(false);
      return;
    }
    const visits = await res.json();

    // Build last 6 months labels
    const months = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push(d.toLocaleString('default', { month: 'short', year: 'numeric' }));
    }

    const counts = new Array(6).fill(0);
    visits.forEach(v => {
      // consider approved visits only
      if (!v.status || v.status.toUpperCase() !== 'APPROVED') return;
      const dateStr = v.requestDate || v.visitDate || null;
      if (!dateStr) return;
      const dt = new Date(dateStr);
      const label = dt.toLocaleString('default', { month: 'short', year: 'numeric' });
      const idx = months.indexOf(label);
      if (idx >= 0) counts[idx] += 1;
    });

    const el2 = document.getElementById('monthlyDonationsChart');
    if (!el2) { showLoader(false); return; }
    const ctx2 = el2.getContext && el2.getContext('2d');
    if (!ctx2) { showLoader(false); return; }
    // if all counts zero, show placeholder
    const hasData = counts.some(c => c > 0);
    if (!hasData) {
      el2.parentElement.innerHTML = '<div style="padding:40px;text-align:center;color:#666">No donation data for recent months</div>';
      showLoader(false);
      return;
    }
    if (monthlyDonationsChartInstance) {
      monthlyDonationsChartInstance.destroy();
    }

    const gradient = ctx2.createLinearGradient(0, 0, 0, 260);
    gradient.addColorStop(0, 'rgba(37,99,235,0.95)');
    gradient.addColorStop(1, 'rgba(59,130,246,0.55)');

    monthlyDonationsChartInstance = new Chart(ctx2, {
      type: 'bar',
      data: {
        labels: months,
        datasets: [{
          label: 'Approved Donations',
          data: counts,
          backgroundColor: gradient,
          borderColor: '#1d4ed8',
          borderWidth: 1,
          borderRadius: 10,
          maxBarThickness: 54
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            titleColor: '#e2e8f0',
            bodyColor: '#e2e8f0',
            padding: 10
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(148,163,184,0.14)', drawBorder: false },
            ticks: { color: '#475569', font: { weight: '600' } }
          },
          y: {
            beginAtZero: true,
            grace: '8%',
            grid: { color: 'rgba(148,163,184,0.18)', drawBorder: false },
            ticks: {
              color: '#475569',
              precision: 0,
              stepSize: 1,
              font: { weight: '600' }
            }
          }
        }
      }
    });

    showLoader(false);
  } catch (e) {
    showLoader(false);
    if (canRenderCharts()) showToast('Failed to load monthly donations chart', 'error');
    console.error(e);
  }
}
