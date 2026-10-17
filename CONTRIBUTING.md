# Contributing to NEXUS Bodycam

Thanks for considering a contribution. This project has one hard architectural
rule and a couple of conventions — everything else is negotiable.

---

## 🚫 The hard rule: it stays client-side

NEXUS Bodycam is a **pure client-side camera overhaul**. It may only change how
the camera *looks and feels*. It must **never** touch:

- movement speed or physics
- hitboxes or collision
- raycasts, bullets or server logic
- anything that travels over the network

A PR that breaks this is rejected regardless of how good it looks. It is the
reason the mod is safe to use on servers.

---

## 🏗️ Building

```bash
git clone https://github.com/jhfuheuiwfh/nexus-bodycam-mod.git
cd nexus-bodycam-mod
./gradlew build          # Windows: gradlew.bat build
```

- **JDK 17** on `JAVA_HOME` (compiles targeting Java 17).
- The result lands in `build/libs/NEXUS-bodycam-*.jar`.
- `libs/` is git-ignored. Put a **TaCZ jar** there if you want to compile the
  weapon-realism bridge; without it the mixins disable themselves and the build
  still succeeds.

---

## ✍️ The changelog rule — mandatory

> **Every** change to the mod **must** get an entry in
> [`docs/CHANGELOG.md`](docs/CHANGELOG.md) **before** the task is considered done.

No exceptions. This covers code, shaders, config, sounds, mixins, GUI, build
scripts and assets. The entry says **what** changed and **why**.

A task without a changelog entry is an incomplete task.

If you touch the public surface, also keep in sync:

- `gradle.properties` → `mod_version`
- `src/main/resources/fabric.mod.json`
- the version line in `README.md`
- the version line in `TODO.md`

---

## 🔀 Submitting a PR

1. Branch off `main`.
2. Make the change.
3. Add the changelog entry.
4. Build it and **test in game** — including the config screen.
5. Open the PR describing what changed and what you verified.

CI builds the jar on every PR. A red build means the PR is not mergeable.

### What to verify in game

- The world renders normally (no black screen, no sliced textures).
- Shaders still work if you use Iris — shadow passes must be skipped, not corrupted.
- The crosshair still lines up with the block you are mining.
- The config screen scrolls and saves.

---

## 🐛 Reporting bugs

Use the [bug report template](../../issues/new?template=bug_report.yml). The two
fields that matter most:

- **Other mods installed** — most breakage is a mod conflict, not this mod.
- **`latest.log`** — the real answer is almost always in there.

Versioning shorthand, so you know how big a jump a release is:

| Scheme    | Meaning                    |
|-----------|----------------------------|
| `1.0.x`   | small fix / patch          |
| `1.x.0`   | new or big feature         |
| `x.0.0`   | big update / rework        |

---

## 💡 Proposing features

Open an issue first for anything non-trivial. Before writing it, ask whether it
passes two filters:

1. **Is it visual-only?** If it affects gameplay, it is out of scope.
2. **Can it run every frame cheaply?** Post-FX runs on every pixel every frame.
   An effect has to earn its cost.

---

## 🌐 Docs site

The `docs/` folder is a Jekyll site published on GitHub Pages, and it exists so
the README can stay short.

```
docs/
├── _config.yml          # baseurl /nexus-bodycam-mod
├── _layouts/default.html
├── _includes/icons.html # SVG sprite — transparent, currentColor
├── assets/{css,js,img}/
├── index.html           # the deep-dive page
├── changelog.md         # wrapper → renders CHANGELOG.md
├── devnotes.md          # wrapper → renders DEVNOTES.md
├── CHANGELOG.md         # raw source, linked from README
└── DEVNOTES.md
```

When adding an icon, add it to `_includes/icons.html` as a `<symbol>`. Keep the
convention: **`stroke="currentColor"` + `fill="none"`**, no background rect, no
hard-coded colour — that is what makes them transparent and theme-aware.

---

## 📄 License

By contributing you agree your work is released under the
[GNU GPL v3.0](LICENSE), the same licence as the project.

This project is substantially AI-generated and says so openly — see
[AI-TRANSPARENCY.md](AI-TRANSPARENCY.md). Contributions that mix AI and human
work are fine as long as you can describe what you did.

---

## ⚠️ Photosensitivity

This project may contain flashing lights. Test with that in mind, and say so in
your PR if you add anything strobing.
