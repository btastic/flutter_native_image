package com.example.flutternativeimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

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

      copyExif(fileName, outputFileName);

      result.success(outputFileName);
      return;
    }
    if(call.method.equals("getImageProperties")) {
      String fileName = call.argument("file");
      File file = new File(fileName);

      if(!file.exists()) {
        result.error("file does not exist", fileName, null);
        return;
      }

      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(fileName,options);

      HashMap<String, Integer> properties = new HashMap<String, Integer>();
      properties.put("width",options.outWidth);
      properties.put("height",options.outHeight);

      result.success(properties);
      return;
    }
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  private void copyExif(String filePathOri, String filePathDest) {
    try {
      ExifInterface oldExif = new ExifInterface(filePathOri);
      ExifInterface newExif = new ExifInterface(filePathDest);

      List<String> attributes =
          Arrays.asList(
              "FNumber",
              "ExposureTime",
              "ISOSpeedRatings",
              "GPSAltitude",
              "GPSAltitudeRef",
              "FocalLength",
              "GPSDateStamp",
              "WhiteBalance",
              "GPSProcessingMethod",
              "GPSTimeStamp",
              "DateTime",
              "Flash",
              "GPSLatitude",
              "GPSLatitudeRef",
              "GPSLongitude",
              "GPSLongitudeRef",
              "Make",
              "Model",
              "Orientation");
      for (String attribute : attributes) {
        setIfNotNull(oldExif, newExif, attribute);
      }

      newExif.saveAttributes();

    } catch (Exception ex) {
      Log.e("FlutterNativeImagePlugin", "Error preserving Exif data on selected image: " + ex);
    }
  }

  private void setIfNotNull(ExifInterface oldExif, ExifInterface newExif, String property) {
    if (oldExif.getAttribute(property) != null) {
      newExif.setAttribute(property, oldExif.getAttribute(property));
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
