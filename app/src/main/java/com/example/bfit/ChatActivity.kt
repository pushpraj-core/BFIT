package com.example.bfit

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bfit.database.FirestoreRepository
import com.example.bfit.databinding.ActivityChatBinding
import com.example.bfit.network.NetworkUtils
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestoreRepository: FirestoreRepository
    private var sessionId: String = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreRepository = FirestoreRepository()
        setupBackNavigation()

        // Back button
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Setup RecyclerView
        chatAdapter = ChatAdapter(chatMessages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = chatAdapter

        // Show empty state initially
        updateEmptyState()

        // Load existing session or check for previous sessions
        val existingSessionId = intent.getStringExtra("sessionId")
        if (existingSessionId != null) {
            sessionId = existingSessionId
            loadChatHistory()
        }

        // Initialize Gemini AI
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        val chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") {
                    text("You are BFIT AI Coach — a helpful, friendly fitness and nutrition expert. " +
                        "Provide practical advice about workouts, diet plans, calories, macros, supplements, " +
                        "and healthy lifestyle. Keep responses concise and actionable. " +
                        "Use emojis sparingly for a friendly tone.")
                },
                content(role = "model") {
                    text("Got it! I'm your BFIT AI Coach 💪 I'll help you with fitness, nutrition, " +
                        "and wellness advice. Ask me anything!")
                }
            )
        )

        // Send button
        binding.askButton.setOnClickListener {
            val question = binding.questionInput.text.toString().trim()
            if (question.isNotEmpty()) {
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                binding.questionInput.text?.clear()
                addMessage(question, isUser = true)

                lifecycleScope.launch {
                    try {
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.askButton.isEnabled = false

                        // Save user message to Firestore
                        firestoreRepository.saveChatMessage(question, isUser = true, sessionId)

                        val response = chat.sendMessage(question)
                        val responseText = response.text ?: "Sorry, I couldn't generate a response."

                        addMessage(responseText, isUser = false)

                        // Save bot response to Firestore
                        firestoreRepository.saveChatMessage(responseText, isUser = false, sessionId)

                    } catch (e: Exception) {
                        val errorMsg = when {
                            e.message?.contains("API key", ignoreCase = true) == true ->
                                "API key not configured. Please add your Gemini API key."
                            e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("connect", ignoreCase = true) == true ->
                                "Network error. Please check your connection and try again."
                            else -> "Error: ${e.localizedMessage}"
                        }
                        addMessage(errorMsg, isUser = false)
                    } finally {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.askButton.isEnabled = true
                    }
                }
            }
        }

        // Chat history button
        binding.chatHistoryButton.setOnClickListener {
            showChatHistoryDialog()
        }
    }

    // Back navigation is handled via OnBackPressedDispatcher registered below
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })
    }

    private fun addMessage(text: String, isUser: Boolean) {
        chatMessages.add(ChatMessage(text, isUser))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (chatMessages.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.chatRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.chatRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                binding.loadingIndicator.visibility = View.VISIBLE
                val history = firestoreRepository.getChatHistory(sessionId)
                chatMessages.clear()
                for (msg in history) {
                    val text = msg["message"] as? String ?: continue
                    val isUser = msg["isUser"] as? Boolean ?: true
                    chatMessages.add(ChatMessage(text, isUser))
                }
                chatAdapter.notifyDataSetChanged()
                if (chatMessages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }
                updateEmptyState()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun showChatHistoryDialog() {
        lifecycleScope.launch {
            try {
                val sessions = firestoreRepository.getChatSessions()
                if (sessions.isEmpty()) {
                    Toast.makeText(this@ChatActivity, "No chat history found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val sessionPreviews = sessions.map { it["preview"] as? String ?: "Chat session" }.toTypedArray()
                val sessionIds = sessions.map { it["sessionId"] as? String ?: "" }

                androidx.appcompat.app.AlertDialog.Builder(this@ChatActivity)
                    .setTitle("Chat History")
                    .setItems(sessionPreviews) { _, which ->
                        // Start new ChatActivity with selected session
                        val intent = android.content.Intent(this@ChatActivity, ChatActivity::class.java)
                        intent.putExtra("sessionId", sessionIds[which])
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("New Chat") { _, _ ->
                        chatMessages.clear()
                        chatAdapter.notifyDataSetChanged()
                        sessionId = UUID.randomUUID().toString()
                        updateEmptyState()
                    }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
