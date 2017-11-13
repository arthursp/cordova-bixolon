# cordova-bixolon
==============

## Installation

### Using the Cordova CLI

```
$ cordova plugin add https://github.com/arthursp/cordova-bixolon.git
```

## Using the plugin

### Add line to print

```javascript
BixolonPrint.addLine({
    text       : String,    // text to print
    textAlign  : String,    // text align, default left
    textWidth  : int,       // text width, default 0
    textHeight : int,       // text height, default 0
    fontType   : String,    // font type, A or B
    fontStyle  : String     // font style, bold or underlined or reversed
});
```
### Print text lines

```javascript
cordova.plugins.bixolonPrint.printText(successCallback, errorCallback, config Object);
```

## Examples

### Print a text

```javascript
cordova.plugins.bixolonPrint.addLine("hello cordova!");
cordova.plugins.bixolonPrint.printText();
```
### Print a base64 image
```javascript
cordova.plugins.bixolonPrint.printBitmapWithBase64(successCallback, errorCallback, image, config);
```

## Usage with IONIC

### Installation

```
$ npm install bixolon-print
```

### Add text
```javascript

[...]
import { BixolonPrint } from 'bixolon-print';
[...]
providers: [BixolonPrint]
[...]
constructor(
    private bixolonPrint: BixolonPrint,
) {}
[...]
this.bixolonPrint.addLine({
    text: "text to print",
    textAlign: 'left',
    fontStyle: 'bold',
    textWidth: 1,
    fontType: 'A',
}).catch(console.log(error));

// return a promise, printerName = bluetooth name of the printer
this.bixolonPrint.printText(JSON.parse('{"printerName" : "'+printerName+'"}'));

this.bixolonPrint.printBitmapWithBase64([image_data], JSON.parse('{"printerName" : "'+printerName+'"}'));

```
