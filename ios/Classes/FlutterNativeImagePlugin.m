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
        
        
        NSURL *finalFileUrl = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:tempFileName]];
        
        
        
        NSString *path = [uncompressedFileUrl path];
        NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];
        NSLog(@"bytes after loading image from file %tu", data.length);
        
        UIImage *img = [[UIImage alloc] initWithData:data];
        NSError *error;
        
        if (img.size.height > 1500 || img.size.width > 1500) {
            printf("image needs resizing");
            CGFloat newWidth = (img.size.width / 100 * percentageArgument);
            CGFloat newHeight = (img.size.height / 100 * percentageArgument);
            
            CGSize newSize = CGSizeMake(newWidth, newHeight);
            
            NSLog(@"using image scale for sizing");
            UIGraphicsBeginImageContextWithOptions(newSize, NO, img.scale);
            
            [img drawInRect:CGRectMake(0, 0, newWidth, newWidth)];
            
            UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
            
            NSLog(@"saving image to %s", [finalFileUrl.absoluteString UTF8String]);
            NSData *imageData = UIImageJPEGRepresentation(newImage, qualityArgument / 100);
            NSLog(@"bytes after conversion to jpeg to %tu", imageData.length);

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
        
        NSLog(@"image did not need resizing");
        NSLog(@"saving image to %s", [finalFileUrl.absoluteString UTF8String]);
        
        NSData *imageData = UIImageJPEGRepresentation(img, qualityArgument / 100);
        
        NSLog(@"bytes after conversion to jpeg to %tu", imageData.length);
        
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
