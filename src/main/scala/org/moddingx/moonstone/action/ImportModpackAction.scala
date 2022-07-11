package org.moddingx.moonstone.action

import com.google.gson.{JsonArray, JsonObject, JsonSyntaxException}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.fileChooser.{FileChooser, FileChooserDescriptorFactory}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.messages.MessagesService
import org.moddingx.moonstone.Util
import org.moddingx.moonstone.model.{FileEntry, Side}

import java.io.IOException
import java.nio.file.{FileSystems, Files, Paths}
import scala.jdk.CollectionConverters._

class ImportModpackAction extends AnAction {

  override def actionPerformed(event: AnActionEvent): Unit = {
    val file = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFileDescriptor(), event.getProject, null)
    if (file != null) {
      try {
        val fs = FileSystems.newFileSystem(file.toNioPath, null: ClassLoader)
        val manifest = fs.getPath("manifest.json")
        if (Files.exists(manifest)) {
          val projectRoot = Paths.get(event.getProject.getBasePath)
          val buildPath = projectRoot.resolve("build").resolve("tmp")
          Files.createDirectories(buildPath)

          val reader = Files.newBufferedReader(manifest)
          val json = Util.GSON.fromJson(reader, classOf[JsonObject])
          reader.close()

          val modlist = new JsonArray
          json.get("files").getAsJsonArray.asScala.map(_.getAsJsonObject).foreach(entry => {
            modlist.add(new FileEntry(entry.get("projectID").getAsInt, entry.get("fileID").getAsInt, Side.COMMON, false).toJson)
          })

          val modlistPath = projectRoot.resolve("modlist.json")
          if (!Files.exists(modlistPath) || new MessageDialogBuilder.YesNo("File already exists", "modlist.json already exists, do you want to overwrite it?").ask(event.getProject)) {
            Files.writeString(modlistPath, Util.GSON.toJson(modlist) + "\n")
            showMessage(event.getProject, "Success", "Modpack Imported")
          }
        } else {
          showMessage(event.getProject, "Could not read file", "No manifest found in modpack export.")
        }

        fs.close()
      } catch {
        case e: IOException => showMessage(event.getProject, "Could not read file", e.getMessage)
        case e: JsonSyntaxException => showMessage(event.getProject, "Invalid modpack export", e.getMessage)
        case e: Exception => showMessage(event.getProject, "An error occurred", e.getClass.getSimpleName + ": " + e.getMessage)
      }
    }
  }
  
  private def showMessage(project: Project, title: String, msg: String): Unit = {
    MessagesService.getInstance().showMessageDialog(project, null, msg, title, Array("Ok"), 0, 0, null, null, false, null)
  }
}
