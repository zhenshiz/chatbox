plugins {
    id("net.neoforged.moddev.legacyforge")
    id("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin")
    id("io.freefair.lombok")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
base.archivesName = property("mod.id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

repositories {
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    maven("https://maven.shedaniel.me/") { name = "Cloth Config API" }
    maven("https://api.modrinth.com/maven") {name = "Modrinth"}
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    modImplementation("me.shedaniel.cloth:cloth-config-forge:${property("deps.cloth-config")}")
    modImplementation("maven.modrinth:watermedia:${property("deps.watermedia")}")

    jarJar("org.mvel:mvel2:2.5.0.Final")
    implementation("org.mvel:mvel2:2.5.0.Final")

    compileOnly("maven.modrinth:rhino:${property("deps.rhino")}")
    compileOnly("maven.modrinth:kubejs:${property("deps.kubejs")}")
    compileOnly("maven.modrinth:terra-entity:${property("deps.terra-entity")}")
    compileOnly("maven.modrinth:geckolib:${property("deps.geckolib")}")
}

legacyForge {
    version = property("deps.minecraft") as String + "-" + property("deps.forge") as String
    validateAccessTransformers = true

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
        }
    }
    sourceSets["main"].resources.srcDir("src/main/generated")
}

mixin {
    add(sourceSets.main.get(), "${property("mod.id")}-refmap.json")
    config("${property("mod.id")}.mixins.json")
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/neoforge.mods.toml", "**/*.accesswidener")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    jar {
        manifest.attributes["MixinConfigs"] = "${project.property("mod.id")}.mixins.json"
        finalizedBy("reobfJar")
    }
}

java {
    withSourcesJar()
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21
    else if (stonecutter.eval(stonecutter.current.version, ">=1.17")) JavaVersion.VERSION_17
    else JavaVersion.VERSION_1_8
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

val supportedMinecraftVersions: List<String> = com.google.common.collect.ImmutableList.builder<String>()
    .addAll(
        (property("publish.additionalVersions") as String?)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList())
    .add(stonecutter.current.version)
    .build()

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["version"] = project.property("mod.version") as String
    }

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        expand(props)
    }
}

publishMods {
    file = tasks.named<org.gradle.jvm.tasks.Jar>("reobfJar").map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    val modVersion = property("mod.version") as String
    type = if (modVersion.contains("alpha")) ALPHA
    else if (modVersion.contains("beta")) BETA
    else STABLE

    displayName = "${property("mod.name")} $modVersion for ${stonecutter.current.version} Forge"
    version = "${modVersion}-${property("deps.minecraft")}-forge"
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("forge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env.MODRINTH_API_KEY.orNull()
        minecraftVersions.addAll(supportedMinecraftVersions)
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env.CURSEFORGE_API_KEY.orNull()
        minecraftVersions.addAll(supportedMinecraftVersions)
    }
}
