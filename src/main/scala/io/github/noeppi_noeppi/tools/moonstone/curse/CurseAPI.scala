package io.github.noeppi_noeppi.tools.moonstone.curse

import com.google.gson.{JsonArray, JsonElement, JsonSyntaxException}
import io.github.noeppi_noeppi.tools.moonstone.{PackConfig, Util}

import java.io.{FileNotFoundException, IOException, InputStreamReader}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.nio.charset
import scala.jdk.CollectionConverters._
import scala.reflect.{ClassTag, classTag}

object CurseAPI {

  def query[R <: JsonElement : ClassTag](endpoint: String): Option[R] = query[R, R](endpoint, identity)
  
  def query[R <: JsonElement : ClassTag, T](endpoint: String, factory: R => T): Option[T] = {
    try {
      val url = new URL("https://addons-ecs.forgesvc.net/api/v2/" + endpoint)
      val c = url.openConnection()
      c.addRequestProperty("Accept", "application/json")
      c.addRequestProperty("Cache-Control", "max-age=0")
      c.addRequestProperty("Connection", "keep-alive")
      c.connect()
      val reader = new InputStreamReader(c.getInputStream)
      val json: R = Util.GSON.fromJson(reader, classTag[R].runtimeClass)
      reader.close()
      c.getInputStream.close()
      c match {
        case connection: HttpURLConnection => connection.disconnect()
        case _ =>
      }
      Some(factory(json))
    } catch {
      case _: FileNotFoundException => None
      case e: IOException =>
        System.err.println("Failed to access curse API.")
        e.printStackTrace()
        None
      case e @ (_: JsonSyntaxException | _: NumberFormatException | _: NullPointerException) =>
        System.err.println("Failed to process curse API response.")
        e.printStackTrace()
        None
    }
  }
  
  def searchMods(term: String, config: PackConfig): List[Int] = {
    // categoryID=6 which should work, does not and gives an empty list all the time.
    // However sectionId=6 seems to work
    query[JsonArray, List[Int]]("addon/search?gameId=432&gameVersion=" + config.minecraft + "&gameVersion=" + config.loader + "&sectionId=6&sort=5&pageSize=30&searchFilter=" + URLEncoder.encode(term, charset.StandardCharsets.UTF_8), json => {
      json.asScala.flatMap(e => if (e.isJsonObject) Some(e.getAsJsonObject) else None)
        .filter(e => e.has("id"))
        .map(e => e.get("id").getAsInt).toList
    }).getOrElse(Nil)
  }
}
