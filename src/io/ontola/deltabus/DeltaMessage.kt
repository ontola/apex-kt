package io.ontola.deltabus

data class DeltaMessage(
    val topic: String,
    val key: String,
    val message: String
)
