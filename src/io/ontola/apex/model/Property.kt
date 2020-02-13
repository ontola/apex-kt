package io.ontola.apex.model

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.jetbrains.exposed.sql.Table
import java.util.*

object Properties : Table("properties") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    override val primaryKey = PrimaryKey(id, name = "PKConstraintName")
    val resource = integer("resource_id").references(Resources.id).index()
    val predicate = varchar("predicate", 2000)
    val order = integer("order").default(0).nullable()

    val value = varchar("value", 1_000_000)
    val datatype = varchar("datatype", 2000)
    val language = varchar("language", 255)
    val node = integer("prop_resource").references(Resources.id).nullable()
}

class Property(
    val id: UUID,
    val predicate: IRI,
    val order: Int = 0,
    val value: String,
    val datatype: String,
    val language: String,
    val node: ResourceReference?
) {
    fun rdfValue(): Value {
        val factory = SimpleValueFactory.getInstance()
        return when (datatype) {
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode" -> factory.createIRI(this.value)
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode" -> factory.createBNode(this.value)
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" -> factory.createLiteral(this.value, this.language)
            else -> factory.createLiteral(this.value, factory.createIRI(this.datatype))
        }
    }
}
