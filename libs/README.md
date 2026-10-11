# `libs/` — local dependencies (not committed)

This folder is **intentionally empty on GitHub**. JARs placed here are
**ignored by `.gitignore`** because they are third-party binaries that
cannot be redistributed.

## What you need to build NEXUS Bodycam

| File | Where to get it |
|---|---|
| `TACZ-Refabricated-1.20.1-0.7.1-forge1.1.8-hotfix2.jar` | [TACZ - Timeless and Classics Zero](https://modrinth.com/mod/tacz) / [Refabricated port](https://modrinth.com/mod/tacz-refabricated) |

Drop the jar(s) in this folder — `build.gradle` picks them up with:

```groovy
modCompileOnly fileTree(dir: 'libs', include: '*.jar')
```

> **Note:** the TACZ jar used during development was patched by hand
> (two `Fabric-Loom-*` manifest lines removed and four
> `com/tacz/guns/client/renderer/item/*ItemRenderer.class` files deleted),
> otherwise Loom/tiny-remapper refuse to compile against it. If you hit
> those errors with a fresh jar, apply the same two patches.
