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
  /* ── Latest-release download resolver ───────────────────────────.
     Every element with [data-download-latest] is a download link that
     must ALWAYS point to the newest published JAR — never a hardcoded
     version.
     - No JS yet (or API down): the href stays `releases/latest`, GitHub's
       built-in redirect to the newest release page. Never a 404.
     - JS on: we fetch the latest release from the GitHub API, find the
       first .jar asset and upgrade every matching link to the direct
       asset URL. Also refreshes [data-version-label] texts with the
       release tag, so "Download v..." stays truthful forever.
     Versioned release pages do not need manual edits anymore: just
     publish a release and everything on the site follows.             */
  var GH_RELEASE_API = "https://api.github.com/repos/jhfuheuiwfh/nexus-bodycam-mod/releases/latest";
  var latestLinks = document.querySelectorAll("[data-download-latest]");
  if (latestLinks.length) {
    fetch(GH_RELEASE_API, { headers: { "Accept": "application/vnd.github+json" } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (rel) {
        if (!rel || !rel.assets) { return; }
        var jar = null;
        for (var i = 0; i < rel.assets.length; i++) {
          if (/\.jar$/i.test(rel.assets[i].name)) { jar = rel.assets[i]; break; }
        }
        if (jar && jar.browser_download_url) {
          latestLinks.forEach(function (a) { a.href = jar.browser_download_url; });
        }
        if (rel.tag_name) {
          document.querySelectorAll("[data-version-label]").forEach(function (el) {
            el.textContent = rel.tag_name;
          });
        }
      })
      .catch(function () { /* keep the releases/latest fallback silently */ });
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
