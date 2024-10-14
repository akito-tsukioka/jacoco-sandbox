apply {
    plugin("org.springframework.boot")
}

dependencies {
    implementation(project(":view"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}
