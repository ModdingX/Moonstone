# Moonstone

Moonstone is an IntelliJ plugin to manage CurseForge modlists in IntelliJ.

Files named `modlist.json` will be opened with MoonStone.

It should be a json list of mod files where a mod file is a json object wih the following format:

  * `project`: The project id
  * `file`: The file id
  * `side`: Where that mod is required. Either `common`, `client` or `server`
  * `locked`: Whether this dependency is locked, so it can't be updated.
  * `installed`: Whether the mod has been manually installed or is installed as a dependency of another mod.

To install this plugin, add `https://noeppi-noeppi.github.io/MinecraftUtilities/maven/updatePlugins.xml` as a plugin repository.

The data can for example be used by the [ModGradle PackDev](https://github.com/noeppi-noeppi/ModGradle/tree/master/plugin/src/main/java/io/github/noeppi_noeppi/tools/modgradle/plugins/packdev) gradle plugin.