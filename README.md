# flutter_native_image
Native Flutter Image tools

This plugin aims to have native tools to resize images and reduce their quality by compression. The code is somewhat hacky (especially the iOS part), but it works for my needs and hasn't crashed on me. Feel free to improve it if you want to.

Right now there is only a compression function available.

## Usage
You have to give it a file from the file system and optionally provide a quality (1-100) and a resizing percentage (1-100).

Each platform will use it's proper tools to handle the resizing.

```dart
var compressedFile = await FlutterNativeImage.compressImage(file.path,
    quality: quality, percentage: percentage);
```

### Credits
Shoutouts to Trevor from Vocaro.com. He had the fitting algorithm for resizing images in Objective-C.

Source: http://vocaro.com/trevor/blog/2009/10/12/resize-a-uiimage-the-right-way/