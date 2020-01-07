import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val klockVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.61"
}

group = "io.ontola"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.cio.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    compile("org.jetbrains.exposed:exposed-core:$exposedVersion")
    compile("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    compile("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    compile("io.ktor:ktor-server-cio:$ktorVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("ch.qos.logback:logback-classic:$logbackVersion")
    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-auth:$ktorVersion")
    compile("io.ktor:ktor-auth-jwt:$ktorVersion")
    compile("io.ktor:ktor-jackson:$ktorVersion")
    testCompile("io.ktor:ktor-server-tests:$ktorVersion")
    compile("com.zaxxer:HikariCP:3.4.1")
    compile("com.h2database:h2:1.4.200")
    compile("org.postgresql:postgresql:42.2.9")

    implementation("org.eclipse.rdf4j:rdf4j-runtime:2.5.2") {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    implementation("com.soywiz.korlibs.klock:klock-jvm:$klockVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio:2.5.2")
    implementation("com.squareup.okhttp3:okhttp:4.2.1")
    // Fixes undefined HexBinaryAdapter in org.eclipse.rdf4j.rio.helpers::AbstractRDFParser
    implementation("javax.xml.bind:jaxb-api:2.3.0")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
