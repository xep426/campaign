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
import io.github.xep426.campaign.ui.UiMessage
import io.github.xep426.campaign.ui.components.Hairline
import io.github.xep426.campaign.ui.campaigns.CampaignsScreen
import io.github.xep426.campaign.ui.campaigns.CampaignsViewModel
import io.github.xep426.campaign.ui.history.HistoryScreen
import io.github.xep426.campaign.ui.history.HistoryViewModel
import io.github.xep426.campaign.ui.theme.CampaignTheme
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.Ink
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.Muted
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
    CampaignTheme {
        MainShell()
    }
}

@Composable
private fun MainShell() {
    var tab by remember { mutableStateOf(Tab.TODAY) }
    val snackbars = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Ink,
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
                        onCarry = vm::carryForward,
                        onRename = vm::rename,
                        onSetNotes = vm::setNotes,
                        onClose = vm::close,
                        onCreate = vm::create,
                        onDelete = vm::delete,
                    )
                }

                Tab.HISTORY -> {
                    val vm: HistoryViewModel = hiltViewModel()
                    val state by vm.ui.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(Unit) {
                        vm.refreshDate()
                        onPauseOrDispose { }
                    }

                    HistoryScreen(state = state, onReopen = vm::reopen)
                }
            }
        }
    }
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
    Column(Modifier.background(Ink)) {
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
                    targetValue = if (on) Ember else Muted,
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
