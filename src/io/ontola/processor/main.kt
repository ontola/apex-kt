/*
 * Apex
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.processor

import io.ontola.deltabus.*
import io.ontola.deltabus.kafka.produce
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.*

/**
 * Listens to kafka streams and processes delta streams into the file system.
 *
 * TODO: Add error handling service
 */
@InternalCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    val ctx = ORIContext.getCtx()

    printInitMessage(ctx.config)

    ensureOutputFolder(ctx.config)

    var primaryFlag = ""
    if (args.isNotEmpty()) {
        primaryFlag = args[0]
    }

    when (primaryFlag) {
//        "--clean-old-versions" -> {
//            cleanOldVersionsAsync().await()
//        }
        else -> {
            val cmd = arrayListOf("processDeltas", primaryFlag).joinToString(" ")
            val deltas = launch(coroutineContext) {
                val channel = processDeltas(produce(primaryFlag == "--from-beginning"))
                deltasToDocuments(channel).collect()
            }

            joinAll(deltas)
        }
    }

    joinAll()
}

fun ensureOutputFolder(settings: Properties) {
    val baseDirectory = File(settings.getProperty("ori.api.dataDir"))
    if (!baseDirectory.exists()) {
        baseDirectory.mkdirs()
    }
}

fun printInitMessage(p: Properties) {
    println("================================================")
    println("Starting Apex\n")
    val keys = p.keys()
    while (keys.hasMoreElements()) {
        val key = keys.nextElement() as String
        val value = p.get(key)
        println(key.substring("ori.api.".length) + ": " + value)
    }
    println("================================================")
}
