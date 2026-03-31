package net.pythonsden.ao3_.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import net.pythonsden.ao3_.viewmodel.DownloadsViewModel
import net.pythonsden.ao3_.viewmodel.SortOrder
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDir.collectAsState()
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isOrganizing by viewModel.isOrganizing.collectAsState()

    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToMove by remember { mutableStateOf<File?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = currentDir != viewModel.baseDir || isSearchExpanded) {
        if (isSearchExpanded) {
            isSearchExpanded = false
            viewModel.setSearchQuery("")
        } else {
            viewModel.navigateUp()
        }
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete ${if (fileToDelete?.isDirectory == true) "Folder" else "Work"}") },
            text = { Text("Are you sure you want to delete '${fileToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!) {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }
                        fileToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("Search files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { 
                        isSearchExpanded = false
                        viewModel.setSearchQuery("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                },
                singleLine = true
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (currentDir != viewModel.baseDir) {
                        val pathDisplay = currentDir.absolutePath.removePrefix(viewModel.baseDir.absolutePath)
                        if (pathDisplay.isNotEmpty()) {
                            Text(
                                text = pathDisplay,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = { viewModel.setSortOrder(SortOrder.NAME); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = { viewModel.setSortOrder(SortOrder.DATE); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Size") },
                            onClick = { viewModel.setSortOrder(SortOrder.SIZE); showSortMenu = false }
                        )
                    }
                }

                if (fileToMove != null) {
                    IconButton(onClick = {
                        viewModel.moveFile(fileToMove!!, currentDir) {
                            Toast.makeText(context, "Moved ${fileToMove?.name}", Toast.LENGTH_SHORT).show()
                            fileToMove = null
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Here")
                    }
                    IconButton(onClick = { fileToMove = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Move")
                    }
                } else {
                    IconButton(onClick = { 
                        viewModel.organizeFiles { count ->
                            Toast.makeText(context, "Organized $count files", Toast.LENGTH_SHORT).show()
                        }
                    }, enabled = !isOrganizing) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Organize")
                    }
                }
            }
        }
        
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isNotEmpty()) "No results matching '$searchQuery'" else "No files found.")
            }
        } else {
            LazyColumn {
                if (currentDir != viewModel.baseDir && !isSearchExpanded) {
                    item {
                        ListItem(
                            headlineContent = { Text("..") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.navigateUp() }
                        )
                        HorizontalDivider()
                    }
                }
                
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { 
                            if (!item.isDirectory) {
                                Text("${item.length() / 1024} KB") 
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (item.isDirectory) Icons.Default.Folder else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (item == fileToMove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { fileToMove = item }) {
                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move")
                                }
                                IconButton(onClick = { fileToDelete = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (item.isDirectory) {
                                viewModel.navigateTo(item)
                                isSearchExpanded = false
                            } else {
                                if (item.extension.lowercase() == "epub") {
                                    val encodedPath = URLEncoder.encode(item.absolutePath, StandardCharsets.UTF_8.toString())
                                    navController.navigate("reader/$encodedPath")
                                } else {
                                    openFile(context, item)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}
