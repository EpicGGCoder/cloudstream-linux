pluginManagement {
    repositories {
        // Aliyun mirror first — proxies Maven Central without rate limits
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        mavenCentral()
        google()
    }
}

rootProject.name = "cloudstream-desktop"

// Note: :shared is vendored but not yet included in the build because
// the Mihon settings widgets need compose.resources + tooling.preview setup.
// We'll enable it in Phase 2 when porting the upstream Compose UI.
include(":library", ":app")
