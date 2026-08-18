# Campaign

Three things a day, chosen deliberately. Some of them become campaigns —
longer efforts carried forward one step at a time.

## What it is

A hard limit of **three** tasks per day, and no fourth. The evening screen
is the heart of it: at a time you set, the app asks what tomorrow is for,
and shows you today one last time so anything worth continuing can be
promoted into a campaign.

A **campaign** is a longer effort. It carries exactly one next step, in
free text. Pulling that step drops it into an open slot on a day and clears
it from the campaign — what comes after is a decision you make when you get
there, not a backlog you accumulate.

There are no streaks, no points, no charts, no tags, no priorities and no
subtasks. History is a record to read back, not a scoreboard.

## Screens

| Screen | What it does |
| --- | --- |
| **End of day** | Tomorrow's three slots, today in review, promote, confirm |
| **Today** | The three slots; tap a row to complete it |
| **Campaigns** | Active campaigns, each with one next step to pull |
| **History** | Past days and finished campaigns |
| **Widget** | Today's three slots on the home screen, view-only |

The design comes from `campaign-mock.html` in this repo — open it in a
browser to see all five screens side by side. It is the reference for
palette, type and copy.

## Building

Open in Android Studio and run, the same as Kalimetra. `local.properties`
points at the Android SDK and is untracked, since the path differs per
machine.

Requires an Android SDK with **compileSdk 36**; minSdk is 26.

```bash
./gradlew :app:installDebug
```

## Structure

One module, deliberately — see the note in `settings.gradle.kts`. Packages
carry the shape a split would use, and promote to modules cleanly if the
app ever grows into them.

| Package | Holds |
| --- | --- |
| `domain` | Models and repository interfaces — no Android imports |
| `data` | Room database, DataStore settings, repository implementations |
| `ui` | Theme, shared components, and the four screens |
| `widget` | The Glance home-screen widget |
| `notify` | The evening alarm, its receivers and the notification |
| `di` | Hilt wiring |

## Decisions worth knowing

**Dark only.** The product's centre of gravity is a notification at 21:00
in a dim room. A light theme would put a white rectangle in your face at
exactly that moment. The corollary is a rule: no `values-night` resources
anywhere.

**The three-slot limit lives in the schema**, as a unique index on
`(date, slot)` — not in a view model. A limit enforced only in the UI holds
until the first race.

**The evening alarm asks to be exact, and degrades if refused.** It was
inexact at first, on the reasoning that "the day is over" needn't land on
the minute. A live `dumpsys alarm` disproved that: the platform gave the
21:00 alarm a delivery window of a **full hour**. It now uses
`setExactAndAllowWhileIdle` where `canScheduleExactAlarms()` allows and
falls back otherwise, so the app works without the grant and is punctual
with it. Grant it under **Settings → Apps → Campaign → Alarms & reminders**.

**Opening the app must not cancel a prompt that is about to fire.** The
scheduler leaves a booked alarm alone while its delivery window is still
open; only a fire, a reboot, or a settings change forces a re-book. Without
that rule the failure is self-reinforcing — a late notification makes you
open the app, and opening the app is what guarantees it never arrives.
`EndOfDayScheduler.isStillDeliverable` is the rule, kept pure and tested.

**The widget toggles tasks in place, and the row is the target.** This
shipped view-only first, on the argument that a completion target
millimetres from a launcher icon gets tapped by accident. The worry was
right; the conclusion was wrong. The fix is a target big enough to hit on
purpose: a row is the full card width and ~58dp tall on a real 4×2
placement, against a 14dp mark and Android's 48dp minimum. Rows toggle
(same grammar as Today), the header opens the app, an empty row opens the
app at that slot. The cost is the tick animation — RemoteViews cannot draw
a path, so the mark swaps instead of drawing itself.

**The widget collects its data inside the composition, never above it.**
This is the bug that took three wrong fixes to find, so it is worth stating
plainly: loading the slots in `provideGlance` and closing over the result
gives the composition a *snapshot*. Glance keeps a session alive per
widget, and a redraw on a live session recomposes that same captured
value — so the card faithfully redrew identical content and the toggle
looked dead, while the database had already changed. It only appeared to
work in testing because a killed process forced `provideGlance` to re-run.

`todaySlotsFlow` + `collectAsState` inside `provideContent` makes the Room
flow the source, so a write recomposes with the new value. `loadTodaySlots`
survives only to seed the first paint.

`ToggleTaskAction` still calls `update(context, glanceId)` afterwards, for
the cold case where no session is alive to recompose. `GlanceWidgetRefresher`
**logs** its failures now; the silent `runCatching` is part of why this took
so long to see.

**Nothing is saved as you type on the evening screen.** The three fields
are drafts until "Set tomorrow". Everywhere else in the app a tap takes
effect immediately; here the button is the commitment, because deliberation
is the point.

## Conventions

- Comments explain **why**, not what. A comment that restates the code is
  noise; one that records the decision behind it survives the next rewrite.
- Colours and fonts come from `ui/theme` tokens. No raw hex and no
  `FontFamily.Serif` anywhere else. The five values in `colors.xml` are the
  exception — a drawable cannot read a Compose `Color` — and are kept in
  step by hand.
- Strings live in `strings.xml` with an English and a German locale.
  German is informal *du*. The app name stays "Campaign"; the concept is
  "Kampagne".
- Fonts are system stacks; the mock's Fraunces and IBM Plex Mono are not
  bundled. `ui/theme/AppFonts.kt` documents the three-step swap.

## Localisation notes

Three things here are not the usual string extraction:

**Date patterns are resources, not constants.** `format_date_header` is
`EEEE d MMMM` in English and `EEEE, d. MMMM` in German — word order and
punctuation are part of the translation. Month and weekday names come from
the device locale on their own. See `ui/components/Formats.kt`.

**View models emit `UiMessage`, not `String`.** A view model naming a
resource id instead of holding a `Context` keeps it testable off-device,
and means a snackbar resolves in the language the user is reading *now*
rather than the one in force when the view model was built.

**"Tuesday evening" is one word in German.** `eod_eyebrow` is a format
string (`%1$s evening` / `%1$sabend`) rather than a concatenation at the
call site, which is the only way both languages come out right.
