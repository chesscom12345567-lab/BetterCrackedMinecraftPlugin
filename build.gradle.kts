plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "com.github.hydriel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
