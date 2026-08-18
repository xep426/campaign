# Kalimetra R8/ProGuard rules (applied to release builds, minify on).
#
# Most of the stack ships its own consumer rules inside the AARs:
#   - Room (KSP-generated code, no reflection on entities)
#   - Hilt/Dagger (generated code, largely automatic)
#   - Compose
#   - OkHttp/Okio (consumer rules bundled)
# Only project-specific gaps and standard dontwarns live here.

# ── Kotlin metadata ─────────────────────────────────────────────────
# Keep @Metadata so libraries that reflect over Kotlin declarations
# (nullability, default args) keep working after shrinking.
-keep class kotlin.Metadata { *; }

# ── kotlinx.coroutines ──────────────────────────────────────────────
# Standard rules: ServiceLoader entries + optional debug/agent classes.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.debug.**

# ── OkHttp / Okio ───────────────────────────────────────────────────
# Consumer rules are bundled with the libraries; these silence warnings
# about optional platform integrations that are not on the classpath.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── org.json ────────────────────────────────────────────────────────
# Provided by the Android platform (used by ClaudeFoodParser) — nothing
# to keep or dontwarn.

# ── Enums revived by name ───────────────────────────────────────────
# ThemeMode / TdeeMethod (and any future com.kalimetra enum)
# are persisted as strings and restored via Enum.valueOf()/`.name`.
# R8 must neither strip nor rename their constants.
-keepclassmembers enum com.kalimetra.** { *; }
