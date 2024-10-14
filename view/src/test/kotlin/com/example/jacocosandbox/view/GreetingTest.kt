package com.example.jacocosandbox.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GreetingTest{
    @Test
    fun `値の確認`() {
        val expected =
            Greeting(
                id = 1,
                content = "Hello, Test!",
            )

        assertEquals(expected, Greeting(1, "Hello, Test!"))
    }
}