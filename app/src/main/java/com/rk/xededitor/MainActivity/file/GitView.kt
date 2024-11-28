package com.rk.xededitor.MainActivity.file

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.ui.theme.KarbonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File

object GitView {

    private val gitCommits = mutableStateListOf<Triple<String, String, String>>()
    private val gitStatus = mutableStateListOf<Pair<String, String>>()
    private var gitJob: Job? = null

    fun updateRoot(file: File, activity: MainActivity) {
        gitJob?.cancel()
        clear()
        gitJob = activity.lifecycleScope.launch(Dispatchers.Default) {
            val gitRoot = FileManager.findGitRoot(file)
            if (gitRoot != null) {
                val dateFormat = android.text.format.DateFormat.getMediumDateFormat(activity)
                val timeFormat = android.text.format.DateFormat.getTimeFormat(activity)
                val git = Git.open(gitRoot)
                val objectId = git.repository.resolve("HEAD")
                val commits = git.log().add(objectId).setMaxCount(3).call()
                    .map { commit ->
                        val date = dateFormat.format(commit.committerIdent.`when`)
                        val time = timeFormat.format(commit.committerIdent.`when`)
                        Triple(
                            first = "${commit.committerIdent.name} <${commit.committerIdent.emailAddress}>",
                            second = commit.shortMessage,
                            third = "$date at $time <${commit.abbreviate(8).name()}>"
                        )

                    }

                val status = with(git.status().call()) {
                    added.map { "Added" to it } +
                            changed.map { "Changed" to it } +
                            removed.map { "Removed" to it } +
                            missing.map { "Missing" to it } +
                            modified.map { "Modified" to it } +
                            conflicting.map { "Conflict" to it } +
                            untracked.map { "Untracked" to it }
                }

                withContext(Dispatchers.Main) {
                    gitCommits.addAll(commits)
                    gitStatus.addAll(status)
                }
            }
        }
    }

    fun setup(gitView: ComposeView) {
        // Fix an issue where the view does not fill the drawer width
        (gitView.parent.parent as ViewGroup).addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val newWidth = right - left
            if (gitView.width != newWidth) {
                gitView.updateLayoutParams<MarginLayoutParams> { width = newWidth }
            }
        }
        gitView.setContent {
            KarbonTheme {
                Surface(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        ) {
                            for ((index, value) in gitStatus.withIndex()) {
                                val (status, fileName) = value
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 16.dp),
                                    text = "$status: $fileName",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (index != gitStatus.indices.last) {
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                        ) {
                            for ((index, value) in gitCommits.withIndex()) {
                                val (nameAndEmail, shortMsg, sha1AndTime) = value
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = shortMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = nameAndEmail,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = sha1AndTime,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index != gitCommits.indices.last) {
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun clear() {
        gitCommits.clear()
        gitStatus.clear()
    }
}
