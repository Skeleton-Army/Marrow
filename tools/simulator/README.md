# Marrow Weaver Simulator

A desktop playground for testing the Weaver path planner. Place targets and
obstacles on a 144x144 in field and watch the generated path and an animated
robot in real time.

This is a **development-only tool**. It is not published with the `core`
library and exists purely to sanity-check Weaver behavior by hand.

## Running

Requires a JDK on your `PATH`/`JAVA_HOME` (the simulator is compiled with
`javac` against the `core` sources).

```sh
./gradlew runWeaverSimulator
```

On Windows:

```powershell
.\gradlew.bat runWeaverSimulator
```

## Controls

### Mouse

| Action | Result |
| --- | --- |
| Left-click empty space | Add the current mode's item (target, obstacle, or start) |
| Right-click | Remove the nearest target or obstacle |
| Drag an obstacle body | Move it |
| Drag an edge handle | Resize it |
| Drag a polygon corner | Rotate it |

### Keyboard

| Key | Action |
| --- | --- |
| `T` | Target mode |
| `O` | Obstacle mode |
| `S` | Start mode |
| `R` | Toggle target reordering |
| `C` | Clear all targets and obstacles |
| `P` | Toggle obstacle shape (circle / polygon) |
| `[` / `]` | Decrease / increase polygon side count (3-12) |
| `A` / `D` | Decrease / increase intake width |
| `+` / `-` | Increase / decrease obstacle size |

## Notes

- The path is recomputed after every edit using `PathConfig` with the current
  intake width.
- With reordering on (default) Weaver finds its own target order; turn it off
  to visit targets in the order they were added.
- Any generation error is shown in the on-screen HUD.
