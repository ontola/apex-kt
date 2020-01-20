package io.ontola.linkeddelta

import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all the statements of a subject from the store, disregarding the predicate and object of the statement
 */
class PurgeProcessor : BaseProcessor() {
    override val graphIRI = "http://purl.org/linked-delta/purge".iri()

    override fun match(st: Statement): Boolean {
        return withoutGraph(st.context) == graphIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            current.filter { s -> s.subject == st.subject },
            emptyStArr
        )
    }
}
