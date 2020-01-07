package io.ontola.rdf.serialization

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
public annotation class IRIProvider(val property: String)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
public annotation class PropertyProvider()

///**
// * Instructs to use specific serializer for class, property or type argument.
// *
// * If argument is omitted, plugin will generate default implementation inside the class.
// */
//@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
//public annotation class SerializableProperty(
//    val with: KClass<out KSerializer<*>> = KSerializer::class, // it means -- use default serializer by default
//    val iri: String
//)
