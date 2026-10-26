# Marrow Weaver Simulator

A desktop playground for testing the Weaver path planner. Place targets and
obstacles on a 144x144 in field and watch the generated path and an animated
robot in real time.

This is a **development-only tool**. It is not published with the `core`
library and exists purely to sanity-check Weaver behavior by hand.

## Running

In Android Studio, open the **Weaver Simulator** run configuration, or run the
Gradle task directly:

```sh
./gradlew :tools:simulator:run
```

On Windows:

```powershell
.\gradlew.bat :tools:simulator:run
```

This module depends on `:core`, so it needs the Android SDK configured like the
rest of the project.

## Controls

### Mouse

| Action                 | Result                                                   |
|------------------------|----------------------------------------------------------|
| Left-click empty space | Add the current mode's item (target, obstacle, or start) |
| Left-click a target    | Select it to edit its heading                            |
| Drag a heading handle  | Point the selected target in any direction               |
| Right-click            | Remove the nearest target or obstacle                    |
| Drag an obstacle body  | Move it                                                  |
| Drag an edge handle    | Resize it                                                |
| Drag a polygon corner  | Rotate it                                                |

### Side panel

| Control                    | Result                                             |
|----------------------------|----------------------------------------------------|
| Target / Obstacle / Start  | Choose what a left-click places                    |
| Deselect                   | Clear the current target selection                 |
| Polygon                    | Toggle the obstacle shape (circle / polygon)       |
| Sides                      | Set the polygon side count (3-12)                  |
| Size                       | Set the obstacle size                              |
| Intake width               | Set the planner intake width                       |
| Reorder targets            | Let Weaver choose the visit order                  |
| Smooth path                | Toggle path smoothing                              |
| Max targets                | Cap how many targets are visited (`0` = all)       |
| Lock heading               | Lock the selected target's heading                 |
| Heading / -15 / +15        | Set or nudge the selected target's heading         |
| Lock new targets           | Start newly placed targets with a locked heading   |
| New target heading         | Set the heading used for new locked targets        |
| Start pose heading         | Rotate the start pose heading                      |
| Clear all                  | Remove all targets and obstacles                   |
| Generate code              | Show and copy the equivalent `Weaver.builder()` code |
| Copy / Paste scenario      | Move the current setup through the clipboard       |

### Menu

| Item                                       | Shortcut | Result                                  |
|--------------------------------------------|----------|-----------------------------------------|
| Scenario > Copy current scenario           | `Ctrl+C` | Copy the current setup as scenario text |
| Scenario > Paste scenario from clipboard   | `Ctrl+V` | Load a scenario from the clipboard      |

## Scenarios

The simulator can copy and paste its whole setup as scenario text: start pose
(including heading), targets and their locked headings, obstacles, and the
relevant `PathConfig` values. Copy a scenario from chat, then use
**Scenario > Paste scenario from clipboard**. Use **Scenario > Copy current
scenario** to put the current board on the clipboard so it can be shared.

Example scenario text:

```json
{
  "format": "marrow-scenario",
  "version": 1,
  "name": "Facing-away cluster",
  "description": "Start facing north with a packed cluster behind it.",
  "start": { "x": 24, "y": 70, "headingDeg": 90 },
  "targets": [
    { "x": 24, "y": 62 },
    { "x": 24.4, "y": 61.7 },
    { "x": 23.7, "y": 61.9 },
    { "x": 24.1, "y": 62.3 }
  ],
  "obstacles": [
    { "type": "circle", "x": 72, "y": 60, "radius": 6 },
    {
      "type": "polygon",
      "points": [
        { "x": 40, "y": 40 },
        { "x": 50, "y": 40 },
        { "x": 50, "y": 50 },
        { "x": 40, "y": 50 }
      ]
    }
  ],
  "config": {
    "intakeWidth": 13,
    "robotWidth": 17,
    "robotHeight": 16,
    "clearance": 4,
    "smoothing": true,
    "reorder": true,
    "maxTargets": 3
  }
}
```

- `headingDeg` (or `headingRad`) on a target means its heading is **locked**.
  Add `"locked": false` to keep the target free even if a heading is present.
- The `start` heading is used by the planner; rotate it with the **Start pose
  heading** control when building a scenario by hand.
- Missing `config` values fall back to the `PathConfig` defaults.
- Unknown keys are ignored, so newer scenarios remain readable.

## Notes

- The **Generate code** button in the side panel shows and copies the equivalent
  `Weaver.builder()` code for the current setup to the clipboard.
- Targets are free by default: the path can reach them from any direction.
  Click a target and tick **Lock heading** to lock its heading, then drag its
  heading handle or use the **Heading** controls to set the direction. **Lock new
  targets** controls whether newly placed targets start with a locked heading.
- Locked target headings are preserved whether smoothing is on or off. Toggle
  **Smooth path**, which only changes how free tangents are estimated.
- The path is recomputed after every edit using `PathConfig` with the current
  intake width. Each target's policy and heading are included in the generated
  `Weaver.builder()` code.
- With reordering on (default) Weaver finds its own target order; turn it off
  to visit targets in the order they were added.
- A non-zero **Max targets** caps the number of targets visited; when there are
  more targets than the cap, Weaver keeps the largest reachable set and picks
  the cheapest order. Chosen targets are drawn filled green, left-behind ones
  gray.
- Any generation error is shown in the on-screen HUD.
