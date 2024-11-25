package com.rk.runner.runners.shell

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.rk.libcommons.R
import com.rk.runner.RunnerImpl
import com.rk.runner.commonUtils.runCommand
import java.io.File
import com.rk.resources.drawable
import com.rk.runner.commonUtils.runCommandTermux

class TermuxShellRunner: RunnerImpl {
    override fun run(file: File, context: Context) {
        if (file.name.endsWith("kts")){
            runCommandTermux(
                context = context,
                exe = "\$PREFIX/bin/zsh",
                args = arrayOf("-c", "echo Running: ${file.name} && kotlin ${file.name} && zsh"),
                background = false,
                dir = file.parentFile?.canonicalPath
            )
        } else {
            runCommandTermux(
                context = context,
                exe = "\$PREFIX/bin/bash",
                args = arrayOf("-c", "echo Running: ${file.name} && bash ${file.name} && bash"),
                background = false,
                dir = file.parentFile?.canonicalPath
            )
        }
    }

    override fun getName(): String = "Termux"

    override fun getDescription(): String = "Run in Termux"

    override fun getIcon(context: Context): Drawable? =  ContextCompat.getDrawable(context, drawable.terminal)

    override fun isRunning(): Boolean = false

    override fun stop() = TODO("Not yet implemented")
}

class ShellRunner(private val failsafe: Boolean) : RunnerImpl {
    override fun run(file: File, context: Context) {
        if (failsafe) {
            runCommand(
                alpine = false,
                shell = "/system/bin/sh",
                args = arrayOf("-c", file.absolutePath),
                workingDir = file.parentFile!!.absolutePath,
                context = context,
            )
        } else {
            runCommand(
                alpine = true,
                shell = "/bin/bash",
                args = arrayOf(file.absolutePath),
                workingDir = file.parentFile!!.absolutePath,
                context = context,
            )
        }
    }

    override fun getName(): String {
        return if (failsafe) {
            "Android Shell"
        } else {
            "Alpine Shell"
        }
    }

    override fun getDescription(): String {
        return if (failsafe) {
            "Android"
        } else {
            "Alpine"
        }
    }

    override fun getIcon(context: Context): Drawable? {
        return ContextCompat.getDrawable(
            context,
            if (failsafe) {
                com.rk.runner.R.drawable.android
            } else {
                drawable.bash
            },
        )
    }
    override fun isRunning(): Boolean {
        return false
    }
    override fun stop() {
        TODO("Not yet implemented")
    }
}
