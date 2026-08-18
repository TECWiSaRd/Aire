package com.aire.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aire.domain.MemoryRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: MemoryViewModel) {
    val allRecords by viewModel.records.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val records = if (searchQuery.isBlank()) allRecords else searchResults

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Vault") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchMemories(it)
                },
                placeholder = { Text("Search your memories...") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "Your vault is empty." else "No matches found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecordCard(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: MemoryRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = record.category.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (record.summary.isNotBlank()) {
                Text(record.summary, style = MaterialTheme.typography.bodyMedium)
            }
            
            if (record.locationName != null) {
                Text(
                    text = "📍 ${record.locationName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (record.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    record.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}
