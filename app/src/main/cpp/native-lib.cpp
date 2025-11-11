#include <jni.h>
#include <string>
#include "whisper.h"

// 全局模型实例
static struct whisper_context *ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aiaccounting_app_api_WhisperManager_loadModel(JNIEnv *env, jobject thiz, jstring model_path) {
    // 释放已有模型
    if (ctx != nullptr) {
        whisper_free(ctx);
        ctx = nullptr;
    }
    // 加载模型（路径为 Android 中的绝对路径）
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    ctx = whisper_init_from_file(path);
    env->ReleaseStringUTFChars(model_path, path);
    return ctx != nullptr; // 返回是否加载成功
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aiaccounting_app_api_WhisperManager_transcribe(JNIEnv *env, jobject thiz, jbyteArray pcm_data, jint sample_rate) {
    if (ctx == nullptr) {
        return env->NewStringUTF("Model not loaded");
    }

    // 1. 获取原始 PCM 数据（jbyteArray → int16_t 数组）
    jbyte *pcm_bytes = env->GetByteArrayElements(pcm_data, nullptr);
    int pcm_size_bytes = env->GetArrayLength(pcm_data);
    int16_t *pcm_int16 = reinterpret_cast<int16_t*>(pcm_bytes);
    int pcm_frame_count = pcm_size_bytes / 2;  // 每个 int16_t 占 2 字节

    // 2. 转换 int16_t 到 float（归一化到 [-1.0, 1.0]）
    float *pcm_float = new float[pcm_frame_count];
    for (int i = 0; i < pcm_frame_count; ++i) {
        pcm_float[i] = static_cast<float>(pcm_int16[i]) / 32768.0f;  // 关键转换
    }

    // 3. 配置识别参数，与电脑版whisper-cli保持一致
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);
    params.language = "zh"; // 改为 "zh" 识别中文
    params.n_threads = 4;
    params.audio_ctx = 0;
    params.no_timestamps = false; // 改为false，与电脑版保持一致，包含时间戳
    params.print_timestamps = true; // 打印时间戳
    params.print_progress = true;  // 打印进度信息
    
    // 其他关键参数配置，与电脑版whisper-cli保持一致
    params.suppress_blank = true; // 抑制空白
    params.suppress_nst = false;  // 不抑制非语音标记
    params.temperature = 0.0f;    // 温度参数
    params.max_initial_ts = 1.0f;
    params.length_penalty = -1.0f;
    params.no_speech_thold = 0.6f; // 无语音检测阈值
    
    // 添加beam search参数，与电脑版保持一致
    params.greedy.best_of = 5;        // 与电脑版whisper-cli的默认值一致
    params.beam_search.beam_size = 5; // 与电脑版whisper-cli的默认值一致
    params.beam_search.patience = -1.0f;

    // 4. 调用 whisper_full，传入 float 类型的 PCM 数据
    std::string result;
    if (whisper_full(ctx, params, pcm_float, pcm_frame_count) == 0) {  // 第 4 个参数是帧数量（float 数组长度）
        int n_segments = whisper_full_n_segments(ctx);
        for (int i = 0; i < n_segments; ++i) {
            result += whisper_full_get_segment_text(ctx, i);
        }
    } else {
        result = "Transcription failed";
    }

    // 5. 释放资源（避免内存泄漏）
    delete[] pcm_float;  // 释放动态分配的 float 数组
    env->ReleaseByteArrayElements(pcm_data, pcm_bytes, 0);
    return env->NewStringUTF(result.c_str());
}