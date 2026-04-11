const BASE_URL = "/api";

document.addEventListener("DOMContentLoaded", () => {
  const locationBox = document.getElementById("locationBox");
  const hospitalList = document.getElementById("hospitalList");
  const loading = document.getElementById("loading");
  const noDataMsg = document.getElementById("noDataMsg");

  fetch(`${BASE_URL}/auth/check-session`, { credentials: "include" }).then((res) => {
    if (res.status === 401) window.location.href = "/login";
  });

  fetch("/api/admin/public-config")
    .then((res) => (res.ok ? res.json() : null))
    .then((cfg) => {
      const controls = cfg && cfg.controls ? cfg.controls : {};
      if (controls.maintenanceMode === true || controls.enableDonations === false) {
        loading.style.display = "none";
        locationBox.innerText = "Donation visits are currently disabled by admin.";
        return;
      }
      startLocationFlow();
    })
    .catch(() => startLocationFlow());

  function startLocationFlow() {
    if (!navigator.geolocation) {
      locationBox.innerText = "Geolocation not supported.";
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        locationBox.innerText = `Your Location: ${lat.toFixed(4)}, ${lng.toFixed(4)}`;
        fetchNearbyHospitals(lat, lng);
      },
      () => {
        locationBox.innerText = "Location access denied.";
        loading.style.display = "none";
      }
    );
  }

  function fetchNearbyHospitals(lat, lng) {
    fetch(`${BASE_URL}/hospitals/nearby?lat=${lat}&lng=${lng}`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => {
        loading.style.display = "none";
        hospitalList.innerHTML = "";

        if (!data || data.length === 0) {
          noDataMsg.style.display = "block";
          return;
        }

        data.forEach((h) => {
          hospitalList.innerHTML += `
            <div class="hospital-card">
              <h3>${h.name}</h3>
              <p>${h.address}</p>
              <span>Distance: ${h.distance} km</span>
              <button class="btn btn-main" onclick="requestVisit('${encodeURIComponent(h.name)}')">
                Request Visit
              </button>
            </div>
          `;
        });
      })
      .catch(() => {
        loading.innerText = "Unable to load hospitals.";
      });
  }
});

function requestVisit(hospitalName) {
  window.location.href = `/request-visit?hospital=${hospitalName}`;
}
