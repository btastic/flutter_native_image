import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

class FlutterNativeImage {
  static const MethodChannel _channel =
      const MethodChannel('flutter_native_image');

  static Future<String> get platformVersion =>
      _channel.invokeMethod('getPlatformVersion');

  static Future<File> compressImage(String fileName,
      {int percentage = 70, int quality = 70}) async {
    var file = await _channel.invokeMethod("compressImage",
        {'file': fileName, 'quality': quality, 'percentage': percentage});

    return new File(file);
  }

  static Future<ImageProperties> getImageProperties(String fileName) async {
    var properties = Map.from(await _channel.invokeMethod("getImageProperties",{'file': fileName}));
    return new ImageProperties(width: properties["width"],height: properties["height"]);
  }

}

class ImageProperties {
  int width;
  int height;
  
  ImageProperties({int width = 0, int height = 0}) {
    this.width = width;
    this.height = height;
  }
}