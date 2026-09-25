plugins {
    id("fabric-loom") version "1.17.20"
    `java-library`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    // Optional: Trinkets' API, for marking an Atlas worn as an accessory rather than
    // sitting in vanilla inventory. Not published on a maven this project already trusts
    // (see mapstitch-fabric's own build files), so vendored locally from the exact jar
    // this server runs - compileOnly only, real presence is checked at runtime via
    // FabricLoader.isModLoaded("trinkets") before any Trinkets class is touched.
    compileOnly(files("libs/trinkets-4.0.0+26.1.jar"))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
