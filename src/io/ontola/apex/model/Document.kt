package io.ontola.apex.model

import io.ontola.rdf.serialization.IRIProvider
import io.ontola.rdf.serialization.ResourceProvider
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Documents : Table("documents") {
    val id: Column<Int> = integer("id").autoIncrement().uniqueIndex()
    val iri = varchar("iri", 2000)
}

@IRIProvider("iri")
class Document(
    val id: Int,
    var iri: String,
    @ResourceProvider()
    val resources: MutableCollection<Resource>
)
