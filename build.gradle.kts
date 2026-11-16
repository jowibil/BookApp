// Top-level build file. Per-module plugins are applied in each module's own build file.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// AGP 9's built-in Kotlin defaults to KGP 2.2.10; pin the latest stable Kotlin (per AGENTS.md).
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}
