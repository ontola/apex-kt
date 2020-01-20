package io.ontola.linkeddelta

import io.ontola.rdf.dsl.iri
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement

/**
 * Removes all subject-predicate combinations which match the statement from the store and is added afterwards.
 */
class ReplaceProcessor : BaseProcessor() {
    override val graphIRI = "http://purl.org/linked-delta/replace".iri()
    private val supplantIRI = "http://purl.org/linked-delta/supplant".iri()
    private val oldSupplantIRI = "http://purl.org/link-lib/supplant".iri()

    override fun match(st: Statement): Boolean {
        val context = withoutGraph(st.context)
        return context == graphIRI || context == supplantIRI || context == oldSupplantIRI
    }

    override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
        return DeltaProcessorResult(
            emptyStArr,
            emptyStArr,
            statementWithoutContext(st)
        )
    }
}
