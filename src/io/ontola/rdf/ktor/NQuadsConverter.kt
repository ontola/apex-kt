package io.ontola.rdf.ktor

import com.soywiz.klock.DateTime
import com.soywiz.klock.ISO8601
import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.content.OutgoingContent
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import io.ontola.apex.model.Property
import io.ontola.apex.model.Resource
import io.ontola.apex.model.ResourceReference
import io.ontola.rdf.serialization.IRIProvider
import io.ontola.rdf.serialization.PropertyProvider
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
            if (value is Collection<*>) {
                value.filterNotNull().map { v -> serialize(v, channel) }
            } else {
                serialize(value, channel)
            }
        }
    }
}

@ExperimentalStdlibApi
private suspend fun serialize(value: Any, output: ByteWriteChannel) {
    val type = value::class
    val iriProvider = type.findAnnotation<IRIProvider>()
    if (iriProvider === null) {
        throw Error("Resource not rdf serializable") // TODO: type error
    }

    val property = type.members.find { m -> m.name == iriProvider.property }
    val subject = property?.call(value).toString()

    type
        .members
        .filter { m -> m.hasAnnotation<SerialName>() }
        .forEach { m ->
            val predicate = m.findAnnotation<SerialName>()!!.value
            val `object` = m.call(value)
            if (`object` !== null) {
                writeTriple(subject, predicate, `object`, output)
            }
        }

    val prop = type.members.find { m -> m.hasAnnotation<PropertyProvider>() }

    prop?.let { properties ->
            (properties.call(value) as Collection<Property>).forEach { property ->
                val predicate = property.predicate.toString()
                val `object` = property.value()
                writeTriple(subject, predicate, `object`, output)
            }
        }
}

private suspend fun writeTriple(s: String, p: String, o: Any, output: ByteWriteChannel) {
    output.writeStringUtf8("<")
    output.writeStringUtf8(s)
    output.writeStringUtf8("> <")
    output.writeStringUtf8(p)
    output.writeStringUtf8("> ")
    encodeValue(o, output)
    output.writeStringUtf8(" .\n")
}

const val xsdLiteralPostfix = "\"^^<http://www.w3.org/2001/XMLSchema#"

private suspend fun encodeValue(value: Any, output: ByteWriteChannel) {
    if (value is IRI) {
        output.writeStringUtf8("<$value>")
        return
    }
    if (value is Resource) {
        output.writeStringUtf8("<${value.iri}>")
        return
    }

    if (value is DateTime) {
        output.writeStringUtf8("\"${value.format(ISO8601.DATETIME_COMPLETE)}${xsdLiteralPostfix}dateTime>")
        return
    }

    if (value is ResourceReference) {
        output.writeStringUtf8("<${value.iri}>")
        return
    }

    if (value is String) {
        val convertedValue = value.replace("\n", "\\n")
        output.writeStringUtf8("\"")
        output.writeStringUtf8(convertedValue)
        output.writeStringUtf8(xsdLiteralPostfix)
        output.writeStringUtf8("string>")
        return
    }

    val xsdType = when (value) {
        is Boolean -> "boolean"
        is Byte -> "byte"
        is Short -> "short"
        is Int -> "int"
        is Long -> "long"
        is Float -> "float"
        is Double -> "double"
        is Char -> "char"
        else -> "string"
    }

    output.writeStringUtf8("\"")
    output.writeStringUtf8(value.toString())
    output.writeStringUtf8(xsdLiteralPostfix)
    output.writeStringUtf8(xsdType)
    output.writeStringUtf8(">")
}
