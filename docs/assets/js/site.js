/* =========================================================================
   NEXUS Bodycam — docs
   JS mínimo: acordeões dos ícones (árvore/diagrama) + copiar código.
   Sem dependências. Tudo degrada bem sem JS (conteúdo segue legível).
   ========================================================================= */
(function () {
  "use strict";

  /* ── Acordeões ─────────────────────────────────────────────────────
     Marca o gatilho com aria-expanded. O CSS cuida da animação
     (grid-template-rows 0fr -> 1fr), então aqui só alternamos o estado. */
  document.querySelectorAll(".acordeao__gatilho").forEach(function (gat) {
    gat.addEventListener("click", function () {
      var aberto = gat.getAttribute("aria-expanded") === "true";
      gat.setAttribute("aria-expanded", String(!aberto));

      // acessibilidade: esconde o painel do fluxo de tabulação quando fechado
      var painel = gat.nextElementSibling;
      if (painel) {
        painel.setAttribute("aria-hidden", String(aberto));
        painel.querySelectorAll("a, button").forEach(function (f) {
          if (aberto) { f.setAttribute("tabindex", "-1"); }
          else { f.removeAttribute("tabindex"); }
        });
      }
    });
  });

  /* ── Copiar código ────────────────────────────────────────────────
     Cria o botão em todo <pre class="bloco"> automaticamente. */
  document.querySelectorAll("pre").forEach(function (pre) {
    var wrap = document.createElement("div");
    wrap.className = "bloco";
    pre.parentNode.insertBefore(wrap, pre);
    wrap.appendChild(pre);

    var btn = document.createElement("button");
    btn.type = "button";
    btn.className = "bloco__copia";
    btn.textContent = "copiar";
    btn.setAttribute("aria-label", "Copiar código");
    wrap.appendChild(btn);

    btn.addEventListener("click", function () {
      var txt = pre.innerText;
      var ok = function () {
        btn.textContent = "copiado!";
        btn.style.color = "var(--acento)";
        setTimeout(function () {
          btn.textContent = "copiar";
          btn.style.color = "";
        }, 1600);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(txt).then(ok, function () {});
      } else {
        // fallback para navegadores sem Clipboard API
        var ta = document.createElement("textarea");
        ta.value = txt;
        ta.style.position = "fixed";
        ta.style.opacity = "0";
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand("copy"); ok(); } catch (e) {}
        document.body.removeChild(ta);
      }
    });
  });
  /* ── Timecode REC do hero ─────────────────────────────────────────
     Só cosmético: imita o contador de uma câmera gravando.
     Para quando a aba fica oculta (não gasta CPU à toa). */
  var tc = document.getElementById("tc");
  if (tc) {
    var t0 = Date.now(), tique = null;

    function pintar() {
      var s = Math.floor((Date.now() - t0) / 1000);
      var hh = String(Math.floor(s / 3600)).padStart(2, "0");
      var mm = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
      var ss = String(s % 60).padStart(2, "0");
      tc.textContent = hh + ":" + mm + ":" + ss;
    }
    function ligar()  { if (!tique) { tique = setInterval(pintar, 1000); pintar(); } }
    function desligar(){ clearInterval(tique); tique = null; }

    // respeita quem prefere menos movimento/animação
    if (!window.matchMedia || !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      ligar();
      document.addEventListener("visibilitychange", function () {
        if (document.hidden) { desligar(); } else { ligar(); }
      });
    }
  }
})();
