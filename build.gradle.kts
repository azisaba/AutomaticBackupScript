plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "net.azisaba.automaticbackupscript"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("io.ktor:ktor-client-core:3.4.0")
    implementation("io.ktor:ktor-client-cio:3.4.0")
    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("com.akuleshov7:ktoml-file:0.7.1")
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
    jvmToolchain(25)
}
