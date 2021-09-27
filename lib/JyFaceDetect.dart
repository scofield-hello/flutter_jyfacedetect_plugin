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
      {this.width = 240,
      this.height = 320,
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
  final bool result;
  final String msg;
  final Uint8List bitmap;
  final int left;
  final int top;
  final int right;
  final int bottom;
  final double rollAngle;
  final double yawAngle;
  final double pitchAngle;
  const JyFaceDetectResult(
      this.result,
      this.msg,
      this.bitmap,
      this.left,
      this.top,
      this.right,
      this.bottom,
      this.rollAngle,
      this.yawAngle,
      this.pitchAngle);
}

class JyDetectSdkInitResult {
  ///初始化结果,为true时表示初始化成功,反之false.
  final bool result;

  ///提示信息,成功时为success, 失败时为错误原因.
  final String msg;
  const JyDetectSdkInitResult(this.result, this.msg);
}

class JyFaceDetectPreviewFrame {
  final int height;
  final int width;
  final Uint8List yuvData;
  const JyFaceDetectPreviewFrame(this.yuvData, this.width, this.height);
}

class JyFaceDetectEventType {
  static const EVENT_CAMERA_OPENED = 0;
  static const EVENT_PREVIEW = 1;
  static const EVENT_PREVIEW_STOP = 2;
  static const EVENT_CAMERA_CLOSED = 3;
  static const EVENT_DETECT_START = 4;
  static const EVENT_DETECT_RESULT = 5;
}

class JyFaceDetectPlugin {
  static JyFaceDetectPlugin _instance;

  static const _methodChannel = const MethodChannel("JyFaceDetectSdk");
  static const _eventChannel = const EventChannel("JyFaceDetectSdkEvent");

  factory JyFaceDetectPlugin() => _instance ??= JyFaceDetectPlugin._();

  JyFaceDetectPlugin._() {
    _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  void _onEvent(dynamic event) {
    if (!_onInitSdkResult.isClosed) {
      _onInitSdkResult
          .add(JyDetectSdkInitResult(event['result'], event['msg']));
    }
  }

  final _onInitSdkResult = StreamController<JyDetectSdkInitResult>.broadcast();

  ///初始化结果返回时触发.
  Stream<JyDetectSdkInitResult> get onInitSdkResult => _onInitSdkResult.stream;

  ///初始化人脸检测SDK.
  ///初始化结果在[onInitSdkResult]中返回.
  Future<void> initFaceDetectSdk() async {
    _methodChannel.invokeMethod("initFaceDetectSdk");
  }

  void dispose() {
    _onInitSdkResult.close();
    _instance = null;
  }
}

class JyFaceDetectViewController {
  static const _EVENT_CHANNEL_NAME = "JyFaceDetectViewEvent";
  static const _METHOD_CHANNEL_NAME = "JyFaceDetectView";
  MethodChannel _methodChannel;
  EventChannel _eventChannel;

  void _onEvent(dynamic event) {
    switch (event['event']) {
      case JyFaceDetectEventType.EVENT_CAMERA_OPENED:
        if (!_onCameraOpened.isClosed) {
          _onCameraOpened.add(null);
        }
        break;
      case JyFaceDetectEventType.EVENT_PREVIEW:
        if (!_onPreview.isClosed) {
          _onPreview.add(JyFaceDetectPreviewFrame(
              event['yuvData'], event['width'], event['height']));
        }
        break;
      case JyFaceDetectEventType.EVENT_PREVIEW_STOP:
        if (!_onPreviewStop.isClosed) {
          _onPreviewStop.add(null);
        }
        break;
      case JyFaceDetectEventType.EVENT_CAMERA_CLOSED:
        if (!_onCameraClosed.isClosed) {
          _onCameraClosed.add(null);
        }
        break;
      case JyFaceDetectEventType.EVENT_DETECT_START:
        if (!_onDetectStart.isClosed) {
          _onDetectStart.add(null);
        }
        break;
      case JyFaceDetectEventType.EVENT_DETECT_RESULT:
        if (!_onDetectResult.isClosed) {
          _onDetectResult.add(JyFaceDetectResult(
              event['result'],
              event['msg'],
              event['bitmap'],
              event["left"],
              event["top"],
              event["right"],
              event["bottom"],
              event["rollAngle"],
              event["yawAngle"],
              event["pitchAngle"]));
        }
        break;
      default:
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

  final _onPreview = StreamController<JyFaceDetectPreviewFrame>.broadcast();

  ///每一帧预览画面都会触发.
  Stream<JyFaceDetectPreviewFrame> get onPreview => _onPreview.stream;

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

  ///检测结果返回时触发.
  Stream<JyFaceDetectResult> get onDetectResult => _onDetectResult.stream;

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
    _onCameraClosed.close();
    _onCameraOpened.close();
    _onPreview.close();
    _onPreviewStop.close();
    _onDetectStart.close();
    _onDetectResult.close();
  }
}
