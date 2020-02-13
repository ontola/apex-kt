package io.ontola.deltabus

import io.ontola.apex.model.Document
import io.ontola.apex.service.DocumentService
import io.ontola.linkeddelta.applyDelta
import io.ontola.linkeddelta.processors
import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.impl.LinkedHashModel

suspend fun writeDelta(svc: DocumentService, iri: IRI, delta: Model) {
    val doc = svc.getDocument(iri)
    val model = docToModel(doc)
    val next = applyDelta(processors, model, delta)
    svc.updateDocument(iri, next)
}

private fun docToModel(doc: Document?): Model {
    val model = LinkedHashModel()

    doc
        ?.resources
        ?.map { resource -> Pair(resource.iri.iri(), resource.properties) }
        ?.forEach { (resource, properties) ->
            properties.map { property ->
                model.add(resource, property.predicate, property.rdfValue())
            }
        }

    return model
}
