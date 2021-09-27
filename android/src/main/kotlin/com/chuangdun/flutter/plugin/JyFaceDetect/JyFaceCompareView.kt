package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.camera.CameraConstant
import com.camera.JYCamera
import com.camera.impl.CameraCallback
import com.common.Facecompare
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


private const val EVENT_CAMERA_OPENED = 0
private const val EVENT_PREVIEW = 1
private const val EVENT_PREVIEW_STOP = 2
private const val EVENT_CAMERA_CLOSED = 3
private const val EVENT_COMPARE_START = 4
private const val EVENT_COMPARE_RESULT = 5
private const val TAG = "JyFaceCompareView"

class JyFaceCompareView(private val context: Context, messenger: BinaryMessenger, id: Int, createParams: Map<*, *>) : PlatformView,
        MethodChannel.MethodCallHandler, EventChannel.StreamHandler{


    private val textureView: TextureView = TextureView(context)
    private val methodChannel = MethodChannel(messenger, "${COMPARE_VIEW_REGISTRY_NAME}_$id")
    private var eventChannel = EventChannel(messenger, "${COMPARE_VIEW_EVENT_REGISTRY_NAME}_$id")
    private val threadPool = ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>())
    private val uiHandler = Handler()
    private var eventSink: EventChannel.EventSink? = null
    private var mMediaPlayer:MediaPlayer? = null
    private val mCamera: JYCamera
    private var mCompareStart = false
    init {
        val width = createParams["width"] as Int
        val height = createParams["height"] as Int
        val previewWidth = createParams["previewWidth"] as Int
        val previewHeight = createParams["previewHeight"] as Int
        val rotate = createParams["rotate"] as Int
        textureView.layoutParams = ViewGroup.LayoutParams(
            dp2px(context,width.toFloat()),
            dp2px(context,height.toFloat()))
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        mCamera = initCamera(previewWidth, previewHeight, rotate)
    }

    private fun initCamera(previewWidth: Int, previewHeight: Int, rotate: Int):JYCamera{
        return JYCamera.Builder(context)
                .setCameraType(CameraConstant.CAMERA_1)
                .setCameraPreviewSize(previewWidth, previewHeight)
                .setCameraPictureSize(previewWidth, previewHeight)
                .setCameraRotation(rotate)
                .mirror()
                .setCameraCallback(object : CameraCallback {
                    override fun onOpenedCamera() {
                        Log.d(TAG, "Camera opened.")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_CAMERA_OPENED
                            ))
                        }
                    }

                    override fun onPreviewFrame(yuvData: ByteArray, bitmap: Bitmap, width: Int, height: Int) {
                        //Log.d(TAG, "Preview onFrame: width:$width, height:$height")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_PREVIEW,
                                    "yuvData" to yuvData,
                                    "width" to width,
                                    "height" to height
                            ))
                        }
                    }

                    override fun onClosedCamera() {
                        Log.d(TAG, "Camera closed")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_CAMERA_CLOSED
                            ))
                        }
                    }

                    override fun onStopPreview() {
                        Log.d(TAG, "Preview stop")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_PREVIEW_STOP
                            ))
                        }
                    }
                })
                .build()
    }

    private fun startCompare(threshold: Int, faceBitmapData: ByteArray){
        if (mCompareStart){
            Log.w(TAG, "已在比对之中...")
            return
        }
        mCompareStart = true
        uiHandler.post {
            eventSink?.success(mapOf(
                    "event" to EVENT_COMPARE_START
            ))
        }
        val detectTask = Runnable {
            playSound(R.raw.start_face_compare, 4000)
            val srcBitmap = BitmapFactory.decodeByteArray(faceBitmapData, 0, faceBitmapData.size)
            while (mCompareStart){
                try {
                    Thread.sleep(200)
                }catch (e: InterruptedException){
                    Log.e(TAG, "线程睡眠200毫秒失败.")
                }
                val bitmap = mCamera.takePhoto()
                val timeStart = System.currentTimeMillis()
                val similar = Facecompare.getInstance().faceVerify(srcBitmap, bitmap)
                Log.d(TAG, "compare similar: $similar, time: " + (System.currentTimeMillis() - timeStart))
                if (similar >= threshold){
                    mCompareStart = false
                    playSound(R.raw.face_verified, 1500)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    uiHandler.post {  eventSink?.success(mapOf(
                            "event" to EVENT_COMPARE_RESULT,
                            "result" to true,
                            "msg" to "已通过人脸比对",
                            "similar" to similar,
                            "bitmap" to outputStream.toByteArray()
                    ))}
                }else{
                    uiHandler.post {  eventSink?.success(mapOf(
                            "event" to EVENT_COMPARE_RESULT,
                            "result" to false,
                            "msg" to "人脸比对不通过，正在重试...",
                            "similar" to similar,
                            "bitmap" to null
                    ))}
                }
            }
        }
        threadPool.execute(detectTask)
    }

    private fun playSound(resid: Int, waitMillis: Long){
        try {
            mMediaPlayer = MediaPlayer.create(context, resid)
            mMediaPlayer!!.start()
            Thread.sleep(waitMillis)
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
        }catch (e: InterruptedException){
            Log.e(TAG, "线程睡眠waitMillis毫秒失败.${e.message}")
        }catch (e: Exception) {
            Log.e(TAG, "MediaPlayer错误.${e.message}")
        }
    }

    override fun getView(): View {
        Log.i(TAG, "JyFaceCompareView:getView")
        return textureView
    }

    override fun onFlutterViewAttached(flutterView: View) {
        Log.i(TAG, "JyFaceCompareView:onFlutterViewAttached")
    }

    override fun onFlutterViewDetached() {
        Log.i(TAG, "JyFaceCompareView:onFlutterViewDetached")
    }

    override fun dispose() {
        Log.i(TAG, "JyFaceCompareView:dispose")
        if (!threadPool.isShutdown){
            threadPool.shutdownNow()
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "JyFaceCompareView:onMethodCall:${call.method}")
        when(call.method){
            "startPreview" -> {
                mCamera.doStartPreview(CameraDecide.faceCompareId, textureView)
            }
            "stopPreview" -> {
                mCamera.doStopPreview()
            }
            "stopCamera" -> {
                mCamera.doStopCamera()
            }
            "startCompare" -> {
                val arguments = call.arguments as Map<*, *>
                val threshold = arguments["threshold"] as Int
                val faceBitmapData = arguments["bitmap"] as ByteArray
                startCompare(threshold, faceBitmapData)
            }
            "stopCompare" -> {
                mCompareStart = false
            }
            "releaseCamera" -> {
                mCamera.releaseAll()
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        this.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}