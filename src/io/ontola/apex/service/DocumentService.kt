package io.ontola.apex.service

import io.ktor.util.Attributes
import io.ontola.apex.model.*
import io.ontola.apex.model.Properties
import org.jetbrains.exposed.sql.*
import io.ontola.apex.service.DatabaseFactory.dbQuery
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

    suspend fun addResource(resource: Document): Document {
        var key: Int? = null
        dbQuery {
            key = Documents.insert {
                it[iri] = resource.iri
            } get Documents.id
        }
        return getDocument(key!!)!!.also {
            onChange(ChangeType.CREATE, key!!, it)
        }
    }

    suspend fun getAllDocuments(ctx: Attributes, page: Int?): List<Document> = dbQuery {
        val linkedResources = Resources.alias("lr")

        joinedPropertyTable(
            (Documents.id eq Resources.document)
                .and(Resources.id eq Properties.resource)
                .and((Properties.node eq linkedResources[Resources.id]).or(Properties.node.isNull()))
        )
            .limit(1000, offset = (((page ?: 1) - 1) * 1000).coerceIn(1, Int.MAX_VALUE))
            .let { parseResultSet(it).values.toList() }
    }

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

    suspend fun updateDocument(resource: Document): Document? {
        val id = resource.id
        return if (id === null) {
            addResource(resource)
        } else {
            dbQuery {
                Documents.update({ Documents.id eq id }) {
                    it[iri] = resource.iri
                }
            }
            getDocument(id).also {
                onChange(ChangeType.UPDATE, id, it)
            }
        }
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
                Resources.id,
                Properties.id,
                linkedResources[Resources.id]
            )
    }
}
