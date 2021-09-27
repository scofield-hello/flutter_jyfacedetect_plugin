package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.AliveDetect.AliveDetect
import com.camera.CameraConstant
import com.camera.JYCamera
import com.camera.impl.CameraCallback
import com.serenegiant.cdpids.CMIdsFace
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


private const val EVENT_CAMERA_OPENED = 0
private const val EVENT_PREVIEW = 1
private const val EVENT_PREVIEW_STOP = 2
private const val EVENT_CAMERA_CLOSED = 3
private const val EVENT_DETECT_START = 4
private const val EVENT_DETECT_RESULT = 5
private const val TAG = "JyFaceDetectView"

class JyFaceDetectView(private val context: Context, private val aliveDetect: AliveDetect,
                       messenger: BinaryMessenger, id: Int, createParams: Map<*, *>) : PlatformView,
        MethodChannel.MethodCallHandler, EventChannel.StreamHandler{


    private val textureView: TextureView = TextureView(context)
    private val blackTextureView: TextureView = TextureView(context)
    private val linearLayout:FrameLayout = FrameLayout(context)
    private val methodChannel = MethodChannel(messenger, "${VIEW_REGISTRY_NAME}_$id")
    private var eventChannel = EventChannel(messenger, "${VIEW_EVENT_REGISTRY_NAME}_$id")
    private val threadPool = ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>())
    private val uiHandler = Handler()
    private var eventSink: EventChannel.EventSink? = null
    private val mCamera: JYCamera
    private val mBlackCamera: JYCamera
    private var mDetectStart = false
    private var mMediaPlayer:MediaPlayer? = null
    init {
        val width = createParams["width"] as Int
        val height = createParams["height"] as Int
        val previewWidth = createParams["previewWidth"] as Int
        val previewHeight = createParams["previewHeight"] as Int
        val rotate = createParams["rotate"] as Int
        Log.d(TAG, "width:$width, height:$height")
        textureView.layoutParams = FrameLayout.LayoutParams(
            dp2px(context,width.toFloat() / 2),
            dp2px(context, height.toFloat()),
            Gravity.LEFT)
        blackTextureView.layoutParams = FrameLayout.LayoutParams(
            dp2px(context,width.toFloat() / 2),
            dp2px(context,height.toFloat()),
            Gravity.RIGHT
            )
        linearLayout.layoutParams = ViewGroup.LayoutParams(
            dp2px(context,width.toFloat()),
            dp2px(context,height.toFloat()))
        linearLayout.addView(textureView)
        linearLayout.addView(blackTextureView)
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        mCamera = initCamera(previewWidth, previewHeight, rotate)
        mBlackCamera = initBlackCamera(previewWidth, previewHeight, rotate)
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

    private fun initBlackCamera(previewWidth: Int, previewHeight: Int, rotate: Int):JYCamera{
        return JYCamera.Builder(context)
            .setCameraType(CameraConstant.CAMERA_1)
            .setCameraPreviewSize(previewWidth, previewHeight)
            .setCameraPictureSize(previewWidth, previewHeight)
            .setCameraRotation(rotate)
            .mirror()
            .build()
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
                    Thread.sleep(200)
                }catch (e: InterruptedException){
                    Log.e(TAG, "线程睡眠500毫秒失败.")
                }
                val bitmap = mCamera.takePicture()
                val blackBitmap = mBlackCamera.takePicture()
                val reData = FloatArray(150 * 150 * 4)
                val cmIdsFace = arrayOfNulls<CMIdsFace>(1)
                when(val ret = aliveDetect.aliveDetect_InFloatWithPosition(bitmap, blackBitmap, cmIdsFace, reData)) {
                    0 -> {
                        if (cmIdsFace.size == 1){
                            var faceInfo = cmIdsFace[0]!!
                            Log.d(TAG, "face in top:${faceInfo.top}, right:${faceInfo.right}, " +
                                    "bottom: ${faceInfo.bottom}, left: ${faceInfo.left}")
                            Log.d(TAG, "face roll_angle:${faceInfo.roll_angle}, " +
                                    "pitch_angle:${faceInfo.pitch_angle}, " +
                                    "yaw_angle:${faceInfo.yaw_angle}")
                            if (faceInfo.isRightAngle()){
                                mDetectStart = false
                                playSound(R.raw.face_detected, 1500)
                                val outputStream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                fireResultEvent(result = true,
                                    msg = "已通过活体检测，正在识别中...",
                                    bitmap = outputStream.toByteArray(),
                                    face = faceInfo)
                            }else{
                                fireResultEvent(result = false, msg = "人脸偏斜，请您正对摄像头")
                            }
                        }else{
                            Log.i(TAG, "检测到${cmIdsFace.size}张人脸.")
                            fireResultEvent(result = false, msg = "画面中不允许出现多张人脸")
                        }
                    }
                    else ->{
                        fireResultEvent(result = false, msg = "活体检测不通过，请重试(错误码:$ret)")
                    }
                }
            }
        }
        threadPool.execute(detectTask)
    }

    private fun fireResultEvent(result:Boolean, msg:String,
                                bitmap:ByteArray? = null,
                                face:CMIdsFace? = null):Unit{
        uiHandler.post {
            eventSink?.success(mapOf(
                "event" to EVENT_DETECT_RESULT,
                "result" to result,
                "msg" to msg,
                "bitmap" to bitmap,
                "left" to face?.left,
                "top" to face?.top,
                "right" to face?.right,
                "bottom" to face?.bottom,
                "rollAngle" to face?.roll_angle,
                "yawAngle" to face?.yaw_angle,
                "pitchAngle" to face?.pitch_angle
            ))
        }
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
        return linearLayout
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
            "startPreview" -> {
                val cameraIds = CameraDecide.doubleId
                Log.d(TAG,"camera id list:${cameraIds}")
                mCamera.doStartPreview(cameraIds[0], textureView)
                mBlackCamera.doStartPreview(cameraIds[1], blackTextureView)
                Log.d(TAG,"width:${textureView.width}, height:${textureView.height}")
            }
            "stopPreview" -> {
                mCamera.doStopPreview()
                mBlackCamera.doStopPreview()
            }
            "stopCamera" -> {
                mCamera.doStopCamera()
                mBlackCamera.doStopCamera()
            }
            "startDetect" -> {
                startFaceDetect()
            }
            "stopDetect" -> {
                mDetectStart = false
            }
            "releaseCamera" -> {
                mCamera.releaseAll()
                mBlackCamera.releaseAll()
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