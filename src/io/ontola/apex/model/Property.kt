package io.ontola.apex.model

import com.soywiz.klock.DateTime
import io.ontola.rdf.createIRI
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.impl.SimpleBNode
import org.eclipse.rdf4j.model.impl.SimpleIRI
import org.eclipse.rdf4j.model.impl.SimpleLiteral
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

    var boolean: Boolean? = null,
    var string: String? = null,
    var text: String? = null,
    var dateTime: DateTime? = null,
    var integer: Long? = null,
//    val bigInt: BigInteger?,
//    val uuid: UUID?,
    var node: ResourceReference? = null,
    var iri: IRI? = null
) {
    fun value(): Any {
        val value = iri ?: string ?: node ?: dateTime ?: integer
//        val value = boolean ?: string ?: text ?: dateTime ?: uuid ?: node

//        if (value === null) {
//            throw Error("Property has no value") // TODO: type exception
//        }

        return value ?: "type not implemented ($id)"
    }
    fun setValue(value: Any) {
        when (value) {
            is SimpleIRI -> this.iri = createIRI(value.stringValue())
//          is SimpleBNode -> this.iri = createIRI(value.stringValue())
            is SimpleLiteral -> this.string = value.label
            else -> throw Error("Unsupported property type ${value}")
        }
    }
}
