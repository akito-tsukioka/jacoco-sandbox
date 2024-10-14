package com.example.jacocosandbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class JacocoSandboxApplication

fun main(args: Array<String>) {
    runApplication<JacocoSandboxApplication>(*args)
}
