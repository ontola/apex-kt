package io.ontola.apex

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CachingHeaders
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.jackson.jackson
import io.ktor.request.port
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ontola.apex.service.DatabaseFactory
import io.ontola.apex.service.ResourceService
import io.ontola.rdf.ktor.ContentType
import io.ontola.rdf.ktor.nquads

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ConditionalHeaders)

//    install(DefaultHeaders) {
//        header("X-Engine", "Ktor") // will send this header with each response
//    }

//    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
//    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy

//    install(Authentication) {}

    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Application.NQuads.withoutParameters() ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                else -> null
            }
        }
    }

    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

        nquads {}
    }

    DatabaseFactory.init(testing)

    val resourceService = ResourceService()

    routing {
        get("/") {
            val origin = call.request.origin
            call.attributes.put(
                RequestVariables.origin,
                "${origin.scheme}://${origin.host}${call.request.port()?.let { ":$it" }}"
            )
            val page = call.request.queryParameters["page"]?.toInt()
            val resources = resourceService.getAllResources(call.attributes, page)

            call.respond(resources)
        }

        get("/{id}") {
            val id = call.parameters["id"]

            val origin = call.request.origin
            call.attributes.put(
                RequestVariables.origin,
                "${origin.scheme}://${origin.host}${call.request.port()?.let { ":$it" }}"
            )
            val resource = resourceService.getResource(id!!.toInt(10), call.attributes)
            if (resource == null) {
                call.respond(HttpStatusCode.NotFound, "404 not found")
            } else {
                call.respond(resource)
            }
        }
    }
}
