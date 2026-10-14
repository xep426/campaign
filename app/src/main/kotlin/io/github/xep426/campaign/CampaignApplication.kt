package io.github.xep426.campaign

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Nothing to do on start any more.
 *
 * It used to create a notification channel and re-book the evening alarm
 * on every launch, because the alarm chain was one link at a time and
 * anything that broke a link — a force-stop, a battery optimiser, an OEM's
 * idea of housekeeping — ended the ritual silently. Campaigns do not roll
 * over at an hour, so there is no evening to announce and no chain to
 * keep alive.
 */
@HiltAndroidApp
class CampaignApplication : Application()
