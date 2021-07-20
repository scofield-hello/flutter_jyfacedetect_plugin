package com.chuangdun.flutter.plugin.JyFaceDetect

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*


object CameraDecide {
    //黑白摄像头
    private val CAMERA_ID_INFRARED = arrayOf(arrayOf("2B16-BCD3"), arrayOf("2B16-BCD7"))

    // 单目
    private val CAMERA_ID_ONE = arrayOf(arrayOf("2B16-BCD2", "2B16-BCD3", "2B16-BCD5", "2B16-BCD9"))

    // 单目180
    private val CAMERA_ID_ONE_180 = arrayOf(arrayOf("2B16-BCD8"))

    //活体
    private val CAMERA_ID_DOUBLE = arrayOf(arrayOf("2B16-BCD6"), arrayOf("2B16-BCD7"))

    // 双单目
    private val CAMERA_ID_ONE_ONE = arrayOf(
        arrayOf("2B16-BCD2", "2B16-BCD3", "2B16-BCD5", "2B16-BCD8", "2B16-BCD9"),
        arrayOf("2B16-BCD3", "2B16-BCD5", "2B16-BCD8", "2B16-BCD9")
    )

    // 单目+活体
    private val CAMERA_ID_ONE_DOUBLE = arrayOf(
        arrayOf("2B16-BCD6"),
        arrayOf("2B16-BCD7"),
        arrayOf("2B16-BCD2", "2B16-BCD3", "2B16-BCD5", "2B16-BCD8", "2B16-BCD9")
    )

    // 活体+活体
    private val CAMERA_ID_DOUBLE_DOUBLE = arrayOf(
        arrayOf("2B16-BCD6"),
        arrayOf("2B16-BCD7"),
        arrayOf("2B16-BCDA"),
        arrayOf("2B16-BCDB")
    )

    // 证件
    /*2B19-200 外接高拍仪*/
    private val CAMERA_ID_PHOTO = arrayOf(
        arrayOf(
            "2B16-6689",
            "2B18-0200",
            "2B19-0200",
            "2B19-0201",
            "2B30-0200",
            "090C-F37D",
            "2B19-200"
        )
    )

    //人脸-柜外
    private val CAMERA_ID_FACE_SECONDARY = arrayOf(arrayOf("2B16-BCD3"))

    //TODO 2020-09-23 zjb:2B16-BCD3 不会有使用人脸比对的情况,有就定制项目
    // 人脸比对
    private val CAMERA_ID_FACE =
        arrayOf(arrayOf("2B16-BCD9", "2B16-BCD6", "2B16-BCD5", "2B16-BCD2"))
    val oneId: Int
        get() {
            val ids = getCameraId(CAMERA_ID_ONE)
            return if (hasSOCCamera() && ids[0] >= 0) ids[0] + 1 else ids[0]
        }

    fun hasOneId(): Boolean {
        return hasIds(CAMERA_ID_ONE)
    }

    val one180Id: Int
        get() {
            val ids = getCameraId(CAMERA_ID_ONE_180)
            return if (hasSOCCamera() && ids[0] >= 0) ids[0] + 1 else ids[0]
        }
    val faceSecondaryId: Int
        get() {
            val ids = getCameraId(CAMERA_ID_FACE_SECONDARY)
            return if (hasSOCCamera() && ids[0] >= 0) ids[0] + 1 else ids[0]
        }
    val doubleId: IntArray
        get() {
            val ids = getCameraId(CAMERA_ID_DOUBLE)
            val hasSOCCamera = hasSOCCamera()
            var id = ids[0]
            ids[0] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[1]
            ids[1] = if (hasSOCCamera && id >= 0) id + 1 else id
            return ids
        }

    fun hasDoubleId(): Boolean {
        return hasIds(CAMERA_ID_DOUBLE)
    }

    val oneOneId: IntArray
        get() {
            val ids = getCameraId(CAMERA_ID_ONE_ONE)
            val hasSOCCamera = hasSOCCamera()
            var id = ids[0]
            ids[0] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[1]
            ids[1] = if (hasSOCCamera && id >= 0) id + 1 else id
            return ids
        }

    fun hasOneOneId(): Boolean {
        return hasIds(CAMERA_ID_ONE_ONE)
    }

    val oneDoubleId: IntArray
        get() {
            val ids = getCameraId(CAMERA_ID_ONE_DOUBLE)
            val hasSOCCamera = hasSOCCamera()
            var id = ids[0]
            ids[0] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[1]
            ids[1] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[2]
            ids[2] = if (hasSOCCamera && id >= 0) id + 1 else id
            return ids
        }

    fun hasOneDoubleId(): Boolean {
        return hasIds(CAMERA_ID_ONE_DOUBLE)
    }

