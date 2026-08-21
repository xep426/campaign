# Decisions

Why the code looks the way it does. Most of these were arrived at by
getting them wrong first; the wrong versions are kept here because a
decision without its failure is just an opinion, and opinions get undone.

The reasoning also lives next to the code it governs — this is the map.

## The day boundary is the load-bearing idea

`CampaignDay.of(now, turnsAt)` is four lines and it deleted a Tomorrow
pane, a draft-and-confirm button, a separate end-of-day screen, and a rule
for what happened when those two disagreed.

All of that machinery existed to bridge one gap: you decide tomorrow's
three things in the evening, but the calendar insists it is still today. So
the app needed a second place to edit a day it was not in yet.

Move the boundary to the moment you actually decide and the gap closes. At
22:00 the list is simply empty and you fill it; what you write is "today's"
by definition, because today started two minutes ago. There is nothing to
switch between, because there is only ever one list.

**The cost, stated plainly:** after the turn, the day just ended is gone
from the app. A task finished at 22:15 cannot be ticked off. That is the
honest consequence of a hard boundary and the price of not having two
lists.

## One setting, two jobs

The same hour turns the list over *and* fires the notification. Splitting
them would let the app prompt you to choose tomorrow's three things while
still showing you today's — which is exactly the mismatch the second screen
existed to paper over.

## The planning window

Between the turn and midnight the campaign day is ahead of the wall clock.
In that window the three are a plan, not a checklist: no completion marks,
and a tap edits. Recording "done" on a day that has not begun is a claim
the app should not offer to make.

This needed the view model to carry *two* dates. At midnight only the wall
date changes — the campaign day stays put — so a single date would have
left the screen stuck in planning mode with nothing to signal the change.

## Dark only

The centre of gravity is a prompt at 22:00 in a dim room. A light theme
would put a white rectangle in your face at exactly that moment. The
corollary is a rule: **no `values-night` resources anywhere**, because
anything resolving against the system's night flag could only introduce a
second, contradicting palette.

## The evening prompt is inexact, and that is Play's doing

It shipped inexact, on the reasoning that "the day is over" needn't land on
the minute. A live `dumpsys alarm` disproved that: the platform gave the
21:00 alarm a delivery window of a **full hour**.

Exact scheduling was the fix — and Google Play took it back. The permission
is restricted to apps whose core function is an exact alarm: clocks,
timers, calendars showing event notifications. A tasks app is none of
those, and listings that declare it are refused. It was fine while this was
a sideloaded personal build; publishing reverses that.

Correctness does not depend on it. The day turns because `CampaignDay`
reads the clock, not because an alarm fired — only the prompt can be late.
If punctuality ever has to be bought back, `setAlarmClock` is exact without
the restricted permission, at the price of a permanent alarm icon in the
status bar.

## Opening the app must not cancel the prompt

Every launch called `reschedule()`, which asked for the next 22:00 — and at
22:04, with the alarm still pending inside its window, "next" is tomorrow.
The pending alarm was replaced and that evening's prompt was lost.

Worse, it was self-reinforcing: a late notification makes you open the app
to see why, and opening the app is what guaranteed it would never come.
`EndOfDayScheduler` now leaves a live alarm alone unless the caller has a
reason to override it, and `isStillDeliverable` is a pure function with
tests because it is the rule that failed.

## The widget

**It toggles tasks, and the row is the target.** It shipped view-only on
the argument that a completion target millimetres from a launcher icon gets
tapped by accident. The worry was right and the conclusion was wrong — the
fix is a target big enough to hit on purpose. Measured on the 4×2 slot One
UI hands over — 429.7×233.1dp, not the 250×110 the provider declares — a
row is ~59dp and the full width of the card. A 14dp mark would sit far
under Android's 48dp minimum; the row clears it.

The figure to distrust here is a guessed one. This paragraph first said
58dp from memory, was then "corrected" to 40dp on the strength of a code
comment, and only settled once `dumpsys appwidget` gave the real slot and
[widget-study.html](widget-study.html) computed the row from it.

**It reads the same day rule as the app.** It did not, once: the widget
kept using `LocalDate.now()` after the app moved its boundary, so between
22:00 and midnight the card showed the day that had just ended while the
screen showed the new one. Two surfaces, two definitions of "today", and
the widget is the one people trust because they see it without asking.

**During the planning window it shows no marks and a tap opens the app.**
`ToggleTaskAction` refuses as well, because a card rendered before the turn
still carries toggle actions in its RemoteViews and the launcher will fire
one minutes later.

**Size is the design.** Every dimension comes from the slot the launcher
actually gave us. A layout of constants would draw the same small card in a
bigger box, which reads as broken rather than big. The rules are
portable enough to model outside the app: [widget-study.html](widget-study.html)
runs the same arithmetic in a browser, which is where the card is now
designed rather than by rebuilding the APK.

**The tap redraws the card itself** rather than trusting the repository's
refresh, which loses a race against Samsung's Freecess freezing the app
process — observed in logcat, with the write landing in the database while
the card kept the old state.

