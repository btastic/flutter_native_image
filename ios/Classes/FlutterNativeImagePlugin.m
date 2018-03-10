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
        NSString *file = [[_arguments objectForKey:@"file"] stringValue];
        NSString *fileName = [[file lastPathComponent] stringByDeletingPathExtension];
        
        NSString *temp =  [fileName stringByAppendingString:fileExtension];
        
        NSURL *furl = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:temp]];
        
        int quality = [[_arguments objectForKey:@"quality"] intValue];
        int percentage = [[_arguments objectForKey:@"percentage"] intValue];
        
        NSURL *url = [NSURL URLWithString:file];
        NSData *data = [NSData dataWithContentsOfURL:url];
        UIImage *img = [[UIImage alloc] initWithData:data];
        
        if (img.size.height > 1500 || img.size.width > 1500) {
            CGFloat newWidth = (img.size.width / 100 * percentage);
            CGFloat newHeight = (img.size.height / 100 * percentage);
            
            CGSize newSize = CGSizeMake(newWidth, newHeight);
            UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
            
            [img drawInRect:CGRectMake(0, 0, newWidth, newWidth)];
            
            UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
            [UIImageJPEGRepresentation(newImage, quality / 100) writeToFile:furl.absoluteString atomically:YES];
            result(furl.absoluteString);
            return;
        }
        
        [UIImageJPEGRepresentation(img, quality / 100) writeToFile:furl.absoluteString atomically:YES];
        
        result(furl.absoluteString);
        return;
    } else {
        result(FlutterMethodNotImplemented);
    }
}
@end
