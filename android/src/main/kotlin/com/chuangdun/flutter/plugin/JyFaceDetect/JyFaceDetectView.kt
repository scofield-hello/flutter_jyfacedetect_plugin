package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.AliveDetect.AliveDetect
import com.camera.CameraConstant
import com.camera.JYCamera
import com.camera.impl.CameraCallback
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.serenegiant.cdpids.CMIdsFace
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


private const val EVENT_CAMERA_OPENED = 0
private const val EVENT_PREVIEW = 1
private const val EVENT_PREVIEW_STOP = 2
private const val EVENT_CAMERA_CLOSED = 3
private const val EVENT_DETECT_START = 4
private const val EVENT_DETECT_RESULT = 5
private const val EVENT_INIT_RESULT = 6
private const val TAG = "JyFaceDetectView"

class JyFaceDetectView(private val context: Context, messenger: BinaryMessenger, id: Int, createParams: Map<*, *>) : PlatformView,
        MethodChannel.MethodCallHandler, EventChannel.StreamHandler{


    private val textureView: TextureView = TextureView(context)
    private val methodChannel = MethodChannel(messenger, "${VIEW_REGISTRY_NAME}_$id")
    private var eventChannel = EventChannel(messenger, "${VIEW_EVENT_REGISTRY_NAME}_$id")
    private val threadFactory = ThreadFactoryBuilder().setNameFormat("JyFaceDetectPool_%d").build()
    private val threadPool = ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), threadFactory)
    private val mAliveDetect:AliveDetect = AliveDetect(context)
    private val uiHandler = Handler()
    private var eventSink: EventChannel.EventSink? = null
    private val mCamera: JYCamera
    private var mDetectStart = false
    private var mMediaPlayer:MediaPlayer? = null
    init {
        val width = createParams["width"] as Int
        val height = createParams["height"] as Int
        val previewWidth = createParams["previewWidth"] as Int
        val previewHeight = createParams["previewHeight"] as Int
        val rotate = createParams["rotate"] as Int
        textureView.layoutParams = ViewGroup.LayoutParams(width, height)
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

    private fun initFaceDetectSdk(){
        mAliveDetect.setAliveDetectInitListener { result: Int, description: String ->
            run {
                Log.i(TAG, "活体检测模块初始化:$result, description:$description")
                uiHandler.post {
                    eventSink?.success(mapOf(
                            "event" to EVENT_INIT_RESULT,
                            "result" to (result == 0),
                            "msg" to description
                    ))
                }
            }
        }
        mAliveDetect.init_In()
    }

    private fun startFaceDetect(){
        if (mDetectStart){
            Log.w(TAG, "已在检测之中...")
            return
        }
        mDetectStart = true
        uiHandler.post {
            eventSink?.success(mapOf(
                    "event" to EVENT_DETECT_START
            ))
        }
        val detectTask = Runnable {
            playSound(R.raw.start_detect_face, 4000)
            while (mDetectStart){
                try {
                    Thread.sleep(500)
                }catch (e: InterruptedException){
                    Log.e(TAG, "线程睡眠500毫秒失败.")
                }
                val bitmap = mCamera.takePicture()
                val faceList: Array<out CMIdsFace> = mAliveDetect.detectFace(bitmap, null)
                        ?: continue
                when {
                    faceList.size == 1 -> {
                        Log.d(TAG, "face in top:${faceList[0].top}, right:${faceList[0].right}, " +
                                "bottom: ${faceList[0].bottom}, left: ${faceList[0].left}")
                        Log.d(TAG, "face roll_angle:${faceList[0].roll_angle}, " +
                                "pitch_angle:${faceList[0].pitch_angle}, " +
                                "yaw_angle:${faceList[0].yaw_angle}")
                        if (faceList[0].isRightAngle()){
                            mDetectStart = false
                            playSound(R.raw.face_detected, 1500)
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            uiHandler.post {
                                eventSink?.success(mapOf(
                                        "event" to EVENT_DETECT_RESULT,
                                        "bitmap" to outputStream.toByteArray(),
                                        "left" to faceList[0].left,
                                        "top" to faceList[0].top,
                                        "right" to faceList[0].right,
                                        "bottom" to faceList[0].bottom,
                                        "rollAngle" to faceList[0].roll_angle,
                                        "yawAngle" to faceList[0].yaw_angle,
                                        "pitchAngle" to faceList[0].pitch_angle
                                ))
                        }}
                    }
                    faceList.isNullOrEmpty() -> {
                        Log.i(TAG, "没有检测到人脸.")
                    }
                    else -> {
                        Log.i(TAG, "检测到${faceList.size}张人脸.")
                    }
                }
            }
        }
        threadPool.execute(detectTask)
    }

    private fun playSound(resid:Int, waitMillis:Long){
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
        Log.i(TAG, "JyFaceDetectView:getView")
        return textureView
    }

    override fun onFlutterViewAttached(flutterView: View) {
        Log.i(TAG, "JyFaceDetectView:onFlutterViewAttached")
    }

    override fun onFlutterViewDetached() {
        Log.i(TAG, "JyFaceDetectView:onFlutterViewDetached")
    }

    override fun dispose() {
        Log.i(TAG, "JyFaceDetectView:dispose")
        if (!threadPool.isShutdown){
            threadPool.shutdownNow()
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "JyFaceDetectView:onMethodCall:${call.method}")
        when(call.method){
            "initFaceDetectSdk" -> {
                initFaceDetectSdk()
            }
            "startPreview" -> {
                mCamera.doStartPreview(1, textureView)
            }
            "stopPreview" -> {
                mCamera.doStopPreview()
            }
            "stopCamera" -> {
                mCamera.doStopCamera()
            }
            "startDetect" -> {
                startFaceDetect()
            }
            "stopDetect" -> {
                mDetectStart = false
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
}