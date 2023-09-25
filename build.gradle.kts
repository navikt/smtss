group = "no.nav.syfo"
version = "1.0.0"

val jacksonVersion = "2.15.2"
val ktorVersion = "2.3.4"
val logbackVersion = "1.4.11"
val logstashEncoderVersion = "7.4"
val prometheusVersion = "0.16.0"
val smCommonVersion = "2.0.0"
val kotlinVersion = "1.9.10"
val junitJupiterVersion = "5.10.0"
val commonsCodecVersion = "1.16.0"
val syfoXmlCodegen = "2.0.1"
val ibmMqVersion = "9.3.3.1"
val jaxbApiVersion = "2.4.0-b180830.0359"
val commonsTextVersion = "1.10.0"
val jedisVersion = "5.0.0"
val ktfmtVersion = "0.44"
val mockkVersion = "1.13.8"
val nimbusdsVersion = "9.35"
val testcontainersVersion = "1.19.0"


plugins {
    id("application")
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.21.0"
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
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

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

    implementation("com.ibm.mq:com.ibm.mq.allclient:$ibmMqVersion")

    implementation("no.nav.helse.xml:tss-samhandler-data:$syfoXmlCodegen")

    implementation("no.nav.helse:syfosm-common-mq:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-networking:$smCommonVersion")

    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")

    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    implementation("redis.clients:jedis:$jedisVersion")


    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
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
