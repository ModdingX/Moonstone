# Moonstone

Moonstone is an IntelliJ plugin to manage CurseForge modlists in IntelliJ.

Files named `modlist.json` will be opened with Moonstone. These contain the modlist in json data but provide a nice GUI to manage the modlist.

### How to use

To install the plugin, see https://moddingx.org/jetbrains

The `modlist.json` file can be consumed by tool like [ModGradle PackDev](https://github.com/ModdingX/ModGradle/blob/master/docs/packdev.md) to build and run a modpack.

### modlist.json format

The contents of the `modlist.json` file should be a json object with the following format:

  * `platform`: A string that indicates the modding platform. Currently, supported values are `curseforge` and `modrinth`.
  * `loader`: A string describing the mod loader for the modpack.
  * `minecraft`: The minecraft version that the modpack uses.
  * `installed`: A list of *mod files* that are explicitly installed.
  * `dependencies`: A list of *mod files* that are installed only as dependencies for other files.

A *mod file* describes a json object with the following format:

  * `project`: The project id
  * `file`: The file id
  * `side`: Where that mod is required. Either `common`, `client` or `server`
  * `locked`: Whether this dependency is locked, so it can't be updated.
