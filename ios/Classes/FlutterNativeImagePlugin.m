#import "FlutterNativeImagePlugin.h"
#import <UIKit/UIKit.h>
#import "UIImage+Resize.h"

@implementation FlutterNativeImagePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"flutter_native_image"
                                     binaryMessenger:[registrar messenger]];
    FlutterNativeImagePlugin* instance = [[FlutterNativeImagePlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (UIImage *)normalizedImage:(UIImage *)image {
  if (image.imageOrientation == UIImageOrientationUp) return image;

  UIGraphicsBeginImageContextWithOptions(image.size, NO, image.scale);
  [image drawInRect:(CGRect){0, 0, image.size}];
  UIImage *normalizedImage = UIGraphicsGetImageFromCurrentImageContext();
  UIGraphicsEndImageContext();
  return normalizedImage;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSDictionary *_arguments;
    
    if ([@"getPlatformVersion" isEqualToString:call.method]) {
        result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    }
    else if ([@"compressImage" isEqualToString:call.method]) {
        _arguments = call.arguments;
        
        NSString *fileExtension = @"_compressed.jpg";
        
        int qualityArgument = [[_arguments objectForKey:@"quality"] intValue];
        int percentageArgument = [[_arguments objectForKey:@"percentage"] intValue];
        NSString *fileArgument = [_arguments objectForKey:@"file"];
        NSURL *uncompressedFileUrl = [NSURL URLWithString:fileArgument];
        
        NSString *fileName = [[fileArgument lastPathComponent] stringByDeletingPathExtension];
        NSString *tempFileName =  [fileName stringByAppendingString:fileExtension];
        NSString *finalFileName = [NSTemporaryDirectory() stringByAppendingPathComponent:tempFileName];
        
        NSString *path = [uncompressedFileUrl path];
        NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];
        
        UIImage *img = [[UIImage alloc] initWithData:data];

        if (img.size.height > 1500 || img.size.width > 1500) {
            printf("image needs resizing");
            CGFloat newWidth = (img.size.width / 100 * percentageArgument);
            CGFloat newHeight = (img.size.height / 100 * percentageArgument);
            
            CGSize newSize = CGSizeMake(newWidth, newHeight);
            
            UIImage *resizedImage = [img resizedImage:newSize interpolationQuality:kCGInterpolationHigh];
            resizedImage = [self normalizedImage:resizedImage];
            NSData *imageData = UIImageJPEGRepresentation(resizedImage, qualityArgument / 100);

            if ([[NSFileManager defaultManager] createFileAtPath:finalFileName contents:imageData attributes:nil]) {
                result(finalFileName);
            } else {
                result([FlutterError errorWithCode:@"create_error"
                                            message:@"Temporary file could not be created"
                                            details:nil]);
            }

            
            result(finalFileName);
            return;
        }
        
        NSData *imageData = UIImageJPEGRepresentation(img, qualityArgument / 100);
        
        if ([[NSFileManager defaultManager] createFileAtPath:finalFileName contents:imageData attributes:nil]) {
            result(finalFileName);
        } else {
            result([FlutterError errorWithCode:@"create_error"
                                       message:@"Temporary file could not be created"
                                       details:nil]);
        }
        
        result(finalFileName);
        return;
    } else {
        result(FlutterMethodNotImplemented);
    }
}
@end
