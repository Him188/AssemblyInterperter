plugins {
    kotlin("multiplatform") version "1.4.20-RC" apply false
}

group = "moe.him188"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
}