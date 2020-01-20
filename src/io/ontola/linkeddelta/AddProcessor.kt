package io.ontola.linkeddelta

import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Unconditionally adds the statement to the store, leaving any old statement
 */
class AddProcessor : BaseProcessor() {
    override val graphIRI = "http://purl.org/linked-delta/add".iri()

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            statementWithoutContext(st),
            emptyStArr,
            emptyStArr
        )
    }
}
