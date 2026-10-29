import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.personal"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against the locally installed IDE (no download, matches the real target).
        local(providers.gradleProperty("ideaPath").orElse("/Applications/IntelliJ IDEA.app"))
        pluginVerifier()
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testRuntimeOnly("junit:junit:4.13.2") // IntelliJ test runtime expects org.junit.rules.TestRule
}

// Compile with whichever JDK runs Gradle (2026.2 platform jars are Java 25 class files, so it must be 25+
// for that IDE; 21+ for 2025.3), but always emit Java 21 bytecode so one ZIP loads in both.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            local(providers.gradleProperty("ideaPath").orElse("/Applications/IntelliJ IDEA.app"))
        }
    }
    buildSearchableOptions = false
}

tasks.test {
    useJUnitPlatform()
}
