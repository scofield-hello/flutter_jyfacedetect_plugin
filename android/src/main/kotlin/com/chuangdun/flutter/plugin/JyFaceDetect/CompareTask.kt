package com.chuangdun.flutter.plugin.JyFaceDetect

import android.graphics.Bitmap
import android.util.Log
import com.common.Facecompare
import java.util.concurrent.Callable

private const val TAG = "CompareTask"

class CompareTask(private val src: Bitmap, private val dest: Bitmap) : Callable<CompareResult> {
    override fun call(): CompareResult {
        val timeStart = System.currentTimeMillis()
        val similar = Facecompare.getInstance().faceVerify(src, dest)
        Log.d(TAG, "compare result: $similar")
        Log.d(TAG, "compare time: " + (System.currentTimeMillis() - timeStart))
        return CompareResult(similar, dest)
    }

}

class CompareResult(val similar:Int, val bitmap: Bitmap) {

}