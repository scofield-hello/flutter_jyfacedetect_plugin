# JyFaceDetect

A new flutter plugin project.

## Getting Started

```dart
JyFaceDetectViewController _controller;

@override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller = JyFaceDetectViewController();
    _controller.onInitSdkResult.listen((initResult) {
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
    _controller.initFaceDetectSdk();
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
                    height: 320,
                    width: 240,
                    child: JyFaceDetectView(
                      controller: _controller,
                      onJyFaceDetectViewCreated: _onJyFaceDetectViewCreated,
                      creationParams: JyFaceDetectViewParams(width: 240, height: 320),
                    ),
                  ),
                  if (_detectResult != null)
                    Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.black, width: 1.0),
                      ),
                      margin: EdgeInsets.symmetric(horizontal: 16.0),
                      alignment: Alignment.center,
                      height: 320,
                      width: 240,
                      child: Image.memory(
                        _detectResult.bitmap,
                        fit: BoxFit.contain,
                      ),
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
  }

```