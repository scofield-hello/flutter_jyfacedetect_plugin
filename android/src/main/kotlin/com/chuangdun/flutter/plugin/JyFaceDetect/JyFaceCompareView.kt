package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.AliveDetect.AliveDetect
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
//柜机
private const val DEVICE_MODEL_Z10S = "Z10S"
//大屏双面屏
private const val DEVICE_MODEL_Z21 = "Z21"
//小屏幕双面屏
private const val DEVICE_MODEL_Z20 = "Z20"
//平板
private const val DEVICE_MODEL_M70 = "M70"

class JyFaceCompareView(private val context: Context, private val aliveDetect: AliveDetect, messenger: BinaryMessenger, id: Int, createParams: Map<*, *>) : PlatformView,
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
    private val width:Int
    private val height:Int
    private val minLeftPx:Int
    private val maxRightPx:Int
    private val minTopPx:Int
    private val maxBottomPx:Int
    private val pictureWidth:Int
    private val pictureHeight:Int
    init {
        val widthAsDP = (createParams["width"] as Int)
        width = dp2px(context, widthAsDP.toFloat())
        val heightAsDP = (createParams["height"] as Int)
        height = dp2px(context, heightAsDP.toFloat())
        val previewWidth = createParams["previewWidth"] as Int
        val previewHeight = createParams["previewHeight"] as Int
        val rotate = createParams["rotate"] as Int
        textureView.layoutParams = ViewGroup.LayoutParams(width,height)
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        mCamera = initCamera(previewWidth, previewHeight, rotate)
        Log.i(TAG, "current device MODEL:${Build.MODEL}")
        when(Build.MODEL){
            in setOf(DEVICE_MODEL_Z10S) -> {
                minLeftPx = 190
                maxRightPx = 410
                minTopPx = 10
                maxBottomPx = 300
                pictureWidth = 240
                pictureHeight = 320
            }
            DEVICE_MODEL_Z21 -> {
                minLeftPx = 210
                maxRightPx = 650
                minTopPx = 10
                maxBottomPx = 600
                pictureWidth = 480
                pictureHeight = 640
            }
            DEVICE_MODEL_Z20 -> {
                minLeftPx = 130
                maxRightPx = 420
                minTopPx = 10
                maxBottomPx = 390
                pictureWidth = 300
                pictureHeight = 400
            }
            DEVICE_MODEL_M70 -> {
                minLeftPx = 130
                maxRightPx = 420
                minTopPx = 10
                maxBottomPx = 390
                pictureWidth = 300
                pictureHeight = 400
            }
            else -> {
                Log.w(TAG, "未适配的设备类型:${Build.MODEL}")
                minLeftPx = 190
                maxRightPx = 410
                minTopPx = 10
                maxBottomPx = 300
                pictureWidth = 240
                pictureHeight = 320
            }
        }
    }

    private fun initCamera(previewWidth: Int, previewHeight: Int, rotate: Int):JYCamera{
        return JYCamera.Builder(context)
                .setCameraType(CameraConstant.CAMERA_1)
                .setCameraPreviewSize(previewWidth, previewHeight)
                .setCameraPictureSize(previewWidth, previewHeight)
                .setCameraRotation(rotate)
                //.mirror()
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
                val bitmap: Bitmap = mCamera.takePicture() ?: continue
                Log.d(TAG, "image size:${bitmap.width} * ${bitmap.height}")
                val faceList = aliveDetect.detectFace(bitmap, null)
                if (faceList == null || faceList.isEmpty()){
                    fireCompareResult(result = false, msg = "人脸比对不通过，未检测到人脸")
                    continue
                }
                if (faceList.size > 1){
                    fireCompareResult(result = false, msg = "人脸比对不通过，检测到${faceList.size}张人脸")
                    continue
                }
                val face = faceList[0]
                Log.d(TAG, "face size:${face.right - face.left} * ${face.bottom - face.top}")
                if (face.right - face.left < 100 || face.bottom - face.top < 100){
                    fireCompareResult(result = false, msg = "人脸区域太小，请您前进一小步")
                    continue
                }
                if (face.left < minLeftPx){
                    fireCompareResult(result = false, msg = "人脸需位于预览框中央，请向右移动一点${face.left}")
                    continue
                }
                if (face.right > maxRightPx){
                    fireCompareResult(result = false, msg = "人脸需位于预览框中央，请向左移动一点${face.right}")
                    continue
                }
                if (face.top < minTopPx ){
                    fireCompareResult(result = false, msg = "人脸需位于预览框中央，请向后移动一点${face.top}")
                    continue
                }
                if (face.bottom > maxBottomPx ){
                    fireCompareResult(result = false, msg = "人脸需位于预览框中央，请向前移动一点${face.bottom}")
                    continue
                }
                /*if (face.left < 220 || (width - face.right) < 220 ||
                        face.top < 20 || (height - face.bottom) < 20){
                    //fireCompareResult(result = false, msg = "人脸比对不通过，请将人脸处于预览框中央")
                    fireCompareResult(result = false, msg = "人脸比对不通过，${face.left} * ${width - face.right}")
                    continue
                }*/
                if (!face.isRightAngle()){
                    fireCompareResult(result = false, msg = "人脸比对不通过，请您平视并正对摄像头")
                    continue
                }
                val similar = Facecompare.getInstance().faceVerify(srcBitmap, bitmap)
                if (similar >= threshold){
                    mCompareStart = false
                    playSound(R.raw.face_verified, 1500)
                    val cropBitmap = Bitmap.createBitmap(bitmap, minLeftPx - 10, 0, pictureWidth, pictureHeight)
                    val outputStream = ByteArrayOutputStream()
                    cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    fireCompareResult(result = true, msg = "人脸比对已通过，请您保持两秒不要移动", similar = similar, bitmap = outputStream.toByteArray())
                }else{
                    fireCompareResult(result = false, msg = "人脸比对不通过，相似度低", similar = similar)
                }
            }
        }
        threadPool.execute(detectTask)
    }


    private fun fireCompareResult(result:Boolean, msg:String, similar:Int=0, bitmap:ByteArray?=null):Unit{
        uiHandler.post {  eventSink?.success(mapOf(
            "event" to EVENT_COMPARE_RESULT,
            "result" to result,
            "msg" to msg,
            "similar" to similar,
            "bitmap" to bitmap
        ))}
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