package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ChatBotModel
import com.example.data.ai.ChatBotPersona
import com.example.data.ai.ChatMessage
import com.example.data.model.ResumeFull
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCoachChatScreen(
    messages: List<ChatMessage>,
    activePersona: ChatBotPersona,
    selectedModel: ChatBotModel,
    isThinking: Boolean,
    activeResume: ResumeFull?,
    onSendMessage: (String) -> Unit,
    onSelectPersona: (ChatBotPersona) -> Unit,
    onSelectModel: (ChatBotModel) -> Unit,
    onClearChat: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var attachResumeContext by remember { mutableStateOf(true) }

    // Auto scroll on new messages
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.testTag("ai_coach_chat_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AI Career Coach", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Gemini",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = activePersona.title,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Model Selector Button
                    Box {
                        OutlinedButton(
                            onClick = { showModelDropdown = true },
                            modifier = Modifier
                                .testTag("chat_model_selector_button")
                                .padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedModel.displayName.replace("Gemini ", ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false }
                        ) {
                            ChatBotModel.values().forEach { modelOption ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(modelOption.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (modelOption == selectedModel) PrimaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        modelOption.badge,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (modelOption == selectedModel) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                modelOption.idealFor,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectModel(modelOption)
                                        showModelDropdown = false
                                    },
                                    leadingIcon = {
                                        if (modelOption == selectedModel) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = PrimaryIndigo)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Clear Chat Button
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("chat_clear_history_button")
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear Chat")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Persona Selector Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatBotPersona.values()) { persona ->
                    val isSelected = persona == activePersona
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPersona(persona) },
                        label = {
                            Text(
                                text = persona.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            when (persona) {
                                ChatBotPersona.CAREER_COACH -> Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                ChatBotPersona.MOCK_INTERVIEWER -> Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                                ChatBotPersona.SALARY_NEGOTIATOR -> Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                                ChatBotPersona.TECH_SPECIALIST -> Icon(Icons.Outlined.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigo,
                            selectedLeadingIconColor = PrimaryIndigo
                        ),
                        modifier = Modifier.testTag("persona_chip_${persona.name.lowercase()}")
                    )
                }
            }

            // Attached Resume Context Banner (if active resume exists)
            if (activeResume != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Context: ${activeResume.title.ifBlank { "Untitled Resume" }} (${activeResume.personalInfo.fullName.ifBlank { "Candidate" }})",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = attachResumeContext,
                            onCheckedChange = { attachResumeContext = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // Chat Messages Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Initial Persona Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                AiSparkleGradientStart,
                                                AiSparkleGradientEnd
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePersona.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = activePersona.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = activePersona.initialGreeting,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Messages list
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        onCopyText = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied AI Message", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Thinking Indicator
                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryIndigo
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Gemini is analyzing...",
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Prompt Suggestions
            val quickPrompts = when (activePersona) {
                ChatBotPersona.CAREER_COACH -> listOf(
                    "Critique my resume summary",
                    "Suggest in-demand skills for Senior roles",
                    "Rewrite a bullet using the STAR method",
                    "How to explain a career transition?"
                )
                ChatBotPersona.MOCK_INTERVIEWER -> listOf(
                    "Ask me a behavioral question",
                    "Ask me a conflict resolution question",
                    "Ask a system scaling question",
                    "Score my last answer out of 10"
                )
                ChatBotPersona.SALARY_NEGOTIATOR -> listOf(
                    "How to negotiate a 20% higher base salary?",
                    "Give me a counteroffer email template",
                    "How to ask for more equity / RSUs?",
                    "Handling 'What are your salary expectations?'"
                )
                ChatBotPersona.TECH_SPECIALIST -> listOf(
                    "Explain Redis caching strategies",
                    "System design for high-traffic API",
                    "SQL indexing best practices",
                    "Microservices vs Modular Monolith"
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            inputText = prompt
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = prompt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Ask ${activePersona.title}...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotEmpty()) {
                                onSendMessage(textToSend)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .testTag("chat_send_button")
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isThinking) PrimaryIndigo
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = if (inputText.isNotBlank() && !isThinking) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will reset the active conversation with your AI Career Coach.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChat()
                        showClearConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_chat_button")
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) PrimaryIndigo else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                shadowElevation = if (isUser) 1.dp else 2.dp,
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("chat_bubble_${message.role}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isUser && message.modelName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryIndigo.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = message.modelName.replace("gemini-", "").replace("-preview", ""),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (!isUser) {
                            IconButton(
                                onClick = { onCopyText(message.text) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ME",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
