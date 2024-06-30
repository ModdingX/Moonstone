package org.moddingx.moonstone.model

import com.google.gson.{JsonElement, JsonObject, JsonSyntaxException}

case class FileEntry(project: JsonElement, file: JsonElement, side: Side, locked: Boolean) {

  def toJson: JsonObject = {
    val json = new JsonObject
    json.add("project", project)
    json.add("file", file)
    json.addProperty("side", side.id)
    json.addProperty("locked", locked)
    json
  }
  
  def withFile(newFile: JsonElement): FileEntry = FileEntry(project, newFile, side, locked)
  def withSide(newSide: Side): FileEntry = FileEntry(project, file, newSide, locked)
  def withLock(isLocked: Boolean): FileEntry = FileEntry(project, file, side, isLocked)
}

object FileEntry {
  
  def fromJson(json: JsonObject, api: Int = FileListIO.API): Option[FileEntry] = {
    try {
      val project = json.get("project")
      val file = json.get("file")
      val side = if (json.has("side")) Side.byId(json.get("side").getAsString) else Side.COMMON
      val locked = json.has("locked") && json.get("locked").getAsBoolean
      Some(FileEntry(project, file, side, locked))
    } catch {
      case e: JsonSyntaxException =>
        System.err.println("Invalid file entry.")
        e.printStackTrace()
        None
    }
  }
}
