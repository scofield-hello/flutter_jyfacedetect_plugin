import 'dart:async';

import 'package:JyFaceDetect/JyFaceDetect.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  JyFaceDetectPlugin _plugin;
  JyFaceDetectViewController _controller;
  String _currentState = "初始化";
  JyFaceDetectResult _detectResult;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _plugin = JyFaceDetectPlugin();
    _controller = JyFaceDetectViewController();
    _plugin.onFaceDetectSdkInitResult.listen((initResult) {
      print("onInitSdkResult");
      if (initResult.result) {
        Future.delayed(Duration(milliseconds: 500), () {
          _controller.startPreview();
        }).then((value) {
          _controller.startPreview();
        });
      } else {
        setState(() {
          _currentState = "人脸检测初始化失败.";
        });
      }
    });
    _controller.onCameraOpened.listen((_) {
      print("onCameraOpened");
      setState(() {
        _currentState = "相机已打开";
      });
      //开始人脸检测
      _controller.startDetect();
    });
    _controller.onCameraClosed.listen((_) {
      print("onCameraClosed");
      setState(() {
        _currentState = "相机已关闭";
      });
    });
    _controller.onPreviewStop.listen((_) {
      print("onPreviewStop");
      setState(() {
        _currentState = "预览已停止";
      });
    });
    _controller.onDetectStart.listen((_) {
      print("onDetectStart");
      setState(() {
        _currentState = "开始人脸检测";
      });
    });
    _controller.onDetectResult.listen((detectResult) {
      print("onDetectResult");
      setState(() {
        _detectResult = detectResult;
        _currentState = "人脸检测完成";
      });
    });
  }

  void _onJyFaceDetectViewCreated() {
    print("_onJyFaceDetectViewCreated");
    _plugin.initFaceDetectSdk();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(_currentState),
              Row(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.black, width: 1.0),
                    ),
                    alignment: Alignment.center,
                    width: 640,
                    height: 240,
                    child: JyFaceDetectView(
                      controller: _controller,
                      onJyFaceDetectViewCreated: _onJyFaceDetectViewCreated,
                      creationParams:
                          JyFaceDetectViewParams(width: 640, height: 240),
                    ),
                  ),
                  if (_detectResult != null)
                    Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.black, width: 1.0),
                      ),
                      margin: EdgeInsets.symmetric(horizontal: 16.0),
                      alignment: Alignment.center,
                      height: 240,
                      width: 320,
                      child: _detectResult.result
                          ? Image.memory(
                              _detectResult.bitmap,
                              fit: BoxFit.contain,
                            )
                          : Text(_detectResult.msg),
                    ),
                  Padding(
                    child: OutlineButton(
                      onPressed: () {
                        //开始检测
                        _controller.startDetect();
                      },
                      child: Text("再次检测"),
                    ),
                    padding: EdgeInsets.symmetric(horizontal: 16.0),
                  ),
                  Padding(
                    child: OutlineButton(
                      onPressed: () {
                        //停止检测
                        _controller.stopDetect();
                        setState(() {
                          _currentState = "人脸检测停止";
                        });
                      },
                      child: Text("停止检测"),
                    ),
                    padding: EdgeInsets.symmetric(horizontal: 16.0),
                  ),
                ],
              ),
            ],
          )),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        print("didChangeAppLifecycleState:resume");
        _controller.startPreview();
        break;
      case AppLifecycleState.inactive:
        print("didChangeAppLifecycleState:inactive");
        break;
      case AppLifecycleState.paused:
        print("didChangeAppLifecycleState:pause");
        _controller.stopDetect();
        _controller.stopPreview();
        break;
      default:
        break;
    }
  }

  @override
  void dispose() {
    super.dispose();
    WidgetsBinding.instance.removeObserver(this);
    _controller.stopCamera();
    _controller.releaseCamera();
    _controller.dispose();
    _plugin.dispose();
  }
}
