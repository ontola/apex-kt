package io.ontola.rdf.ktor

import io.ktor.http.ContentType
import io.ktor.http.HeaderValueParam

object ContentType {
    object Application {
        val NQuads = ContentType("application", "n-quads", listOf(HeaderValueParam("charset", "UTF-8")))
        val NTriples = ContentType("application", "n-triples", listOf(HeaderValueParam("charset", "UTF-8")))
    }

    object Text {
        val Turtle = ContentType("text", "turtle")
    }
}