**And the widget collects its data inside the composition.** This was the
bug that took three wrong fixes: loading slots in `provideGlance` and
closing over the result gives the composition a snapshot, and Glance keeps
a session alive per widget, so a redraw recomposes that same captured
value. The card faithfully redrew identical content while the database had
already changed. It only appeared to work in testing because a killed
process forced `provideGlance` to re-run.

## Reordering hands over during composition, not in an effect

A drop cannot commit and reset in the same breath — the write is
asynchronous, so the rows fall back to their old places until the database
answers, then swap again. Nor can the local order simply be kept: once the
data *has* swapped, applying the permutation on top of it reads as the drag
being undone.

So `ReorderableColumn` holds the permutation and drops it the instant the
data supersedes it — decided **during composition** by comparing keys. In a
`LaunchedEffect` it runs one frame late, and one frame of the old order is
still a visible flicker.

The last cause was subtler still: `pointerInput` is keyed on the item count
alone, so the gesture lambdas kept the `keys` from first composition.
`rememberUpdatedState` is what makes them read the living values.

## The index on (date, slot) is plain, not unique

It was unique on the theory that the schema should enforce the limit of
three. It never did — there is no CHECK on `slot`, so the database would
accept `slot = 7` without complaint. That limit lives in
`TaskRepositoryImpl` and always did.

What uniqueness *did* do was make reordering absurd: SQLite checks the
index per row, so every reorder had to park all three rows on negative
slots and write the real values in a second pass — negative slots the
unique index accepted quite happily, which rather made the point.

## A campaign shows finished work, not outstanding work

The card listed open tasks, on the reasoning that a campaign IS its
outstanding work. That put the same information in two places: an open task
that matters is one of today's three, and Today is where those are read.
Meanwhile the thing no other screen recorded — how far a long effort had
actually come — was reduced to a number in the metadata line.

So the card shows completed steps, newest first, and the count that used to
be a separate `COUNT(*)` group-by is now just `doneTasks.size`. A count and
the rows it summarises are two things that can disagree; one query cannot.

**Collapsed by default**, because the honest arithmetic is thirty campaigns
with twenty finished steps each — six hundred lines, and a screen that
opens on six hundred lines is not an overview. The open set is keyed by
campaign id rather than list index: the list reorders as campaigns are
completed, and an index would carry the open state onto whichever campaign
slid into that place.

Carrying work forward moved entirely to Today, where the picker already
existed and where the three slots it competes for are visible. The campaign
card was the only other way to pull a task, so this had to be checked
before the list came out rather than after.

## Archiving is gone; completing is the only way to close

ARCHIVED and COMPLETED both closed a campaign, and §8 asked for "archive or
complete". In practice they wrote the same row with a different word in it:
same removal from the list, same place in History, same reversibility, and
the history tally counted both as "finished" regardless. The only
difference a user could ever see was the colour of a tag.

Two buttons side by side that differ only by a label are a question the
screen asks and never answers — and it was asked, which is how this came
up. Deleting already means "this should not be in my history".

Dropping the enum value needed a migration, and not for the obvious reason:
`toDomain` maps an unparseable status to ACTIVE on purpose, so without one,
every archived campaign would have come back to life in the active list on
first launch. Silently, with its `closedAt` still set. `MIGRATION_2_3`
rewrites them to COMPLETED.

## A campaign authors nothing

The first version gave campaigns a `nextStep` field you had to write and
maintain, which turned out to be a second place to plan the same work: you
wrote the step on the campaign, then wrote it again as a task. A campaign
authors nothing at all: it has a name, and it has the tasks assigned to it.

Creating one from a task **asks for its name** rather than inheriting the
task's. A task is a step; the campaign is the larger effort it serves.
"Update my address" is not the campaign — "sell the property" is.
Inheriting produced campaigns that had to be renamed immediately, every
time.

## Carrying a task forward moves it

One task is slotted to one day at a time. The day it came from stops
listing it, which is the cost of not accumulating duplicates — and the
reason History records finished work rather than a day-by-day ledger. Once
tasks move, "what did I choose on Tuesday" is not a question the data can
answer.

## Text fields: three ways out, one outcome

Enter, tapping elsewhere, and the field simply going away all keep what was
typed. The third is the one that bites — it produces no focus event — and
Compose does not clear focus on a tap elsewhere by itself, so the screen
has to do it. A flag makes the commit idempotent, since focus loss and
disposal usually both fire.

## Conventions

- Comments explain **why**, not what. Several here exist specifically to
  stop a future reader undoing a fix.
- Colours and fonts come from `ui/theme` tokens. No raw hex and no
  `FontFamily.Serif` anywhere else. The handful of values in `colors.xml`
  are the exception — a drawable cannot read a Compose `Color`.
- Date **patterns** are localised, not just text: `EEEE d MMMM` against
  `EEEE, d. MMMM`. A hardcoded pattern reads as broken German.
- View models emit `UiMessage` (a resource id plus args) rather than
  `String`, so they hold no `Context` and a snackbar resolves in the
  language being read now.
- Fonts are system stacks. `ui/theme/AppFonts.kt` documents the three-step
  swap to bundled Fraunces and IBM Plex Mono.
- `campaigns.nextStep` is a dead column, kept deliberately: dropping a
  column in SQLite means recreating the table, and there is real user data.
  Remove it the next time the schema changes for a reason that already
  needs a migration.
