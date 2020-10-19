package com.github.salomonbrys.gradle.sass

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import java.io.File

class SassExtension(project: Project) {

    val DEFAULT_DOWNLOAD_URL = "https://github.com/sass/dart-sass/releases/download"

    val DEFAULT_VERSION = "1.27.0"

    val DEFAULT_SASS_EXE = if (OperatingSystem.current().isWindows) "sass.bat" else "sass"

    val DEFAULT_SASS_DIR = File("${project.gradle.gradleUserHomeDir}/sass")

    sealed class Exe {
        data class Download(var downloadBaseUrl: String, var version: String, var outputDir: File) : Exe()
        data class Local(var path: String) : Exe()
    }

    var exe: Exe = Exe.Download(DEFAULT_DOWNLOAD_URL, DEFAULT_VERSION, DEFAULT_SASS_DIR)

    @JvmOverloads
    fun download(action: Action<Exe.Download> = Action {}) {
        exe = Exe.Download(DEFAULT_DOWNLOAD_URL, DEFAULT_VERSION, DEFAULT_SASS_DIR).apply(action)
    }

    fun download(action: Exe.Download.() -> Unit) {
        exe = Exe.Download(DEFAULT_DOWNLOAD_URL, DEFAULT_VERSION, DEFAULT_SASS_DIR).apply(action)
    }

    fun download(action: Closure<*>) {
        exe = Exe.Download(DEFAULT_DOWNLOAD_URL, DEFAULT_VERSION, DEFAULT_SASS_DIR).apply(action)
    }

    @JvmOverloads
    fun local(action: Action<Exe.Local> = Action {}) {
        exe = Exe.Local(DEFAULT_SASS_EXE).apply(action)
    }

    fun local(action: Exe.Local.() -> Unit) {
        exe = Exe.Local(DEFAULT_SASS_EXE).apply(action)
    }

    fun local(action: Closure<*>) {
        exe = Exe.Local(DEFAULT_SASS_EXE).apply(action)
    }
}
