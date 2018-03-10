#import "FlutterNativeImagePlugin.h"
#import <UIKit/UIKit.h>

@implementation FlutterNativeImagePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"flutter_native_image"
            binaryMessenger:[registrar messenger]];
  FlutterNativeImagePlugin* instance = [[FlutterNativeImagePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSDictionary *_arguments;
    if ([@"getPlatformVersion" isEqualToString:call.method]) {
        result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    else if ([@"compressImage" isEqualToString:call.method]) {
        _arguments = call.arguments;
        NSString file = [[_arguments objectForKey:@"file"] stringValue]
        int quality = [[_arguments objectForKey:@"quality"] intValue];
        int percentage = [[_arguments objectForKey:@"percentage"] intValue];
    
        NSData *data = [NSData dataWithContentsOfURL:file];
        UIImage img = [[UIImage alloc] initWithData:data];
        NSData* compressed = UIImageJPEGRepresentation(img, quality / 100);

    } else {
        result(FlutterMethodNotImplemented);
    }
}

+(UIImage *)compressImage:(UIImage *)image{
    float actualHeight = image.size.height;
    float actualWidth = image.size.width;
    float maxHeight = 1136.0f;
    float maxWidth = 640.0f;
    float imgRatio = actualWidth/actualHeight;
    float maxRatio = maxWidth/maxHeight;
    float compressionQuality = 1;//50 percent compression

    if (actualHeight > maxHeight || actualWidth > maxWidth){
        if(imgRatio < maxRatio){
            //adjust width according to maxHeight
            imgRatio = maxHeight / actualHeight;
            actualWidth = imgRatio * actualWidth;
            actualHeight = maxHeight;
        }
        else if(imgRatio > maxRatio){
            //adjust height according to maxWidth
            imgRatio = maxWidth / actualWidth;
            actualHeight = imgRatio * actualHeight;
            actualWidth = maxWidth;
        }
        else{
            actualHeight = maxHeight;
            actualWidth = maxWidth;
        }
    }else{
        actualHeight = maxHeight;
        actualWidth = maxWidth;
        compressionQuality = 1;
    }

    CGRect rect = CGRectMake(0.0, 0.0, actualWidth, actualHeight);
    UIGraphicsBeginImageContext(rect.size);
    [image drawInRect:rect];
    UIImage *img = UIGraphicsGetImageFromCurrentImageContext();
    NSData *imageData = UIImageJPEGRepresentation(img, compressionQuality);
    UIGraphicsEndImageContext();

    return [UIImage imageWithData:imageData];
}

@end
