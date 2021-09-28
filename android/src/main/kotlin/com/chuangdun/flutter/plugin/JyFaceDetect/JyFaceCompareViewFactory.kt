package com.chuangdun.flutter.plugin.JyFaceDetect

import android.content.Context
import com.AliveDetect.AliveDetect
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class JyFaceCompareViewFactory(
    context: Context,
    private val aliveDetect: AliveDetect,
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, createParams: Any): PlatformView {
        return JyFaceCompareView(
            context,
            aliveDetect = aliveDetect,
            messenger = messenger,
            id = viewId,
            createParams = createParams as Map<*, *>
        )
    }
}