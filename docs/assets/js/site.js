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
