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

import io.ontola.deltabus.ORio.Companion.parseToModel
import org.eclipse.rdf4j.model.Model
import java.util.*

private val config: Properties = ORIContext.getCtx().config
internal val deltaTopic = config.getProperty("ori.api.kafka.topic") ?: throw Exception("Delta topic not set")

class UnknownRecordType(msg: String) : Error(msg)

/**
 * Convert a {DeltaMessage} to in-memory triple representation
 */
fun parseMessage(record: DeltaMessage): Model =
    when (record.topic) {
        deltaTopic -> parseToModel(record.message)
        else -> throw UnknownRecordType("Unknown record type")
    }

/**
 * Checks if a kafka event is a delta event,
 * meaning it contains data synchronization updates.
 */
fun isDelta(event: DeltaMessage): Boolean {
    return event.topic == deltaTopic
}
