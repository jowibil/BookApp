pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Readium's Pdfium PDF adapter depends on PdfiumAndroid and AndroidPdfViewer,
        // which are only published on JitPack. Scoped so no other artifact comes from there.
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.marain87")
            }
        }
    }
}

rootProject.name = "BookAppp"
include(":app")
