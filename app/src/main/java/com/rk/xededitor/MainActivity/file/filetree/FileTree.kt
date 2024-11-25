package com.rk.xededitor.MainActivity.file.filetree

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rk.settings.PreferencesData
import com.rk.settings.PreferencesKeys
import com.rk.xededitor.DefaultScope
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.MainActivity.MainActivity.Companion.activityRef
import com.rk.xededitor.MainActivity.file.FileAction
import com.rk.xededitor.MainActivity.file.FileManager
import com.rk.xededitor.MainActivity.handlers.MenuItemHandler
import com.rk.xededitor.databinding.FiletreeLayoutBinding
import com.rk.xededitor.ui.theme.KarbonTheme
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File

class FileTree(val context: MainActivity, val path: String, val parent: ViewGroup) {
    val binding: FiletreeLayoutBinding
    
    private val viewModel: FileTreeViewModel by lazy {
        ViewModelProvider(context)[FileTreeViewModel::class.java]
    }
    
    class FileTreeViewModel() : ViewModel() {
        val fileListLoader: FileLoader = FileLoader()
        private val treeMap = HashMap<String,Tree<File>>()
        
        fun getTree(path: String):Tree<File>?{
            return treeMap[path]
        }
        
        fun setTree(path: String,tree:Tree<File>){
            treeMap[path] = tree
        }
        
    }
    
    
    private fun createTree(
        fileListLoader: FileLoader, rootPath: String
    ): Tree<File> {
        val tree = Tree.createTree<File>()
        tree.apply {
            this.generator = FileNodeFactory(
                File(rootPath), fileListLoader
            )
            initTree()
        }
        return tree
    }
    
    init {
        val inflater = LayoutInflater.from(context)
        binding = FiletreeLayoutBinding.inflate(inflater, parent, true)
        
        if (viewModel.getTree(path) == null) {
            viewModel.setTree(path,createTree(viewModel.fileListLoader, path))
        }
        
        setupTreeView()
    }
    
    
    private fun setupTreeView() {
        (binding.treeview as TreeView<File>).apply {
            binding.treeview.binder = FileBinder(binding = binding, fileLoader = viewModel.fileListLoader, onFileLongClick = { file ->
                activityRef.get()?.apply {
                    projectManager.getSelectedProjectRootFile()?.let {
                        FileAction(this, it, file)
                    }
                }
            }, onFileClick = { file ->
                activityRef.get()?.let {
                    if (it.isPaused) {
                        return@let
                    }
                    
                    it.adapter!!.addFragment(file)
                    if (!PreferencesData.getBoolean(
                            PreferencesKeys.KEEP_DRAWER_LOCKED,
                            false,
                        )
                    ) {
                        it.binding!!.drawerLayout.close()
                    }
                    
                    DefaultScope.launch(Dispatchers.Main) {
                        delay(2000)
                        MenuItemHandler.update(it)
                    }
                }
            }, context = context
            )
            
            binding.treeview.tree = viewModel.getTree(path)!!
            setItemViewCacheSize(100)
            supportHorizontalScroll = true
            bindCoroutineScope(findViewTreeLifecycleOwner()?.lifecycleScope ?: return)
            
            nodeEventListener = binder as FileBinder
            selectionMode = TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
        }
        
        DefaultScope.launch {
            viewModel.fileListLoader.loadFiles(path)
            binding.treeview.refresh()
        }

        val state = mutableStateListOf<Triple<String, String, String>>()
        context.lifecycleScope.launch(Dispatchers.Default) {
            val gitRoot = FileManager.findGitRoot(File(path))
            if (gitRoot != null) {
                val dateFormat = android.text.format.DateFormat.getMediumDateFormat(context)
                val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
                val git = Git.open(gitRoot)
                val objectId = git.repository.resolve("HEAD")
                val result = git.log().add(objectId).setMaxCount(3).call()
                    .map { commit ->
                        val date = dateFormat.format(commit.committerIdent.`when`)
                        val time = timeFormat.format(commit.committerIdent.`when`)
                        Triple(
                            first = "${commit.committerIdent.name} <${commit.committerIdent.emailAddress}>\n",
                            second = commit.shortMessage,
                            third = "$date at $time <${commit.abbreviate(8).name()}>"
                        )

                    }
                withContext(Dispatchers.Main) {
                    state.clear()
                    state.addAll(result)
                }
            }
        }
        binding.gitCommitView.setContent {
            KarbonTheme {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                ) {
                    Column(
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                            .padding(vertical = if (state.isNotEmpty()) 4.dp else 0.dp)
                    ) {
                        for ((index, value) in state.withIndex()) {
                            val (nameAndEmail, shortMsg, sha1AndTime) = value
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp),
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
                            if (index != state.indices.last) {
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
