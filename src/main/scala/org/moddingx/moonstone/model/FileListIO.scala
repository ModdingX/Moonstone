package org.moddingx.moonstone.model

import com.google.gson.{JsonArray, JsonElement, JsonObject, JsonSyntaxException}
import org.moddingx.moonstone.Util
import org.moddingx.moonstone.platform.ModdingPlatform

import java.io.{InputStreamReader, Reader, Writer}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.annotation.WillClose
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object FileListIO {
  
  val API: Int = 2
  
  private val latestMC = try {
    val in: Reader = new InputStreamReader(new URL("https://piston-meta.mojang.com/mc/game/version_manifest.json").openStream(), StandardCharsets.UTF_8)
    val json = Util.GSON.fromJson(in, classOf[JsonObject])
    in.close()
    json.get("latest").getAsJsonObject.get("release").getAsString
  } catch {
    case _: Exception => "1.16.5"
  }
  
  def load(@WillClose reader: Reader): ReadResult = {
    try {
      val json = Util.GSON.fromJson(reader, classOf[JsonElement])
      reader.close()
      
      if (json == null) return ReadResult(Data.FALLBACK, needsUpdate = true)
      
      val api: Int =
        if (json.isJsonArray) 1
        else json.getAsJsonObject.get("api").getAsInt
      
      val data: Data = api match {
        case 1 => loadAPI1(json.getAsJsonArray)
        case API => loadAPI2(json.getAsJsonObject)
        case _ => Data.FALLBACK
      }
      
      // Update for any API that differs from the current one.
      data.asResult(api != API)
    } catch {
      case e: JsonSyntaxException =>
        System.err.println("Failed to read modlist file")
        e.printStackTrace()
        ReadResult(Data.FALLBACK, needsUpdate = true)
    }
  }
  
  def save(writer: Writer, data: Data): Unit = {
    val json = new JsonObject
    json.addProperty("api", FileListIO.API)
    json.addProperty("platform", data.platform.name)
    json.addProperty("loader", data.loader)
    json.addProperty("minecraft", data.mcVersion)
    
    val installed = new JsonArray()
    data.installed.map(_.toJson).foreach(installed.add)
    json.add("installed", installed)
    
    val dependencies = new JsonArray()
    data.dependencies.map(_.toJson).foreach(dependencies.add)
    json.add("dependencies", dependencies)
    
    writer.write(Util.GSON.toJson(json) + "\n")
  }
  
  def loadAPI2(json: JsonObject): Data = {
    val platform = ModdingPlatform.get(if (json.has("platform")) json.get("platform").getAsString else "")
    val loader = if (json.has("loader")) json.get("loader").getAsString else "forge"
    val mcVersion = if (json.has("minecraft")) json.get("minecraft").getAsString else latestMC
    
    val installed = mutable.Set[FileEntry]()
    val dependencies = mutable.Set[FileEntry]()
    
    if (json.has("installed")) {
      for (elem <- json.get("installed").getAsJsonArray.asScala if elem.isJsonObject; entry = elem.getAsJsonObject) {
        FileEntry.fromJson(entry, api = 1).flatMap(platform.validateEntry) match {
          case Some(info) => installed.addOne(info)
          case None =>
        }
      }
    }
    
    if (json.has("dependencies")) {
      for (elem <- json.get("dependencies").getAsJsonArray.asScala if elem.isJsonObject; entry = elem.getAsJsonObject) {
        FileEntry.fromJson(entry, api = 1).flatMap(platform.validateEntry) match {
          case Some(info) => dependencies.addOne(info)
          case None =>
        }
      }
    }
    
    Data(platform, loader, mcVersion, installed.toSet, dependencies.toSet)
  }
  
  def loadAPI1(json: JsonArray): Data = {
    val installed = mutable.Set[FileEntry]()
    val dependencies = mutable.Set[FileEntry]()
    
    for (elem <- json.asScala if elem.isJsonObject; entry = elem.getAsJsonObject) {
      val isInstalled = !entry.has("installed") || entry.get("installed").getAsBoolean
      
      FileEntry.fromJson(entry, api = 1).flatMap(ModdingPlatform.CURSE.validateEntry) match {
        case Some(info) if isInstalled =>
          installed.addOne(info)
        case Some(info) if !isInstalled =>
          dependencies.addOne(info)
        case _ =>
      }
    }
    
    Data(ModdingPlatform.CURSE, "forge", latestMC, installed.toSet, dependencies.toSet)
  }
  
  case class ReadResult(data: Data, needsUpdate: Boolean)
  
  case class Data(
                   platform: ModdingPlatform,
                   loader: String,
                   mcVersion: String,
                   installed: Set[FileEntry],
                   dependencies: Set[FileEntry]
                 ) {
    
    def asResult(needsUpdate: Boolean): ReadResult = {
      val uniqueInstalled: Set[FileEntry] = installed.groupBy(_.project).values.flatMap(_.toSeq.headOption).toSet
      val allInstalledProjects: Set[JsonElement] = uniqueInstalled.map(_.project)
      
      val uniqueDependencies: Set[FileEntry] = dependencies.groupBy(_.project).values.flatMap(_.toSeq.headOption).toSet
      val cleanedDependencies: Set[FileEntry] = uniqueDependencies.filter(entry => !allInstalledProjects.contains(entry.project))
      
      val data = Data(
        platform, loader.toLowerCase(Locale.ROOT), mcVersion.toLowerCase(Locale.ROOT),
        uniqueInstalled, cleanedDependencies
      )
      
      ReadResult(data, needsUpdate || loader != loader.toLowerCase(Locale.ROOT) || mcVersion != mcVersion.toLowerCase(Locale.ROOT)
        || installed.size != uniqueInstalled.size || dependencies.size != cleanedDependencies.size)
    }
  }
  
  object Data {
    
    lazy val FALLBACK: Data = Data(ModdingPlatform.CURSE, "forge", latestMC, Set(), Set())
  }
}
