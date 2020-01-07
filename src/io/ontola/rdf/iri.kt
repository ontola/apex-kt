package io.ontola.rdf

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

private val factory = SimpleValueFactory.getInstance()

fun createIRI(iri: String): IRI {
    return factory.createIRI(iri)
}

fun tryCreateIRI(iri: String): IRI? {
    return try {
        factory.createIRI(iri)
    } catch (e: Exception) {
        null
    }
}

fun createIRI(ns: String, term: String): IRI {
    return factory.createIRI("$ns$term")
}

fun getQueryParameter(iri: Resource, parameter: String): IRI? {
    if (iri is BNode) {
        return null
    }

    return iri
        .stringValue()
        .toHttpUrlOrNull()
        ?.queryParameter(parameter)
        ?.let { graph -> createIRI(graph) }
}
