plugins {
    kotlin("multiplatform")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    jvm()

    targets.forEach { target ->
        target.compilations.forEach { compilation ->
            compilation.allKotlinSourceSets.forEach { sourceSet ->
                sourceSet.languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
    }

    nativeTarget.apply {
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.0")
            }
        }
        val nativeMain by getting {

        }
        val jvmMain by getting
        val nativeTest by getting
    }
}