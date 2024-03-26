plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "vxcc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "alex's repo"
        url = uri("http://207.180.202.42:8080/libs")
        isAllowInsecureProtocol = true
    }
    maven { url = uri("https://www.jitpack.io") }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("me.alex_s168:blitz:0.3")
    implementation("com.github.kotlinx.ast:common:3e186acfa3")
    implementation("com.github.kotlinx.ast:parser-antlr-kotlin:3e186acfa3")
    implementation("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:3e186acfa3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}
