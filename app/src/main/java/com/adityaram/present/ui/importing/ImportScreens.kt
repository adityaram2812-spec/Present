package com.adityaram.present.ui.importing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityaram.present.domain.import.Confidence
import com.adityaram.present.domain.import.ParsedTimetableClass
import com.adityaram.present.ui.theme.LocalPresentColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTimetableScreen(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    debugContent: (@Composable () -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    when (val currentState = state) {
        is ImportState.Idle, is ImportState.Parsing, is ImportState.Error -> {
            ImportSelectionScreen(
                state = currentState,
                onFileSelected = { uri, mime -> viewModel.parseUri(uri, mime) },
                onBack = onBack,
                debugContent = debugContent
            )
        }
        is ImportState.GroupSelection -> {
            GroupSelectionScreen(
                groups = currentState.groups,
                onConfirm = { selectedGroup, isFull -> 
                    viewModel.confirmGroupSelection(currentState.layoutData, selectedGroup, isFull)
                },
                onCancel = { viewModel.resetState() }
            )
        }
        is ImportState.Review, is ImportState.ConfirmOverwrite -> {
            var classToEdit by remember { mutableStateOf<ParsedTimetableClass?>(null) }
            val classes = if (currentState is ImportState.Review) currentState.classes else (currentState as ImportState.ConfirmOverwrite).classes
            
            ReviewTimetableScreen(
                classes = classes,
                onUpdateClass = viewModel::updateClass,
                onDeleteClass = viewModel::deleteClass,
                onConfirm = viewModel::confirmImport,
                onCancel = { viewModel.resetState() },
                onEditClass = { classToEdit = it }
            )
            
            if (classToEdit != null) {
                EditParsedClassDialog(
                    parsedClass = classToEdit!!,
                    onDismiss = { classToEdit = null },
                    onConfirm = { updated ->
                        viewModel.updateClass(updated)
                        classToEdit = null
                    }
                )
            }
            
            if (currentState is ImportState.ConfirmOverwrite) {
                val colors = LocalPresentColors.current
                AlertDialog(
                    onDismissRequest = { viewModel.cancelReplacement() },
                    title = {
                        Text(text = "Replace existing timetable?", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text("You already have a timetable saved in Present.")
                            Spacer(Modifier.height(8.dp))
                            Text("Importing this timetable will replace your current timetable with the new one.")
                            Spacer(Modifier.height(8.dp))
                            Text("Your existing attendance history will not be deleted.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.executeTimetableReplacement(classes) }) {
                            Text("Replace & Import", color = colors.critical)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelReplacement() }) {
                            Text("Cancel", color = colors.primaryText)
                        }
                    },
                    containerColor = colors.surface,
                    titleContentColor = colors.primaryText,
                    textContentColor = colors.secondaryText
                )
            }
        }
        is ImportState.Success -> {
            LaunchedEffect(Unit) {
                onNavigateToHome()
                viewModel.resetState()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSelectionScreen(
    state: ImportState,
    onFileSelected: (Uri, String?) -> Unit,
    onBack: () -> Unit,
    debugContent: (@Composable () -> Unit)? = null
) {
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { 
        android.util.Log.d("PresentImport", "IMPORT_IMAGE_SELECTED: URI Ready")
        onFileSelected(it, "image/*") 
    } }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { 
        android.util.Log.d("PresentImport", "IMPORT_JSON_SELECTED")
        onFileSelected(it, "application/json") 
    } }

    val colors = LocalPresentColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    navigationIconContentColor = colors.primaryText
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Import timetable",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Bring in your existing schedule and we'll turn it into classes.",
                fontSize = 16.sp,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            val colors = LocalPresentColors.current
            
            if (state is ImportState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.critical.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp).fillMaxWidth()
                ) {
                    Text(
                        text = state.message,
                        color = colors.critical,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            ImportOptionCard(
                icon = Icons.Rounded.Add,
                colors = colors,
                title = "Upload timetable image",
                subtitle = "Use a screenshot or photo of your timetable",
                onClick = { imageLauncher.launch("image/*") }
            )
            
            ImportOptionCard(
                icon = Icons.Rounded.List,
                colors = colors,
                title = "Import timetable JSON",
                subtitle = "Import a timetable directly from a JSON file",
                onClick = { jsonLauncher.launch("application/json") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Never auto-added without review. You'll inspect and edit all detected sessions before anything is added to your timetable.",
                fontSize = 14.sp,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 32.dp),
                lineHeight = 20.sp
            )
        }

        if (state is ImportState.Parsing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) { }, // Prevent touches behind loader
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    CircularProgressIndicator(color = colors.accent, modifier = Modifier.padding(bottom = 24.dp))
                    Text(
                        text = "Reading your timetable",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Please wait while Present extracts your classes. This may take a little while.",
                        fontSize = 14.sp,
                        color = colors.secondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.width(240.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(icon: ImageVector, colors: com.adityaram.present.ui.theme.PresentColors, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = colors.secondaryText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewTimetableScreen(
    classes: List<ParsedTimetableClass>,
    onUpdateClass: (ParsedTimetableClass) -> Unit,
    onDeleteClass: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onEditClass: (ParsedTimetableClass) -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler { showDiscardDialog = true }

    if (showDiscardDialog) {
        DiscardConfirmationDialog(
            onConfirmDiscard = {
                showDiscardDialog = false
                onCancel()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    val colors = LocalPresentColors.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = onConfirm) {
                        Text("Confirm Import", fontWeight = FontWeight.Bold, color = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    navigationIconContentColor = colors.primaryText
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "Log Class Session",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Review and approve extracted lectures before syncing them to your live attendance tracker.",
                    fontSize = 16.sp,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            items(classes) { parsedClass ->
                ReviewClassCard(
                    parsedClass = parsedClass,
                    onUpdate = onUpdateClass,
                    onDelete = onDeleteClass,
                    onEdit = onEditClass
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ReviewClassCard(
    parsedClass: ParsedTimetableClass,
    onUpdate: (ParsedTimetableClass) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (ParsedTimetableClass) -> Unit
) {
    val isLowConfidence = parsedClass.confidence == Confidence.LOW
    val colors = LocalPresentColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowConfidence) colors.critical.copy(alpha = 0.05f) else colors.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isLowConfidence) androidx.compose.foundation.BorderStroke(1.dp, colors.critical.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = parsedClass.subjectName ?: "Unknown Subject",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (parsedClass.subjectName == null) colors.critical else colors.primaryText
                )
                
                IconButton(onClick = { onDelete(parsedClass.id) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = colors.secondaryText)
                }
            }

            if (isLowConfidence && !parsedClass.uncertaintyReason.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = colors.critical, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = parsedClass.uncertaintyReason ?: "", color = colors.critical, fontSize = 14.sp)
                }
            }

            Text(
                text = "${dayName(parsedClass.dayOfWeek)} • ${formatTime(parsedClass.startTime)} - ${formatTime(parsedClass.endTime)}",
                color = colors.secondaryText,
                fontSize = 14.sp
            )
            
            if (parsedClass.startTime == 680) { // 11:20
                println("DEBUG FLOW UI Card: ${parsedClass.subjectName} Start=${parsedClass.startTime} End=${parsedClass.endTime}")
            }
            
            if (!parsedClass.room.isNullOrBlank() || !parsedClass.teacher.isNullOrBlank()) {
                val details = listOfNotNull(
                    parsedClass.room?.let { "Room $it" },
                    parsedClass.teacher?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                
                Text(
                    text = details,
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Text(text = "Tap to edit", color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).clickable { 
                onEdit(parsedClass)
            })
        }
    }
}

@Composable
fun EditParsedClassDialog(
    parsedClass: ParsedTimetableClass,
    onDismiss: () -> Unit,
    onConfirm: (ParsedTimetableClass) -> Unit
) {
    var subject by remember { mutableStateOf(parsedClass.subjectName ?: "") }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Class") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name") }
                )
                // Simplified edit for V1 - only subject name editing to clear uncertainty safely. 
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { 
                    onConfirm(parsedClass.copy(
                        subjectName = subject.takeIf { it.isNotBlank() },
                        confidence = Confidence.HIGH, // user manually confirmed
                        uncertaintyReason = null
                    )) 
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun dayName(day: Int?): String = when (day) {
    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> "Unknown Day"
}

private fun formatTime(minutes: Int?): String {
    if (minutes == null) return "??"
    val h = minutes / 60
    val m = minutes % 60
    val amPm = if (h >= 12) "PM" else "AM"
    val disp = if (h == 0) 12 else if (h > 12) h - 12 else h
    return String.format("%02d:%02d %s", disp, m, amPm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionScreen(
    groups: List<String>,
    onConfirm: (selectedGroup: String?, isFullTimetable: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isFullTimetable by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(groups.firstOrNull()) }
    val colors = LocalPresentColors.current

    BackHandler { showDiscardDialog = true }

    if (showDiscardDialog) {
        DiscardConfirmationDialog(
            onConfirmDiscard = {
                showDiscardDialog = false
                onCancel()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Which schedule?") },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    navigationIconContentColor = colors.primaryText,
                    titleContentColor = colors.primaryText
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "We found multiple batches in this timetable. Choose yours so Present can import the relevant classes.",
                fontSize = 16.sp,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                RadioButton(
                    selected = !isFullTimetable,
                    onClick = { isFullTimetable = false },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.secondaryText)
                )
                Text(
                    text = "My schedule (Choose your division/batch)",
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (!isFullTimetable) {
                // Dropdown or list of groups
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, bottom = 8.dp)
                            .clickable { selectedGroup = group },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.secondaryText)
                        )
                        val displayText = if (group.equals("shared", ignoreCase = true)) "All batches" else "Batch $group"
                        Text(text = displayText, color = colors.primaryText, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isFullTimetable,
                    onClick = { isFullTimetable = true },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.secondaryText)
                )
                Text(
                    text = "Full timetable (Import all detected batches)",
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onConfirm(if (isFullTimetable) null else selectedGroup, isFullTimetable) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DiscardConfirmationDialog(
    onConfirmDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave timetable import?", color = LocalPresentColors.current.primaryText) },
        text = { Text("Your timetable has been extracted, but it hasn't been imported yet. Leaving now will discard these results.", color = LocalPresentColors.current.secondaryText) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep reviewing", color = LocalPresentColors.current.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirmDiscard) {
                Text("Leave", color = LocalPresentColors.current.critical)
            }
        },
        containerColor = LocalPresentColors.current.surface,
        titleContentColor = LocalPresentColors.current.primaryText,
        textContentColor = LocalPresentColors.current.secondaryText
    )
}
