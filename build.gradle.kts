import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val jacksonVersion = "2.15.2"
val ktorVersion = "2.3.2"
val logbackVersion = "1.4.8"
val logstashEncoderVersion = "7.4"
val prometheusVersion = "0.16.0"
val smCommonVersion = "1.0.12"
val kotlinVersion = "1.9.0"
val junitJupiterVersion = "5.9.3"
val commonsCodecVersion = "1.15"
val syfoXmlCodegen = "1.0.4"
val ibmMqVersion = "9.3.3.0"
val jaxbApiVersion = "2.4.0-b180830.0359"
val commonsTextVersion = "1.10.0"
val jedisVersion = "4.4.3"
val ktfmtVersion = "0.44"
val mockkVersion = "1.13.5"
val nimbusdsVersion = "9.31"
val testcontainersVersion = "1.18.3"
val jvmVersion = "17"


application {
    mainClass.set("no.nav.syfo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

plugins {
    kotlin("jvm") version "1.9.0"
    id("io.ktor.plugin") version "2.3.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.cyclonedx.bom") version "1.7.4"
    id("com.diffplug.spotless") version "6.20.0"
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
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
    // override transient version 1.11 from io.ktor:ktor-client-apache due to security vulnerability
    // https://devhub.checkmarx.com/cve-details/Cxeb68d52e-5509/
    implementation("commons-codec:commons-codec:$commonsCodecVersion")

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

    implementation ("redis.clients:jedis:$jedisVersion")


    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") 
    }

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation ("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.ApplicationKt"
    }

    create("printVersion") {
        println(project.version)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = jvmVersion
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {}
        testLogging.showStandardStreams = true
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

}
