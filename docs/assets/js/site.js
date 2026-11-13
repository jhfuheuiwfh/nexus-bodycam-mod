/* =========================================================================
   NEXUS Bodycam — docs
   Minimal JS: the icon accordions (tree/diagram) + copy code.
   No dependencies. Everything degrades gracefully without JS (content
   stays readable).
   ========================================================================= */
(function () {
  "use strict";

  /* ── Accordions ─────────────────────────────────────────────────────
     Marks the trigger with aria-expanded. CSS owns the animation
     (grid-template-rows 0fr -> 1fr), so all we do here is flip state. */
  document.querySelectorAll(".acordeao__gatilho").forEach(function (gat) {
    gat.addEventListener("click", function () {
      var open = gat.getAttribute("aria-expanded") === "true";
      gat.setAttribute("aria-expanded", String(!open));

      // accessibility: hide the panel from the tab order while collapsed
      var panel = gat.nextElementSibling;
      if (panel) {
        panel.setAttribute("aria-hidden", String(open));
        panel.querySelectorAll("a, button").forEach(function (f) {
          if (open) { f.setAttribute("tabindex", "-1"); }
          else { f.removeAttribute("tabindex"); }
        });
      }
    });
  });

  /* ── Copy code ────────────────────────────────────────────────────
     Creates the button on every <pre class="bloco"> automatically. */
  document.querySelectorAll("pre").forEach(function (pre) {
    var wrap = document.createElement("div");
    wrap.className = "bloco";
    pre.parentNode.insertBefore(wrap, pre);
    wrap.appendChild(pre);

    var btn = document.createElement("button");
    btn.type = "button";
    btn.className = "bloco__copia";
    btn.textContent = "copy";
    btn.setAttribute("aria-label", "Copy code");
    wrap.appendChild(btn);

    btn.addEventListener("click", function () {
      var txt = pre.innerText;
      var done = function () {
        btn.textContent = "copied!";
        btn.style.color = "var(--acento)";
        setTimeout(function () {
          btn.textContent = "copy";
          btn.style.color = "";
        }, 1600);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(txt).then(done, function () {});
      } else {
        // fallback for browsers without the Clipboard API
        var ta = document.createElement("textarea");
        ta.value = txt;
        ta.style.position = "fixed";
        ta.style.opacity = "0";
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand("copy"); done(); } catch (e) {}
        document.body.removeChild(ta);
      }
    });
  });
  /* ── Download popup + latest-loader resolver ────────────────────────.
     Every element with [data-download-trigger] opens the shared download
     dialog. The dialog contains one direct .jar link for each supported
     loader (Fabric and Forge).
     - No JS yet (or API down): the dialog links stay on the newest known
       release URLs, so the site still works.
     - JS on: we fetch all releases, find the newest published release per
       loader, and upgrade both links. [data-version-label] gets the
       newest overall display version.
     Versioned release pages do not need manual edits anymore: just
     publish a release and everything on the site follows.             */
  var GH_RELEASES_API = "https://api.github.com/repos/jhfuheuiwfh/nexus-bodycam-mod/releases?per_page=100";
  var modal = document.getElementById("download-modal");
  var triggers = document.querySelectorAll("[data-download-trigger]");
  var links = {
    fabric: document.querySelector('[data-download-option="fabric"]'),
    forge:  document.querySelector('[data-download-option="forge"]')
  };
  var lastTrigger = null;

  function parseVersion(release) {
    var text = String(release.tag_name || release.name || "");
    var match = text.match(/^v?(\d+)\.(\d+)\.(\d+)(?:[+-].*)?$/i);
    return match ? [+match[1], +match[2], +match[3]] : null;
  }

  function publishTime(release) {
    var time = Date.parse(release.published_at || release.created_at || "");
    return Number.isFinite(time) ? time : 0;
  }

  function isNewer(candidate, current) {
    if (!current) { return true; }
    var a = parseVersion(candidate);
    var b = parseVersion(current);

    if (a && b) {
      for (var i = 0; i < 3; i++) {
        if (a[i] !== b[i]) { return a[i] > b[i]; }
      }
    } else if (a || b) {
      return Boolean(a);
    }
    return publishTime(candidate) > publishTime(current);
  }

  function isForgeRelease(release, jarNames) {
    var signature = [release.tag_name || "", release.name || ""].concat(jarNames).join(" ").toLowerCase();
    return /(^|[^a-z])forge([^a-z]|$)/.test(signature);
  }

  function displayVersion(release) {
    var text = String(release.tag_name || release.name || "");
    var match = text.match(/^v?(\d+\.\d+\.\d+)(?:\+([^-\s]+))?/i);
    if (!match) { return text; }
    return "v" + match[1] + (match[2] ? "+" + match[2] : "");
  }

  function updateReleaseLinks(candidates) {
    ["fabric", "forge"].forEach(function (loader) {
      var release = candidates[loader];
      var link = links[loader];
      if (!release || !link) { return; }

      var jar = release.assets.find(function (asset) { return /\.jar$/i.test(asset.name || ""); });
      if (jar && jar.browser_download_url) { link.href = jar.browser_download_url; }

      var version = modal.querySelector('[data-loader-version="' + loader + '"]');
      if (version) { version.textContent = displayVersion(release); }
    });

    var overall = candidates.fabric && candidates.forge
      ? (isNewer(candidates.forge, candidates.fabric) ? candidates.forge : candidates.fabric)
      : (candidates.fabric || candidates.forge);
    if (overall) {
      var shown = displayVersion(overall);
      document.querySelectorAll("[data-version-label]").forEach(function (el) {
        el.textContent = shown;
      });
    }
  }

  function fetchLatestDownloads() {
    if (typeof fetch !== "function" || (!links.fabric && !links.forge)) { return; }

    fetch(GH_RELEASES_API, { headers: { "Accept": "application/vnd.github+json" } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (releases) {
        if (!Array.isArray(releases)) { return; }

        var candidates = { fabric: null, forge: null };
        releases.forEach(function (release) {
          if (!release || release.draft || release.prerelease || !Array.isArray(release.assets)) { return; }

          var jar = release.assets.find(function (asset) { return /\.jar$/i.test(asset.name || ""); });
          if (!jar) { return; }

          var jarNames = release.assets
            .filter(function (asset) { return /\.jar$/i.test(asset.name || ""); })
            .map(function (asset) { return asset.name || ""; });
          var loader = isForgeRelease(release, jarNames) ? "forge" : "fabric";

          if (isNewer(release, candidates[loader])) { candidates[loader] = release; }
        });

        updateReleaseLinks(candidates);
      })
      .catch(function () { /* keep the newest known fallback silently */ });
  }

  function openModal(trigger) {
    if (!modal) { return; }
    lastTrigger = trigger || null;
    document.body.classList.add("download-modal-open");

    if (typeof modal.showModal === "function") {
      modal.showModal();
    } else {
      modal.setAttribute("open", "");
    }

    var firstOption = modal.querySelector(".download-option");
    if (firstOption) { firstOption.focus({ preventScroll: true }); }
  }

  function closeModal() {
    if (!modal || !modal.open) { return; }

    document.body.classList.remove("download-modal-open");
    if (typeof modal.close === "function") {
      modal.close();
    } else {
      modal.removeAttribute("open");
      restoreFocus();
    }
  }

  function restoreFocus() {
    if (lastTrigger && document.contains(lastTrigger)) {
      lastTrigger.focus({ preventScroll: true });
    }
  }

  if (modal && triggers.length) {
    triggers.forEach(function (trigger) {
      trigger.addEventListener("click", function (event) {
        event.preventDefault();
        openModal(trigger);
      });
    });

    modal.querySelectorAll("[data-download-close]").forEach(function (link) {
      link.addEventListener("click", function (event) {
        event.preventDefault();
        closeModal();
      });
    });

    // Clicking the dark backdrop closes the native dialog.
    modal.addEventListener("click", function (event) {
      if (event.target === modal) { closeModal(); }
    });

    // Native <dialog> handles Escape automatically; this covers the fallback.
    document.addEventListener("keydown", function (event) {
      if ((event.key === "Escape" || event.key === "Esc") && modal.open) { closeModal(); }
    });

    modal.addEventListener("close", function () {
      document.body.classList.remove("download-modal-open");
      restoreFocus();
    });

    fetchLatestDownloads();
  }
  /* ── Hero REC timecode ───────────────────────────────────────────
     Purely cosmetic: imitates the counter of a recording camera.
     Stops when the tab is hidden (no wasted CPU). */
  var tc = document.getElementById("tc");
  if (tc) {
    var t0 = Date.now(), tick = null;

    function paint() {
      var s = Math.floor((Date.now() - t0) / 1000);
      var hh = String(Math.floor(s / 3600)).padStart(2, "0");
      var mm = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
      var ss = String(s % 60).padStart(2, "0");
      tc.textContent = hh + ":" + mm + ":" + ss;
    }
    function on()  { if (!tick) { tick = setInterval(paint, 1000); paint(); } }
    function off() { clearInterval(tick); tick = null; }

    // respect people who prefer less motion/animation
    if (!window.matchMedia || !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      on();
      document.addEventListener("visibilitychange", function () {
        if (document.hidden) { off(); } else { on(); }
      });
    }
  }
})();
