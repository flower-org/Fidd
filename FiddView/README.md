# FiddView

JavaFX desktop module for the Fidd application. Contains the desktop UI layer and packages it as a runnable application
image.

Main entry point:

- `com.fidd.view.AppLauncher`

Build configuration:

- `FiddView/build.gradle`

## Packaging approach

This module currently uses:

- `org.openjfx.javafxplugin` for JavaFX dependencies
- `org.beryx.runtime` for runtime image packaging
- `jpackage` through the runtime plugin

The current `runtime` block builds an `app-image`, not an installer.

So the target machine does not need a separately installed Java runtime.

## Main tasks

Build the runtime image:

```bash
gradlew :FiddView:runtime
```

Build the packaged application image:

```bash
gradlew :FiddView:jpackageImage
```

## How multi-platform packaging works

1. Linux runner executes `gradlew :FiddView:jpackageImage`
2. Windows runner executes `gradlew :FiddView:jpackageImage`
3. macOS runner executes `gradlew :FiddView:jpackageImage`

Each run produces an image for its own platform only.

## If installers are needed later

To build installers instead of plain app images, `installerType` must be changed per platform.

Examples:

- Linux: `deb` or `rpm`
- Windows: `msi` or `exe`
- macOS: `pkg` or `dmg`

This is still platform-specific, so installer builds also need to run on the corresponding OS.

## Notes

- JavaFX native dependencies are also platform-specific, so packaging must be validated on each target OS.
- The bundled runtime is trimmed using runtime/jlink options from `build.gradle`.