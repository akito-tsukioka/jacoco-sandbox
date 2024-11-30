package com.example.jacocosandbox.controller

import com.example.jacocosandbox.model.GetGreeting
import com.example.jacocosandbox.view.Greeting
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(GreetingController::class)
class GreetingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var getGreeting: GetGreeting

    @Test
    fun `nameに指定がない場合、Hello, World!が返ること`() {
        `when`(getGreeting("World")).thenReturn(
            Greeting(
                id = 1,
                content = "Hello, World!",
            )
        )

        // Act & Assert
        mockMvc.get("/greeting")
            .andExpect {
                status { isOk() }
                content {
                    Greeting(
                        id = 1,
                        content = "Hello, World!",
                    )
                }
            }
    }
}