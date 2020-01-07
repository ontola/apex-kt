package io.ontola.rdf.dsl

import io.ontola.rdf.createIRI
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar

private val factory = SimpleValueFactory.getInstance()

@DslMarker
annotation class ResourceMarker

@ResourceMarker
class ResourceModel(
    var iri: Resource = factory.createBNode(),
    var defaultContext: IRI? = null
) {
    internal val children = arrayListOf<ResourceProp>()

    fun toModel(): Model {
        val model = LinkedHashModel()

        for (c in children) {
            model.addAll(c.toModel())
        }

        return model
    }
}

@ResourceMarker
class ResourceProp(
    private val parent: Resource,
    private val predicate: IRI,
    val context: IRI? = null
) {
    private val dataValues = arrayListOf<Value>()
    private val objectValues = arrayListOf<ResourceModel>()

    fun toModel(): Model {
        val model = LinkedHashModel()

        for (v in dataValues) {
            model.add(
                parent,
                predicate,
                v,
                context
            )
        }

        for (v in objectValues) {
            model.addAll(v.toModel())
            model.add(
                parent,
                predicate,
                v.iri,
                context
            )
        }

        return model
    }

    internal fun addValue(value: Any?) {
        val obj = when (value) {
            is Resource -> value
            is Literal -> value

            is BigDecimal -> factory.createLiteral(value)
            is BigInteger -> factory.createLiteral(value)
            is Byte -> factory.createLiteral(value)
            is Boolean -> factory.createLiteral(value)
            is Date -> factory.createLiteral(value)
            is Double -> factory.createLiteral(value)
            is Float -> factory.createLiteral(value)
            is Int -> factory.createLiteral(value)
            is Long -> factory.createLiteral(value)
            is Short -> factory.createLiteral(value)
            is String -> factory.createLiteral(value)
            is XMLGregorianCalendar -> factory.createLiteral(value)

            else -> throw Exception("Neither a value nor a block given")
        }

        if (obj != null) {
            dataValues.add(obj)
        }
    }

    operator fun ResourceModel.unaryPlus() {
        this@ResourceProp.objectValues.add(this)
    }
}

fun String.iri(): IRI {
    return createIRI(this)
}

fun String.xsdString(): Literal {
    return factory.createLiteral(this)
}

/** Create a model builder. */
fun model(
    iri: Resource = factory.createBNode(),
    defaultContext: IRI? = null,
    init: ResourceModel.() -> Unit
): ResourceModel {
    val model = ResourceModel(iri, defaultContext = defaultContext)
    model.init()

    return model
}

infix fun ResourceProp.model(init: ResourceModel.() -> Unit): ResourceModel {
    val child = ResourceModel()
    child.init()

    +child

    return child
}

/**
 * Add a property on a model.
 * Use a block to create an anonymous nested resource, which inherits the context.
 */
fun ResourceModel.prop(
    predicate: IRI,
    value: Any? = null,
    context: IRI? = null,
    init: (ResourceProp.() -> Unit)? = null
): ResourceProp {
    val childContext = context ?: defaultContext

    val prop = ResourceProp(
        this.iri,
        predicate,
        childContext
    )
    children.add(prop)

    if (init != null) {
        prop.init()
    } else {
        val `object`: Value = when (value) {
            is Resource -> value
            is Literal -> value

            is BigDecimal -> factory.createLiteral(value)
            is BigInteger -> factory.createLiteral(value)
            is Byte -> factory.createLiteral(value)
            is Boolean -> factory.createLiteral(value)
            is Date -> factory.createLiteral(value)
            is Double -> factory.createLiteral(value)
            is Float -> factory.createLiteral(value)
            is Int -> factory.createLiteral(value)
            is Long -> factory.createLiteral(value)
            is Short -> factory.createLiteral(value)
            is String -> factory.createLiteral(value)
            is XMLGregorianCalendar -> factory.createLiteral(value)

            else -> return prop
        }
        prop.addValue(`object`)
    }

    return prop
}
