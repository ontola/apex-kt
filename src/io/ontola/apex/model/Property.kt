package io.ontola.apex.model

import com.soywiz.klock.DateTime
import org.eclipse.rdf4j.model.IRI
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.util.*

object Properties : Table("properties") {
    val id = uuid("id").autoGenerate()
//    val createdAt = double("created_at")
//    val updatedAt = double("updated_at")
    val resource = integer("resource_id").references(Resources.id).index()
    val predicate = varchar("predicate", 2000)
    val order = integer("order").default(0).nullable()

    val boolean = bool("prop_boolean").nullable()
    val string = varchar("prop_string", 2000).nullable()
    val text = varchar("prop_text", 1_000_000).nullable()
    val dateTime = datetime("prop_datetime").nullable()
    val integer = long("prop_integer").nullable()
//    val bigInt = long("prop_bigint").nullable()
//    val uuid = uuid("prop_uuid").nullable()
    val node = integer("prop_resource").references(Resources.id).nullable()
    val iri = varchar("prop_iri", 2000).nullable()
}

class Property(
    val id: UUID,
//    val createdAt: DateTime,
//    val updatedAt: DateTime,
    val predicate: IRI,
    val order: Int = 0,

    val boolean: Boolean?,
    val string: String?,
    val text: String?,
    val dateTime: DateTime?,
    val integer: Long?,
//    val bigInt: BigInteger?,
//    val uuid: UUID?,
    val node: ResourceReference?,
    val iri: IRI?
) {
    fun value(): Any {
        val value = iri ?: string ?: node ?: dateTime ?: integer
//        val value = boolean ?: string ?: text ?: dateTime ?: uuid ?: node

//        if (value === null) {
//            throw Error("Property has no value") // TODO: type exception
//        }

        return value ?: "type not implemented ($id)"
    }
}
