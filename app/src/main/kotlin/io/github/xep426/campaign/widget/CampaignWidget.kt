package io.github.xep426.campaign.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.xep426.campaign.MainActivity
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.EmberDeep
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Today's three slots on the home screen.
 *
 * INTERACTIVE, which answers the PRD's second open question — and the
 * shape of the answer matters more than the yes.
 *
 * This shipped view-only first, on the argument that a completion target
 * millimetres from a launcher icon gets tapped by accident. That worry was
 * right and the conclusion was wrong: the fix is a target big enough to
 * hit on purpose, not no target at all. On the 4×2 slot One UI actually
 * hands over — 429.7×233.1dp, measured, not the 250×110 the provider
 * declares — each row is ~59dp. A 14dp mark would sit far under
 * Android's 48dp minimum; the row clears it comfortably.
 *
 * At the DECLARED minimum the row is ~30dp and does not clear it. That
 * size only occurs below API 31, where targetCell* is ignored, and there
 * the header is dropped too (see HEADER_FLOOR) — a card already in its
 * degraded shape. Worth knowing rather than worth designing around.
 *
 * Hence the division:
 *  - a filled row TOGGLES it, exactly as tapping a row does on Today. Two
 *    surfaces, one grammar; nobody has to learn the widget separately.
 *  - the header OPENS the app — the natural "go there" affordance, and the
 *    one part of the card that is not a task.
 *  - an empty row opens the app too, so the unchosen slot stays reachable.
 *
 * EVERY TARGET IS ANNOUNCED, which is a later correction and the more
 * important half. The division above is sound and was invisible: two rows
 * that behave differently looked identical, so the only way to find out
 * what a tap did was to make it. Now the header carries a chevron and a
 * rule beneath it, a filled row carries its mark, and an empty row carries
 * a plus. The rule is that nothing on this card is tappable without
 * saying so — and the header, being the way out, says it loudest.
 *
 * What this costs is the tick animation: RemoteViews cannot draw a path,
 * so CompletionMark's self-drawing check becomes an instant swap out here.
 * That is a real loss on the app's one flourish, and it is the price of
 * the act being possible without unlocking into the app at all.
 *
 * SIZE IS THE DESIGN HERE. The card defaults to 4×2 because at 3×2 it read
 * as a note pinned to the home screen rather than as the day's three
 * things — and this app has exactly one surface competing with a wall of
 * app icons for the user's attention. Every dimension below comes from
 * [WidgetMetrics], derived from the slot the launcher actually gave us; a
 * layout of constants would draw the same small card in a big box, which
 * is the failure that makes a resized widget look broken rather than big.
 *
 * NO GlanceTheme, and no values-night resource anywhere near this file.
 * Campaign has exactly one palette (see [io.github.xep426.campaign.ui.theme]), so
 * anything resolving against the system's night flag could only introduce
 * a second one that disagrees.
 */
class CampaignWidget : GlanceAppWidget() {

    /**
     * [SizeMode.Exact], so [LocalSize] reports the slot the launcher
     * handed over rather than the provider's declared minimum. Without it
     * [WidgetMetrics] would solve every card against the same numbers and
     * the scaling below would be decoration.
     */
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The app's day, not the calendar's — after the turn hour this is
        // already tomorrow's date. Using LocalDate.now() here is what made
        // the card disagree with the screen between 22:00 and midnight.
        val whichDay = runCatching { currentCampaignDay(context) }
            .getOrElse { WidgetDay(LocalDate.now(), isPlanning = false) }
        val today = whichDay.date

        // A widget that cannot read the database still has to draw
        // something, and three empty slots is the honest answer — it is
        // what an untouched day looks like anyway.
        val empty = List<WidgetSlot?>(DailyTask.SLOTS_PER_DAY) { null }
        val firstPaint = runCatching { loadTodaySlots(context, today) }.getOrDefault(empty)
        val live = runCatching { todaySlotsFlow(context, today) }.getOrDefault(flowOf(empty))

        // Resolved here, not in composition: a Glance widget has no
        // Compose resource environment, so every string and the date
        // PATTERN itself (German reads "Mi., 19. Aug") come off the
        // context before content is provided.
        val dateFormat = DateTimeFormatter.ofPattern(
            context.getString(R.string.format_widget_date)
        )
        val dateLabel = today.format(dateFormat).uppercase(Locale.getDefault())
        val title = context.getString(R.string.widget_title)
        val emptyLabel = context.getString(R.string.widget_empty_slot)

