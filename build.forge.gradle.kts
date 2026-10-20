plugins {
    id("net.neoforged.moddev.legacyforge")
    id("dev.kikugie.postprocess.jsonlang")
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
    maven("https://cursemaven.com") {name = "CurseForge"}
}

dependencies {
    implementation("org.java-websocket:Java-WebSocket:1.6.0") { isTransitive = false }
    jarJar("org.java-websocket:Java-WebSocket:1.6.0") { isTransitive = false }
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    modImplementation("me.shedaniel.cloth:cloth-config-forge:${property("deps.cloth-config")}")
    modCompileOnly("maven.modrinth:watermedia:${property("deps.watermedia")}")
    modCompileOnly("curse.maven:watermedia-869524:8519098")

    jarJar("org.mvel:mvel2:2.5.0.Final")
    implementation("org.mvel:mvel2:2.5.0.Final")

    compileOnly("maven.modrinth:rhino:${property("deps.rhino")}")
    compileOnly("maven.modrinth:kubejs:${property("deps.kubejs")}")
    compileOnly("maven.modrinth:terra-entity:${property("deps.terra-entity")}")
    compileOnly("maven.modrinth:geckolib:${property("deps.geckolib")}")
}

legacyForge {
    version = property("deps.minecraft") as String + "-" + property("deps.forge") as String

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

dependencies {
    add("additionalRuntimeClasspath", "org.java-websocket:Java-WebSocket:1.6.0") { isTransitive = false }
    add("additionalRuntimeClasspath", "org.mvel:mvel2:2.5.0.Final")
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

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["version"] = project.property("mod.version") as String
    }

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        expand(props)
    }
}

apply(from = rootProject.file("gradle/publish-conventions.gradle"))
