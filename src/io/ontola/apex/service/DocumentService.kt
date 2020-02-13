package io.ontola.apex.service

import io.ktor.util.Attributes
import io.ontola.apex.model.*
import io.ontola.apex.model.Properties
import org.jetbrains.exposed.sql.*
import io.ontola.apex.service.DatabaseFactory.dbQuery
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Model
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull

class DocumentService {

    private val listeners = mutableMapOf<Int, suspend (Notification<Document?>) -> Unit>()

    fun addChangeListener(id: Int, listener: suspend (Notification<Document?>) -> Unit) {
        listeners[id] = listener
    }

    fun removeChangeListener(id: Int) = listeners.remove(id)

    private suspend fun onChange(type: ChangeType, id: Int, entity: Document? = null) {
        listeners.values.forEach {
            it.invoke(Notification(type, id, entity))
        }
    }

    suspend fun addResource(resource: NewResource): Document {
        var key: Int? = null
        dbQuery {
            key = Resources.insert {
                it[iri] = resource.iri.stringValue()
            } get Resources.id
        }
        return getDocument(key!!)!!.also {
            onChange(ChangeType.CREATE, key!!, it)
        }
    }

    suspend fun getAllDocuments(ctx: Attributes, page: Int?): List<Document> = dbQuery {
        val documents = Documents
            .selectAll()
            .orderBy(Documents.id to SortOrder.ASC)
            .limit(100, offset = (((page ?: 1) - 1) * 100).coerceIn(1, Int.MAX_VALUE))

        documents.mapNotNull { document -> getDocument(document[Documents.id]) }
    }

    suspend fun getDocument(iri: IRI): Document? = this.getDocument(idFromOriIRI(iri))

    suspend fun getDocument(id: Int): Document? = dbQuery {
        val linkedResources = Resources.alias("lr")

        joinedPropertyTable(
            (Documents.id eq id)
                .and(Documents.id eq Resources.document)
                .and(Resources.id eq Properties.resource)
                .and(Properties.resource eq Resources.id)
                .and((Properties.node eq linkedResources[Resources.id]).or(Properties.node.isNull()))
        )
            .limit(1000)
            .let { parseResultSet(it)[id] }
    }

    suspend fun updateDocument(resource: NewResource): Document? {
        val id = resource.id
        return if (id === null) {
            addResource(resource)
        } else {
            dbQuery {
                Resources.update({ Resources.id eq id }) {
                    it[iri] = resource.iri.stringValue()
                }
            }
            getDocument(id).also {
                onChange(ChangeType.UPDATE, id, it)
            }
        }
    }

    suspend fun updateDocument(iri: IRI, data: Model) = dbQuery {
        val id = this.idFromOriIRI(iri)
        val existing = getDocument(id)
        if (existing === null) {
            println("Create resource")
            val docId = Documents.insert {
                it[this.id] = id
                it[this.iri] = iri.stringValue()
            } get Documents.id

            val resourceIRIs = data.map { s -> s.subject.stringValue() }.distinct()

            val resourceIds = Resources.batchInsert(resourceIRIs) { resource ->
                this[Resources.iri] = resource
                this[Resources.document] = docId
            }.associate { row -> row[Resources.iri] to row[Resources.id] }

            Properties.batchInsert(data) { prop ->
                this[Properties.resource] = resourceIds[prop.subject.stringValue()]!!
                this[Properties.predicate] = prop.predicate.stringValue()
                val obj = prop.`object`
                this[Properties.value] = obj.stringValue()
                when (obj) {
                    is IRI -> {
                        this[Properties.datatype] = "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode"
                        this[Properties.language] = ""
                    }
                    is BNode -> {
                        this[Properties.datatype] = "http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode"
                        this[Properties.language] = ""
                    }
                    is Literal -> {
                        this[Properties.datatype] = obj.datatype.stringValue()
                        this[Properties.language] = obj.language.orElse("")
                    }
                    else -> {
                        throw Error("Unexpected object type ($obj)")
                    }
                }
            }
            println("document insert complete")
        } else {
            println("Update resource")
            Resources.update({ Resources.id eq id }) {
//              it[iri] = resource.iri.stringValue()
            }
            getDocument(id).also {
                onChange(ChangeType.UPDATE, id, it)
            }
        }
    }

    private fun idFromOriIRI(iri: IRI): Int {
        return iri.stringValue().split("/").last().toInt()
    }

    private fun joinedPropertyTable(op: Op<Boolean>): Query {
        val linkedResources = Resources.alias("lr")

        return (Documents.leftJoin(
            (Resources.leftJoin(
                (Properties.leftJoin(linkedResources, { node }, { linkedResources[Resources.id] })),
                { Resources.id },
                { Properties.resource }
            )),
            { Documents.id },
            { Resources.document }
        ))
            .select(op)
            .orderBy(
                Documents.id to SortOrder.ASC,
                Resources.id to SortOrder.ASC,
                Properties.predicate to SortOrder.ASC,
                Properties.order to SortOrder.ASC
            )
            .groupBy(
                Documents.id,
                Documents.iri,
                Resources.id,
                Resources.iri,
                Resources.document,
                Properties.id,
//                Properties.resource,
//                Properties.predicate,
                *Properties.columns.toTypedArray(),
                linkedResources[Resources.id],
                linkedResources[Resources.document],
                linkedResources[Resources.iri]
            )
    }
}
