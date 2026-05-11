import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.compose.reload.gradle.isHotReloadBuild
import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.hotreload)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildconfig)
    id("dev.hydraulic.conveyor") version "1.12"
}

val riftVersion = properties["rift.version"] as String
group = "dev.nohus"
version = riftVersion

buildConfig {
    val environment = (properties["rift.environment"] as? String) ?: "dev"
    buildConfigField("String", "environment", "\"$environment\"")
    buildConfigField("Boolean", "isDevEnvironment", "${environment == "dev"}")
    buildConfigField("String", "version", "\"${properties["rift.version"]}\"")
    buildConfigField("long", "buildTimestamp", "${Instant.now().toEpochMilli()}")
    buildConfigField("String", "buildUuid", "\"${properties["rift.buildUuid"]}\"")
    buildConfigField("String", "sentryDsn", "${properties["rift.sentryDsn"]}")
    buildConfigField("String", "postHogToken", "${properties["rift.postHogToken"]}")
    buildConfigField("String", "focusedLoggers", "${properties["rift.focusedLoggers"]}")
    buildConfigField("String", "logLevel", "${properties["rift.logLevel"]}")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jogamp.org/deployment/maven")
    google()
}

dependencies {
    // Compose
    implementation(libs.compose.desktop.macos.x64)
    implementation(libs.compose.desktop.macos.arm64)
    implementation(libs.compose.desktop.windows)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.desktop) {
        exclude("org.jetbrains.compose.material")
        exclude("org.jetbrains.compose.material3")
    }

    // Kamel
    implementation(libs.kamel.image)
    implementation(libs.kamel.decoder.image.bitmap)
    implementation(libs.kamel.decoder.animatedimage)
    implementation(libs.kamel.fetcher.resources)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)
    implementation(libs.log4j.api)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)

    // Other
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.lang3)
    implementation(libs.commons.exec)
    implementation(libs.commons.math3)
    implementation(libs.httpclient5)
    implementation(libs.jose4j)
    implementation(libs.flatlaf)
    implementation(libs.jsoup)
    implementation(libs.autolink)
    implementation(libs.jts.core)
    implementation(libs.haze)
    implementation(libs.conveyor.control)
    implementation(libs.androidx.collection)
    implementation(libs.jbr.api)
    implementation(libs.cache4k)

    // OpenAL Audio
    implementation(libs.joal.main)
    implementation(libs.gluegen.rtmain)

    implementation(libs.jlayer)

    // JavaCV / OpenCV (Windows + macOS only)
    val opencvVersion = libs.versions.opencv.get()
    implementation("org.bytedeco:opencv:$opencvVersion") {
        exclude(group = "org.bytedeco", module = "opencv-platform")
        exclude(group = "org.bytedeco", module = "openblas-platform")
    }
    runtimeOnly("org.bytedeco:opencv:$opencvVersion:windows-x86_64")
    runtimeOnly("org.bytedeco:opencv:$opencvVersion:macosx-x86_64")
    runtimeOnly("org.bytedeco:opencv:$opencvVersion:macosx-arm64")
    runtimeOnly("org.bytedeco:openblas:0.3.28-1.5.11:windows-x86_64")
    runtimeOnly("org.bytedeco:openblas:0.3.28-1.5.11:macosx-x86_64")
    runtimeOnly("org.bytedeco:openblas:0.3.28-1.5.11:macosx-arm64")

    // Smack (XMPP)
    implementation(libs.smack.java8)
    implementation(libs.smack.tcp)
    implementation(libs.smack.im)
    implementation(libs.smack.extensions)

    // SystemTray
    implementation(libs.dorkbox.collections)
    implementation(libs.dorkbox.executor)
    implementation(libs.dorkbox.desktop)
    implementation(libs.dorkbox.jna)
    implementation(files("libs/OS.jar"))
    implementation(libs.dorkbox.updates)
    implementation(libs.dorkbox.utilities)
    implementation(libs.javassist)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.slf4j.api)
    implementation(files("libs/SystemTray.jar"))

    // Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinxjson)

    // Sentry
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry)
    implementation(libs.sentry.logback)

    implementation(libs.posthog)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}

ksp {
    arg("KOIN_DEFAULT_MODULE", "false")
}

compose.desktop {
    application {
        mainClass = "dev.nohus.rift.MainKt"
        jvmArgs("--add-opens=java.desktop/java.awt=ALL-UNNAMED", "--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")

        buildTypes.release.proguard {
            version.set(libs.versions.proguard.get())
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.sql", "java.naming", "jdk.naming.dns")
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("dev.nohus.rift.MainKt")
    jvmArgs(
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
        optIn.add("org.jetbrains.compose.resources.ExperimentalResourceApi")
        freeCompilerArgs.add("-Xreturn-value-checker=check")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.JETBRAINS
        implementation = JvmImplementation.VENDOR_SPECIFIC
    }
}

configurations.all {
    if (isCanBeResolved || isCanBeConsumed) {
        attributes {
            attribute(Attribute.of("ui", String::class.java), "awt")
        }
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_no-empty-first-line-in-class-body" to "disabled",
                "ktlint_standard_function-expression-body" to "disabled",
                "ktlint_standard_class-signature" to "disabled",
            ))
        targetExclude("**/generated/**")
    }
}
