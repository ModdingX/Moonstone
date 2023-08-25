package org.moddingx.moonstone.platform.modrinth

import com.google.gson.{JsonArray, JsonElement}
import org.moddingx.moonstone.logic.Destroyable
import org.moddingx.moonstone.platform.ModList

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class ModrinthCache(list: ModList) extends Destroyable {

  private val api = new ModrinthAPI
  private val projects = mutable.Map[String, ModrinthProject]()
  private val versions = mutable.Map[String, ModrinthVersion]()
  private val projectVersions = mutable.Map[String, Seq[String]]()
  private val searchResults = mutable.Map[String, Seq[String]]()

  def project(id: String): ModrinthProject = projects.getOrElseUpdate(id, {
    ModrinthTypes.project(api.request("project/" + encode(id)).getAsJsonObject)
  })

  def version(id: String): ModrinthVersion = versions.getOrElseUpdate(id, {
    ModrinthTypes.version(api.request("version/" + encode(id)).getAsJsonObject)
  })
  
  def loadProjects(ids: Set[String]): Unit = {
    val missingProjects = ids.filter(id => !projects.contains(id))
    if (missingProjects.nonEmpty) {
      val array = new JsonArray()
      for (id <- missingProjects) array.add(id)
      val json = api.request("projects", "ids" -> array)
      for (elem <- json.getAsJsonArray.asScala) {
        val project = ModrinthTypes.project(elem.getAsJsonObject)
        projects.put(project.id, project)
      }
    }
  }

  def loadVersions(ids: Set[String]): Unit = {
    val missingVersions = ids.filter(id => !versions.contains(id))
    if (missingVersions.nonEmpty) {
      val array = new JsonArray()
      for (id <- missingVersions) array.add(id)
      val json = api.request("versions", "ids" -> array)
      for (elem <- json.getAsJsonArray.asScala) {
        val version = ModrinthTypes.version(elem.getAsJsonObject)
        versions.put(version.id, version)
      }
    }
  }
  
  def getVersions(project: String): Seq[ModrinthVersion] = {
    if (projectVersions.contains(project)) {
      val versions = projectVersions(project)
      loadVersions(versions.toSet)
      versions.map(ver => version(ver))
    } else {
      val json = api.request("project/" + encode(project) + "/version", versionFilter: _*)
      val versionsBuilder = Seq.newBuilder[ModrinthVersion]
      for (elem <- json.getAsJsonArray.asScala) {
        val version = ModrinthTypes.version(elem.getAsJsonObject)
        versions.put(version.id, version)
        versionsBuilder.addOne(version)
      }
      val builtVersions = versionsBuilder.result().sortBy(_.date)
      projectVersions.put(project, builtVersions.map(_.id))
      builtVersions
    }
  }
  
  def search(query: String): Seq[ModrinthProject] = {
    if (searchResults.contains(query)) {
      val projects = searchResults(query)
      loadVersions(projects.toSet)
      projects.map(p => project(p))
    } else {
      val json = api.request("search",
        "query" -> query,
        "facets" -> facetFilter,
        "limit" -> 30
      ).getAsJsonObject
      
      val projectsBuilder = Seq.newBuilder[ModrinthProject]
      for (elem <- json.get("hits").getAsJsonArray.asScala) {
        val project = ModrinthTypes.project(elem.getAsJsonObject)
        projects.put(project.id, project)
        projectsBuilder.addOne(project)
      }
      val builtProjects = projectsBuilder.result()
      searchResults.put(query, builtProjects.map(_.id))
      builtProjects
    }
  }

  private def encode(str: String): String = URLEncoder.encode(str, StandardCharsets.UTF_8)

  private def versionFilter: Seq[(String, JsonElement)] = {
    val versions = new JsonArray()
    versions.add(list.mcVersion)
    val loaders = new JsonArray()
    loaders.add(list.loader)
    Seq(
      "game_versions" -> versions,
      "loaders" -> loaders
    )
  }
  
  private def facetFilter: JsonElement = {
    // [["versions:1.16.5"], ["project_type:modpack"]]
    val array = new JsonArray()
    
    val versions = new JsonArray()
    versions.add("versions:" + list.mcVersion)
    array.add(versions)
    
    val loaders = new JsonArray()
    loaders.add("categories:" + list.loader)
    array.add(loaders)
    
    array
  }
  
  def metadataChange(): Unit = {
    projectVersions.clear()
    searchResults.clear()
  }
  
  override def destroy(): Unit = {
    projects.clear()
    versions.clear()
    projectVersions.clear()
    searchResults.clear()
  }
}
