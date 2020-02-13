package io.ontola.apex.service;

import io.ontola.apex.model.*
import io.ontola.rdf.dsl.iri
import org.jetbrains.exposed.sql.Query;
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.alias

private val linkedResources = Resources.alias("lr")

fun parseResultSet(set: Query): HashMap<Int, Document> {
    val documents = HashMap<Int, Document>()

    for (resultRow in set) {
        val document = ensureDocument(documents, resultRow)
        val resource = ensureResource(resultRow, document)
        ensureProperty(resultRow, resource)
    }

    return documents
}

private fun ensureDocument(documents: MutableMap<Int, Document>, row: ResultRow): Document {
    val documentId = row[Documents.id]

    return documents.getOrPut(documentId) {
        Document(
            id = documentId,
            iri = row[Documents.iri],
            resources = mutableListOf()
        )
    }
}

private fun ensureResource(row: ResultRow, document: Document): Resource {
    val resourceId = row[Resources.id]
    val existing = document.resources.find { d -> d.id == resourceId }
    if (existing !== null) {
        return existing
    }

    return Resource(
        id = resourceId,
        iri = row[Resources.iri],
        properties = mutableListOf()
    ).also { document.resources += it }
}

private fun ensureProperty(row: ResultRow, resource: Resource): Property {
    val predicate = row[Properties.predicate].iri()
    val order = row[Properties.order] ?: 0

    val existing = resource
        .properties
        .find { p -> p.predicate == predicate && p.order == order }
    if (existing !== null) {
        return existing
    }

    return Property(
        id = row[Properties.id],
        predicate = row[Properties.predicate].iri(),
        order = row[Properties.order] ?: 0,
        value = row[Properties.value],
        datatype = row[Properties.datatype],
        language = row[Properties.language],
        node = row[Properties.node]?.let {
            ResourceReference(
                id = it,
                iri = row[linkedResources[Resources.iri]].iri()
            )
        }
    ).also { resource.properties += it }
}
