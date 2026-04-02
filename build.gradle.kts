plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.android.library") version "8.7.3" apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false
}
