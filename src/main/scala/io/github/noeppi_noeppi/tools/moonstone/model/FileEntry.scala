package io.github.noeppi_noeppi.tools.moonstone.model

import com.google.gson.{JsonObject, JsonSyntaxException}

case class FileEntry(projectId: Int, fileId: Int, side: Side, locked: Boolean) {

  def toJson: JsonObject = {
    val json = new JsonObject
    json.addProperty("project", projectId)
    json.addProperty("file", fileId)
    json.addProperty("side", side.id)
    json.addProperty("locked", locked)
    json
  }
  
  def withVersion(newVersion: Int): FileEntry = FileEntry(projectId, newVersion, side, locked)
  def withSide(newSide: Side): FileEntry = FileEntry(projectId, fileId, newSide, locked)
  def withLock(isLocked: Boolean): FileEntry = FileEntry(projectId, fileId, side, isLocked)
}

object FileEntry {
  
  def fromJson(json: JsonObject): Option[FileEntry] = {
    try {
      val projectId = json.get("project").getAsInt
      val fileId = json.get("file").getAsInt
      val side = if (json.has("side")) Side.byId(json.get("side").getAsString) else Side.COMMON
      val locked = json.has("locked") && json.get("locked").getAsBoolean
      Some(FileEntry(projectId, fileId, side, locked))
    } catch {
      case e: JsonSyntaxException =>
        System.err.println("Invalid CurseFile entry.")
        e.printStackTrace()
        None
    }
  }
}
