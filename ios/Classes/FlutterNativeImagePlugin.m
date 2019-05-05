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
        int widthArgument = [[_arguments objectForKey:@"targetWidth"] intValue];
        int heightArgument = [[_arguments objectForKey:@"targetHeight"] intValue];
        NSString *fileArgument = [_arguments objectForKey:@"file"];
        NSURL *uncompressedFileUrl = [NSURL URLWithString:fileArgument];
        
        NSString *fileName = [[fileArgument lastPathComponent] stringByDeletingPathExtension];
        NSString *uuid = [[NSUUID UUID] UUIDString];
        NSString *tempFileName =  [NSString stringWithFormat:@"%@%@%@", fileName, uuid, fileExtension];
        NSString *finalFileName = [NSTemporaryDirectory() stringByAppendingPathComponent:tempFileName];
        
        NSString *path = [uncompressedFileUrl path];
        NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];
        
        UIImage *img = [[UIImage alloc] initWithData:data];

        CGFloat newWidth = (widthArgument == 0 ? (img.size.width / 100 * percentageArgument) : widthArgument);
        CGFloat newHeight = (heightArgument == 0 ? (img.size.height / 100 * percentageArgument) : heightArgument);
        
        CGSize newSize = CGSizeMake(newWidth, newHeight);
        
        UIImage *resizedImage = [img resizedImage:newSize interpolationQuality:kCGInterpolationHigh];
        resizedImage = [self normalizedImage:resizedImage];
        NSData *imageData = UIImageJPEGRepresentation(resizedImage, qualityArgument / 100.0);

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
    else if ([@"getImageProperties" isEqualToString:call.method]) {
        _arguments = call.arguments;
        
        NSString *fileArgument = [_arguments objectForKey:@"file"];
        NSURL *uncompressedFileUrl = [NSURL URLWithString:fileArgument];
        NSString *fileName = [[fileArgument lastPathComponent] stringByDeletingPathExtension];

        NSString *path = [uncompressedFileUrl path];
        NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];

        UIImage *img = [[UIImage alloc] initWithData:data];
        
        // Class ALAsset that provides a way to get EXIF attributes has been deprecated since iOS 8+,
        // but the replacing class PHAsset does not have a way to obtain image orientation.
        // For the purposes of FlutterNativeImagePlgin it's ok to leave it as undefined, as
        // all images captured/stored on iOS effectively have "normal" orientation so
        // it should not affect image crop/resize operations.
        int orientation = 0; // undefined orientation
        NSDictionary *dict = @{ @"width" : @(lroundf(img.size.width)),
                                @"height" : @(lroundf(img.size.height)),
                                @"orientation": @((NSInteger)orientation)};

        result(dict);
        return;
    }
    else if([@"cropImage" isEqualToString:call.method]) {
    	_arguments = call.arguments;

	NSString *fileExtension = @"_cropped.jpg";

    	NSString *fileArgument = [_arguments objectForKey:@"file"];
    	NSURL *uncompressedFileUrl = [NSURL URLWithString:fileArgument];
    	int originX = [[_arguments objectForKey:@"originX"] intValue];
    	int originY = [[_arguments objectForKey:@"originY"] intValue];
    	int width = [[_arguments objectForKey:@"width"] intValue];
    	int height = [[_arguments objectForKey:@"height"] intValue];

        NSString *fileName = [[fileArgument lastPathComponent] stringByDeletingPathExtension];   
        NSString *uuid = [[NSUUID UUID] UUIDString];
        NSString *tempFileName =  [NSString stringWithFormat:@"%@%@%@", fileName, uuid, fileExtension];
        NSString *finalFileName = [NSTemporaryDirectory() stringByAppendingPathComponent:tempFileName];
        
        NSString *path = [uncompressedFileUrl path];
        NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];
        
        UIImage *img = [[UIImage alloc] initWithData:data];
        img = [self normalizedImage:img];

        if(originX<0 || originY<0 
        	|| originX>img.size.width || originY>img.size.height 
        	|| originX+width>img.size.width || originY+height>img.size.height) {
        	result([FlutterError errorWithCode:@"bounds_error"
                                        message:@"Bounds are outside of the dimensions of the source image"
                                        details:nil]);
        }

		CGRect cropRect = CGRectMake(originX, originY, width, height);
		CGImageRef imageRef = CGImageCreateWithImageInRect([img CGImage], cropRect);
		UIImage *croppedImg = [UIImage imageWithCGImage:imageRef];
		CGImageRelease(imageRef);

		NSData *imageData = UIImageJPEGRepresentation(croppedImg, 1.0);

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
    else {
        result(FlutterMethodNotImplemented);
    }
}
@end
