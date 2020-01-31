package io.ontola.processor

import com.soywiz.klock.DateTime
import io.ontola.apex.model.NewDocument
import io.ontola.apex.model.NewResource
import io.ontola.apex.model.Property
import io.ontola.apex.service.DocumentService
import io.ontola.rdf.createIRI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import java.util.*

fun deltasToDocuments(flow: Flow<Pair<IRI, Model>>): Flow<*> {
    return flow
        .map { modelToDocument(it) }
        .map {
            /* TODO: upsert */
            DocumentService().addResource(it)
        }
}

internal fun modelToDocument(delta: Pair<IRI, Model>): NewDocument {
    val (docIRI, model) = delta
    val resources = mutableListOf<NewResource>()

    /* TODO: convert Model to Document->resource->property */
    /*
     * 1. Create Document with {docIRI}
     * 2. Group the statements in {model} on their subject
     * 3. For each group; add every statement as property
     */
    for (statement in model) {
        println(statement)
    }
    println()

    val grouped_resources = model.groupBy({it.subject})
    for (group in grouped_resources) {
        val new_resource = NewResource(createIRI(group.key.toString()))
        for (property in group.value) {
            val new_property = Property(
                id = UUID.randomUUID(),
                predicate = property.predicate)
            new_property.setValue(property.`object`)
            new_resource.properties!!.add(new_property)
        }
        resources.add(new_resource)
    }

    return NewDocument(null, docIRI, resources)
}
