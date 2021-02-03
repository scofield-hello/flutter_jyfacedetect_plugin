package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.AliveDetect.AliveDetect

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

private const val TAG = "JyFaceDetectPlugin"
const val VIEW_REGISTRY_NAME = "JyFaceDetectView"
const val VIEW_EVENT_REGISTRY_NAME = "JyFaceDetectViewEvent"
const val SDK_METHOD_REGISTRY_NAME = "JyFaceDetectSdk"
const val SDK_EVENT_REGISTRY_NAME = "JyFaceDetectSdkEvent"

/** JyFaceDetectPlugin */
class JyFaceDetectPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private val uiHandler = Handler()
  private lateinit var mAliveDetect: AliveDetect
  private lateinit var context: Context
  private lateinit var sdkMethodChannel: MethodChannel
  private lateinit var sdkEventChannel: EventChannel
  private var eventSink: EventChannel.EventSink? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine")
    context = flutterPluginBinding.applicationContext
    mAliveDetect = AliveDetect(context)
    sdkMethodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, SDK_METHOD_REGISTRY_NAME)
    sdkEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, SDK_EVENT_REGISTRY_NAME)
    sdkMethodChannel.setMethodCallHandler(this)
    sdkEventChannel.setStreamHandler(this)
    val viewFactory = JyFaceDetectViewFactory(context, mAliveDetect, flutterPluginBinding.binaryMessenger)
    flutterPluginBinding.platformViewRegistry.registerViewFactory(VIEW_REGISTRY_NAME, viewFactory)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
    sdkMethodChannel.setMethodCallHandler(null)
    sdkEventChannel.setStreamHandler(null)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    Log.i(TAG, "JyFaceDetectPlugin:onMethodCall:${call.method}")
    when(call.method){
      "initFaceDetectSdk" -> {
        initFaceDetectSdk()
      }
    }
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    this.eventSink = events
  }

  override fun onCancel(arguments: Any?) {
    this.eventSink = null
  }

  private fun initFaceDetectSdk(){
    mAliveDetect.setAliveDetectInitListener { result: Int, description: String ->
      run {
        Log.i(TAG, "活体检测模块初始化:$result, description:$description")
        uiHandler.post {
          eventSink?.success(mapOf(
                  "result" to (result == 0),
                  "msg" to description
          ))
        }
      }
    }
    mAliveDetect.init_In()
  }
}
