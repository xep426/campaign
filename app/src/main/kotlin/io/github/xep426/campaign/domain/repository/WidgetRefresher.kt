package io.github.xep426.campaign.domain.repository

/**
 * Port for poking the home-screen widget after a write. The widget package
 * binds the Glance-backed implementation; repositories fire it without
 * ever seeing Glance — which is why this stays a bare `refresh()` with no
 * widget identity in the signature.
 */
fun interface WidgetRefresher {
    suspend fun refresh()
}
