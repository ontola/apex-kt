package io.ontola.processor

import io.ontola.apex.model.Document
import io.ontola.apex.service.DocumentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model

fun deltasToDocuments(flow: Flow<Pair<IRI, Model>>): Flow<*> {
    return flow
        .map { modelToDocument(it) }
        .map {
            /* TODO: upsert */
            DocumentService().addResource(it)
        }
}

internal fun modelToDocument(delta: Pair<IRI, Model>): Document {
    val (docIRI, model) = delta
    /* TODO: convert Model to Document->resource->property */
    /*
     * 1. Create Document with {docIRI}
     * 2. Group the statements in {model} on their subject
     * 3. For each group; add every statement as property
     */
    return Document(0, "", mutableListOf())
}
