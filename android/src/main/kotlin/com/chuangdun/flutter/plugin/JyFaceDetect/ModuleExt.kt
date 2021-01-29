package com.chuangdun.flutter.plugin.JyFaceDetect

import com.serenegiant.cdpids.CMIdsFace

fun CMIdsFace.isRightAngle():Boolean{
    return pitch_angle in -15.0f..0.0f
            && yaw_angle in -10.0f..10.0f
            && roll_angle in -10.0f..10.0f
}