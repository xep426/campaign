package io.github.xep426.campaign

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.xep426.campaign.ui.ThemeViewModel
import io.github.xep426.campaign.ui.UiMessage
import io.github.xep426.campaign.ui.components.Hairline
import io.github.xep426.campaign.ui.campaigns.CampaignsScreen
import io.github.xep426.campaign.ui.campaigns.CampaignsViewModel
import io.github.xep426.campaign.ui.history.HistoryScreen
import io.github.xep426.campaign.ui.history.HistoryViewModel
import io.github.xep426.campaign.ui.theme.AppColors
import io.github.xep426.campaign.ui.theme.CampaignTheme
import io.github.xep426.campaign.ui.theme.DaylightPalette
import io.github.xep426.campaign.ui.theme.DuskPalette
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.today.TodayScreen
import io.github.xep426.campaign.ui.today.TodayViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private enum class Tab(@param:StringRes val labelRes: Int) {
    TODAY(R.string.tab_today),
    CAMPAIGNS(R.string.tab_campaigns),
    HISTORY(R.string.tab_history),
}

/**
 * One shell, three tabs, no modal screens.
 *
 * The end-of-day screen used to live here as a full-screen route the
 * notification opened. It is gone: once the day turns at the user's chosen
 * hour, the Tasks tab IS the empty list waiting to be filled, so the
 * notification only has to open the app. See
 * [io.github.xep426.campaign.domain.model.CampaignDay].
 */
@Composable
fun CampaignApp() {
    val theme: ThemeViewModel = hiltViewModel()
    val dark by theme.dark.collectAsStateWithLifecycle()

    CampaignTheme(palette = if (dark) DuskPalette else DaylightPalette) {
        MainShell(dark = dark, onToggleTheme = theme::toggle)
    }
}

@Composable
private fun MainShell(dark: Boolean, onToggleTheme: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.TODAY) }
    val snackbars = remember { SnackbarHostState() }

    Scaffold(
        containerColor = AppColors.ink,
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = { BottomBar(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            when (tab) {
                Tab.TODAY -> {
                    val vm: TodayViewModel = hiltViewModel()
                    val state by vm.ui.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(Unit) {
                        vm.refreshDate()
                        onPauseOrDispose { }
                    }
                    Messages(vm.messages, snackbars)

                    TodayScreen(
                        state = state,
                        onToggle = vm::toggle,
                        onSetSlot = vm::setSlot,
                        onDelete = vm::delete,
                        onAssign = vm::assign,
                        onPromote = vm::promote,
                        onUnassign = vm::unassign,
                        onCarry = vm::carryForward,
                        onSetTurnTime = vm::setTurnTime,
                        onMoveSlot = vm::moveSlot,
                    )
                }

                Tab.CAMPAIGNS -> {
                    val vm: CampaignsViewModel = hiltViewModel()
                    val state by vm.ui.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(Unit) {
                        vm.refreshDate()
                        onPauseOrDispose { }
                    }
                    Messages(vm.messages, snackbars)

                    CampaignsScreen(
                        state = state,
                        onRename = vm::rename,
                        onSetNotes = vm::setNotes,
                        onComplete = vm::complete,
                        onCreate = vm::create,
                        onDelete = vm::delete,
                    )
                }

                Tab.HISTORY -> {
                    val vm: HistoryViewModel = hiltViewModel()
                    val state by vm.ui.collectAsStateWithLifecycle()

                    HistoryScreen(state = state, onReopen = vm::reopen)
                }
            }

            // Above the screens rather than inside one, because it governs
            // all three. Aligned to the top-right corner, where the screens
            // keep their margin free — every tab starts with an eyebrow on
            // the left and nothing on the right.
            ThemeToggle(
                dark = dark,
                onClick = onToggleTheme,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Dusk or daylight, as one glyph.
 *
 * A half-filled circle rather than a sun or a moon: those need two icons
 * and a decision about whether the icon shows the state or the action,
 * which is the classic way a theme toggle ends up meaning the opposite of
 * what a user reads. A half circle just flips, and the filled half says
 * which side is currently dark.
 *
 * Text rather than a drawable, like the ⋯ menu and the widget's ›. The
 * app draws its small controls with glyphs and this is one of them.
 */
@Composable
private fun ThemeToggle(dark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (dark) "◐" else "◑",
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.muted,
        modifier = modifier
            // Padding inside the clickable, so the target is bigger than
            // the glyph — it sits in a screen corner where a near-miss
            // would otherwise land on the title.
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun Messages(messages: Flow<UiMessage>, host: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(messages, context) {
        messages.collect { message ->
            scope.launch {
                host.currentSnackbarData?.dismiss()
                host.showSnackbar(message.resolve(context))
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.background(AppColors.ink)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                val tint by animateColorAsState(
                    targetValue = if (on) AppColors.ember else AppColors.muted,
                    animationSpec = tween(220),
                    label = "tab",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabIcon(tab, tint)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(tab.labelRes).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                    )
                }
            }
        }
    }
}

/**
 * Drawn rather than imported: three dots for the three slots, a standard
 * for a campaign, ruled lines for the record. Material's icon set has no
 * mark that means "exactly three things", and that is the one idea the
 * whole app is about.
 */
@Composable
private fun TabIcon(tab: Tab, tint: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(19.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()

        when (tab) {
            Tab.TODAY -> {
                listOf(0.22f, 0.5f, 0.78f).forEach { y ->
                    drawCircle(tint, radius = 1.7.dp.toPx(), center = Offset(w / 2f, h * y))
                }
            }

            Tab.CAMPAIGNS -> {
                drawLine(
                    tint,
                    Offset(w * 0.24f, h * 0.10f),
                    Offset(w * 0.24f, h * 0.92f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                // The pennant: out, notch back, out again.
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.24f, h * 0.18f)
                    lineTo(w * 0.84f, h * 0.18f)
                    lineTo(w * 0.66f, h * 0.38f)
                    lineTo(w * 0.84f, h * 0.58f)
                    lineTo(w * 0.24f, h * 0.58f)
                }
                drawPath(
                    path,
                    tint,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                    ),
                )
            }

            Tab.HISTORY -> {
                listOf(0.24f to 0.86f, 0.5f to 0.86f, 0.76f to 0.55f).forEach { (y, len) ->
                    drawLine(
                        tint,
                        Offset(w * 0.12f, h * y),
                        Offset(w * (0.12f + len * 0.88f), h * y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
