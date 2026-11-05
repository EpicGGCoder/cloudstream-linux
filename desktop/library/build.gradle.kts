import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    version = "1.0.1"

    applyDefaultHierarchyTemplate()

    // JVM only — no Android target. This is the key change vs upstream.
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("com.lagradost.cloudstream3.InternalAPI")
                optIn("com.lagradost.cloudstream3.Prerelease")
            }
        }

        commonMain.dependencies {
            implementation(libs.androidx.annotation)
            implementation(libs.jackson.module.kotlin)
            implementation(libs.jsoup)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksoup)
            implementation(libs.ktor.http)
            implementation(libs.nicehttp)
            implementation(libs.rhino)
            implementation(libs.bundles.cryptography)
        }

        // Intermediate source set shared between jvm targets.
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.newpipeextractor)
            }
        }

        jvmMain { dependsOn(jvmCommonMain) }
    }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

buildkonfig {
    packageName = "com.lagradost.api"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "MDL_API_KEY", System.getenv("MDL_API_KEY") ?: "")
        buildConfigField(FieldSpec.Type.STRING, "TRAKT_CLIENT_ID", System.getenv("TRAKT_CLIENT_ID") ?: "")
    }
}
