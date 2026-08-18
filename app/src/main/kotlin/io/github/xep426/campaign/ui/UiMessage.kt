package io.github.xep426.campaign.ui

import android.content.Context
import androidx.annotation.StringRes

/**
 * A message a view model wants shown, as a resource id plus its arguments.
 *
 * View models must not hold a Context — it would tie them to a
 * configuration and make them untestable off-device — but they are still
 * the layer that knows *what* happened. So they name the sentence and let
 * the composition resolve it, which also means a message picks up the
 * right language even if the locale changed after the view model was
 * created.
 */
data class UiMessage(
    @param:StringRes val id: Int,
    val args: List<Any> = emptyList(),
) {
    fun resolve(context: Context): String =
        if (args.isEmpty()) context.getString(id)
        else context.getString(id, *args.toTypedArray())
}

fun uiMessage(@StringRes id: Int, vararg args: Any) = UiMessage(id, args.toList())
