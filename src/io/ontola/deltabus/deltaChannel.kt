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

import io.ontola.apex.service.DatabaseFactory
import io.ontola.apex.service.DocumentService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.exitProcess

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun processDeltas(producer: Flow<DeltaMessage>) = withContext(Dispatchers.Default) {
    try {
        pipeline(producer).collect()
    } catch (e: Exception) {
        println("Fatal error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
fun pipeline(flow: Flow<DeltaMessage>, testing: Boolean = false): Flow<*> {
    val svc = DocumentService()
    DatabaseFactory.init(testing)

    return flow
        .filter { isDelta(it) }
        .map { parseMessage(it) }
        .map { splitDelta(it) }
        .flattenConcat()
        .map { writeDelta(svc, it.first, it.second) }
        .catch { e -> println("[pipeline] Caught $e") }
}
