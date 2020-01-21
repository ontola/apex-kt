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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import kotlin.system.exitProcess

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun processDeltas(producer: Flow<DeltaMessage>): Flow<Pair<IRI, Model>> = withContext(Dispatchers.Default) {
    try {
        pipeline(producer)
    } catch (e: Exception) {
        println("Fatal error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
fun pipeline(flow: Flow<DeltaMessage>): Flow<Pair<IRI, Model>> {
    return flow
        .filter { isDelta(it) }
        .map { parseMessage(it) }
        .map { splitDelta(it) }
        .flattenConcat()
        .catch { e -> println("Caught $e") }
}
