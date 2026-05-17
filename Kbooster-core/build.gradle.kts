plugins {
    kotlin("jvm")
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation("com.squareup:kotlinpoet:1.18.1")

    compileOnly("com.google.android:android:4.1.1.4")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "com.github.badrqaba"
            artifactId = "Kbooster-core"
            version = project.version.toString()
        }
    }
}