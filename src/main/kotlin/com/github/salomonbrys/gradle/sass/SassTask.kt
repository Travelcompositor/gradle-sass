package com.github.salomonbrys.gradle.sass

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File
import java.io.Serializable
import java.util.Date
import java.util.StringJoiner
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

@CacheableTask
open class SassTask : ConventionTask() {

    @OutputDirectory
    var outputDir = project.buildDir.resolve("sass")

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    var inputDir = project.projectDir.resolve("src/main/webapp")

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    var loadPath = project.projectDir.resolve("src/main/webapp")


    enum class Url {
        RELATIVE,
        ABSOLUTE
    }

    sealed class SourceMaps : Serializable {
        abstract val embedSource: Boolean

        data class None(override var embedSource: Boolean = false) : SourceMaps()
        data class Embed(override var embedSource: Boolean = false) : SourceMaps()
        data class File(override var embedSource: Boolean = false, var url: Url = Url.RELATIVE) : SourceMaps() {
            val relative = Url.RELATIVE
            val absolute = Url.ABSOLUTE
        }
    }

    @Input
    var sourceMaps: SourceMaps = SourceMaps.File()

    @org.gradle.api.tasks.Input
    var style: String = "expanded"


    init {
        this.dependsOn(project.tasks["sassPrepare"])
    }

    fun noSourceMap() {
        sourceMaps = SourceMaps.None()
    }

    @JvmOverloads
    fun embedSourceMap(action: Action<SourceMaps.Embed> = Action {}) {
        sourceMaps = SourceMaps.Embed().apply(action)
    }

    fun embedSourceMap(action: SourceMaps.Embed.() -> Unit) {
        sourceMaps = SourceMaps.Embed().apply(action)
    }

    fun embedSourceMap(action: Closure<*>) {
        sourceMaps = SourceMaps.Embed().apply(action)
    }

    @JvmOverloads
    fun fileSourceMap(action: Action<SourceMaps.File> = Action {}) {
        sourceMaps = SourceMaps.File().apply(action)
    }

    fun fileSourceMap(action: SourceMaps.File.() -> Unit) {
        sourceMaps = SourceMaps.File().apply(action)
    }

    fun fileSourceMap(action: Closure<*>) {
        sourceMaps = SourceMaps.File().apply(action)
    }


    @TaskAction
    internal fun compileSass() {
        val now = Date()
        val ext = project.extensions["sass"] as SassExtension
        val exe = ext.exe
        var execute = when (exe) {
            is SassExtension.Exe.Local -> exe.path
            is SassExtension.Exe.Download -> "${exe.outputDir.absolutePath}/${exe.version}/dart-sass/${ext.DEFAULT_SASS_EXE}"
        }
        var allFiles = project.fileTree(inputDir).filter { it.extension.equals("scss") && !it.name.startsWith("_") }.files
        val tasksInparalel = Math.max(4, Runtime.getRuntime().availableProcessors() - 3) // 4 como minimo porque sino tendremos el error de linea de mas de 8192 caracteres
        val chunkSize = Math.ceil(allFiles.size.toDouble() / tasksInparalel).toInt()

        val chunkedList = allFiles.chunked(chunkSize)
        val myPool = ForkJoinPool(chunkedList.size)
        val tasks = mutableListOf<ForkJoinTask<*>>()
        for (files in chunkedList) {
            tasks.add(myPool.submit {
                compileFile(files, execute)
            })
        }
        for (task in tasks) {
            task.get()
        }
        println("[sassCompile] All files compiled in ${Date().time - now.time} ms with exec")
    }

    internal fun compileFile(files: List<File>, execute: String) {
        project.exec {
            workingDir = project.projectDir
            executable = execute
            val sm = sourceMaps
            var arguments =
                    getFileArgument(files).split(" ") +
                            listOf(
                                    "--style=${style}"
                                    , "--update"
                            ) +
                            when (sm) {
                                is SourceMaps.None -> listOf("--no-source-map")
                                is SourceMaps.Embed -> listOf("--embed-source-map")
                                is SourceMaps.File -> listOf("--source-map-urls", sm.url.name.toLowerCase())
                            } +
                            when (sourceMaps.embedSource) {
                                true -> listOf("--embed-sources")
                                false -> listOf("--no-embed-sources")
                            } +
                            listOf("--load-path=${loadPath}")
            args = arguments
            //val argumentString = arguments.joinToString(separator = " ")
            //println("[sassCompile] EXECUTING: $execute $argumentString")
        }
    }

    internal fun getFileArgument(files: List<File>): String {
        var result = StringJoiner(" ")
        for (file in files) {
            val outputPath = File(outputDir.path + "/" + file.relativeTo(inputDir).path).relativeTo(project.projectDir)
            result.add("${file.relativeTo(project.projectDir).path}:${outputPath.path.replace(".scss", ".css")}")
        }
        return result.toString()
    }
}
