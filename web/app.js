(function () {
  "use strict";

  var LIMIT = 20;
  var nextCursor = null;
  var loading = false;
  var done = false;
  var observer = null;

  var grid = document.getElementById("photo-grid");
  var loadingEl = document.getElementById("loading");
  var errorEl = document.getElementById("error");
  var endEl = document.getElementById("end");

  function imageUrl(path) {
    if (!path) return "";
    if (path.startsWith("http")) return path;
    return API_BASE_URL + path;
  }

  function formatDate(dateStr) {
    if (!dateStr) return "";
    var d = new Date(dateStr);
    return d.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  function renderCard(item) {
    var card = document.createElement("div");
    card.className = "card";

    var img = document.createElement("img");
    img.src = imageUrl(item.image_url_thumbnail || item.image_url);
    img.alt = item.user_caption || "Fender art submission";
    img.loading = "lazy";
    card.appendChild(img);

    var caption = document.createElement("div");
    caption.className = "caption";

    if (item.username) {
      var username = document.createElement("span");
      username.className = "username";
      username.textContent = item.username;
      caption.appendChild(username);
    }

    if (item.captured_date) {
      var date = document.createElement("span");
      date.className = "date";
      date.textContent = " \u00b7 " + formatDate(item.captured_date);
      caption.appendChild(date);
    }

    card.appendChild(caption);
    grid.appendChild(card);
  }

  function fetchPage() {
    if (loading || done) return;
    loading = true;
    loadingEl.hidden = false;
    errorEl.hidden = true;

    var url = API_BASE_URL + "/submissions?limit=" + LIMIT;
    if (nextCursor) url += "&cursor=" + encodeURIComponent(nextCursor);

    fetch(url)
      .then(function (res) {
        if (!res.ok) throw new Error("HTTP " + res.status);
        return res.json();
      })
      .then(function (data) {
        data.items.forEach(renderCard);
        nextCursor = data.next_cursor;

        if (!data.has_more || data.items.length === 0) {
          done = true;
          loadingEl.hidden = true;
          endEl.hidden = grid.children.length === 0;
        }
      })
      .catch(function () {
        loadingEl.hidden = true;
        errorEl.hidden = false;
        done = true;
      })
      .finally(function () {
        loading = false;
        if (!done && observer) {
          observer.unobserve(loadingEl);
          observer.observe(loadingEl);
        }
      });
  }

  // Infinite scroll via IntersectionObserver
  if ("IntersectionObserver" in window) {
    observer = new IntersectionObserver(
      function (entries) {
        if (entries[0].isIntersecting) {
          fetchPage();
        }
      },
      { rootMargin: "200px" }
    );
    observer.observe(loadingEl);
  }

  // Initial load
  fetchPage();
})();
