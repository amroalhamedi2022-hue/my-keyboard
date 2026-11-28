#include <jni.h>
#include <whisper.h>
#include <string>
#include <vector>
#include <algorithm>
#include <memory>
#include <atomic>
#include <cstdint>
#include <mutex>
static std::atomic<int64_t> cancelled_through{0};
static std::mutex inference_mutex;
static std::unique_ptr<whisper_context,decltype(&whisper_free)> loaded(nullptr,whisper_free);
static int64_t loaded_id=0;
static std::string loaded_model;
extern "C" JNIEXPORT void JNICALL Java_com_zerokeyboard_ime_voice_NativeWhisper_releaseSession(JNIEnv *,jclass,jlong id){
 std::lock_guard<std::mutex> lock(inference_mutex);if(loaded_id==id){loaded.reset();loaded_id=0;loaded_model.clear();}
}
static bool should_abort(void *data){return *static_cast<int64_t *>(data)<=cancelled_through.load(std::memory_order_acquire);}
extern "C" JNIEXPORT void JNICALL Java_com_zerokeyboard_ime_voice_NativeWhisper_abort(JNIEnv *,jclass,jlong id){
 int64_t previous=cancelled_through.load();while(previous<id&&!cancelled_through.compare_exchange_weak(previous,id)){}
}
static void discard_log(enum ggml_log_level, const char *, void *) {}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_zerokeyboard_ime_voice_NativeWhisper_decode(JNIEnv *env,jclass,jstring path,jfloatArray pcm,jint count,jint threads,jlong id,jint language){
 if(!path||!pcm||count<1600||count>480000||count>env->GetArrayLength(pcm))return nullptr;
 try{
  std::lock_guard<std::mutex> lock(inference_mutex);int64_t session=id;if(should_abort(&session))return nullptr;
  whisper_log_set(discard_log,nullptr);
  const char *raw=env->GetStringUTFChars(path,nullptr);if(!raw)return nullptr;
  std::string model(raw);env->ReleaseStringUTFChars(path,raw);
  if(loaded_id!=session||loaded_model!=model||!loaded){
   loaded.reset();loaded_id=0;loaded_model.clear();auto cp=whisper_context_default_params();cp.use_gpu=false;
   loaded.reset(whisper_init_from_file_with_params_no_state(model.c_str(),cp));
   if(!loaded||!whisper_is_multilingual(loaded.get())){loaded.reset();return nullptr;}
   loaded_id=session;loaded_model=model;
  }
  if(should_abort(&session))return nullptr;
  std::unique_ptr<whisper_state,decltype(&whisper_free_state)> state(whisper_init_state(loaded.get()),whisper_free_state);
  if(!state||should_abort(&session))return nullptr;
  std::vector<float> audio(count);env->GetFloatArrayRegion(pcm,0,count,audio.data());if(env->ExceptionCheck())return nullptr;
  auto params=whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.n_threads=std::clamp((int)threads,1,4);params.language=language==1?"ar":language==2?"en":"auto";params.translate=false;params.no_context=true;
  params.print_progress=false;params.print_realtime=false;params.print_timestamps=false;params.print_special=false;
  params.abort_callback=should_abort;params.abort_callback_user_data=&session;
  int result=whisper_full_with_state(loaded.get(),state.get(),params,audio.data(),count);std::fill(audio.begin(),audio.end(),0.f);
  if(result!=0||should_abort(&session))return nullptr;
  std::string text;for(int i=0;i<whisper_full_n_segments_from_state(state.get());++i){const char *s=whisper_full_get_segment_text_from_state(state.get(),i);if(s)text+=s;}
  if(text.size()>65536)return nullptr;
  jbyteArray out=env->NewByteArray((jsize)text.size());if(out&&!text.empty())env->SetByteArrayRegion(out,0,(jsize)text.size(),reinterpret_cast<const jbyte *>(text.data()));return out;
 }catch(...){return nullptr;}
}
