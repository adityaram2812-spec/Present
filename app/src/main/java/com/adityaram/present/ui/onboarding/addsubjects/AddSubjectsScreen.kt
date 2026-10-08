package com.adityaram.present.ui.onboarding.addsubjects

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adityaram.present.data.model.Subject
import com.adityaram.present.ui.components.PresentTextField
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectsScreen(
    viewModel: AddSubjectsViewModel,
    onNavigateNext: () -> Unit
) {
    val colors = LocalPresentColors.current
    val subjects by viewModel.subjects.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var currentEditingSubject by remember { mutableStateOf<Subject?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = Dimens.spacing24, start = Dimens.spacing24, end = Dimens.spacing24)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Add subjects",
                style = Typography.displaySmall,
                color = colors.primaryText,
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "List the classes you have this semester.",
                style = Typography.bodyLarge,
                color = colors.secondaryText,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacing32))

        Box(modifier = Modifier.weight(1f)) {
            if (subjects.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.mutedText.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    Text(
                        text = "No subjects yet",
                        style = Typography.headlineMedium,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Tap plus to add a subject.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        SubjectRow(
                            subject = subject,
                            onEdit = { 
                                currentEditingSubject = it 
                                showDialog = true 
                            },
                            onDelete = { viewModel.deleteSubject(it) }
                        )
                    }
                }
            }
        }
    }
    
    // Bottom Action Area
    Box(
        modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.background.copy(alpha = 0.95f)
                )
                .padding(Dimens.spacing24),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing16)
        ) {
            OutlinedButton(
                onClick = { 
                    currentEditingSubject = null
                    showDialog = true 
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add subject", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Text("Add subject", style = Typography.labelLarge)
            }
            
            Button(
                onClick = onNavigateNext,
                enabled = subjects.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryText, 
                    contentColor = colors.background,
                    disabledContainerColor = colors.elevatedSurface,
                    disabledContentColor = colors.mutedText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDialog) {
        AddEditSubjectDialog(
            initialName = currentEditingSubject?.name ?: "",
            isEdit = currentEditingSubject != null,
            onDismiss = {
                showDialog = false
                currentEditingSubject = null
            },
            onSave = { name ->
                if (currentEditingSubject != null) {
                    viewModel.editSubject(currentEditingSubject!!, name)
                } else {
                    viewModel.addSubject(name)
                }
                showDialog = false
                currentEditingSubject = null
            }
        )
    }
}

@Composable
fun SubjectRow(
    subject: Subject,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing12))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing12))
            .clickable { onEdit(subject) }
            .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                style = Typography.headlineMedium,
                color = colors.primaryText,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        IconButton(
            onClick = { onDelete(subject) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete",
                tint = colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectDialog(
    initialName: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = LocalPresentColors.current
    var name by remember { mutableStateOf(initialName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primaryText,
        textContentColor = colors.secondaryText,
        title = {
            Text(
                text = if (isEdit) "Edit subject" else "New subject",
                style = Typography.headlineMedium
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter subject name",
                    style = Typography.bodyMedium,
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(Dimens.spacing16))
                PresentTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Subject name",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.mutedText)
            ) {
                Text("Cancel")
            }
        }
    )
}
