import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("io.eve.ktannot") version "0.1.0"
    kotlin("jvm") version "2.2.0"
}

version = "1.0"

val arcLibraryVersion = "v1.0.6"
val fullModuleName = "graphics-g3d"

fun arcLibraryModule(name: String): String {
    val modName = if (name.contains(":")) name.split(":").joinToString("-") else name
    return "com.github.Zelaux.ArcLibrary:$modName:$arcLibraryVersion"
}

sourceSets {
    main {
        kotlin.srcDir("src")
        java.srcDir("src")
    }
}

repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.xpdustry.com/mindustry") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = uri("https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository") }
    maven { url = uri("https://raw.githubusercontent.com/Zelaux/Repo/master/repository") }
    ivy {
        url = uri("https://github.com/")
        patternLayout { artifact("/[organisation]/[module]/releases/download/[revision]/[artifact].jar") }
        metadataSources { artifact() }
    }
    ivy {
        url = uri("https://github.com/")
        patternLayout { artifact("/[organisation]/[module]/releases/download/master/[revision].jar") }
        metadataSources { artifact() }
    }
}

dependencies {
    compileOnly("com.github.Anuken.Mindustry:core:v159.7")
    implementation("io.eve.ktannot:annotations:0.1.0")
    implementation("org.dyn4j:dyn4j:5.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation(arcLibraryModule(fullModuleName))
}

ktAnnotations {
    mindustryMode = true
    genPackage = "aero.gen"
    sourceDir = "src"
    outputDir = "generated/ktannot/main/kotlin"
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.release = 17
}

val modArtifactName = rootProject.name

tasks.register("jarAndroid") {
    dependsOn("jar")
    val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    doLast {
        if (sdkRoot == null || !File(sdkRoot).exists()) {
            throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")
        }
        val platformRoot = File("$sdkRoot/platforms/").listFiles()
            ?.sorted()
            ?.reversed()
            ?.find { f -> File(f, "android.jar").exists() }
        if (platformRoot == null) {
            throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")
        }
        val dependencies = (configurations.compileClasspath.get().toList() +
                configurations.runtimeClasspath.get().toList() +
                listOf(File(platformRoot, "android.jar")))
            .joinToString(" ") { "--classpath ${it.path}" }
        val d8 = if (isWindows) "d8.bat" else "d8"
        exec {
            workingDir = File("${layout.buildDirectory.get().asFile}/libs")
            commandLine(d8, *dependencies.split(" ").toTypedArray(), "--min-api", "14",
                "--output", "${modArtifactName}Android.jar", "${modArtifactName}Desktop.jar")
        }
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("${modArtifactName}Desktop.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(rootDir) { include("mod.hjson") }
    from("assets/") { include("**") }
}

tasks.register<Jar>("deploy") {
    dependsOn("jarAndroid", "jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("${modArtifactName}.jar")
    from(
        zipTree(File("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Desktop.jar")),
        zipTree(File("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Android.jar"))
    )
    doLast {
        delete(File("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Desktop.jar"))
        delete(File("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Android.jar"))
    }
}