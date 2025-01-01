plugins {
    val kotlinVersion = "2.0.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "com.github.hatoyuze.restarter"
version = "0.5.0"

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    mavenCentral()
}

dependencies {
    // skia runtime library will be added when run program
    implementation("org.jetbrains.skiko:skiko-awt:0.8.18")
}

dependencies {
    testImplementation(kotlin("test"))


    // test for drawing function
    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.7.67")
    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.7.67")
    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.67")

    // dataframe for Uploader
    testImplementation("org.jetbrains.kotlinx:dataframe-core:0.15.0")
    testImplementation("org.jetbrains.kotlinx:dataframe-excel:0.15.0")
}
