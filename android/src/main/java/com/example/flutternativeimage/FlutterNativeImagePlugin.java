package com.example.flutternativeimage;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class FlutterNativeImagePlugin implements MethodCallHandler {
    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";
    private static final String ORIENTATION_KEY = "orientation";
    private static final String TARGET_WIDTH_KEY = "targetWidth";
    private static final String TARGET_HEIGHT_KEY = "targetHeight";
    private static final String FILE_PATH_KEY = "file";
    private static final String PERCENTAGE_KEY = "percentage";
    private static final String ANGLE_KEY = "angle";
    private static final String QUALITY_KEY = "quality";
    private static final String ORIGIN_X_KEY = "originX";
    private static final String ORIGIN_Y_KEY = "originY";

    private final Activity activity;

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_native_image");
        channel.setMethodCallHandler(new FlutterNativeImagePlugin(registrar.activity()));
    }

    private FlutterNativeImagePlugin(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "compressImage":
                compressImage(call, result);
                break;
            case "getImageProperties":
                getImageProperties(call, result);
                break;
            case "cropImage":
                cropImage(call, result);
                break;
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "rotateImage":
                rotateImage(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void compressImage(MethodCall call, Result result) {
        String fileName = call.argument(FILE_PATH_KEY);
        int resizePercentage = call.argument(PERCENTAGE_KEY);
        int targetWidth = call.argument(TARGET_WIDTH_KEY) == null ? 0 : (int) call.argument(TARGET_WIDTH_KEY);
        int targetHeight = call.argument(TARGET_HEIGHT_KEY) == null ? 0 : (int) call.argument(TARGET_HEIGHT_KEY);
        int quality = call.argument(QUALITY_KEY);

        File file = new File(fileName);
        if (!file.exists()) {
            result.error("file does not exist", fileName, null);
            return;
        }

        Bitmap bmp = BitmapFactory.decodeFile(fileName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int newWidth = targetWidth == 0 ? (bmp.getWidth() / 100 * resizePercentage) : targetWidth;
        int newHeight = targetHeight == 0 ? (bmp.getHeight() / 100 * resizePercentage) : targetHeight;

        bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);

        // reconfigure bitmap to use RGB_565 before compressing
        // fixes https://github.com/btastic/flutter_native_image/issues/47
        Bitmap newBmp = bmp.copy(Bitmap.Config.RGB_565, false);
        newBmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);

        try {
            String outputFileName = File.createTempFile(
                    getFilenameWithoutExtension(file).concat("_compressed"),
                    ".jpg",
                    activity.getExternalCacheDir()
            ).getPath();

            OutputStream outputStream = new FileOutputStream(outputFileName);
            bos.writeTo(outputStream);

            copyExif(fileName, outputFileName);

            result.success(outputFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            result.error("file does not exist", fileName, null);
        } catch (IOException e) {
            e.printStackTrace();
            result.error("something went wrong", fileName, null);
        }
    }

    private void getImageProperties(MethodCall call, Result result) {
        String fileName = call.argument(FILE_PATH_KEY);
        File file = new File(fileName);

        if (!file.exists()) {
            result.error("file does not exist", fileName, null);
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);
        HashMap<String, Integer> properties = new HashMap<String, Integer>();
        properties.put(WIDTH_KEY, options.outWidth);
        properties.put(HEIGHT_KEY, options.outHeight);

        int orientation = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (IOException ex) {
            // EXIF could not be read from the file; ignore
        }
        properties.put(ORIENTATION_KEY, orientation);

        result.success(properties);
    }

    private void cropImage(MethodCall call, Result result) {
        String fileName = call.argument(FILE_PATH_KEY);
        int originX = call.argument(ORIGIN_X_KEY);
        int originY = call.argument(ORIGIN_Y_KEY);
        int width = call.argument(WIDTH_KEY);
        int height = call.argument(HEIGHT_KEY);

        File file = new File(fileName);

        if (!file.exists()) {
            result.error("file does not exist", fileName, null);
            return;
        }

        Bitmap bmp = BitmapFactory.decodeFile(fileName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            bmp = Bitmap.createBitmap(bmp, originX, originY, width, height);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            result.error("bounds are outside of the dimensions of the source image", fileName, null);
        }

        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bmp.recycle();
        OutputStream outputStream = null;
        try {
            String outputFileName = File.createTempFile(
                    getFilenameWithoutExtension(file).concat("_cropped"),
                    ".jpg",
                    activity.getExternalCacheDir()
            ).getPath();


            outputStream = new FileOutputStream(outputFileName);
            bos.writeTo(outputStream);

            copyExif(fileName, outputFileName);

            result.success(outputFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            result.error("file does not exist", fileName, null);
        } catch (IOException e) {
            e.printStackTrace();
            result.error("something went wrong", fileName, null);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void rotateImage(MethodCall call, Result result) {
        int angle = call.argument(ANGLE_KEY);
        String filePath = call.argument(FILE_PATH_KEY);

        File file = new File(filePath);
        if (!file.exists()) {
            result.error("File does not exist", filePath, null);
            return;
        }

        Bitmap originalBitmap = BitmapFactory.decodeFile(filePath);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                matrix,
                true
        );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        try {
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            fos.write(bos.toByteArray());
            fos.flush();
            fos.close();
            result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
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

    private static String getFilenameWithoutExtension(File file) {
        String fileName = file.getName();
        if (fileName.indexOf(".") > 0) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return fileName;
        }
    }
}
