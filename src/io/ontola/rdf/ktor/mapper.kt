package io.ontola.rdf.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.features.ContentNegotiation

fun ContentNegotiation.Configuration.nquads(contentType: io.ktor.http.ContentType = ContentType.Application.NQuads,
                                            block: ObjectMapper.() -> Unit = {}) {
    val converter = NQuadsConverter()
    register(contentType, converter)
}
