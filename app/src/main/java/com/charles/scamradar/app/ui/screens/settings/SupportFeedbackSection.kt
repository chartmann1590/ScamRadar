package com.charles.scamradar.app.ui.screens.settings

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.BuildConfig
import com.charles.scamradar.app.data.feedback.BugReport
import com.charles.scamradar.app.data.feedback.BugReportRepo
import com.charles.scamradar.app.data.feedback.CreateIssueRequest
import com.charles.scamradar.app.data.feedback.DiagnosticsHelper
import com.charles.scamradar.app.data.feedback.GithubClient
import com.charles.scamradar.app.data.feedback.GithubComment
import com.charles.scamradar.app.data.feedback.GithubIssue
import com.charles.scamradar.app.data.feedback.ImageAttachmentHelper
import com.charles.scamradar.app.data.feedback.PostCommentRequest
import com.charles.scamradar.app.data.feedback.toBugReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SupportFeedbackSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { BugReportRepo(context) }
    val reports by repo.bugReports.collectAsState(initial = emptyList())
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<BugReport?>(null) }

    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Support & Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Send bugs or feedback to the GitHub issue tracker and follow replies here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { showReportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Report a Problem")
            }
            if (reports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Submitted reports", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                reports.forEachIndexed { index, report ->
                    ReportRow(report = report, onClick = { selectedReport = report })
                    if (index != reports.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }

    if (showReportDialog) {
        ReportProblemDialog(repo = repo, onDismiss = { showReportDialog = false })
    }
    selectedReport?.let { report ->
        IssueDetailsDialog(report = report, repo = repo, onDismiss = { selectedReport = null })
    }
}

