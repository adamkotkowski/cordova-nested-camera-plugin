#nested-camera-plugin
---------------

Disclaimer:
This plugin is work in progress. I don't guarantee that it works. Feel free to use it, fix it, change it.


## Installation
To install this plugin, follow the [Command-line Interface Guide](http://cordova.apache.org/docs/en/edge/guide_cli_index.md.html#The%20Command-line%20Interface).
If you are not using the Cordova Command-line Interface, follow [Using Plugman to Manage Plugins](http://cordova.apache.org/docs/en/edge/plugin_ref_plugman.md.html).

## Supported platforms
For now it's only Android

## How to use it
Import script BackgroundCamera.js
Example below:
```
function initBackgroundCamera() {
	var mainPhotoDiv = $('#mainPhoto');
	var position = mainPhotoDiv.offset();
	var options = {
		locationTop : position.top,
		locationLeft : position.left,
		width : mainPhotoDiv.outerWidth(),
		height : mainPhotoDiv.outerHeight()
	};
	navigator.backgroundCamera.init(null, null, options);
}
function showBackgroundCamera() {
	navigator.backgroundCamera.show();
}
function hideBackgroundCamera() {
	navigator.backgroundCamera.hide();
}
```
Example app.js:

```
var app = {
	// Application Constructor
	initialize : function() {
		this.bindEvents();
	},
	// Bind Event Listeners
	//
	// Bind any events that are required on startup. Common events are:
	// 'load', 'deviceready', 'offline', and 'online'.
	bindEvents : function() {
		document.addEventListener('deviceready', this.onDeviceReady, false);
		document.addEventListener("pause", this.devicePaused, false);
		document.addEventListener("resume", this.deviceResume, false);
		document.addEventListener("backbutton", function() {
			navigator.backgroundCamera.cleanup();
			navigator.backgroundCamera.cleanup();
			navigator.app.exitApp();
		}, true);
	},
	// deviceready Event Handler
	//
	onDeviceReady : function() {
		initBackgroundCamera();
		showBackgroundCamera(); 
	},
	devicePaused : function() {
		navigator.backgroundCamera.cleanup();
	},
	deviceResume : function() {
		initBackgroundCamera();
        showBackgroundCamera(); 
	}
};
```