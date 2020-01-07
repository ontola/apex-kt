package io.ontola.apex.service

import com.soywiz.klock.DateTime
import io.ktor.util.Attributes
import io.ontola.apex.RequestVariables
import io.ontola.apex.model.*
import io.ontola.apex.model.Properties
import org.jetbrains.exposed.sql.*
import io.ontola.apex.service.DatabaseFactory.dbQuery
import io.ontola.rdf.dsl.iri
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import java.time.ZoneOffset
import java.util.*

class ResourceService {

    private val listeners = mutableMapOf<Int, suspend (Notification<Resource?>) -> Unit>()

    fun addChangeListener(id: Int, listener: suspend (Notification<Resource?>) -> Unit) {
        listeners[id] = listener
    }

    fun removeChangeListener(id: Int) = listeners.remove(id)

    private suspend fun onChange(type: ChangeType, id: Int, entity: Resource? = null) {
        listeners.values.forEach {
            it.invoke(Notification(type, id, entity))
        }
    }

    suspend fun getAllResources(ctx: Attributes, page: Int?): List<Resource> = dbQuery {
        val linkedResources = Resources.alias("lr")
        joinedPropertyTable(
            (Resources.id eq Properties.resource)
                .and(Properties.resource eq Resources.id)
                .and((Properties.node eq linkedResources[Resources.id]).or(Properties.node.isNull()))
        )
            .limit(1000, offset = (((page ?: 1) - 1) * 1000).coerceIn(1, Int.MAX_VALUE))
            .let {
                parseResultSet(it, ctx).values.toList()
            }
    }

    suspend fun getResource(id: Int, ctx: Attributes): Resource? = dbQuery {
        val linkedResources = Resources.alias("lr")

        joinedPropertyTable(
            (Resources.id eq Properties.resource)
                .and(Resources.id eq id)
                .and(Properties.resource eq Resources.id)
                .and((Properties.node eq linkedResources[Resources.id]).or(Properties.node.isNull()))
        )
            .limit(1000)
            .let { parseResultSet(it, ctx)[id] }
    }

    suspend fun updateResource(resource: NewResource, ctx: Attributes): Resource? {
        val id = resource.id
        return if (id === null) {
            addResource(resource, ctx)
        } else {
            dbQuery {
                Resources.update({ Resources.id eq id }) {
                    it[iri] = resource.iri.stringValue()
//                    val now = DateTime.now().unixMillisDouble
//                    it[createdAt] = now
//                    it[updatedAt] = now
//                    it[confirmed] = resource.confirmed
                }
            }
            getResource(id, ctx).also {
                onChange(ChangeType.UPDATE, id, it)
            }
        }
    }

    suspend fun addResource(resource: NewResource, ctx: Attributes): Resource {
        var key: Int? = null
        dbQuery {
            key = Resources.insert {
                it[iri] = resource.iri.stringValue()
//                val now = DateTime.now().unixMillisDouble
//                it[createdAt] = now
//                it[updatedAt] = now
//                it[confirmed] = resource.confirmed
            } get Resources.id
        }
        return getResource(key!!, ctx)!!.also {
            onChange(ChangeType.CREATE, key!!, it)
        }
    }

    suspend fun deleteResource(id: Int): Boolean {
        return dbQuery {
            Resources.deleteWhere { Resources.id eq id } > 0
        }.also {
            if (it) onChange(ChangeType.DELETE, id)
        }
    }

    private fun parseResultSet(set: Query, ctx: Attributes): HashMap<Int, Resource> {
        val resources = HashMap<Int, Resource>()
        val linkedResources = Resources.alias("lr")
        val origin = ctx[RequestVariables.origin]

        for (resultRow in set) {
            val resourceId = resultRow[Resources.id]

            if (!resources.containsKey(resourceId)) {
                val resource = Resource(
                    id = resourceId,
                    iri = resultRow[Resources.iri],
//                    createdAt = DateTime.fromUnix(resultRow[Resources.createdAt]),
//                    updatedAt = DateTime.fromUnix(resultRow[Resources.updatedAt]),
//                    confirmed = resultRow[Resources.confirmed],
                    properties = mutableListOf()
                )
                resource.host = origin
                resources[resourceId] = resource
            }

            val resource = resources[resourceId]!!

            if (resultRow[Properties.id] !== null) {
                (resource.properties as MutableCollection<Property>).add(Property(
                    id = resultRow[Properties.id],
//                    createdAt = resultRow[Properties.createdAt].let { DateTime.fromUnix(it) },
//                    updatedAt = resultRow[Properties.updatedAt].let { DateTime.fromUnix(it) },
                    predicate = resultRow[Properties.predicate].iri(),
                    order = resultRow[Properties.order] ?: 0,

//                    boolean = resultRow[Properties.boolean],
                    string = resultRow[Properties.string],
//                    text = resultRow[Properties.text],
                    dateTime = resultRow[Properties.dateTime]?.let {
                        val unixMillis = resultRow[Properties.dateTime]!!
                            .atZone(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                        DateTime.fromUnix(unixMillis)
                    },
                    integer = resultRow[Properties.integer],
//                    bigInt = resultRow[Properties.bigInt]?.let { BigInteger.valueOf(it) } ,
//                    uuid = resultRow[Properties.uuid],
                    node = resultRow[Properties.node]?.let {
                        ResourceReference(
                            id = it,
                            iri = resultRow[linkedResources[Resources.iri]].iri()
                        )
                    },
                    iri = resultRow[Properties.iri]?.let {
                        if (it.contains("://")) {
                            it.iri()
                        } else {
                            // TODO: bugsnag
                            null
                        }
                    }
                ))
            }
        }

        return resources
    }

    private fun joinedPropertyTable(op: Op<Boolean>): Query {
        val linkedResources = Resources.alias("lr")

        return (Resources.leftJoin(
            (Properties.leftJoin(linkedResources, { node }, { linkedResources[Resources.id] })),
            { Resources.id },
            { Properties.resource }
        ))
            .select(op)
            .orderBy(
                Resources.id to SortOrder.ASC,
                Properties.predicate to SortOrder.ASC,
                Properties.order to SortOrder.ASC
            )
            .groupBy(Resources.id, Properties.id, linkedResources[Resources.id])
    }
}
