# Moonstone

Moonstone is a tool to manage CurseForge or Modrinth modlists in IntelliJ.

Files named `modlist.json` will be opened with Moonstone. These contain the modlist in json data but provide a nice GUI to manage the modlist.
It is intended to be used alongside [PackDev](https://github.com/ModdingX/PackDev) to develop modpacks.

Moonstone can run as a standalone program or as an IntelliJ plugin.
Instructions on how to install the IntelliJ plugin can be found at https://moddingx.org/jetbrains

![](https://user-images.githubusercontent.com/63002502/181509686-7532fe4f-81c4-4beb-8e1f-20206bf7b646.png)

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
