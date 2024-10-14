package com.example.jacocosandbox.controller

import com.example.jacocosandbox.model.GetGreeting
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class GreetingController(
    private val model: GetGreeting
) {

    @GetMapping("/greeting")
    fun getGreeting(
        @RequestParam(value = "name", defaultValue = "World") name: String,
    ) = model(name)
}
