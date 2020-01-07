package io.ontola.rdf.serialization.jsonrdf

import io.ontola.apex.model.Resource
import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import org.eclipse.rdf4j.model.IRI

/**
 * This demo shows how user can define his own custom text format.
 *
 * Because text formats usually record field names,
 * here we are using writeElement method, which provide information about current field.
 * Also, unlike binary demo, there are object separators, which are written in
 * writeBegin and writeEnd methods.
 */

class Parser(private val inp: Reader) {
    var cur: Int = inp.read()

    fun next() {
        cur = inp.read()
    }

    fun skipWhitespace(vararg c: Char) {
        while (cur >= 0 && (cur.toChar().isWhitespace() || cur.toChar() in c))
            next()
    }

    fun expect(c: Char) {
        check(cur == c.toInt()) { "Expected '$c'" }
        next()
    }

    fun expectAfterWhiteSpace(c: Char) {
        skipWhitespace()
        expect(c)
    }

    fun nextUntil(vararg c: Char): String {
        val sb = StringBuilder()
        while (cur >= 0 && cur.toChar() !in c) {
            sb.append(cur.toChar())
            next()
        }
        return sb.toString()
    }
}

class JsonRDFOutput(private val out: PrintWriter) : ElementValueEncoder () {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        if (desc is ArrayListClassDesc) {
            out.print("[\n")
            return this
        }

        out.print("{\n")
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (desc is ArrayListClassDesc) {
            out.print("\n]")
            return
        }

        out.print("\n}")
    }

    /**
     * encodeElement should return false, if this field must be skipped
     */
    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        if (index > 0) out.print(", \n")

        if (desc is ArrayListClassDesc) {
            return true
        }

        val propName = desc.getElementName(index)
        if (propName.contains("://")) {
            out.print("<$propName>");
        } else {
            out.print("\"$propName\"");
        }
        out.print(": ")
        return true
    }

    override fun encodeNull() = out.print("null")

    /**
     * encodeValue is called by default, if primitives encode methods
     * (like encodeInt, etc) are not overridden.
     */
    override fun encodeValue(value: Any) {
        if (value is IRI) {
            out.print("<$value>")
            return
        }
        if (value is Resource) {
            out.print("<${value.iri}>")
            return
        }
        if (value is Boolean || value is Int) {
            out.print(value.toString())
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

        out.print("\"$value\"")

        if (xsdType !== "string") {
            out.print("^^<http://www.w3.org/2001/XMLSchema#${xsdType}>")
        }
    }
}

class JsonRDFInput(val inp: Parser) : ElementValueDecoder() {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val type = typeParams[0]
        if (type is Collection<*>) {
            inp.expectAfterWhiteSpace('[')
            return this
        }

        inp.expectAfterWhiteSpace('{')
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        inp.expectAfterWhiteSpace('}')
    }

    /**
     * readElement must return index of field that should be read next.
     * If there are no more fields, return READ_DONE.
     * If you want to read fields in order without call to this method,
     * return READ_ALL (this is default behaviour).
     */
    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        inp.skipWhitespace(',')
        val name = inp.nextUntil(':', '}')
        if (name.isEmpty())
            return READ_DONE
        val index = desc.getElementIndex(name)
        inp.expect(':')
        return index
    }

    private fun decodeToken(): String {
        inp.skipWhitespace()
        return inp.nextUntil(' ', ',', '}')
    }

    override fun decodeNotNullMark(): Boolean {
        inp.skipWhitespace()
        if (inp.cur != 'n'.toInt()) return true
        return false
    }

    override fun decodeNull(): Nothing? {
        check(decodeToken() == "null") { "'null' expected" }
        return null
    }

    override fun decodeBoolean(): Boolean = decodeToken().toBoolean()
    override fun decodeByte(): Byte = decodeToken().toByte()
    override fun decodeShort(): Short = decodeToken().toShort()
    override fun decodeInt(): Int = decodeToken().toInt()
    override fun decodeLong(): Long = decodeToken().toLong()
    override fun decodeFloat(): Float = decodeToken().toFloat()
    override fun decodeDouble(): Double = decodeToken().toDouble()

    override fun decodeEnum(enumDescription: SerialDescriptor): Int {
        return enumDescription.getElementIndexOrThrow(decodeToken())
    }

    override fun decodeString(): String {
        inp.expectAfterWhiteSpace('"')
        val value = inp.nextUntil('"')
        inp.expect('"')
        return value
    }

    override fun decodeChar(): Char = decodeString().single()
}

data class JsonRDFConfiguration @UnstableDefault constructor(
    val useCanonical: Boolean = true
) {
    companion object {
        @JvmStatic
        @UnstableDefault
        val Default = JsonRDFConfiguration()
    }
}

class JsonRDF

constructor(
    @JvmField internal val configuration: JsonRDFConfiguration = JsonRDFConfiguration.Default,
    context: SerialModule = EmptyModule
): AbstractSerialFormat(context), StringFormat  {

    @ImplicitReflectionSerializer
    override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T {
        val input = JsonRDFInput(Parser(StringReader(string)))
        return input.decode(deserializer)
    }

    override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String {
        val sw = StringWriter()
        val out = JsonRDFOutput(PrintWriter(sw))
        out.encode(serializer, obj)

        return sw.toString()
    }

    companion object : StringFormat {
        @UnstableDefault
        val plain = JsonRDF(JsonRDFConfiguration.Default)
        override val context: SerialModule get() = EmptyModule

        @UnstableDefault
        override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String =
            plain.stringify(serializer, obj)

        @ImplicitReflectionSerializer
        @UnstableDefault
        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
            plain.parse(deserializer, string)
    }
}
