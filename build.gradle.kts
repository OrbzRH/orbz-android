// AGP 9 compiles Kotlin itself (built-in Kotlin). The Kotlin Gradle plugin is only pinned here so the compiler,
// the Compose compiler plugin and the serialization plugin all run the same Kotlin version.
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
}
