import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_21


val jacksonVersion = "2.18.3"
val ktorVersion = "3.1.1"
val logbackVersion = "1.5.18"
val logstashEncoderVersion = "8.0"
val prometheusVersion = "0.16.0"
val kotlinVersion = "2.1.20"
val junitJupiterVersion = "5.12.1"
val commonsCodecVersion = "1.18.0"
val syfoXmlCodegen = "2.0.1"
val ibmMqVersion = "9.4.2.0"
val jaxbApiVersion = "2.4.0-b180830.0359"
val commonsTextVersion = "1.13.0"
val valkeyVersion = "5.3.0"
val ktfmtVersion = "0.44"
val mockkVersion = "1.13.17"
val nimbusdsVersion = "10.0.2"
val testcontainersVersion = "1.20.6"


///Due to vulnerabilities
val commonsCompressVersion = "1.27.1"
val nettyhandlerVersion = "4.1.119.Final"

plugins {
    id("application")
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.6"
    id("com.diffplug.spotless") version "7.0.2"
}

application {
    mainClass.set("no.nav.syfo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints { 
         implementation("io.netty:netty-handler:$nettyhandlerVersion")
     }
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:$commonsCodecVersion") {
            because("override transient from io.ktor:ktor-client-apache")
        }
    }

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:$ibmMqVersion")

    implementation("no.nav.helse.xml:tss-samhandler-data:$syfoXmlCodegen")

    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")

    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    implementation("io.valkey:valkey-java:$valkeyVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    constraints {
        implementation("org.apache.commons:commons-compress:$commonsCompressVersion") {
            because("Due to vulnerabilities, see CVE-2024-26308")
        }
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
}

kotlin {
    compilerOptions {
        jvmTarget = javaVersion
    }
}

tasks {

    shadowJar {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.syfo.ApplicationKt",
                ),
            )
        }
    }

    test {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }


    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

}
