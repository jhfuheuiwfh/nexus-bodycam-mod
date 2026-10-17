# NEXUS Bodycam — Site de Demonstração

Bem-vindo à página de demonstração do NEXUS Bodycam. Este site é uma apresentação leve do mod — instalação, previews e links úteis — hospedada via GitHub Pages.

---

## Preview

<div style="display:flex;gap:12px;flex-wrap:wrap;">

![Smooth steps](https://raw.githubusercontent.com/jhfuheuiwfh/nexus-bodycam-mod/main/gameplay/smooth-steps.gif){width="320"}

![Flying camera shake](https://raw.githubusercontent.com/jhfuheuiwfh/nexus-bodycam-mod/main/gameplay/flying.gif){width="320"}

</div>

> Outros previews e vídeos estão no repositório em `gameplay/`.

---

## O que é

NEXUS Bodycam transforma a câmera do Minecraft 1.20.1 (Fabric) numa experiência first-person estilo bodycam: respiração audível sincronizada, bob/rolagem realista, pipeline de lentes/post-FX, efeitos aquáticos e realismo visual para armas (integração opcional com TACZ). É client-side e foca apenas na sensação visual.

---

## Instalação rápida

1. Instale Fabric Loader para Minecraft 1.20.1.
2. Baixe o JAR da release mais recente em: https://github.com/jhfuheuiwfh/nexus-bodycam-mod/releases
3. Coloque `NEXUS-bodycam-*.jar` na pasta `mods/`.

Requisitos: Java 17+, Fabric API.

---

## Construir a partir do código

```bash
git clone https://github.com/jhfuheuiwfh/nexus-bodycam-mod.git
cd nexus-bodycam-mod
# coloque o jar do TACZ em libs/ se quiser compilar com integração
./gradlew build
```

O artefato fica em `build/libs/`.

---

## Links rápidos

- Repositório: https://github.com/jhfuheuiwfh/nexus-bodycam-mod
- README completo: https://github.com/jhfuheuiwfh/nexus-bodycam-mod/blob/main/README.md
- AI disclosure: https://github.com/jhfuheuiwfh/nexus-bodycam-mod/blob/main/AI-TRANSPARENCY.md

---

Se quiser, posso expandir isso para um site Jekyll completo, adicionar temas e uma página de changelog automática.
