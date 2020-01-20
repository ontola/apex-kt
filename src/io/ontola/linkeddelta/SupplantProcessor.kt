package io.ontola.linkeddelta

import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Replaces an entire resource with the new representation
 */
class SupplantProcessor : BaseProcessor() {
    override val graphIRI = "http://purl.org/linked-delta/disabled".iri()

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            statementWithoutContext(st),
            current.filter { s -> s.subject == st.subject },
            emptyStArr
        )
    }
}
