package com.bookappp.app.ui.licenses

import androidx.annotation.RawRes
import com.bookappp.app.R

/**
 * License families shipped in the app, each with its full text bundled as a raw resource
 * so the notices work offline (AGENTS.md, Licensing and legal).
 */
enum class LicenseKind(
    val displayName: String,
    @RawRes val textRes: Int,
) {
    Apache2("Apache License 2.0", R.raw.apache_2_0),
    Bsd3("BSD 3-Clause License", R.raw.bsd_3_clause),
    Mit("MIT License", R.raw.mit),
    Ofl("SIL Open Font License 1.1", R.raw.ofl_1_1),
}

/** One distributed third-party component and the notice its license requires. */
data class LicenseNotice(
    val component: String,
    val license: LicenseKind,
    val copyright: String,
)

/**
 * Third-party components bundled in the app. Copyright lines are taken from each
 * project's own LICENSE/NOTICE. Update this list whenever dependencies change.
 */
val licenseNotices: List<LicenseNotice> = listOf(
    LicenseNotice(
        component = "Readium Kotlin Toolkit (shared, streamer, navigator)",
        license = LicenseKind.Bsd3,
        copyright = "Copyright (c) 2017, Readium",
    ),
    LicenseNotice(
        component = "Readium PDFium adapter (document, navigator)",
        license = LicenseKind.Bsd3,
        copyright = "Copyright (c) 2017, Readium",
    ),
    LicenseNotice(
        component = "OpenDyslexic (bundled with the Readium navigator)",
        license = LicenseKind.Ofl,
        copyright = "Copyright (c) 2019-07-29, Abbie Gonzalez, with Reserved Font Name OpenDyslexic",
    ),
    LicenseNotice(
        component = "AccessibleDfA (bundled with the Readium navigator)",
        license = LicenseKind.Ofl,
        copyright = "Copyright (c) 2015, Orange, with Reserved Font Name Accessible-Dfa",
    ),
    LicenseNotice(
        component = "iA Writer Duospace (bundled with the Readium navigator)",
        license = LicenseKind.Ofl,
        copyright = "Copyright (c) 2018, Information Architects Inc.; based on IBM Plex, Copyright (c) 2017 IBM Corp.",
    ),
    LicenseNotice(
        component = "PDFium",
        license = LicenseKind.Bsd3,
        copyright = "Copyright 2014 PDFium Authors",
    ),
    LicenseNotice(
        component = "Kotlin standard library, kotlinx.coroutines, kotlinx.serialization",
        license = LicenseKind.Apache2,
        copyright = "Copyright JetBrains s.r.o. and Kotlin contributors",
    ),
    LicenseNotice(
        component = "AndroidX / Jetpack (Compose, Room, DataStore, Navigation, Lifecycle, Media3)",
        license = LicenseKind.Apache2,
        copyright = "Copyright The Android Open Source Project",
    ),
    LicenseNotice(
        component = "Hilt and Dagger",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2012 The Dagger Authors",
    ),
    LicenseNotice(
        component = "Coil",
        license = LicenseKind.Apache2,
        copyright = "Copyright Coil Contributors",
    ),
    LicenseNotice(
        component = "Okio",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2013 Square, Inc.",
    ),
    LicenseNotice(
        component = "jsoup",
        license = LicenseKind.Mit,
        copyright = "Copyright (c) 2009-2026 Jonathan Hedley",
    ),
    LicenseNotice(
        component = "Timber",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2013 Jake Wharton",
    ),
    LicenseNotice(
        component = "Guava",
        license = LicenseKind.Apache2,
        copyright = "Copyright The Guava Authors",
    ),
    LicenseNotice(
        component = "Accompanist Drawable Painter",
        license = LicenseKind.Apache2,
        copyright = "Copyright The Android Open Source Project",
    ),
    LicenseNotice(
        component = "AndroidPdfViewer (marain87 fork)",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2017 Bartosz Schiller",
    ),
    LicenseNotice(
        component = "PdfiumAndroid (marain87 fork)",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2015 Bekket McClane, 2016 Bartosz Schiller",
    ),
    LicenseNotice(
        component = "Koi",
        license = LicenseKind.Apache2,
        copyright = "Copyright 2015, 2016 Xiaoke Zhang",
    ),
)
