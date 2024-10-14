package com.example.jacocosandbox.model

import com.example.jacocosandbox.view.Greeting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GetGreetingTest {
    companion object {
        lateinit var greeting: GetGreeting

        @BeforeAll
        @JvmStatic
        fun init() {
            greeting = GetGreeting()
        }
    }

    @Test
    fun `値の確認`() {
        val expected = Greeting(1, "Hello, Test!")
        val actual = greeting("Test")

        assertEquals(expected, actual)
    }
}