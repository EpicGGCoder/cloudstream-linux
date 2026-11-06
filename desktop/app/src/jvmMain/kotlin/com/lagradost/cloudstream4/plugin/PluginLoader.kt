package com.lagradost.cloudstream4.plugin

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

/**
 * Loads `.cs3` plugin files (renamed APKs containing Dalvik DEX bytecode)
 * into the JVM at runtime.
 *
 * Pipeline:
 *   .cs3 file
 *     → extract classes.dex (it's a ZIP)
 *     → convert DEX → JVM bytecode via dex2jar
 *     → URLClassLoader loads the resulting JAR
 *     → reflection instantiates the manifest's pluginClassName
 *     → that class extends BasePlugin, which on load() registers MainAPI providers
 */
object PluginLoader {
    private val log = LoggerFactory.getLogger(PluginLoader::class.java)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private lateinit var cacheDir: File

    fun init(dataDir: File) {
        cacheDir = File(dataDir, "plugin-cache").apply { mkdirs() }
    }

    data class PluginManifest(
        val name: String,
        val pluginClassName: String,
        val version: Int = 1,
        val requiresResources: Boolean = false,
        val internalName: String = "",
    )

    suspend fun loadPlugin(file: File, sourceUrl: String? = null): BasePlugin? = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            log.error("Plugin file does not exist: ${file.absolutePath}")
            return@withContext null
        }
        try {
            val manifest = readManifest(file) ?: run {
                log.error("No manifest.json in ${file.name}; not a CS3 plugin.")
                return@withContext null
            }
            log.info("Loading plugin: {} (class={})", manifest.name, manifest.pluginClassName)

            val dexFile = extractDex(file, manifest.internalName) ?: run {
                log.error("No classes.dex in ${file.name}")
                return@withContext null
            }
            val jarFile = convertDexToJar(dexFile, manifest.internalName) ?: run {
                log.error("dex2jar conversion failed for ${file.name}")
                return@withContext null
            }

            val parent = PluginLoader::class.java.classLoader
            val loader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent)

            val pluginClass = try {
                loader.loadClass(manifest.pluginClassName)
            } catch (e: ClassNotFoundException) {
                log.error("Plugin class ${manifest.pluginClassName} not found in ${file.name}", e)
                return@withContext null
            }

            if (!BasePlugin::class.java.isAssignableFrom(pluginClass)) {
                log.error("${manifest.pluginClassName} does not extend BasePlugin")
                return@withContext null
            }

            val instance = try {
                pluginClass.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                log.error("Failed to instantiate ${manifest.pluginClassName}", e)
                return@withContext null
            } as BasePlugin

            instance.filename = file.absolutePath

            // Plugins override either load() (cross-platform) or load(Context) (Android).
            // Pass a real Context stub (not null) so plugins that call
            // context.getSharedPreferences() don't NPE.
            val contextStub = android.content.Context()
            try {
                val loadWithContext = pluginClass.methods.firstOrNull {
                    it.name == "load" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].name == "android.content.Context"
                }
                if (loadWithContext != null) {
                    log.debug("Invoking load(Context) for ${manifest.name}")
                    loadWithContext.invoke(instance, contextStub)
                } else {
                    instance.load()
                }
            } catch (e: Throwable) {
                val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
                log.warn("Plugin ${manifest.name} load() threw: ${cause::class.simpleName}: ${cause.message}")
                log.warn("Provider registration may still have happened before the throw — checking APIHolder")
            }

            log.info("Plugin loaded: {} (file={})", manifest.name, file.name)
            instance
        } catch (e: Throwable) {
            log.error("Failed to load plugin ${file.name}", e)
            null
        }
    }

    private fun readManifest(file: File): PluginManifest? {
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("manifest.json") ?: return null
                val text = zip.getInputStream(entry).bufferedReader().readText()
                val node = json.parseToJsonElement(text).jsonObject
                val name = node.str("name") ?: file.nameWithoutExtension
                PluginManifest(
                    name = name,
                    pluginClassName = node.str("pluginClassName") ?: return null,
                    version = node.int("version") ?: 1,
                    requiresResources = node.bool("requiresResources") ?: false,
                    internalName = node.str("internalName") ?: name,
                )
            }
        } catch (e: Exception) {
            log.error("Failed to read manifest from ${file.name}", e)
            null
        }
    }

    private fun extractDex(cs3File: File, internalName: String): File? {
        val outDir = File(cacheDir, "dex").apply { mkdirs() }
        val outFile = File(outDir, "$internalName.dex")
        return try {
            ZipFile(cs3File).use { zip ->
                val dexEntries = zip.entries()
                    .toList()
                    .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                    .sortedBy { it.name }
                if (dexEntries.isEmpty()) return null
                zip.getInputStream(dexEntries.first()).use { it.copyTo(outFile.outputStream()) }
            }
            outFile
        } catch (e: Exception) {
            log.error("Failed to extract DEX from ${cs3File.name}", e)
            null
        }
    }

    private fun convertDexToJar(dexFile: File, internalName: String): File? {
        val outFile = File(cacheDir, "jar/$internalName.jar").apply { parentFile?.mkdirs() }
        if (outFile.exists() && outFile.lastModified() > dexFile.lastModified()) {
            log.debug("Using cached JAR for $internalName")
            return outFile
        }
        return try {
            if (convertDexToJarInProcess(dexFile, outFile)) return outFile
            if (convertDexToJarCli(dexFile, outFile)) return outFile
            log.error("No dex2jar converter available (neither library nor CLI).")
            null
        } catch (e: Throwable) {
            log.error("dex2jar conversion failed for ${dexFile.name}", e)
            null
        }
    }

    /** In-process conversion using the dex2jar JVM library (preferred). */
    private fun convertDexToJarInProcess(dexFile: File, outFile: File): Boolean {
        return try {
            val cls = Class.forName("com.googlecode.dex2jar.tools.Main")
            val main = cls.getDeclaredMethod("main", Array<String>::class.java)
            main.isAccessible = true
            main.invoke(null, arrayOf("-d2j", dexFile.absolutePath, "-o", outFile.absolutePath, "--force"))
            outFile.exists()
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            log.warn("In-process dex2jar failed: ${e.message}; trying CLI")
            false
        }
    }

    /**
     * Fallback: invoke the bundled d2j-dex2jar.sh CLI tool.
     * Searches multiple locations:
     *   1. $APPDIR/resources/dex-tools/ (AppImage runtime)
     *   2. Relative to the app's JAR/classpath (dev mode)
     *   3. System-installed d2j-dex2jar
     */
    private fun convertDexToJarCli(dexFile: File, outFile: File): Boolean {
        // Build list of candidate paths — search relative to:
        // - APPDIR env (AppImage mount point)
        // - The classpath (where our JAR lives)
        // - User home (~/.cloudstream/dex-tools/)
        // - System paths
        val candidates = mutableListOf<File>()

        // 1. AppImage: $APPDIR/resources/dex-tools/d2j-dex2jar.sh
        System.getenv("APPDIR")?.let {
            candidates.add(File(it, "resources/dex-tools/d2j-dex2jar.sh"))
            candidates.add(File(it, "app/resources/dex-tools/d2j-dex2jar.sh"))
        }

        // 2. Dev mode: relative to current working directory
        candidates.add(File("app/resources/dex-tools/d2j-dex2jar.sh"))
        candidates.add(File("resources/dex-tools/d2j-dex2jar.sh"))

        // 3. User home: ~/.cloudstream/dex-tools/
        val homeDexTools = File(System.getProperty("user.home"), ".cloudstream/dex-tools/d2j-dex2jar.sh")
        candidates.add(homeDexTools)

        // 4. Relative to the PluginLoader class location (JAR-relative)
        try {
            val codeSource = PluginLoader::class.java.protectionDomain.codeSource
            if (codeSource != null) {
                val jarFile = File(codeSource.location.toURI())
                val jarDir = jarFile.parentFile
                if (jarDir != null) {
                    candidates.add(File(jarDir, "resources/dex-tools/d2j-dex2jar.sh"))
                    candidates.add(File(jarDir.parentFile, "resources/dex-tools/d2j-dex2jar.sh"))
                    // Also check the build resources dir
                    candidates.add(File(jarDir, "dex-tools/d2j-dex2jar.sh"))
                }
            }
        } catch (e: Exception) {
            // Ignore — class path resolution may fail in some contexts
        }

        // 5. System paths
        candidates.add(File("/usr/bin/d2j-dex2jar"))
        candidates.add(File("/usr/local/bin/d2j-dex2jar"))

        // Also check if dex-tools lib JARs are on the classpath (for in-process use)
        // If the in-process method already worked, we won't get here.

        val tool = candidates.firstOrNull { it.exists() && it.canExecute() }
        if (tool == null) {
            log.error("dex2jar CLI not found. Searched: ${candidates.map { it.absolutePath }}")
            log.error("To fix: run ./scripts/fetch-dex-tools.sh, or install d2j-dex2jar system-wide")
            return false
        }

        log.info("Using dex2jar at: ${tool.absolutePath}")
        return try {
            val cmd = if (tool.name.endsWith(".sh")) {
                listOf("bash", tool.absolutePath, dexFile.absolutePath, "-o", outFile.absolutePath, "--force")
            } else {
                listOf(tool.absolutePath, dexFile.absolutePath, "-o", outFile.absolutePath, "--force")
            }
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText()
            val exit = proc.waitFor()
            if (exit != 0) {
                log.error("d2j-dex2jar exited $exit: $out")
                return false
            }
            outFile.exists()
        } catch (e: Exception) {
            log.error("d2j-dex2jar CLI failed", e)
            false
        }
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

private fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()