@Composable
private fun ReportRow(report: BugReport, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(report.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "#${report.number} - ${report.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusBadge(report.status)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val isOpen = status.equals("open", ignoreCase = true)
    val background = if (isOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (isOpen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status.ifBlank { "unknown" }, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReportProblemDialog(repo: BugReportRepo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(GithubClient.configurationError) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedImage = uri
    }
    val canSubmit = GithubClient.isConfigured && title.isNotBlank() && description.isNotBlank() && !isSubmitting

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Report a Problem") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                WarningBox()
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    isError = description.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeDiagnostics, onCheckedChange = { includeDiagnostics = it })
                    Text("Include phone/app diagnostics")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                AttachmentControls(
                    selectedImage = selectedImage,
                    onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onClear = { selectedImage = null }
                )
                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    isSubmitting = true
                    message = null
                    scope.launch {
                        val result = runCatching {
                            val attachmentUrl = selectedImage?.let { ImageAttachmentHelper.uploadImage(context, it) }
                            val body = buildIssueBody(
                                description = description,
                                name = name,
                                email = email,
                                attachmentUrl = attachmentUrl,
                                diagnostics = if (includeDiagnostics) DiagnosticsHelper.collectMarkdown(context) else null
                            )
                            GithubClient.api.createIssue(
                                owner = BuildConfig.GITHUB_REPO_OWNER,
                                repo = BuildConfig.GITHUB_REPO_NAME,
                                request = CreateIssueRequest("[Feedback] ${title.trim()}", body)
                            )
                        }
                        result.onSuccess { issue ->
                            repo.saveBugReport(issue.toBugReport())
                            isSubmitting = false
                            onDismiss()
                        }.onFailure {
                            message = GithubClient.friendlyError(it)
                            isSubmitting = false
                        }
                    }
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IssueDetailsDialog(report: BugReport, repo: BugReportRepo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var issue by remember { mutableStateOf<GithubIssue?>(null) }
    var comments by remember { mutableStateOf<List<GithubComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(GithubClient.configurationError) }
    var reply by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedImage = uri
    }

    LaunchedEffect(report.number, refreshKey) {
        if (!GithubClient.isConfigured) return@LaunchedEffect
        isLoading = true
        error = null
        val result = runCatching {
            val fetchedIssue = GithubClient.api.getIssue(BuildConfig.GITHUB_REPO_OWNER, BuildConfig.GITHUB_REPO_NAME, report.number)
            val fetchedComments = GithubClient.api.getComments(BuildConfig.GITHUB_REPO_OWNER, BuildConfig.GITHUB_REPO_NAME, report.number)
            fetchedIssue to fetchedComments
        }
        result.onSuccess { (fetchedIssue, fetchedComments) ->
            issue = fetchedIssue
            comments = fetchedComments.sortedBy { it.createdAt }
            repo.saveBugReport(fetchedIssue.toBugReport())
        }.onFailure {
            error = GithubClient.friendlyError(it)
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = { if (!isPosting) onDismiss() },
        title = { Text(issue?.title ?: report.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.78f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("#${report.number}", style = MaterialTheme.typography.titleSmall)
                    StatusBadge(issue?.state ?: report.status)
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { refreshKey++ },
                        label = { Text("Refresh") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { runCatching { uriHandler.openUri(issue?.htmlUrl ?: report.htmlUrl) } },
                        label = { Text("Open") },
                        leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) }
                    )
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Comments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (comments.isEmpty()) {
                    Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    comments.forEach { comment ->
                        CommentRow(comment)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Reply") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                AttachmentControls(
                    selectedImage = selectedImage,
                    onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onClear = { selectedImage = null }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = GithubClient.isConfigured && reply.isNotBlank() && !isPosting,
                onClick = {
                    isPosting = true
                    error = null
                    scope.launch {
                        val result = runCatching {
                            val attachmentUrl = selectedImage?.let { ImageAttachmentHelper.uploadImage(context, it) }
                            GithubClient.api.postComment(
                                BuildConfig.GITHUB_REPO_OWNER,
                                BuildConfig.GITHUB_REPO_NAME,
                                report.number,
                                PostCommentRequest(buildCommentBody(reply, attachmentUrl))
                            )
                        }
                        result.onSuccess {
                            reply = ""
                            selectedImage = null
                            refreshKey++
                        }.onFailure {
                            error = GithubClient.friendlyError(it)
                        }
                        isPosting = false
                    }
                }
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.AddComment, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reply")
            }
        },
        dismissButton = {
            TextButton(enabled = !isPosting, onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun WarningBox() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Your report will be submitted to this app's GitHub issue tracker. Do not include passwords, private keys, medical information, financial information, or anything you do not want visible to the repository maintainers. If this repository is public, your report may be publicly visible. Screenshots may contain private information.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AttachmentControls(selectedImage: Uri?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPick) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (selectedImage == null) "Attach screenshot/image" else "Change attachment")
        }
        if (selectedImage != null) {
            TextButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Remove")
            }
        }
    }
    selectedImage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        SelectedImagePreview(it)
    }
}

@Composable
private fun SelectedImagePreview(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Selected attachment preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.Crop
        )
    } ?: Text(uri.lastPathSegment ?: "Image selected", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CommentRow(comment: GithubComment) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${comment.user.login} - ${comment.createdAt}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildIssueBody(
    description: String,
    name: String,
    email: String,
    attachmentUrl: String?,
    diagnostics: String?
): String = buildString {
    appendLine("## Description")
    appendLine()
    appendLine(description.trim())
    appendLine()
    appendLine("## Contact Info")
    appendLine()
    appendLine("- Name: ${name.ifBlank { "Not provided" }}")
    appendLine("- Email: ${email.ifBlank { "Not provided" }}")
    attachmentUrl?.let {
        appendLine()
        appendLine("## Attachment")
        appendLine()
        appendLine("![Screenshot]($it)")
    }
    diagnostics?.let {
        appendLine()
        appendLine(it)
    }
}

private fun buildCommentBody(reply: String, attachmentUrl: String?): String = buildString {
    appendLine("## Reply")
    appendLine()
    appendLine(reply.trim())
    attachmentUrl?.let {
        appendLine()
        appendLine("## Attachment")
        appendLine()
        appendLine("![Screenshot]($it)")
    }
}
