/*
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.deltabus

import io.ontola.rdf.createIRI
import io.ontola.rdf.getQueryParameter
import kotlinx.coroutines.flow.flow
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import java.net.URI
import java.util.*

private typealias PartitionMap = HashMap<Resource, List<Statement>>

/** Splits a delta per document. */
fun splitDelta(event: Model) = flow {
    val partitions = partition(event)
    for ((doc, delta) in partitions) {
        emit(Pair(doc, delta))
    }
}

/** Partitions a delta into separately processable slices. */
private fun partition(data: Model): Map<IRI, Model> {
    val subjectBuckets = splitByDocument(data)
    return partitionPerDocument(subjectBuckets)
}

/**
 * Partitions a model by target graph / base document. Any fragment in the IRI will be trimmed, for it cannot be
 * queried from the web as a document.
 *
 * The context's `graph` query parameter takes precedence over the subject.
 */
private fun splitByDocument(model: Model): PartitionMap {
    val partitions = PartitionMap()

    // TODO: implement an RDFHandler which does this while parsing
    for (s: Statement in model) {
        val doc = doc(s.context?.let { getQueryParameter(s.context, "graph") } ?: s.subject)
        val stmtList = partitions[doc]
        if (!stmtList.isNullOrEmpty()) {
            partitions[doc] = stmtList.plus(s)
        } else {
            partitions[doc] = listOf(s)
        }
    }

    return partitions
}

/** Organizes anonymous resources into the bucket which refers to them */
private fun partitionPerDocument(buckets: PartitionMap): Map<IRI, Model> {
    val bNodeForestReferences = HashMap<BNode, Resource>()
    val forests = buckets
        .filterKeys { key -> key is IRI }
        .map { bucket ->
            val iri = bucket.key as IRI
            val model = LinkedHashModel()
            bucket.value.forEach { statement ->
                val obj = statement.`object`
                if (obj is BNode) {
                    bNodeForestReferences[obj] = statement.subject
                }

                model.add(statement)
            }

            iri to model
        }
        .toMap()

    buckets
        .filterKeys { key -> key is BNode }
        .forEach { bucket ->
            val homeForest = bNodeForestReferences[bucket.key]
            if (homeForest == null) {
                println("Dangling blank node '${bucket.key}'")
            } else {
                forests[homeForest]!!.addAll(bucket.value)
            }
        }

    return forests
}

private fun doc(subject: Resource): Resource {
    if (subject is BNode) {
        return subject
    }

    val subj = URI(subject.stringValue())

    return createIRI(URI(
        subj.scheme,
        subj.userInfo,
        subj.host,
        subj.port,
        subj.path,
        subj.query,
        null
    ).toString())
}
