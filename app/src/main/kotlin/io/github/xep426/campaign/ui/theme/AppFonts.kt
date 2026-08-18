package io.github.xep426.campaign.ui.theme

import androidx.compose.ui.text.font.FontFamily

// ═══════════════════════════════════════════════════════════════════════
//  Font roles — the ONLY place raw FontFamily values may appear. Screens
//  reference these vals, never FontFamily.Serif / .SansSerif / .Monospace.
//
//  Current mapping (system stacks, no bundled binaries):
//    DisplayFamily → FontFamily.Serif      (Noto Serif)
//    BodyFamily    → FontFamily.SansSerif  (Roboto)
//    MonoFamily    → FontFamily.Monospace  (Roboto Mono)
//
//  A SERIF display is a deliberate break from Kalimetra, where the system
//  serif was rejected as unmodern and everything went sans. It earns its
//  place here for the opposite reason: Campaign's whole proposition is an
//  unhurried evening ritual, and the mock's identity — "Three things for
//  tomorrow." set large in Fraunces — is carried by that serif. Sans would
//  make this app look like every other task list, which is the one thing
//  the PRD is trying not to be.
//
//  Noto Serif is a stand-in, not the design. Fraunces is what the mock
//  uses, and it is a variable font with the SOFT and WONK axes that give
//  the headings their warmth — Noto has neither, so headings currently
//  read a little more newspaper and a little less lamplight.
//
//  TO BUNDLE THE REAL FONTS:
//    1. Download the TTFs from Google Fonts:
//       - Fraunces:       https://fonts.google.com/specimen/Fraunces
//       - IBM Plex Mono:  https://fonts.google.com/specimen/IBM+Plex+Mono
//    2. Drop them into app/src/main/res/font/ with lowercase names:
//       fraunces_regular.ttf, fraunces_medium.ttf,
//       ibm_plex_mono_regular.ttf, ibm_plex_mono_medium.ttf
//    3. Replace the two vals below with Font(...)-based families:
//       val DisplayFamily = FontFamily(
//           Font(R.font.fraunces_regular, FontWeight.Normal),
//           Font(R.font.fraunces_medium, FontWeight.Medium),
//       )
//  Nothing else changes — every style routes through these three vals.
// ═══════════════════════════════════════════════════════════════════════

/** Headings and the roman slot numerals. The app's voice. */
val DisplayFamily: FontFamily = FontFamily.Serif

/** Task titles and all running UI text. */
val BodyFamily: FontFamily = FontFamily.SansSerif

/** Dates, section labels, counts — anything the eye should scan, not read. */
val MonoFamily: FontFamily = FontFamily.Monospace
