package com.example.jacocosandbox.model

import com.example.jacocosandbox.view.Greeting
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class GetGreeting {
    private val template: String = "Hello, %s!"
    private val counter = AtomicLong()

    operator fun invoke(name: String): Greeting {
        val id = counter.incrementAndGet()
        if (id <= 0) {
            throw ArithmeticException()
        }

        val content = String.format(template, name)

        return Greeting(id, content)
    }
}