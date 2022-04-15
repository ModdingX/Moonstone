package io.github.noeppi_noeppi.tools.moonstone.action

import com.google.gson.{JsonArray, JsonObject}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.fileChooser.{FileChooser, FileChooserDescriptorFactory}
import com.intellij.openapi.ui.MessageDialogBuilder
import io.github.noeppi_noeppi.tools.moonstone.Util
import io.github.noeppi_noeppi.tools.moonstone.model.{FileEntry, Side}

import java.io.IOException
import java.nio.file.{FileSystems, Files, Paths}

class MoonstoneAction extends AnAction {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val file = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFileDescriptor(), e.getProject, null);
    if (file != null) {
      try {
        val fs = FileSystems.newFileSystem(file.toNioPath, null: ClassLoader)
        val manifest = fs.getPath("manifest.json")
        if (Files.exists(manifest)) {
          val projectRoot = Paths.get(e.getProject.getBasePath)
          // create output path
          val buildPath = projectRoot.resolve("build").resolve("tmp")
          Files.createDirectories(buildPath)

          val reader = Files.newBufferedReader(manifest)
          val json = Util.GSON.fromJson(reader, classOf[JsonObject])
          reader.close()

          val modlist = new JsonArray
          json.get("files").getAsJsonArray.forEach(entry => {
            val entryObject = entry.getAsJsonObject
            modlist.add(new FileEntry(entryObject.get("projectID").getAsInt, entryObject.get("fileID").getAsInt, Side.COMMON, false).toJson)
          })

          val modlistPath = projectRoot.resolve("modlist.json")
          if (Files.exists(modlistPath)) {
            if (!new MessageDialogBuilder.YesNo("File already exists", "This file already exists, do you want to override it?").ask(e.getProject)) {
              return
            }
          }
          Files.write(modlistPath, Util.GSON.toJson(modlist).getBytes())
        }

        fs.close()
      } catch {
        case _: IOException =>
          if (new MessageDialogBuilder.YesNo("File cannot be read", "Do you want to select another file?").ask(e.getProject)) {
            actionPerformed(e)
          };
      }
    }
  }
}
