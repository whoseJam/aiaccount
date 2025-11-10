package com.aiaccounting.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiaccounting.app.R
import com.aiaccounting.app.adapter.ChatAdapter
import com.aiaccounting.app.adapter.ChatMessage
import com.aiaccounting.app.databinding.FragmentChatBinding
import com.aiaccounting.app.viewmodel.ChatViewModel
import com.aiaccounting.app.viewmodel.ExpenseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()
    private lateinit var chatAdapter: ChatAdapter
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isVoiceMode = false
    
    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeViewModels()
        chatViewModel.loadHistoryMessages()
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { message ->
            // Handle delete click
            onDeleteExpenseClick(message)
        }
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }
    }
    
    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.editInput.text.toString().trim()
            if (text.isNotEmpty()) {
                // Save user message through ViewModel
                chatViewModel.saveMessage(text, "user")
                binding.editInput.text?.clear()
            }
        }
        
        binding.btnVoice.setOnClickListener {
            toggleInputMode()
        }
        
        binding.btnPressToTalk.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAudioPermission()) {
                        startRecording()
                    } else {
                        requestAudioPermission()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun observeViewModels() {
        // Observe ChatViewModel
        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.addMessages(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
        
        chatViewModel.newMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                chatAdapter.addMessage(it)
                binding.recyclerChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                
                // If it's a user message, process it with ExpenseViewModel
                if (it.isUser) {
                    expenseViewModel.processChatMessage(it.id, it.text)
                }
            }
        }
        
        // Observe ExpenseViewModel
        expenseViewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !isProcessing
            binding.btnVoice.isEnabled = !isProcessing
        }
        
        expenseViewModel.latestExpense.observe(viewLifecycleOwner) { expense ->
            expense?.let {
                // Save expense message through ChatViewModel
                val expenseData = com.aiaccounting.app.adapter.ExpenseData(
                    category = it.category,
                    amount = it.amount,
                    description = it.description,
                    timestamp = it.timestamp
                )
                chatViewModel.saveExpenseMessage(
                    text = "记账成功：${it.category} ¥${it.amount}",
                    expenseId = it.id,
                    expenseData = expenseData
                )
                Toast.makeText(context, "记账成功", Toast.LENGTH_SHORT).show()
            }
        }
        
        expenseViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Only show non-accounting success messages as regular system messages
                if (!it.startsWith("记账成功：")) {
                    chatViewModel.saveMessage(it, "system")
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        expenseViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                chatViewModel.saveMessage("错误: $it", "error")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }
    
    private fun toggleInputMode() {
        isVoiceMode = !isVoiceMode
        
        if (isVoiceMode) {
            binding.editInput.visibility = View.GONE
            binding.btnPressToTalk.visibility = View.VISIBLE
            binding.btnVoice.setImageResource(R.drawable.ic_keyboard)
            binding.btnSend.visibility = View.GONE
        } else {
            binding.editInput.visibility = View.VISIBLE
            binding.btnPressToTalk.visibility = View.GONE
            binding.btnVoice.setImageResource(R.drawable.ic_microphone)
            binding.btnSend.visibility = View.VISIBLE
        }
    }
    
    private fun startRecording() {
        try {
            audioFile = File(requireContext().cacheDir, "recording_${System.currentTimeMillis()}.wav")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    binding.recyclerChat.smoothScrollToPosition((chatAdapter.itemCount - 1))
                }
            }
            
            isRecording = true
            binding.btnPressToTalk.text = "松开发送"
        } catch (e: Exception) {
            Toast.makeText(context, "录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            binding.btnPressToTalk.text = getString(R.string.press_to_talk)
            
            audioFile?.let { file ->
                if (file.exists()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            binding.recyclerChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                            expenseViewModel.processVoiceInput(file)
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(context, "停止录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onDeleteExpenseClick(message: ChatMessage) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这笔记账吗？删除后将无法恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteExpenseAndMessage(message)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteExpenseAndMessage(message: ChatMessage) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Find and delete the user input message that triggered this expense first
                // (the message right before this expense card)
                val messages = chatAdapter.getMessages()
                val currentIndex = messages.indexOf(message)
                if (currentIndex > 0) {
                    val previousMessage = messages[currentIndex - 1]
                    if (previousMessage.isUser) {
                        chatViewModel.deleteMessageById(previousMessage.id)
                        chatAdapter.removeMessage(previousMessage)
                    }
                }
                
                // Delete the chat message from database (must be before deleting expense due to foreign key)
                chatViewModel.deleteMessageById(message.id)
                
                // Finally delete the expense record from database
                message.expenseId?.let { expenseId ->
                    expenseViewModel.deleteExpense(expenseId)
                }
                
                // Remove from adapter
                chatAdapter.removeMessage(message)
            } catch (e: Exception) {
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    

    
    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            stopRecording()
        }
        _binding = null
    }
}
