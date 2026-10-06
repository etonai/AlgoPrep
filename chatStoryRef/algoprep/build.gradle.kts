plugins {
    java
    application
}

group = "com.chatstory"
version = "0.2.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.friwi:jcefmaven:146.0.10")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.chatstory.Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}
