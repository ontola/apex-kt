package io.ontola.rdf.ktor

import io.ktor.http.ContentType

object ContentType {
    object Application {
        val NQuads = ContentType("application", "n-quads")
        val NTriples = ContentType("application", "n-triples")
    }

    object Text {
        val Turtle = ContentType("text", "turtle")
    }
}
