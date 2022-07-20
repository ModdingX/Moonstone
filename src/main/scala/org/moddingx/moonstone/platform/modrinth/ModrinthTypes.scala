package org.moddingx.moonstone.platform.modrinth

import com.google.gson.{JsonObject, JsonPrimitive}
import org.moddingx.moonstone.Util
import org.moddingx.moonstone.model.{FileEntry, Side}
import org.moddingx.moonstone.platform.{FileDependency, ProjectDependency, ResolvableDependency}

import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._

object ModrinthTypes {

  def project(json: JsonObject): ModrinthProject = {
    val shouldInstallOnClient = json.get("client_side").getAsString != "unsupported"
    val shouldInstallOnServer = json.get("server_side").getAsString != "unsupported"
    ModrinthProject(
      // id is used in regular project requests, project_id is used in search requests
      if (json.has("project_id")) json.get("project_id").getAsString else json.get("id").getAsString,
      json.get("slug").getAsString,
      json.get("project_type").getAsString,
      json.get("title").getAsString,
      json.get("description").getAsString,
      Side.get(shouldInstallOnClient, shouldInstallOnServer),
      Util.option(json, "icon_url")
        .map(_.getAsString)
        .filter(_.nonEmpty) // The search endpoint gives empty strings instead of null
        .flatMap(str => Option(URI.create(str)))
    )
  }

  def version(json: JsonObject): ModrinthVersion = {
    val dependencies: Seq[ModrinthDependency] = Util.option(json, "dependencies")
      .map(_.getAsJsonArray.asScala.toSeq)
      .getOrElse(Seq())
      .map(_.getAsJsonObject)
      .flatMap(dep => {
        if (dep.get("dependency_type").getAsString == "required" && dep.has("version_id") && !dep.get("version_id").isJsonNull) {
          Some(ModrinthVersionDependency(dep.get("version_id").getAsString))
        } else if (dep.get("dependency_type").getAsString == "required" && dep.has("project_id") && !dep.get("project_id").isJsonNull) {
          Some(ModrinthProjectDependency(dep.get("project_id").getAsString))
        } else {
          None
        }
      })
    ModrinthVersion(
      json.get("id").getAsString,
      json.get("project_id").getAsString,
      json.get("name").getAsString,
      dependencies,
      Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(json.get("date_published").getAsString))
    )
  }
}

case class ModrinthProject(
                            id: String,
                            slug: String,
                            projectType: String,
                            name: String,
                            description: String,
                            side: Side,
                            icon: Option[URI]
                          ) {

  def url: Option[URI] = Option(URI.create(s"https://modrinth.com/$projectType/$slug"))
}

case class ModrinthVersion(
                            id: String,
                            projectId: String,
                            name: String,
                            dependencies: Seq[ModrinthDependency],
                            date: Instant
                          )

sealed trait ModrinthDependency {
  def resolve(access: ModrinthAccess): Option[ResolvableDependency]
}

case class ModrinthProjectDependency(id: String) extends ModrinthDependency {

  override def resolve(access: ModrinthAccess): Option[ResolvableDependency] = {
    Some(ProjectDependency(new JsonPrimitive(id), access.list, side = access.cache.project(id).side))
  }
}

case class ModrinthVersionDependency(id: String) extends ModrinthDependency {

  override def resolve(access: ModrinthAccess): Option[ResolvableDependency] = {
    val version = access.cache.version(id)
    Some(FileDependency(new FileEntry(
      new JsonPrimitive(version.projectId),
      new JsonPrimitive(version.id),
      Side.COMMON, false
    ), side = access.cache.project(version.projectId).side))
  }
}