    val doubleDoubleId: IntArray
        get() {
            val ids = getCameraId(CAMERA_ID_DOUBLE_DOUBLE)
            val hasSOCCamera = hasSOCCamera()
            var id = ids[0]
            ids[0] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[1]
            ids[1] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[2]
            ids[2] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[3]
            ids[3] = if (hasSOCCamera && id >= 0) id + 1 else id
            return ids
        }

    fun hasDoubleDoubleId(): Boolean {
        return hasIds(CAMERA_ID_DOUBLE_DOUBLE)
    }

    val photoId: Int
        get() {
            if (hasSOCCamera()) {
                return 0
            }
            val ids = getCameraId(CAMERA_ID_PHOTO)
            return ids[0]
        }
    val faceCompareId: Int
        get() {
            val ids = getCameraId(CAMERA_ID_FACE)
            return if (hasSOCCamera() && ids[0] >= 0) ids[0] + 1 else ids[0]
        }
    val infraredId: Int
        get() {
            val ids = getCameraId(CAMERA_ID_INFRARED)
            val hasSOCCamera = hasSOCCamera()
            var id = ids[0]
            ids[0] = if (hasSOCCamera && id >= 0) id + 1 else id
            id = ids[1]
            ids[1] = if (hasSOCCamera && id >= 0) id + 1 else id
            return id
        }

    private fun getCameraId(ids: Array<Array<String>>): IntArray {
        val resIds = IntArray(ids.size)
        for (index in resIds.indices) {
            resIds[index] = -1
        }
        val file = File("/sys/class/video4linux")
        if (!file.exists()) {
            return resIds
        }
        val files = file.listFiles()
        val list = Arrays.asList(*files)
        Collections.sort(
            list
        ) { o1, o2 -> o1.name.compareTo(o2.name) }
        val listString = ArrayList<String>()
        for (f in list) {
            val deviceFile = File(f, "device")
            if (!deviceFile.exists() || !deviceFile.isDirectory) {
                Log.e("CameraDecide", "getId file:" + f.name + " deviceFile not found")
                continue
            }
            var idProduct: String? = ""
            var idVendor: String? = ""
            try {
                val platform = deviceFile.canonicalFile
                val parent = platform.parentFile
                val pidFile = File(parent, "idProduct")
                val vidFile = File(parent, "idVendor")
                if (!pidFile.exists() || !vidFile.exists()) {
                    Log.e("CameraDecide", "getId idProduct idProduct  not found")
                    continue
                }
                idProduct = getFileString(pidFile)
                idVendor = getFileString(vidFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val str = "$idVendor-$idProduct"
            listString.add(str)
        }
        var flag = ""
        for (index in ids.indices) {
            val idss = ids[index]
            val str = putIndex(idss, listString, resIds, index, flag)
            flag = if (str == null) flag else flag + str
        }
        return resIds
    }

    private fun putIndex(
        idss: Array<String>,
        listString: ArrayList<String>,
        resIds: IntArray,
        index: Int,
        flag: String
    ): String? {
        for (id in idss) {
            for (i in listString.indices) {
                val str = listString[i]
                if (id.equals(str, ignoreCase = true) && !flag.contains(id)) {
                    resIds[index] = i
                    return id
                }
            }
        }
        return null
    }

    private fun hasSOCCamera(): Boolean {
        var br: BufferedReader? = null
        val equalSign = "="
        val markSignStart = "<!--"
        val markSignEnd = "-->"
        val videonameSign = "videoname"
        val videoname = "OV5640"
        try {
            val fis = FileInputStream("/data/camera/media_profiles.xml")
            val isr = InputStreamReader(fis, "UTF-8")
            br = BufferedReader(isr)
            var oneLineBuf: String? = null
            while (br.readLine().also { oneLineBuf = it } != null) {
                if (oneLineBuf!!.length < 3) {
                    continue
                }
                if (oneLineBuf!!.indexOf(markSignStart) == -1) {
                    continue
                }
                if (oneLineBuf!!.indexOf(markSignEnd) == -1) {
                    continue
                }
                if (oneLineBuf!!.indexOf(videonameSign) == -1) {
                    continue
                }
                if (oneLineBuf!!.indexOf(videoname) != -1) {
                    return true
                }
                //String leaveLine0 = oneLineBuf.substring(oneLineBuf.indexOf(markSignStart)+ markSignStart.length(), oneLineBuf.indexOf(markSignEnd));
                //Scanner scanner = new Scanner("videoname%d=\"%[^\"]\" index=%d facing=%d");
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                br!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun hasIds(idss: Array<Array<String>>): Boolean {
        val ids = getCameraId(idss)
        for (id in ids) {
            if (id < 0) {
                return false
            }
        }
        return true
    }

    fun getFileString(file: File): String? {
        if (!file.exists()) {
            return null
        }
        var br: BufferedReader? = null
        try {
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis, "UTF-8")
            br = BufferedReader(isr)
            return br.readLine()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                br!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
}