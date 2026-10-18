import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

// Echo disables Apple targets by default; another upstream consumer retains them
// unless it sets echo.enableNativeTargets=false. Opt in with -Pecho.enableNativeTargets=true.
val echoNativeTargetsEnabled = providers.gradleProperty("echo.enableNativeTargets")
    .map { it.toBooleanStrict() }.orElse(true).get()

kotlin {
    jvmToolchain(21)
    android {
        namespace = "com.abhinav.domain"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
    }
    val xcfName = "domainKit"

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
                // Add KMP dependencies here
                implementation(libs.room.runtime)
                implementation(libs.kotlinx.serialization.json)
                // StateFlow/Flow appear in domain interfaces; declare and export their version
                // instead of inheriting an older coroutine API indirectly through Paging.
                api(libs.kotlinx.coroutines.core)
                implementation(projects.common)
                api(libs.androidx.paging.common)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        if (echoNativeTargetsEnabled) {
            iosMain {
                dependencies {
                    // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                    // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                    // part of KMP’s default source set hierarchy. Note that this source set depends
                    // on common by default and will correctly pull the iOS artifacts of any
                    // KMP dependencies declared in commonMain.
                }
            }
        }

        jvmMain {
            dependencies {
            }
        }
    }
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}
