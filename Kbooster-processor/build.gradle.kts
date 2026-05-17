plugins {
    kotlin("jvm")
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":Kbooster-core"))

    // KSP dependencies
    implementation(libs.ksp.api)
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("com.squareup:kotlinpoet-ksp:1.18.1")

    // This allows you to reference Android classes (like Context or View)
    // in the processor without making the processor an Android module.
    compileOnly("com.google.android:android:4.1.1.4")
}

// Standard Maven Publishing for JVM
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "com.github.badrqaba"
            artifactId = "Kbooster-processor"
            version = project.version.toString()
        }
    }
}