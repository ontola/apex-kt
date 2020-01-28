package io.ontola.apex.model

import io.ontola.apex.model.Documents.iri
import io.ontola.rdf.serialization.IRIProvider
import io.ontola.rdf.serialization.ResourceProvider
import org.eclipse.rdf4j.model.IRI
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Documents : Table("documents") {
    val id: Column<Int> = integer("id").autoIncrement()
    val iri = varchar("iri", 2000)
}

@IRIProvider("iri")
class Document(
    val id: Int,
    var iri: IRI,
    @ResourceProvider()
    val resources: MutableCollection<Resource>
)

data class NewDocument (
    val id: Int?,
    val iri: IRI,
    val resources: MutableCollection<NewResource>
)
