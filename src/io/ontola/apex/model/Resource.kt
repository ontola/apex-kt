package io.ontola.apex.model

import io.ontola.rdf.dsl.iri
import io.ontola.rdf.serialization.IRIProvider
import io.ontola.rdf.serialization.PropertyProvider
import kotlinx.serialization.SerialName
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Resources : Table("resources") {
    val id: Column<Int> = integer("id").autoIncrement()
    val document: Column<Int> = integer("document_id").references(Documents.id)
    val iri = varchar("iri", 2000)
//    val createdAt = double("created_at")
//    val updatedAt = double("updated_at")
//    val confirmed = bool("confirmed").default(false)
}

@IRIProvider("iri")
class Resource(
    val id: Int,
    var iri: Resource,
//    @SerialName("https://schema.org/dateCreated")
//    var createdAt: DateTime,
//    @SerialName("https://schema.org/dateUpdated")
//    var updatedAt: DateTime,
//    @SerialName("https://argu.co/ns/core#confirmed")
//    var confirmed: Boolean,
    @PropertyProvider()
    val properties: MutableCollection<Property>
) {
    var host: String = "http://localhost:8000"
    @SerialName("http://www.w3.org/2002/07/owl#sameAs")
    var sameAs: IRI get() = "$host/kb/$id".iri()
                    set(value) { throw error("Can't override sameAs") }
}

@IRIProvider("iri")
data class ResourceReference(
    val id: Int,
    val iri: IRI
)

data class NewResource(
    val iri: IRI,
    val properties: MutableCollection<Property>? = mutableListOf<Property>()
)
