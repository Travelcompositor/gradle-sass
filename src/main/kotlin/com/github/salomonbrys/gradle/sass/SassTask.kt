package com.github.salomonbrys.gradle.sass

import groovy.lang.Closure
import org.apache.tools.ant.taskdefs.condition.Os
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
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

@CacheableTask
open class SassTask : ConventionTask() {

    @OutputDirectory
    var outputDir = project.buildDir.resolve("sass")

    @InputDirectory
    @PathSensitive(PathSensitivity.ABSOLUTE)
    var inputDir = project.projectDir.resolve("src/main/webapp")

    @InputDirectory
    @PathSensitive(PathSensitivity.ABSOLUTE)
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
        val ext = project.extensions["sass"] as SassExtension
        var files: Set<File>
        val exe = ext.exe
        var execute = when (exe) {
            is SassExtension.Exe.Local -> exe.path
            is SassExtension.Exe.Download -> "${exe.outputDir.absolutePath}/${exe.version}/dart-sass/${ext.DEFAULT_SASS_EXE}"
        }
        val isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        if (isWindows) {
            files = setOf(inputDir)
        } else {
            files = project.fileTree(inputDir).filter { it.extension.equals("scss") && !it.name.startsWith("_") }.files
        }


        val myPool = ForkJoinPool(Runtime.getRuntime().availableProcessors())
        val tasks = mutableListOf<ForkJoinTask<*>>()
        for (file in files) {
            tasks.add(myPool.submit {
                val relativePath = file.relativeTo(inputDir)
                val outputPath = File(outputDir.absolutePath + "/" + relativePath.path)

                project.exec {
                    executable = execute
                    val sm = sourceMaps
                    var arguments =
                            listOf(
                                    "${file.absolutePath}:${outputPath.absolutePath}",
                                    "--style=${style}",
                                    "--update"
                            ) +
                                    when (sm) {
                                        is SourceMaps.None -> listOf("--no-source-map")
                                        is SourceMaps.Embed -> listOf("--embed-source-map")
                                        is SourceMaps.File -> listOf("--source-map-urls", sm.url.name.toLowerCase())
                                    } +
                                    when (sm.embedSource) {
                                        true -> listOf("--embed-sources")
                                        false -> listOf("--no-embed-sources")
                                    } +
                                    when (isWindows) {
                                        true -> listOf("--load-path=${loadPath}")
                                        false -> listOf()
                                    }
                    args = arguments
                    val argumentString = arguments.joinToString(separator = " ")
                    println("${Thread.currentThread().name}[sassCompile] EXECUTING: $execute $argumentString")
                }
            })
        }
        for (task in tasks) {
            task.get()
        }
    }
}
