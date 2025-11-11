package com.aiaccounting.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aiaccounting.app.data.AppDatabase
import com.aiaccounting.app.databinding.FragmentSettingsBinding
import com.aiaccounting.app.viewmodel.ChatViewModel
import com.aiaccounting.app.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aiaccounting.app.api.WhisperManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val chatViewModel: ChatViewModel by activityViewModels()
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private lateinit var database: AppDatabase
    private lateinit var whisperManager: WhisperManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        database = AppDatabase.getDatabase(requireContext())
        
        // Initialize Whisper manager
        whisperManager = WhisperManager(requireContext())
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnClearDatabase.setOnClickListener {
            showClearDatabaseDialog()
        }
        
        binding.btnWhisperTest.setOnClickListener {
            testWhisperTranscription()
        }
    }
    
    private fun testWhisperTranscription() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "正在加载Whisper模型...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Load whisper model
                val modelLoaded = withContext(Dispatchers.IO) {
                    whisperManager.loadWhisperModel()
                }
                
                if (!modelLoaded) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Whisper模型加载失败",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "模型加载成功，准备测试音频...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Read record.mp3 from assets
                val internalRecordFile = File(context?.filesDir, "record.mp3")
                
                withContext(Dispatchers.IO) {
                    // Copy file from assets to internal storage
                    requireContext().assets.open("record.mp3").use { input ->
                        FileOutputStream(internalRecordFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "音频文件已加载，开始转写...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Transcribe from file
                val result = withContext(Dispatchers.IO) {
                    whisperManager.transcribeFromFile(internalRecordFile)
                }
                
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Whisper音频转文字结果")
                        .setMessage("音频文件：assets/record.mp3\n\n识别结果：\n\n$result")
                        .setPositiveButton("确定", null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Whisper测试失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showClearDatabaseDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空数据库")
            .setMessage("确定要清空所有数据吗？此操作不可恢复！\n\n将删除：\n• 所有记账记录\n• 所有聊天消息")
            .setPositiveButton("确定清空") { _, _ ->
                clearDatabase()
            }
            .setNegativeButton("取消", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun clearDatabase() {
        lifecycleScope.launch {
            try {
                // Clear all chat messages first (child table with foreign key)
                chatViewModel.deleteAllMessages()
                // Then clear all expenses (parent table)
                withContext(Dispatchers.IO) {
                    database.expenseDao().deleteAll()
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "数据库已清空",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Recreate activity to refresh all fragments
                    requireActivity().recreate()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "清空失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
