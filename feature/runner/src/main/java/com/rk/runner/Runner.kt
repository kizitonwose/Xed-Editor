package com.rk.runner

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rk.resources.strings
import com.rk.runner.Runner.showRunnerSelectionDialog
import com.rk.runner.runners.jvm.jdk.JavaRunner
import com.rk.runner.runners.node.NodeRunner
import com.rk.runner.runners.python.PythonRunner
import com.rk.runner.runners.shell.ShellRunner
import com.rk.runner.runners.shell.TermuxShellRunner
import com.rk.runner.runners.web.html.HtmlRunner
import com.rk.runner.runners.web.markdown.MarkDownRunner
import java.io.File
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
interface RunnerImpl {
    @Keep
    fun run(file: File, context: Context)
    @Keep
    fun getName(): String
    @Keep
    fun getDescription(): String
    @Keep
    fun getIcon(context: Context): Drawable?
    fun isRunning():Boolean
    fun stop()
}

@Keep
object Runner {
    @Keep
    val registry = HashMap<String, MutableList<RunnerImpl>>()

    init {
        registry["html"] = mutableListOf(HtmlRunner())
        registry["md"] = mutableListOf(MarkDownRunner())
        registry["py"] = mutableListOf(PythonRunner())
        registry["java"] = mutableListOf<RunnerImpl>(JavaRunner("Java"), JavaRunner("Javac"), JavaRunner("Maven"))

        mutableListOf<RunnerImpl>(NodeRunner()).let {
            registry["mjs"] = it
            registry["js"] = it
        }
        mutableListOf<RunnerImpl>(TermuxShellRunner()).let {
            registry["sh"] = it
            registry["bash"] = it
            registry["kts"] = it
        }
    }

    fun isRunnable(file: File): Boolean {
        val ext = file.name.substringAfterLast('.', "")
        return registry.keys.any { it == ext }
    }

    fun run(file: File, context: Context) {
            if (isRunnable(file)) {
                val ext = file.name.substringAfterLast('.', "")
                val runners = registry[ext]
                if (runners?.size!! == 0) {
                    return
                }
                showRunnerConfirmationDialog(context, file.name) {
                    if (runners.size == 1) {
                        Thread { runners[0].run(file, context) }.start()
                    } else {
                            showRunnerSelectionDialog(context, runners) { selectedRunner ->
                                Thread { selectedRunner.run(file, context) }.start()
                        }
                    }
                }
        }
    }

    private fun showRunnerSelectionDialog(
        context: Context,
        runners: List<RunnerImpl>,
        onRunnerSelected: (RunnerImpl) -> Unit,
    ) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_runner_selection, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.runner_recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(context)

        val dialog =
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(strings.choose_runtime))
                .setView(dialogView)
                .setNegativeButton(context.getString(strings.cancel), null)
                .show()

        recyclerView.adapter = RunnerAdapter(runners, dialog, onRunnerSelected)
    }

    private fun showRunnerConfirmationDialog(
        context: Context,
        fileName: String,
        onConfirmed: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Run the file: $fileName?")
            .setNegativeButton(context.getString(strings.cancel), null)
            .setPositiveButton(context.getString(strings.run)) { dialog, _ ->
                dialog.dismiss()
                onConfirmed()
            }
            .show()
    }
}
