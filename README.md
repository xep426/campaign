# Campaign

**Three things at a time. The fourth has to wait.**

An Android app for the undertakings that lose to everyday life — the
hobby, the side project, the thing that has been half-done since spring.
Everything stays on the device: no account, no sync, no server.

## Why

Some things never lose to a better plan. They lose because they are never
the most urgent thing on any given day — and a to-do list is a machine for
surfacing the most urgent thing. Learn the language, fix the damp, finish
the boat: none of it is due, so none of it ever wins.

Campaign refuses to hold more than three of them at once. You break an
undertaking into steps and it shows you ONE — the next one — until you do
it. No deadline, no daily reset, no expiry. It moves at whatever speed you
move it, and it is still there when you come back.

The limit is the whole design. Anything may be written down; only three
things may be in front of you.

## How it works

**A campaign is an undertaking with its steps written down.** Name the
thing, list what it takes, reorder it when the order turns out to be
wrong. Nothing here is due on a date.

**Three of them can be pushed at once.** Pin a campaign and it takes one
of three places; the rest wait. Each pinned campaign shows only its next
open step, because the next step is the only one you can act on and
showing the rest would rebuild the backlog this app exists without.

**Finishing a step reveals the one behind it.** That is the whole loop. A
twenty-step campaign is twenty taps spread over however long it takes, and
the app never asks for more than the next one.

**It counts the silence, not the streak.** A campaign that has not moved
for a while says so — "nothing for 23 days" — and says it louder the
longer it goes. That is the only pressure in the app, and it replaced a
nightly reset that used to throw unfinished work away.

## What it refuses to do

No backlog. No inbox. No tags, folders, projects or priorities. No streaks,
points, badges or charts. No fourth slot. No account, no cloud, no
telemetry.

Every one of these is a deliberate absence. A tool that can hold everything
you might do is a tool that never makes you say what you will do.

There is exactly one number, and it is a reproach rather than a score:
days since a campaign last moved. It replaced a percentage that measured
how full your days were, which stopped meaning anything once days stopped
being the unit.

## Screens

| Screen | |
| --- | --- |
| **Undertakings** | The three pinned campaigns, each showing its next step |
| **Campaigns** | All of them: build the step list, reorder it, pin and unpin |
| **History** | Campaigns you finished, and one-off tasks that belonged to none |
| **Widget** | The same three on the home screen; tap to finish a step |

English and German, informal *du*. Two themes, dusk and daylight, on a
toggle in the top-right corner. Dusk is the default and stays the point:
the app's centre of gravity is a prompt at 22:00 in a dim room.

## Install

Open in Android Studio and run, or:

```bash
./gradlew :app:installDebug
```

Requires an SDK with **compileSdk 36**; minSdk is 26. `local.properties`
points at your SDK and is untracked.

## Under the hood

Kotlin, Jetpack Compose, Room, Hilt, Glance for the widget. One module;
packages carry the shape a split would use (`domain`, `data`, `ui`,
`widget`, `notify`, `di`).

The interesting decisions — the day boundary that removed three screens,
why the widget's row is the tap target, why reordering hands over to the
data during composition — are written up in
[docs/decisions.md](docs/decisions.md), along with the ones that were only
arrived at by getting them wrong first.

[docs/widget-study.html](docs/widget-study.html) is a working model of the
home-screen widget: it runs the real sizing rules in a browser, so the card
can be resized and restyled without rebuilding the app.

[docs/design-study.html](docs/design-study.html) is the HTML mock the app
was built from. It is kept as the original artefact and is **not current**.

## Licence

MIT. See [LICENSE](LICENSE).
