plugins {
    kotlin("jvm") version "2.1.20"
}

group = "ao.argosidps"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.named<Jar>("jar") {
    manifest {
        attributes (
           "Main-Class" to "ao.argosidps.MainKt"
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    //archiveClassifier = "uber"
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }){
        exclude("META-INF/*.SF")   // add these three
        exclude("META-INF/*.RSA")  // lines to exclude
        exclude("META-INF/*.DSA")  // signature files
    }
}

dependencies {
    // Source: https://mvnrepository.com/artifact/org.python/jython-standalone
    implementation("org.python:jython-standalone:2.7.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // Source: https://mvnrepository.com/artifact/org.pcap4j/pcap4j
    implementation("org.pcap4j:pcap4j-core:1.8.2")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")
    implementation("org.pcap4j:pcap4j:1.8.2")
    // Source: https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:5.18.1")
    // Source: https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.17")
    // Source: https://mvnrepository.com/artifact/ch.qos.logback/logback-core
    implementation("ch.qos.logback:logback-core:1.5.32")
    // Source: https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("at.quickme.kotlinmailer:core:1.1.20")
    implementation("at.quickme.kotlinmailer:html:1.1.20")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}