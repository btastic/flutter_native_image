import 'dart:async';

import 'package:flutter/services.dart';

class FlutterNativeImage {
  static const MethodChannel _channel =
      const MethodChannel('flutter_native_image');

  static Future<String> get platformVersion =>
      _channel.invokeMethod('getPlatformVersion');

  static Future<String> compressImage(String fileName,
      {int percentage = 70, int quality = 70}) {
    return _channel.invokeMethod("compressImage",
        {'file': fileName, 'quality': quality, 'percentage': percentage});
  }
}
