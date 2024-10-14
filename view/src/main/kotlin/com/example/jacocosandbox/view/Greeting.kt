package com.example.jacocosandbox.view

data class Greeting(
    val id: Long,
    val content: String,
) {

    companion object {
        val EMPTY = Greeting(-1, "")
    }
}