        provideContent {
            // Collected HERE, not captured from above. A snapshot taken in
            // provideGlance is frozen for the life of the Glance session,
            // so every recomposition redraws the same content and a tap on
            // a warm widget appears to do nothing — see todaySlotsFlow.
            //
            // firstPaint seeds it so the card never flashes three empty
            // slots on the way to showing the real ones.
            val slots by live.collectAsState(initial = firstPaint)

            WidgetCard(
                title = title,
                dateLabel = dateLabel,
                emptyLabel = emptyLabel,
                slots = slots,
                planning = whichDay.isPlanning,
                metrics = WidgetMetrics.forSlot(LocalSize.current),
            )
        }
    }

    @Composable
    private fun WidgetCard(
        title: String,
        dateLabel: String,
        emptyLabel: String,
        slots: List<WidgetSlot?>,
        planning: Boolean,
        metrics: WidgetMetrics,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                // A drawable rather than GlanceModifier.cornerRadius, which
                // needs API 31 while minSdk here is 26.
                .background(ImageProvider(R.drawable.widget_background))
                .padding(
                    horizontal = metrics.horizontalPadding,
                    vertical = metrics.verticalPadding,
                )
            // No card-wide click any more: it would swallow the row taps
            // below it, and every pixel of this card now belongs to a more
            // specific action than "open the app".
        ) {
            // Dropped entirely on a card too short to hold it — see
            // HEADER_FLOOR. Without the header there is no "open the app"
            // target left, so the empty-slot rows carry that alone; a card
            // this small with three tasks set is a card you act on, not one
            // you navigate from.
            if (metrics.showHeader) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(Paper),
                            fontSize = metrics.titleSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    // The only mark on the card that means "this way out".
                    //
                    // Without it the header is a label, and a label does not
                    // read as tappable — which left no way to tell a row that
                    // completes a task from one that opens the app. Every
                    // target on this card is now announced: the chevron here,
                    // the mark on a filled row, the plus on an empty one.
                    Spacer(modifier = GlanceModifier.width(metrics.chevronGap))
                    Text(
                        text = "›",
                        style = TextStyle(
                            color = ColorProvider(Ember),
                            fontSize = metrics.titleSize,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = dateLabel,
                        style = TextStyle(
                            color = ColorProvider(Ember),
                            fontSize = metrics.dateSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                // A rule under the header, so it reads as a band with its own
                // job rather than as the first line of the list. The gap is
                // split around it rather than added to, which keeps the header
                // block the same height as before the rule existed.
                Spacer(modifier = GlanceModifier.height(metrics.headerGap * 0.45f))
                Spacer(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorProvider(HAIRLINE))
                )
                Spacer(modifier = GlanceModifier.height(metrics.headerGap * 0.55f))
            }

            // Always three rows, filled or not. A widget that shrank to the
            // number of tasks set would change height under the user's other
            // icons every evening — and the empty slot is information: it
            // says the third thing is still unchosen.
            //
            // Each row takes defaultWeight, so the three of them SHARE the
            // leftover height instead of stacking at the top. That is what
            // turns a taller card into more air between the tasks rather
            // than a block of text with a blank half beneath it.
            slots.forEachIndexed { index, slot ->
                if (index > 0) {
                    Spacer(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProvider(HAIRLINE))
                    )
                }
                SlotRow(
                    slot = slot,
                    index = index,
                    emptyLabel = emptyLabel,
                    planning = planning,
                    metrics = metrics,
                    // defaultWeight() is a ColumnScope extension, so the
                    // share of the height has to be handed to the row from
                    // here rather than claimed inside it.
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            }
        }
    }

    @Composable
    private fun SlotRow(
        slot: WidgetSlot?,
        index: Int,
        emptyLabel: String,
        planning: Boolean,
        metrics: WidgetMetrics,
        modifier: GlanceModifier,
    ) {
        Row(
            modifier = modifier
                .padding(vertical = metrics.rowPadding)
                // The whole row, not the mark. The mark is 13–18dp; the row
                // is the full width of the card and ~40dp tall even at 4×2,
                // which is the difference between a target you hit and one
                // you hit sometimes.
                //
                // Before the day starts a tap opens the app instead of
                // completing: the row is a plan, and the only useful thing
                // to do with a plan is edit it — which needs a keyboard the
                // home screen does not have.
                .clickable(
                    if (slot == null || planning) {
                        actionStartActivity<MainActivity>()
                    } else {
                        actionRunCallback<ToggleTaskAction>(
                            actionParametersOf(ToggleTaskAction.TASK_ID to slot.id)
                        )
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The roman numeral appears only once the card is wide enough
            // to give it a gutter without squeezing the title. It is the
            // app's signature mark, and a cramped one would read as noise
            // rather than as "this is the second of three".
            if (metrics.showNumerals) {
                Text(
                    text = DailyTask.numeralFor(index),
                    style = TextStyle(
                        color = ColorProvider(if (slot == null) Muted else EmberDeep),
                        fontSize = metrics.numeralSize,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = GlanceModifier.width(metrics.numeralWidth),
                )
            }

            // Absent while the day has not started — same as in the app.
            // A checkbox that cannot be checked is worse than no checkbox:
            // it invites the tap and then refuses it.
            if (!planning) {
                // An empty slot gets a plus, not an empty ring. The ring says
                // "not done yet" and invites a tap that would complete
                // something; the plus says the row leads into the app, which
                // is the only thing an unchosen slot can do.
                if (slot == null) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = ColorProvider(Muted),
                            fontSize = metrics.markSize.value.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = GlanceModifier.width(metrics.markSize),
                    )
                } else {
                    Image(
                        provider = ImageProvider(
                            if (slot.completed) R.drawable.widget_dot_done
                            else R.drawable.widget_dot_open
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier.size(metrics.markSize),
                    )
                }
                Spacer(modifier = GlanceModifier.width(metrics.markGap))
            }

            Text(
                text = slot?.title ?: emptyLabel,
                maxLines = metrics.maxLines,
                style = TextStyle(
                    color = ColorProvider(
                        when {
                            slot == null -> Muted
                            slot.completed -> Muted
                            else -> Paper
                        }
                    ),
                    fontSize = metrics.taskSize,
                    fontWeight = FontWeight.Normal,
                    textDecoration =
                        if (slot?.completed == true) TextDecoration.LineThrough
                        else TextDecoration.None,
                ),
            )
        }
    }

    private companion object {
        /** Paper at ~6% — the same hairline the app draws between rows. */
        val HAIRLINE = androidx.compose.ui.graphics.Color(0x10F0E9DB)
    }
}

/**
 * Every dimension on the card, solved for the slot the launcher gave us.
 *
 * Two kinds of rule live here, and the difference matters:
 *
 *  - CONTINUOUS. Type sizes, padding and the mark scale with the card and
 *    are then clamped. A 4×3 slot gets bigger text because it has room for
 *    bigger text, not because 4×3 is special.
 *  - STRUCTURAL. Numerals and two-line titles switch on at a threshold,
 *    because they are not "the same thing, smaller" — a numeral squeezed
 *    into a 6dp gutter is not a small numeral, it is clutter, and a second
 *    line on a card too short for it just clips.
 *
 * The card is resizable down to roughly 3×2, so the small end is a real
 * configuration and not a theoretical one.
 */
private data class WidgetMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val headerGap: Dp,
    val rowPadding: Dp,
    val markSize: Dp,
    val markGap: Dp,
    val numeralWidth: Dp,
    val chevronGap: Dp,
    val titleSize: TextUnit,
    val dateSize: TextUnit,
    val taskSize: TextUnit,
    val numeralSize: TextUnit,
    val showNumerals: Boolean,
    val showHeader: Boolean,
    val maxLines: Int,
) {
    companion object {

        /**
         * Below this the numeral gutter comes out of the title's width, and
         * the title is the only thing on this card the user actually reads.
         *
         * Sits just under the 250dp that four columns declare, so a card at
         * its declared minimum still gets its numerals — a threshold that
         * trips at exactly the default size is a threshold in the wrong
         * place.
         */
        private val NUMERAL_FLOOR = 240.dp

        /**
         * A second line needs somewhere to put it, and Glance clips rather
         * than shrinks, so this floor is measured, not chosen: below it two
         * lines plus the padding exceed what a row is given.
         *
         * Measured in widget-study.html against the current type scale.
         * At 190dp the margin was 0.2dp — arithmetically a fit and
         * practically a coin toss. It only clears ~3dp at 205dp and
         * plateaus there, so that is where it sits. Raising the task size
         * moved this threshold up with it, which is the honest cost of
         * bigger text and the reason the number lives next to its
         * derivation.
         */
        private val TWO_LINE_FLOOR = 205.dp

        /**
         * Below this the header is dropped so the three rows are not clipped.
         *
         * Insurance, not a fix for an observed failure — worth saying,
         * because a comment claiming a bug it did not see is worse than no
         * comment. The arithmetic is the reason: at this floor the header
         * block plus three single-line rows leaves ~5dp spare. It was
         * 125dp until the type grew; there the margin fell to 1.8dp, so
         * the floor followed the type up.
         * One UI also scales placements by its own ratio
         * (`hsResizeRatio=0.83` on this device) and does not owe
         * minResizeHeight anything, so that margin is not guaranteed.
         *
         * If it ever does bind, tasks win over the title: losing
         * "Campaign · MO., 17. AUG." costs identity, losing the third task
         * costs the point.
         */
        private val HEADER_FLOOR = 135.dp

        fun forSlot(slot: DpSize): WidgetMetrics {
            val w = slot.width
            val h = slot.height

            // Task text drives the card: everything else is sized in
            // relation to it, so the type stays in proportion at any slot
            // rather than each figure drifting on its own ratio.
            //
            // The floor is not a safety margin — at two cells the ratio
            // wants ~8sp, and 15sp is the size below which the thing the
            // widget EXISTS to show stops being readable across a room.
            // Short cards therefore sit on the floor by design and buy the
            // room back out of padding instead.
            //
            // 0.058 with a 13sp floor came first, and it was too small on
            // real hardware: the ratio only reached its ceiling at ~293dp,
            // so the actual 4x2 slot (233dp) drew 13.5sp — the floor was
            // carrying the design and the ratio was decoration. The
            // ceiling is what two lines can afford at 233dp, which is
            // ~18.5sp; 21 is reachable only on a card tall enough to
            // spend it.
            val task = (h.value * 0.074f).coerceIn(15f, 21f)

            return WidgetMetrics(
                horizontalPadding = (w * 0.05f).coerceIn(14.dp, 22.dp),
                // Floors chosen against the WORST case, not the expected
                // one: 70·n − 30 puts two rows at 110dp on a launcher with
                // short cells, and at 13sp text the header plus three rows
                // need ~107dp of that. Anything more generous here clips
                // instead of compressing, because Glance has no way to
                // shrink text to fit. Real phones hand over half again as
                // much and the slack goes to the rows' defaultWeight.
                verticalPadding = (h * 0.055f).coerceIn(8.dp, 20.dp),
                headerGap = (h * 0.045f).coerceIn(7.dp, 18.dp),
                rowPadding = (h * 0.018f).coerceIn(2.dp, 10.dp),
                markSize = (task * 1.05f).dp.coerceIn(14.dp, 20.dp),
                markGap = (task * 0.72f).dp.coerceIn(9.dp, 16.dp),
                numeralWidth = (task * 1.5f).dp.coerceIn(19.dp, 32.dp),
                chevronGap = (task * 0.5f).dp.coerceIn(6.dp, 11.dp),
                // The header carries the card's identity and its one exit, so it
                // is deliberately larger than the tasks rather than a shade
                // above them: at 1.08 it read as another list row.
                titleSize = (task * 1.32f).sp,
                // 9sp was legible on a bench and not across a room. The floor
                // matters more than the ratio here — the date is the shortest
                // string on the card and the first to become decoration.
                dateSize = (task * 0.92f).coerceAtLeast(11f).sp,
                taskSize = task.sp,
                numeralSize = (task * 0.86f).sp,
                showNumerals = w >= NUMERAL_FLOOR,
                showHeader = h >= HEADER_FLOOR,
                maxLines = if (h >= TWO_LINE_FLOOR) 2 else 1,
            )
        }
    }
}
