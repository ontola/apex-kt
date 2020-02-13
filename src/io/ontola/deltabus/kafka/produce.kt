package io.ontola.deltabus.kafka

import io.ontola.deltabus.DeltaMessage
import io.ontola.deltabus.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Duration

@ExperimentalCoroutinesApi
fun produce(fromBeginning: Boolean): Flow<DeltaMessage> = flow {
    val consumer = EventBus.getBus().createSubscriber(fromBeginning)
    if (fromBeginning) {
        EventBus.getBus().resetTopicToBeginning(consumer)
    }

    while (true) {
        val records = consumer.poll(Duration.ofMillis(500))
        for (record in records) {
            emit(DeltaMessage(record.topic(), record.key(), record.value()))
        }
    }
}.flowOn(Dispatchers.IO)
