package io.ontola.linkeddelta

import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate combinations from the store, disregarding the object of the statement
 */
class RemoveProcessor : BaseProcessor() {
    override val graphIRI = "http://purl.org/linked-delta/remove".iri()

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            current.filter { s -> s.subject == st.subject && s.predicate == st.predicate },
            emptyStArr
        )
    }
}
