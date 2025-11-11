package com.aiaccounting.app.api

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileInputStream
import android.media.MediaExtractor
import android.media.MediaFormat

class WhisperManager(private val context: Context) {  // 关键：将 context 声明为私有成员变量
    // 加载 native 库
    init {
        System.loadLibrary("whisperandroid")
    }

    // JNI 接口声明
    private external fun loadModel(modelPath: String): Boolean
    private external fun transcribe(pcmData: ByteArray, sampleRate: Int): String

    // 加载模型（从 assets 复制到应用私有目录）
    fun loadWhisperModel(): Boolean {
        val modelName = "ggml-tiny.bin"
        val modelFile = File(context.filesDir, modelName)
        if (!modelFile.exists()) {
            // 从 assets 复制模型到内部存储
            context.assets.open(modelName).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return loadModel(modelFile.absolutePath)
    }

    // 采集音频并识别（使用 AudioRecord）
    fun transcribeAudio(): String {
        val sampleRate = 16000 // 必须 16kHz（Whisper 要求）
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val pcmData = ByteArray(bufferSize)
        audioRecord.startRecording()
        // 录制 3 秒音频（可根据需求调整）
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 3000) {
            val read = audioRecord.read(pcmData, 0, bufferSize)
            if (read < 0) break
        }
        audioRecord.stop()
        audioRecord.release()

        // 调用 JNI 识别
        return transcribe(pcmData, sampleRate)
    }

    // 从文件读取音频并识别
    fun transcribeFromFile(audioFile: File): String {
        val sampleRate = 16000 // Whisper 要求 16kHz
        
        try {
            // 读取音频文件并转换为PCM格式
            val pcmData = convertAudioToPCM(audioFile, sampleRate)
            
            if (pcmData.isEmpty()) {
                throw Exception("音频文件转换失败或为空")
            }
            
            // 调用 JNI 识别
            val result = transcribe(pcmData, sampleRate)
            return result
        } catch (e: Exception) {
            throw Exception("音频文件处理失败: ${e.message}")
        }
    }

    // 将音频文件转换为PCM格式
    private fun convertAudioToPCM(audioFile: File, sampleRate: Int): ByteArray {
        return try {
            // 使用MediaExtractor解码音频文件
            val extractor = MediaExtractor()
            extractor.setDataSource(audioFile.absolutePath)
            
            // 查找音频轨道
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            
            if (audioTrackIndex == -1) {
                throw Exception("未找到音频轨道")
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            // 读取音频数据 - 使用ByteBuffer
            val buffer = java.nio.ByteBuffer.allocate(1024 * 1024) // 1MB缓冲区
            val pcmData = mutableListOf<Byte>()
            
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                // 将样本数据添加到PCM数据中
                buffer.rewind() // 重置buffer位置
                for (i in 0 until sampleSize) {
                    pcmData.add(buffer.get())
                }
                
                extractor.advance()
            }
            
            extractor.release()
            
            // 转换为ByteArray并返回
            pcmData.toByteArray()
        } catch (e: Exception) {
            throw Exception("音频文件解码失败: ${e.message}")
        }
    }
}