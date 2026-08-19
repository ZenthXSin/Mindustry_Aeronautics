pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        mavenLocal()
    }
    plugins {
        id("io.eve.ktannot") version "0.1.0"
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.eve.ktannot") {
                useModule("io.eve.ktannot:ktannot-gradle-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "Mindustry_Aeronautics"