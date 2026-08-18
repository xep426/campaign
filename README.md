# Campaign

Three deliberate tasks a day, carried by longer campaigns.

An Android app for one person: no accounts, no sync, no server. Everything
lives on the device.

## What it is

A hard limit of **three** tasks a day, and no fourth. The list is not a
backlog you work down — it is a choice you make once a day and then live
with.

**The day turns at an hour you pick**, 22:00 by default, not at midnight.
At that moment the list empties and you choose the next three. Between the
turn and midnight the app knows they are tomorrow's: it shows *"Three
things for tomorrow."*, drops the completion marks, and a tap edits instead
of completing. Claiming to have finished something on a day that has not
begun is not a thing the app will record.

A **campaign** is a longer effort. It is not something you write — it is
the overview of the tasks assigned to it that are still outstanding.
Finishing one is the thing worth looking back on, which is why History
opens on campaigns rather than on individual tasks.

No streaks, no points, no charts, no tags, no priorities, no subtasks.

## Screens

| Screen | What it does |
| --- | --- |
| **Today** | The three slots. Tap to complete, drag to reorder, ⋯ to edit, assign or delete |
| **Campaigns** | Active campaigns, each showing its outstanding tasks |
| **History** | Finished campaigns, and finished tasks that belonged to none |
| **Widget** | Today's three on the home screen, tappable |

## Building

Open in Android Studio and run. `local.properties` points at the Android
SDK and is untracked, since the path differs per machine.

Requires an SDK with **compileSdk 36**; minSdk is 26.

```bash
./gradlew :app:installDebug
```

## Structure

One module. Packages carry the shape a split would use and promote to
modules cleanly if the app ever grows into them.

| Package | Holds |
| --- | --- |
| `domain` | Models and repository interfaces — no Android imports |
| `data` | Room database, DataStore settings, repository implementations |
| `ui` | Theme, shared components, and the three screens |
| `widget` | The Glance home-screen widget |
| `notify` | The evening alarm, its receivers and the notification |
| `di` | Hilt wiring |

## Decisions worth knowing

Most of these were arrived at by getting them wrong first. The reasoning
lives next to the code; this is the short version.

**The day boundary is the load-bearing idea.** `CampaignDay.of(now, turnsAt)`
is four lines and it removed a Tomorrow pane, a draft-and-confirm button, a
separate end-of-day screen and a rule for what happened when the two
disagreed. All of that existed to bridge the gap between when you decide
and when the calendar agrees with you. Move the boundary to the moment you
actually decide and the gap closes.

**One setting, two jobs.** The same hour turns the list over and fires the
notification. Splitting them would let the app prompt you to choose
tomorrow while still showing you today — the mismatch the second screen
existed to paper over.

**Dark only.** The centre of gravity is a prompt at 22:00 in a dim room. A
light theme would put a white rectangle in your face at exactly that
moment. The corollary is a rule: no `values-night` resources anywhere.

**The evening prompt is inexact.** Google Play restricts exact alarms to
apps whose core function is one — clocks, timers, calendars. A tasks app is
none of those, so the prompt can arrive up to an hour late. Correctness does
not depend on it: the day turns because `CampaignDay` reads the clock, not
because an alarm fired.

**The widget toggles tasks, and the row is the target.** It shipped
view-only on the argument that a completion target millimetres from a
launcher icon gets tapped by accident. The worry was right and the
conclusion was wrong — the fix is a target big enough to hit on purpose. A
row is ~58dp on a real 4×2 placement, against a 14dp mark and Android's
48dp minimum.

**The widget reads the same day rule as the app.** It did not, once, and
between 22:00 and midnight the two disagreed about which day it was. There
is one rule and it lives in `CampaignDay`.

**Reordering hands over to the data during composition, not in an effect.**
A drop cannot commit and reset in the same breath — the write is
asynchronous and the rows fall back for a frame. Nor can the local order be
kept, or it applies twice. `ReorderableColumn` holds the permutation and
drops it the moment `keys` supersede it, decided in composition because an
effect runs one frame late.

**The index on (date, slot) is plain, not unique.** It was unique on the
theory that the schema should enforce the limit of three, which it never
did — there is no CHECK on slot. What it did do was force every reorder to
park rows on negative slots before writing the real ones.

## Conventions

- Comments explain **why**, not what. A comment that restates the code is
  noise; one that records the decision behind it survives the next rewrite.
  Several here exist specifically to stop a future reader undoing a fix.
- Colours and fonts come from `ui/theme` tokens. No raw hex and no
  `FontFamily.Serif` anywhere else. The handful of values in `colors.xml`
  are the exception — a drawable cannot read a Compose `Color`.
- Strings live in `strings.xml` with an English and a German locale. German
  is informal *du*. Date **patterns** are localised too, not just text:
  `EEEE d MMMM` against `EEEE, d. MMMM`.
- View models emit `UiMessage` (a resource id plus args) rather than
  `String`, so they hold no `Context` and a snackbar resolves in the
  language being read now.
- Fonts are system stacks. `ui/theme/AppFonts.kt` documents the three-step
  swap to bundled Fraunces and IBM Plex Mono.

## The design study

`docs/design-study.html` is the HTML mock the app was built from — five
screens on one board, and the source of the palette and type. It is kept as
the original artefact and **is not current**: it predates the day-turn
boundary, campaigns-as-overview, the interactive widget and drag-to-reorder,
and it still shows an end-of-day screen that no longer exists. Read it for
the visual language, not the structure.

## Licence

MIT. See [LICENSE](LICENSE).
