pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }

}
rootProject.name = "AssemblyInterpreter"


include(":library")
include(":nativeApp")
include(":jvmApp")