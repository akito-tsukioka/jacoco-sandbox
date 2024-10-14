apply {
    plugin("org.springframework.boot")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":view"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.example.jacocosandbox.JacocoSandboxApplicationKt")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}
