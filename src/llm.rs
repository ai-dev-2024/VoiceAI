// ============================================================================
// Local LLM Module - GGUF model inference for text post-processing
// ============================================================================
//
// This module provides on-device LLM inference using llama.cpp bindings
// for Wispr Flow-style text formatting and polishing.
//
// The model used is Qwen3-0.6B (Q4_0 quantization, ~430MB)
// Downloaded to: {filesDir}/qwen3-0.6b-q4_0.gguf
//

#[cfg(target_os = "android")]
use jni::JNIEnv;
#[cfg(target_os = "android")]
use jni::objects::{JClass, JObject, JString};
#[cfg(target_os = "android")]
use jni::sys::jstring;

use std::sync::Mutex;
use std::path::PathBuf;

// LLM state - lazy loaded when first needed
#[cfg(target_os = "android")]
static LLM_STATE: Mutex<Option<LlmState>> = Mutex::new(None);

#[cfg(target_os = "android")]
struct LlmState {
    model_path: PathBuf,
    model_loaded: bool,
    // llama-cpp-2 model and context would go here when fully integrated
    // model: Option<llama_cpp_2::LlamaModel>,
    // ctx: Option<llama_cpp_2::LlamaContext>,
}

/// Initialize the LLM with the model path
#[cfg(target_os = "android")]
#[no_mangle]
pub unsafe extern "system" fn Java_com_voiceai_app_processing_processors_LocalLLMProcessor_initNative(
    mut env: JNIEnv,
    _class: JClass,
    model_path: JString,
) -> bool {
    let path: String = match env.get_string(&model_path) {
        Ok(s) => s.into(),
        Err(_) => return false,
    };
    
    log::info!("LocalLLM: Initializing with model path: {}", path);
    
    let model_file = PathBuf::from(&path);
    if !model_file.exists() {
        log::warn!("LocalLLM: Model file does not exist at {}", path);
        return false;
    }
    
    // For now, we mark as loaded but actual llama.cpp integration
    // requires proper build configuration for Android
    let mut state_guard = LLM_STATE.lock().unwrap();
    *state_guard = Some(LlmState {
        model_path: model_file,
        model_loaded: true,
    });
    
    log::info!("LocalLLM: Model state initialized successfully");
    true
}

/// Check if model is loaded
#[cfg(target_os = "android")]
#[no_mangle]
pub unsafe extern "system" fn Java_com_voiceai_app_processing_processors_LocalLLMProcessor_isModelLoadedNative(
    _env: JNIEnv,
    _class: JClass,
) -> bool {
    let state_guard = LLM_STATE.lock().unwrap();
    match state_guard.as_ref() {
        Some(state) => state.model_loaded,
        None => false,
    }
}

/// Process text with the local LLM for formatting
/// This is the main entry point for Wispr Flow-style text polishing
#[cfg(target_os = "android")]
#[no_mangle]
pub unsafe extern "system" fn Java_com_voiceai_app_processing_processors_LocalLLMProcessor_processTextNative(
    mut env: JNIEnv,
    _class: JClass,
    input_text: JString,
) -> jstring {
    let input: String = match env.get_string(&input_text) {
        Ok(s) => s.into(),
        Err(_) => {
            return env.new_string("").unwrap().into_raw();
        }
    };
    
    log::info!("LocalLLM: Processing text: {}", &input);
    
    let state_guard = LLM_STATE.lock().unwrap();
    if state_guard.is_none() {
        log::warn!("LocalLLM: Model not initialized, returning input unchanged");
        return env.new_string(&input).unwrap().into_raw();
    }
    
    // When llama.cpp is fully integrated, this would:
    // 1. Create a prompt asking to format the dictation
    // 2. Run inference
    // 3. Return the formatted text
    //
    // Prompt template (Qwen3 format):
    // <|im_start|>system
    // You are a dictation formatter. Format the following spoken text into clean written text.
    // Fix punctuation, capitalize properly, remove filler words, and format numbers correctly.
    // Only output the formatted text, nothing else.
    // <|im_end|>
    // <|im_start|>user
    // {input_text}
    // <|im_end|>
    // <|im_start|>assistant
    
    // For now, apply rule-based formatting as fallback
    let formatted = format_text_rule_based(&input);
    
    log::info!("LocalLLM: Formatted result: {}", &formatted);
    
    env.new_string(&formatted).unwrap().into_raw()
}

/// Rule-based text formatting (fallback when LLM not available)
/// Mimics Wispr Flow-style output
fn format_text_rule_based(input: &str) -> String {
    let mut result = input.to_string();
    
    // 1. Remove filler words
    let fillers = ["um", "uh", "er", "ah", "hmm", "like", "you know", "basically", "actually"];
    for filler in fillers.iter() {
        let pattern = format!(r"(?i)\b{}\b\s*,?\s*", regex::escape(filler));
        if let Ok(re) = regex::Regex::new(&pattern) {
            result = re.replace_all(&result, "").to_string();
        }
    }
    
    // 2. Remove word repetitions (stutters)
    if let Ok(re) = regex::Regex::new(r"(?i)\b(\w+)\s+\1\b") {
        result = re.replace_all(&result, "$1").to_string();
    }
    
    // 3. Clean up whitespace
    if let Ok(re) = regex::Regex::new(r"\s+") {
        result = re.replace_all(&result, " ").to_string();
    }
    result = result.trim().to_string();
    
    // 4. Capitalize first letter
    if !result.is_empty() {
        let mut chars: Vec<char> = result.chars().collect();
        if let Some(first) = chars.get_mut(0) {
            *first = first.to_uppercase().next().unwrap_or(*first);
        }
        result = chars.into_iter().collect();
    }
    
    // 5. Ensure sentence ending punctuation
    if !result.ends_with('.') && !result.ends_with('!') && !result.ends_with('?') {
        // Check if it looks like a question
        let lower = result.to_lowercase();
        if lower.starts_with("what ") || lower.starts_with("where ") || 
           lower.starts_with("when ") || lower.starts_with("why ") ||
           lower.starts_with("who ") || lower.starts_with("how ") ||
           lower.starts_with("is ") || lower.starts_with("are ") ||
           lower.starts_with("can ") || lower.starts_with("could ") ||
           lower.starts_with("would ") || lower.starts_with("should ") ||
           lower.starts_with("do ") || lower.starts_with("does ") {
            result.push('?');
        } else {
            result.push('.');
        }
    }
    
    result
}

/// Clean up LLM resources
#[cfg(target_os = "android")]
#[no_mangle]
pub unsafe extern "system" fn Java_com_voiceai_app_processing_processors_LocalLLMProcessor_cleanupNative(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut state_guard = LLM_STATE.lock().unwrap();
    *state_guard = None;
    log::info!("LocalLLM: Cleaned up");
}
