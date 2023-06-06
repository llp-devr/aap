package solutions.promova.plugins

import io.ktor.http.*

import io.ktor.serialization.kotlinx.json.*

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

import java.util.*

@Serializable
data class Version(val versao: String)

val successGif: ByteArray = Base64.getDecoder().decode("R0lGODlhAQABAPAAAEz/AAAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw==")

fun Application.configureRouting(version: String) {

    install(ContentNegotiation) {
        json()
    }

    routing {
        route("/pjeOffice") {
            get("/requisicao/") {
                // Check for authorized IP addresses
                if (call.request.local.remoteHost.trim() !in setOf("localhost", "127.0.0.1", "::1")) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                // If Origin header is present, allow access
                call.request.headers["Origin"]?.let {
                    call.response.header("Access-Control-Allow-Origin", it)
                }

                val parameters = call.request.queryParameters

                val r = parameters["r"]
                    ?: throw IllegalArgumentException("Missing r in query string")
                val u = parameters["u"]
                    ?: throw IllegalArgumentException("Missing u in query string")

                val task = Task(Json.decodeFromString<Requisicao>(r), u)

                processTask(task)

                call.response.header(HttpHeaders.Date, currentDateAndTime())

                call.respondBytes(successGif)
            }
            options("/requisicao/") {
                call.response.header("Access-Control-Allow-Methods", "GET, OPTIONS, POST")
                call.response.header("Access-Control-Allow-Private-Network", "true")

                call.respond(HttpStatusCode.NoContent)
            }
            get("/versao/") {
                call.response.header("Cache-Control", "no-cache")
                call.response.header("Access-Control-Allow-Origin", "*")
                call.respond(Version(versao = version))
            }
            get("/") { call.respondBytes(successGif) }
            get("{...}") { call.respondBytes(successGif) }
        }

    }
}
