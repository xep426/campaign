pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("""com\.android.*""")
                includeGroupByRegex("""com\.google.*""")
                includeGroupByRegex("""androidx.*""")
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
    }
}

rootProject.name = "Campaign"

// ONE module, deliberately.
//
//    Kalimetra splits into five because it earns it — a remote parser, an
//    adaptive-target calculation worth unit-testing in isolation, a feature
//    surface large enough to sit behind its own boundary. Campaign has four
//    screens, two tables and no network. Splitting it would buy a slower
//    sync and five build files to keep in step, in exchange for boundaries
//    nothing is currently pushing against. Packages carry the same shape
//    (domain / data / ui / widget / notify) and promote to modules cleanly
//    if the app ever grows into them.
include(":app")
