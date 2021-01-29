import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class JyFaceDetectViewParams {
  final int width;
  final int height;
  final int rotate;
  final int previewWidth;
  final int previewHeight;
  const JyFaceDetectViewParams(
      {this.width = 352,
      this.height = 288,
      this.rotate = 0,
      this.previewWidth = 640,
      this.previewHeight = 480});

  Map<String, dynamic> asJson() {
    return {
      "width": width,
      "height": height,
      "rotate": rotate,
      "previewWidth": previewWidth,
      "previewHeight": previewHeight
    };
  }
}

class JyFaceDetectView extends StatelessWidget {
  final _viewType = "JyFaceDetectView";
  final JyFaceDetectViewParams creationParams;
  final JyFaceDetectViewController controller;
  final VoidCallback onJyFaceDetectViewCreated;
  const JyFaceDetectView(
      {Key key,
      this.controller,
      this.onJyFaceDetectViewCreated,
      this.creationParams = const JyFaceDetectViewParams()})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AndroidView(
        viewType: _viewType,
        creationParams: creationParams.asJson(),
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: _onPlatformViewCreated);
  }

  void _onPlatformViewCreated(int id) {
    if (controller != null) {
      controller.onCreate(id);
    }
    if (onJyFaceDetectViewCreated != null) {
      onJyFaceDetectViewCreated();
    }
  }
}

class JyFaceDetectResult {
  ///图片数据.
  final Uint8List bitmap;
  final int left;
  final int top;
  final int right;
  final int bottom;
  final double rollAngle;
  final double yawAngle;
  final double pitchAngle;
  const JyFaceDetectResult(this.bitmap, this.left, this.top, this.right, this.bottom,
      this.rollAngle, this.yawAngle, this.pitchAngle);
}

class JyDetectSdkInitResult {
  ///初始化结果,为true时表示初始化成功,反之false.
  final bool result;

  ///提示信息,成功时为success, 失败时为错误原因.
  final String msg;
  const JyDetectSdkInitResult(this.result, this.msg);
}

class JyFaceDetectEventType {
  static const EVENT_CAMERA_OPENED = 0;
  static const EVENT_PREVIEW = 1;
  static const EVENT_PREVIEW_STOP = 2;
  static const EVENT_CAMERA_CLOSED = 3;
  static const EVENT_DETECT_START = 4;
  static const EVENT_DETECT_RESULT = 5;
  static const EVENT_INIT_RESULT = 6;
}

class JyFaceDetectViewController {
  static const _EVENT_CHANNEL_NAME = "JyFaceDetectViewEvent";
  static const _METHOD_CHANNEL_NAME = "JyFaceDetectView";
  MethodChannel _methodChannel;
  EventChannel _eventChannel;

  void _onEvent(dynamic event) {
    switch (event['event']) {
      case JyFaceDetectEventType.EVENT_CAMERA_OPENED:
        _onCameraOpened.add(null);
        break;
      case JyFaceDetectEventType.EVENT_PREVIEW:
        _onPreview.add(null);
        break;
      case JyFaceDetectEventType.EVENT_PREVIEW_STOP:
        _onPreviewStop.add(null);
        break;
      case JyFaceDetectEventType.EVENT_CAMERA_CLOSED:
        _onCameraClosed.add(null);
        break;
      case JyFaceDetectEventType.EVENT_DETECT_START:
        _onDetectStart.add(null);
        break;
      case JyFaceDetectEventType.EVENT_DETECT_RESULT:
        _onDetectResult.add(JyFaceDetectResult(
            event['bitmap'],
            event["left"],
            event["top"],
            event["right"],
            event["bottom"],
            event["rollAngle"],
            event["yawAngle"],
            event["pitchAngle"]));
        break;
      case JyFaceDetectEventType.EVENT_INIT_RESULT:
        _onInitSdkResult.add(JyDetectSdkInitResult(event['result'], event['msg']));
        break;
    }
  }

  onCreate(int id) {
    _methodChannel = MethodChannel("${_METHOD_CHANNEL_NAME}_$id");
    _eventChannel = EventChannel("${_EVENT_CHANNEL_NAME}_$id");
    _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  final _onCameraOpened = StreamController<void>.broadcast();

  ///相机打开时触发.
  Stream<void> get onCameraOpened => _onCameraOpened.stream;

  final _onPreview = StreamController<void>.broadcast();

  ///每一帧预览画面都会触发.
  Stream<void> get onPreview => _onPreview.stream;

  final _onPreviewStop = StreamController<void>.broadcast();

  ///预览停止时触发.
  Stream<void> get onPreviewStop => _onPreviewStop.stream;

  final _onCameraClosed = StreamController<void>.broadcast();

  ///相机关闭时触发.
  Stream<void> get onCameraClosed => _onCameraClosed.stream;

  final _onDetectStart = StreamController<void>.broadcast();

  ///开始人脸检测时触发.
  Stream<void> get onDetectStart => _onDetectStart.stream;

  final _onDetectResult = StreamController<JyFaceDetectResult>.broadcast();

  ///比对结果返回时触发.
  Stream<JyFaceDetectResult> get onDetectResult => _onDetectResult.stream;

  final _onInitSdkResult = StreamController<JyDetectSdkInitResult>.broadcast();

  ///初始化结果返回时触发.
  Stream<JyDetectSdkInitResult> get onInitSdkResult => _onInitSdkResult.stream;

  ///初始化人脸比对SDK.
  ///初始化结果在[onInitSdkResult]中返回.
  Future<void> initFaceDetectSdk() async {
    _methodChannel.invokeMethod("initFaceDetectSdk");
  }

  ///开始预览画面,需要调用两次.
  Future<void> startPreview() async {
    _methodChannel.invokeMethod("startPreview");
  }

  ///关闭预览.
  Future<void> stopPreview() async {
    _methodChannel.invokeMethod("stopPreview");
  }

  ///关闭相机.
  ///关闭相机前请调用[stopPreview]停止预览.
  Future<void> stopCamera() async {
    _methodChannel.invokeMethod("stopCamera");
  }

  ///开始人脸检测.
  Future<void> startDetect() async {
    _methodChannel.invokeMethod("startDetect");
  }

  ///释放人脸识别模块.
  Future<void> stopDetect() async {
    _methodChannel.invokeMethod("stopDetect");
  }

  ///释放所有相机资源.
  ///释放之前请调用[stopPreview],[stopCamera]关闭相机
  Future<void> releaseCamera() async {
    _methodChannel.invokeMethod("releaseCamera");
  }

  void dispose() {
    _onInitSdkResult.close();
    _onCameraClosed.close();
    _onCameraOpened.close();
    _onPreview.close();
    _onPreviewStop.close();
    _onDetectStart.close();
    _onDetectResult.close();
  }
}
