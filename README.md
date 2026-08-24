# Campaign

**Three things a day. There is nowhere to put a fourth.**

An Android app for choosing what matters instead of collecting what might.
Everything stays on the device — no account, no sync, no server.

## Why

Most task apps are excellent at capture and useless at choosing. They make
it frictionless to write everything down, so you do, and then you carry a
list you will never finish. The list grows, the important things sink, and
opening the app starts to feel like being handed a bill.

You usually already know which two or three things would actually move you
forward this week. Nothing in a normal to-do app makes you say so.

Campaign is built around that one act. It gives you exactly three slots a
day and no way to add a fourth. Choosing what goes in them is the work; the
app has no other feature to distract from it.

## How it works

**Three slots, and the day empties them.** At an hour you pick — 22:00 by
default — the list clears and you choose again. Nothing rolls over on its
own. If something mattered yesterday and you did not do it, you have to
decide, today, whether it still matters enough to spend one of three slots
on. That decision is the entire point.

**The evening is when you decide.** Because the day turns at 22:00 rather
than at midnight, the app spends those hours knowing the list belongs to
tomorrow. It says *"Three things for tomorrow."*, hides the completion
marks, and a tap edits rather than completes. You cannot tick off a day
that has not started, so it does not offer to let you pretend.

**Campaigns are for what does not fit in a day.** Selling a flat, learning
a language, fixing the damp — assign a task to a campaign and it stops
being a loose end. A campaign is not something you write and maintain: it
keeps the record of the steps you actually finished, so you can see how far
a long effort has come. Campaigns are the only way work survives a day
boundary, which is what keeps the daily list honest.

## What it refuses to do

No backlog. No inbox. No tags, folders, projects or priorities. No streaks,
points, badges or charts. No fourth slot. No account, no cloud, no
telemetry.

Every one of these is a deliberate absence. A tool that can hold everything
you might do is a tool that never makes you say what you will do.

There is exactly one number, and it lives in a one-line strip at the foot
of Today: efficiency over the last thirty days — the slots you spent
against the slots there were. It counts against three a day because that is
the premise: the three are the most important things available to you, so
an empty one is capacity that went unused. It shows the fraction beside the
percentage ("37/60 · 62%"), because a percentage with its arithmetic hidden
is how a tally turns into a scold. It is a footer, not a headline; the
three things are the screen.

## Screens

| Screen | |
| --- | --- |
| **Today** | The three slots. Tap to complete, drag to reorder, ⋯ to edit, assign or delete |
| **Campaigns** | Longer efforts, each keeping the record of what you finished |
| **History** | Campaigns you finished, and one-off tasks that belonged to none |
| **Widget** | Today's three on the home screen, tappable |

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
