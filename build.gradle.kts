val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val exposed_version: String by project
val postgresql_version: String by project
val koin_version: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
}

ktor {
    fatJar {
        archiveFileName.set("unsessionhost.jar")
    }
}

group = "lol.unsession"
version = "0.0.1"

application {
    mainClass.set("lol.unsession.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")

    val test by tasks.getting(Test::class) {
        useJUnitPlatform { }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-apache:3.0.0-beta-1")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-cors-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-auth-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-client-encoding:3.0.0-beta-1")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-partial-content-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-freemarker-jvm:3.0.0-beta-1")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")

    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    implementation("com.fleeksoft.ksoup:ksoup:0.1.2")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")

    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.0-beta-1")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    testImplementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    testImplementation("org.postgresql:postgresql:$postgresql_version")
    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-client-encoding:3.0.0-beta-1")
    testImplementation("io.ktor:ktor-client-okhttp-jvm:3.0.0-beta-1")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    testImplementation("io.ktor:ktor-serialization-gson:$ktor_version")
}
