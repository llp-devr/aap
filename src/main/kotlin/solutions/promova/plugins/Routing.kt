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
                val ipAddress = call.request.local.remoteHost.trim()
                if (ipAddress != "localhost" && ipAddress != "127.0.0.1" && ipAddress != "::1") {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val originHeader = call.request.headers["Origin"]
                if (!originHeader.isNullOrEmpty()) {
                    call.response.header("Access-Control-Allow-Origin", originHeader)
                }

                val parameters = call.request.queryParameters

                val r = parameters["r"]
                    ?: throw IllegalArgumentException("Missing r in query string")
                val u = parameters["u"]
                    ?: throw IllegalArgumentException("Missing u in query string")

                val task = Task(Json.decodeFromString<Requisicao>(r), u)

                print("\n")
                print(task)
                print("\n")

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
