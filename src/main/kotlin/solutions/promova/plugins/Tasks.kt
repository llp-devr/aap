package solutions.promova.plugins

import java.nio.charset.Charset

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import java.util.Locale

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.util.EntityUtils

import solutions.promova.CryptoHandler

@Serializable
data class Task(
    val r: Requisicao,
    val u: String
)

@Serializable
data class Requisicao(
    val sessao: String,
    val aplicacao: String,
    val servidor: String,
    val codigoSeguranca: String,
    val tarefaId: String,
    val tarefa: String
)

@Serializable
data class Tarefa(
    val enviarPara: String,
    val mensagem: String,
    val token: String? = null
)

fun processTask(task: Task) {
    val cryptoHandler = CryptoHandler()

    val tarefa = Json.decodeFromString<Tarefa>(task.r.tarefa)
    val url = task.r.servidor + tarefa.enviarPara

    val httpclient = HttpClients.createDefault()
    val httpPost = HttpPost(url)

    httpPost.setHeader("Cookie", task.r.sessao)
    httpPost.setHeader("versao", "0.0.1")

    val params = ArrayList<NameValuePair>()
    params.add(BasicNameValuePair("assinatura", cryptoHandler.sign(tarefa.mensagem)))
    params.add(BasicNameValuePair("cadeiaCertificado", cryptoHandler.certChain()))
    httpPost.entity = UrlEncodedFormEntity(params, Charset.forName("UTF-8"))

    val response = httpclient.execute(httpPost)

    try {
        println("[response bytes] " + EntityUtils.toString(response.entity))
    } finally {
        response.close()
    }

}

fun currentDateAndTime(): String {
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    val currentDate = ZonedDateTime.now()
    return formatter.format(currentDate)
}