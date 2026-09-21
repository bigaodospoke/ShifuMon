import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    // 1.21.1 ainda usa nomes ofuscados: "fabric-loom-remap" e o plugin certo para essa versao
    id("net.fabricmc.fabric-loom-remap") version "1.17.21"
    kotlin("jvm") version "2.2.20"
}

version = property("mod_version")!!
group = property("maven_group")!!

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.impactdev.net/repository/development/") { name = "Cobblemon" }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    // Mapeamentos oficiais da Mojang: os mesmos nomes que o codigo-fonte do Cobblemon usa
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("com.cobblemon:fabric:${property("cobblemon_version")}")

    // Mod Menu e opcional para o jogador; so compilamos contra a API e rodamos no dev
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    modLocalRuntime("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// Configuracao desta maquina (local.properties, fora do git)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}
val modsInstallDir: String? = localProperties.getProperty("mods_install_dir")?.takeIf { it.isNotBlank() }

// Depois de cada build, instala o jar no perfil de teste (mods_install_dir)
val installMod by tasks.registering {
    group = "shifumon"
    description = "Copia o jar para mods_install_dir, removendo versoes antigas do ShifuMon."
    // AbstractArchiveTask: base comum do Jar do Gradle e do RemapJarTask do Loom
    val remappedJar = tasks.named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile }
    inputs.file(remappedJar)
    onlyIf { modsInstallDir != null }
    doLast {
        val targetDir = file(modsInstallDir!!)
        targetDir.mkdirs()
        targetDir.listFiles { candidate -> candidate.name.startsWith("shifumon-") && candidate.name.endsWith(".jar") }
            ?.forEach { old ->
                old.setWritable(true) // o Modrinth App marca os jars como somente leitura
                if (!old.delete()) throw GradleException("Nao foi possivel remover ${old.name}. Feche o jogo e rode o build de novo.")
            }
        val jar = remappedJar.get().asFile
        jar.copyTo(targetDir.resolve(jar.name), overwrite = true)
        logger.lifecycle("ShifuMon instalado em ${targetDir.resolve(jar.name)}")

        // Uma cópia na pasta mods faria o jogo carregar a versão de lá, e não esta
        val modsFolder = targetDir.resolveSibling("mods")
        val strays = modsFolder.listFiles { file -> file.name.startsWith("shifumon") && file.name.endsWith(".jar") }
        if (strays != null && strays.isNotEmpty()) {
            logger.warn("AVISO: ainda existe ${strays.joinToString { it.name }} em ${modsFolder}. Remova pela aba Content do Modrinth App, senao o jogo pode carregar a versao antiga.")
        }
    }
}

tasks.named("build") {
    finalizedBy(installMod)
}
