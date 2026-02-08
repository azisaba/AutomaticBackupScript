plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "xyz.acrylicstyle.automaticbackupscript"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.0")
    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        manifest {
            attributes(mapOf("Main-Class" to "net.azisaba.automaticbackupscript.Main"))
        }
        archiveFileName.set("AutomaticBackupScript.jar")
    }
}

kotlin {
    jvmToolchain(11)
}
