package com.example.flutternativeimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import androidx.annotation.Nullable;

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

public class MethodCallHandlerImpl implements MethodChannel.MethodCallHandler {
    private final Context context;
    private final String LOG_TAG = "FlutterNativeImage";

    MethodCallHandlerImpl(Context context) {
        this.context = context;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        if(call.method.equals("compressImage")) {
            String fileName = call.argument("file");
            int resizePercentage = call.argument("percentage");
            int targetWidth = call.argument("targetWidth") == null ? 0 : (int) call.argument("targetWidth");
            int targetHeight = call.argument("targetHeight") == null ? 0 : (int) call.argument("targetHeight");
            int quality = call.argument("quality");

            File file = new File(fileName);

            if(!file.exists()) {
                result.error("file does not exist", fileName, null);
                return;
            }

            Bitmap bmp = BitmapFactory.decodeFile(fileName);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int newWidth = targetWidth == 0 ? (bmp.getWidth() / 100 * resizePercentage) : targetWidth;
            int newHeight = targetHeight == 0 ? (bmp.getHeight() / 100 * resizePercentage) : targetHeight;

            bmp = Bitmap.createScaledBitmap(
                    bmp, newWidth, newHeight, true);

            // reconfigure bitmap to use RGB_565 before compressing
            // fixes https://github.com/btastic/flutter_native_image/issues/47
            Bitmap newBmp = bmp.copy(Bitmap.Config.RGB_565, false);
            newBmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);

            try {
                String outputFileName = File.createTempFile(
                        getFilenameWithoutExtension(file).concat("_compressed"),
                        ".jpg",
                        context.getExternalCacheDir()
                ).getPath();

                OutputStream outputStream = new FileOutputStream(outputFileName);
                bos.writeTo(outputStream);

                copyExif(fileName, outputFileName, null);

                result.success(outputFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                result.error("file does not exist", fileName, null);
            } catch (IOException e) {
                e.printStackTrace();
                result.error("something went wrong", fileName, null);
            }

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
            properties.put("width", options.outWidth);
            properties.put("height", options.outHeight);

            int orientation = getOrientation(fileName);
            properties.put("orientation", orientation);

            result.success(properties);
            return;
        }
        if(call.method.equals("cropImage")) {
            String fileName = call.argument("file");
            int originX = call.argument("originX");
            int originY = call.argument("originY");
            int width = call.argument("width");
            int height = call.argument("height");

            File file = new File(fileName);

            if(!file.exists()) {
                result.error("file does not exist", fileName, null);
                return;
            }
            Boolean isPNG = fileName.toLowerCase().endsWith(".png");
            Bitmap.CompressFormat format = isPNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            String extension = isPNG ? ".png" : ".jpg";

            Bitmap bmp = decodeBitmapWithResolvedOrientation(fileName);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                bmp = Bitmap.createBitmap(bmp, originX, originY, width, height);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
                result.error("bounds are outside of the dimensions of the source image", fileName, null);
            }

            bmp.compress(format, 100, bos);
            bmp.recycle();
            OutputStream outputStream = null;
            try {
                String outputFileName = File.createTempFile(
                        getFilenameWithoutExtension(file).concat("_cropped"),
                        extension,
                        context.getExternalCacheDir()
                ).getPath();


                outputStream = new FileOutputStream(outputFileName);
                bos.writeTo(outputStream);

                copyExif(fileName, outputFileName, ExifInterface.ORIENTATION_NORMAL);

                result.success(outputFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                result.error("file does not exist", fileName, null);
            } catch (IOException e) {
                e.printStackTrace();
                result.error("something went wrong", fileName, null);
            }finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }



            return;
        }
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else {
            result.notImplemented();
        }
    }

    private void copyExif(String filePathOri, String filePathDest, @Nullable Integer overwriteOrientationAttr) {
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
                            "Model");
            for (String attribute : attributes) {
                setIfNotNull(oldExif, newExif, attribute);
            }

            if (overwriteOrientationAttr == null) {
                setIfNotNull(oldExif, newExif, "Orientation");
            } else {
                newExif.setAttribute("Orientation", String.valueOf(overwriteOrientationAttr));
            }

            newExif.saveAttributes();

        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error preserving Exif data on selected image: " + ex);
        }
    }

    private void setIfNotNull(ExifInterface oldExif, ExifInterface newExif, String property) {
        if (oldExif.getAttribute(property) != null) {
            newExif.setAttribute(property, oldExif.getAttribute(property));
        }
    }

    /**
     * This method resolves the orientation of an image and returns the bitmap normalised.
     * @param filePath Path of the image file. Used to decode the {@link Bitmap} and camera rotation
     * @return {@link Bitmap} object with normalised orientation.
     */
    private Bitmap decodeBitmapWithResolvedOrientation(String filePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        int orientation = getOrientation(filePath);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return bitmap;
        }

        try {
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return oriented;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * Reads the orientation attribute from an image file.
     * @param filePath Path of the image file
     * @return orientation attribute read by {@link ExifInterface} or the default value {@link ExifInterface#ORIENTATION_NORMAL}
     */
    private int getOrientation(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch(IOException ex) {
            // EXIF could not be read from the file; return default
            return ExifInterface.ORIENTATION_NORMAL;
        }
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
