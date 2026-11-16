package com.bookappp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Entry point for Hilt's generated dependency graph.
// No manual DI setup needed here; modules live near the features they serve.
@HiltAndroidApp
class App : Application()
