package com.chuangdun.flutter.plugin.JyFaceDetect

import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin

private const val TAG = "JyFaceDetectPlugin"
const val VIEW_REGISTRY_NAME = "JyFaceDetectView"
const val VIEW_EVENT_REGISTRY_NAME = "JyFaceDetectViewEvent"

/** JyFaceDetectPlugin */
class JyFaceDetectPlugin: FlutterPlugin {


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine")
    val viewFactory = JyFaceDetectViewFactory(flutterPluginBinding.applicationContext,
            flutterPluginBinding.binaryMessenger)
    flutterPluginBinding.platformViewRegistry.registerViewFactory(VIEW_REGISTRY_NAME, viewFactory)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
  }
}
