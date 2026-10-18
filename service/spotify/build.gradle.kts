import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

// Echo disables Apple targets by default; another upstream consumer retains them
// unless it sets echo.enableNativeTargets=false. Opt in with -Pecho.enableNativeTargets=true.
val echoNativeTargetsEnabled = providers.gradleProperty("echo.enableNativeTargets")
    .map { it.toBooleanStrict() }.orElse(true).get()

kotlin {
    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.abhinav.spotify"
        compileSdk = 37
        minSdk = 26
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "spotifyKit"

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

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
                implementation(projects.ktorExt)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.protobuf)
                implementation(libs.ktor.client.logging)

                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.test)

                implementation(libs.common)
                implementation(projects.common)

                implementation(libs.logging)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.kotlin.onetimepassword)
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
                implementation(libs.kotlin.onetimepassword)
            }
        }
    }
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}