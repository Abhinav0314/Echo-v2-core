import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

// Echo disables Apple targets by default; another upstream consumer retains them
// unless it sets echo.enableNativeTargets=false. Opt in with -Pecho.enableNativeTargets=true.
val echoNativeTargetsEnabled = providers.gradleProperty("echo.enableNativeTargets")
    .map { it.toBooleanStrict() }.orElse(true).get()

kotlin {
    android {
        namespace = "org.abhinav.echo.autoeq"
        compileSdk = 37
        minSdk = 26
    }

    val xcfName = "autoEqServiceKit"

    if (echoNativeTargetsEnabled) {
        iosArm64 {
            binaries.framework {
                baseName = xcfName
            }
        }

        iosSimulatorArm64 {
            binaries.framework {
                baseName = xcfName
            }
        }
    }

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.common)
                implementation(projects.ktorExt)
                implementation(libs.ktor.client.encoding)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}
