package com.example.flutternativeimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterNativeImagePlugin
 */
public class FlutterNativeImagePlugin implements MethodCallHandler {
  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_native_image");
    channel.setMethodCallHandler(new FlutterNativeImagePlugin());
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if(call.method.equals("compressImage")) {
      String fileName = call.argument("file");
      int resizePercentage = call.argument("percentage");
      int quality = call.argument("quality");

      File file = new File(fileName);

      if(!file.exists()) {
        result.error("file does not exist", fileName, null);
        return;
      }

      Bitmap bmp = BitmapFactory.decodeFile(fileName);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      if (bmp.getHeight() > 1500 || bmp.getWidth() > 1500) {
        int newWidth = (bmp.getWidth() / 100 * resizePercentage);
        int newHeight = (bmp.getHeight() / 100 * resizePercentage);

        bmp = Bitmap.createScaledBitmap(
                bmp, newWidth, newHeight, false);
      }

      bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);

      String outputFileName = getTempFileNameFromFile(file);

      try {
        OutputStream outputStream = new FileOutputStream(outputFileName);
        bos.writeTo(outputStream);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }

      result.success(outputFileName);
      return;
    }
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  private static String getTempFileNameFromFile(File file) {
    String fileName = getFilenameWithoutExtension(file);
    String newFileName = fileName.concat("_compressed.jpg");

    return new File(pathComponent(file.getPath()), newFileName).getPath();
  }

  private static String pathComponent(String filename) {
    int i = filename.lastIndexOf(File.separator);
    return (i > -1) ? filename.substring(0, i) : filename;
  }

  private static String getFilenameWithoutExtension(File file) {
    String fileName = file.getName();

    if (fileName.indexOf(".") > 0) {
      return fileName.substring(0, fileName.lastIndexOf("."));
    } else {
      return fileName;
    }
  }
}
