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
    })
}

dependencies {
    // Source: https://mvnrepository.com/artifact/org.python/jython-standalone
    implementation("org.python:jython-standalone:2.7.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}