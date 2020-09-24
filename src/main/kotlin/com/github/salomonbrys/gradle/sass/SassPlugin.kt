package com.github.salomonbrys.gradle.sass

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.task
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

class SassPlugin : Plugin<Project> {

    private enum class ArchiveExt(val ext: String) {
        ZIP("zip"),
        TARGZ("tar.gz");

        override fun toString() = ext
    }

    private fun Project.applyPlugin() {

        val sassExtract = tasks.register("sassExtract", Copy::class.java)
        val config = SassExtension(this)
        extensions.add("sass", config)
        afterEvaluate {

            val exe = config.exe
            val (os, ext) = when {
                OperatingSystem.current().isLinux -> "linux" to ArchiveExt.TARGZ
                OperatingSystem.current().isMacOsX -> "macos" to ArchiveExt.TARGZ
                OperatingSystem.current().isWindows -> "windows" to ArchiveExt.ZIP
                else -> throw IllegalStateException("Unsupported operating system")
            }
            val arch = if ("64" in System.getProperty("os.arch")) "x64" else "ia32"
            if (exe is SassExtension.Exe.Download) {
                val archive = "dart-sass-${exe.version}-$os-$arch.$ext"
                val output = File("${gradle.gradleUserHomeDir}/sass/archive/$archive")
                val sassDownload = tasks.register("sassDownload", Download::class.java) {
                    group = "build setup"
                    onlyIf { !output.exists() }
                    src("${exe.downloadBaseUrl}/${exe.version}/$archive")
                    dest(output)
                    tempAndMove(true)
                }

                sassExtract.configure {
                    group = "build setup"
                    dependsOn(sassDownload)
                    from(when (ext) {
                        ArchiveExt.TARGZ -> tarTree(output)
                        ArchiveExt.ZIP -> zipTree(output)
                    })
                    into(exe.outputDir.resolve(exe.version))
                }

            }
        }

        val sassCompile = tasks.register("sassCompile", SassTask::class.java) {
            group = "build"
            inputDir = file("$projectDir/src/main/webapp/")
            dependsOn(sassExtract)
        }
        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { dependsOn(sassCompile) }
    }

    override fun apply(project: Project) {
        project.applyPlugin()
    }
}
