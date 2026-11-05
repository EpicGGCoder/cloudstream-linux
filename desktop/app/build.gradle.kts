import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":library"))
            // Note: :shared module is vendored but currently has Mihon widget compile issues
            // (compose.resources + tooling.preview). We'll re-enable it in Phase 2 when
            // we port the upstream Compose UI. Phase 1 uses our own minimal UI.
            implementation(compose.desktop.currentOs) {
                exclude(group = "org.jetbrains.compose.material", module = "material")
            }
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.nicehttp)
            implementation(libs.ksoup)
            implementation(libs.vlcj)
            implementation(libs.okhttp)
            implementation(libs.logback)
            // Coil 3 for image loading (posters, backdrops)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            // JavaFX WebView: deferred to Phase 2 (will use JCEF instead).
            // Phase 1 uses a stub WebViewResolver that warns when Cloudflare is hit.
            // dex2jar is bundled as a CLI tool in app/resources/dex-tools/
            // (downloaded by scripts/fetch-dex-tools.sh). PluginLoader.kt
            // invokes it as a subprocess when loading .cs3 plugins.
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

// ============================================================================
// Plugin loader test — runs a real .cs3 through the full pipeline headlessly
// ============================================================================
val pluginTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Tests the .cs3 plugin loader end-to-end with a real plugin (CastleTvProvider from CNCVerse)"
    dependsOn(":app:jvmJar", ":library:jvmJar")

    val cp = sourceSets.getByName("jvmMain").runtimeClasspath
    classpath = cp +
        files("test-plugins/CastleTvProvider.jar") +
        files("app/resources/dex-tools/lib/*")

    mainClass.set("com.lagradost.cloudstream4.test.PluginLoaderTest")
    args = listOf(
        "test-plugins/CastleTvProvider.cs3",
        "test-plugins/CastleTvProvider.jar",
    )

    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
    )

    workingDir = rootProject.layout.projectDirectory.asFile
    standardOutput = System.out
    standardInput = System.`in`
}

// ============================================================================
// Desktop packaging — produces .deb + .AppImage via jpackage + appimagetool
// ============================================================================
compose.desktop {
    application {
        mainClass = "com.lagradost.cloudstream4.MainKt"

        nativeDistributions {
            // .AppImage target requires jpackage's app-image first,
            // then we convert with appimagetool in a follow-up step.
            targetFormats(TargetFormat.Deb, TargetFormat.AppImage)

            packageName = "CloudStream"
            packageVersion = "1.0.0"
            description = "CloudStream — Linux port with native .cs3 plugin support"
            vendor = "reCloudStream"
            copyright = "© 2024 reCloudStream. Licensed under the GNU GPL v3."
            licenseFile.set(rootProject.file("LICENSE"))

            // Bundle a JVM so users don't need Java installed.
            // jpackage auto-detects the JRE from JAVA_HOME and bundles it.

            // Linux-specific options
            linux {
                iconFile.set(rootProject.file("icons/icon-linux.png"))
                // installationPath, menuGroup etc. are set via the .deb metadata at install time.
                // jpackage generates the .desktop entry automatically.
            }

            // Increase memory limits for plugin loading + scraping
            jvmArgs(
                "-Xmx2g",
                "-Dfile.encoding=UTF-8",
                // Allow dex2jar reflective access
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
            )

            // Include dex2jar binaries bundled with the app
            appResourcesRootDir.set(rootProject.layout.projectDirectory.dir("resources"))
        }
    }
}

// JavaFX modules are deferred to Phase 2 (JCEF). Phase 1 runs without them.

// ============================================================================
// AppImage post-build step — wraps the jpackage app-image into a portable
// .AppImage using appimagetool.
// ============================================================================
val wrapAppImage by tasks.registering {
    group = "native distribution"
    description = "Wraps the jpackage app-image dir into a portable CloudStream.AppImage (requires appimagetool on PATH)"
    dependsOn("packageDistributionForCurrentOS")

    doLast {
        val buildDir = layout.buildDirectory.dir("compose/binaries").get().asFile
        val appImageDir = buildDir.resolve("main/app-image/CloudStream")
        val outputFile = buildDir.resolve("CloudStream-x86_64.AppImage")

        if (!appImageDir.exists()) {
            logger.warn("App image dir not found at $appImageDir; nothing to wrap.")
            return@doLast
        }

        val tool = listOf("appimagetool", "appimagetool-x86_64.AppImage")
            .firstOrNull { Runtime.getRuntime().exec(arrayOf("which", it)).waitFor() == 0 }

        if (tool == null) {
            logger.lifecycle(
                """
                |appimagetool not found on PATH.
                |To produce the final .AppImage, install it from:
                |  https://github.com/AppImage/AppImageKit/releases
                |Then run:
                |  appimagetool {} {}
                """.trimMargin(),
                appImageDir.absolutePath,
                outputFile.absolutePath,
            )
            return@doLast
        }

        val proc = ProcessBuilder(
            tool, appImageDir.absolutePath, outputFile.absolutePath
        ).inheritIO().start()
        val exit = proc.waitFor()
        if (exit != 0) throw GradleException("$tool exited with $exit")
        logger.lifecycle("✓ AppImage produced at: $outputFile")
    }
}
