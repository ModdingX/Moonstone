package io.github.noeppi_noeppi.tools.moonstone.action

import com.google.gson.{JsonArray, JsonObject}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.fileChooser.{FileChooser, FileChooserDescriptorFactory}
import io.github.noeppi_noeppi.tools.moonstone.Util
import io.github.noeppi_noeppi.tools.moonstone.model.{FileEntry, Side}

import java.io.{FileInputStream, FileOutputStream, FileWriter, IOException}
import java.nio.file.{FileSystems, Files, Paths}
import java.util.Properties

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

          val writer = new FileWriter(projectRoot.resolve("modlist.json").toFile)
          writer.write(Util.GSON.toJson(modlist))
          writer.close()

          // change pack version
          val propertiesFile = projectRoot.resolve("gradle.properties")
          if (!Files.exists(propertiesFile)) {
            Files.createFile(propertiesFile)
          }

          val in = new FileInputStream(propertiesFile.toFile)
          val props = new Properties()
          props.load(in)
          in.close()

          val out = new FileOutputStream(propertiesFile.toFile)
          props.setProperty("version", json.get("version").getAsString)
          props.store(out, null)
        }

        fs.close()
      } catch {
        case _: IOException => None
      }
    }
  }
}
