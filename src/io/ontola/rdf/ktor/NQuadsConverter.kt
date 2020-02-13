package io.ontola.rdf.ktor

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.content.OutgoingContent
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import io.ontola.apex.model.Property
import io.ontola.apex.model.Resource
import io.ontola.rdf.serialization.IRIProvider
import io.ontola.rdf.serialization.PropertyProvider
import io.ontola.rdf.serialization.ResourceProvider
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.serialization.SerialName
import org.eclipse.rdf4j.model.IRI
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class NQuadsConverter : ContentConverter {
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        context.subject

        return "test"
    }

    @ExperimentalStdlibApi
    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: io.ktor.http.ContentType,
        value: Any
    ): OutgoingContent = object : OutgoingContent.WriteChannelContent() {
        override val contentType = ContentType.Application.NQuads
        override suspend fun writeTo(channel: ByteWriteChannel) {
            when(value) {
                is Collection<*> -> value.filterNotNull().map { v -> serialize(v, channel) }
                else -> serialize(value, channel)
            }
        }
    }
}

@ExperimentalStdlibApi
private suspend fun serialize(value: Any, output: ByteWriteChannel) {
    val type = value::class
    val resources = type.members.find { m -> m.hasAnnotation<ResourceProvider>() }

    if (resources === null) {
        return serializeResource(value, output)
    }

    resources.let {
        println((it.call(value) as Collection<Resource>))
        (it.call(value) as Collection<Resource>).forEach { resource ->
            serializeResource(resource, output)
        }
    }
}

@ExperimentalStdlibApi
private suspend fun serializeResource(value: Any, output: ByteWriteChannel) {
    val type = value::class
    val iriProvider = type.findAnnotation<IRIProvider>()
    if (iriProvider === null) {
        throw Error("Resource not rdf serializable ($type)") // TODO: type error
    }

    val property = type.members.find { m -> m.name == iriProvider.property }
    val subject = property?.call(value).toString()

    type
        .members
        .filter { m -> m.hasAnnotation<SerialName>() }
        .forEach { m ->
            val predicate = m.findAnnotation<SerialName>()!!.value
            val `object` = m.call(value)
            if (`object` is IRI) {
                writeTriple(
                    subject,
                    predicate,
                    `object`.stringValue(),
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode",
                    "",
                    output
                )
            }
        }

    val prop = type.members.find { m -> m.hasAnnotation<PropertyProvider>() }

    prop?.let { properties ->
            (properties.call(value) as Collection<Property>).forEach { property ->
                val predicate = property.predicate.toString()
                writeTriple(
                    subject,
                    predicate,
                    property.value,
                    property.datatype,
                    property.language,
                    output
                )
            }
        }
}

private suspend fun writeTriple(
    s: String,
    p: String,
    v: String,
    dt: String,
    l: String,
    output: ByteWriteChannel
) {
    if (s.contains(":")) {
        output.writeStringUtf8("<")
        output.writeStringUtf8(s)
        output.writeStringUtf8(">")
    } else {
        output.writeStringUtf8("_:")
        output.writeStringUtf8(s)
    }
    output.writeStringUtf8(" <")
    output.writeStringUtf8(p)
    output.writeStringUtf8("> ")
    encodeValue(v, dt, l, output)
    output.writeStringUtf8(" .\n")
}

const val xsdLiteralPrefix = "\"^^<"
const val langStringPrefix = "\"@"

private suspend fun encodeValue(value: String, dt: String, l: String, output: ByteWriteChannel) {
    when (dt) {
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode" -> {
            output.writeStringUtf8("<$value>")
            return
        }
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode" -> {
            output.writeStringUtf8("_:$value")
            return
        }
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" -> {
            output.writeStringUtf8("\"")
            output.writeStringUtf8(value)
            output.writeStringUtf8(langStringPrefix)
            output.writeStringUtf8(dt)
            return
        }
    }

    output.writeStringUtf8("\"")
    output.writeStringUtf8(value)
    output.writeStringUtf8(xsdLiteralPrefix)
    output.writeStringUtf8(dt)
    output.writeStringUtf8(">")
    return
}
