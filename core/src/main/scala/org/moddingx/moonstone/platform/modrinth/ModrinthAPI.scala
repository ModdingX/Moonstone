package org.moddingx.moonstone.platform.modrinth

import com.google.gson.JsonElement
import org.moddingx.moonstone.Util

import java.io.IOException
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets

class ModrinthAPI {

  // Trailing slash is important, so URI#resolve works properly
  private val BaseURL = URI.create("https://api.modrinth.com/v2/")
  private val client: HttpClient = HttpClient.newHttpClient()
  
  def request(route: String, query: (String, Any)*): JsonElement = {
    val routeStr = route.dropWhile(_ == '/')
    val queryStr = if (query.isEmpty) {
      ""
    } else {
      query.map(entry => URLEncoder.encode(entry._1, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry._2.toString, StandardCharsets.UTF_8)).mkString("?", "&", "")
    }

    val result: Either[JsonElement, IOException] = try {
      client.send[Either[JsonElement, IOException]](
        HttpRequest.newBuilder()
          .GET()
          .uri(BaseURL.resolve(routeStr + queryStr))
          .header("Accept", "application/json")
          .header("User-Agent", "ModdingX/Moonstone")
          .build(),
        (resp: HttpResponse.ResponseInfo) => {
          if ((resp.statusCode() / 100) == 2 && resp.statusCode() != 204) {
            HttpResponse.BodySubscribers.mapping(
              HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
              (str: String) => try {
                Left(Util.GSON.fromJson(str, classOf[JsonElement]))
              } catch {
                case e: Exception => Right(new IOException("Failed to parse modrinth API response", e))
              }
            )
          } else {
            HttpResponse.BodySubscribers.replacing(Right(new IOException("HTTP Status Code: " + resp.statusCode())))
          }
        }
      ).body()
    } catch {
      case e: InterruptedException =>
        Thread.currentThread.interrupt()
        throw new IOException("Interrupted", e)
    }
    
    result match {
      case Left(json) => json
      case Right(ex) => throw ex
    }
  }
}
