package com.akotkowski.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.camera.FileHelper;
import org.json.JSONArray;
import org.json.JSONException;

import com.akotkowski.camera.Preview.Orientation;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;

public class BackgroundCameraLauncher extends CordovaPlugin {

	public CallbackContext callbackContext;

	private Uri imageUri;
	
	private int width = 200;
	private int height = 200;
	private int locationTop = 100;
	private int locationLeft = 50;

	private Camera mCamera;
	private Preview mPreview;

	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action
	 *            The action to execute.
	 * @param args
	 *            JSONArry of arguments for the plugin.
	 * @param callbackContext
	 *            The callback id used when calling back into JavaScript.
	 * @return A PluginResult object with a status and message.
	 */
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;

		if (action.equals("init")) {

			this.width = args.getInt(0);
			this.height = args.getInt(1);
			this.locationTop = args.getInt(2);
			this.locationLeft = args.getInt(3);

			this.initCamera();
		}
		if (action.equals("cleanup")) {
			this.cleanupCamera();
		}
		if (action.equals("takePhoto")) {
			this.takePicture();
		}
		if (action.equals("show")) {
			if (mPreview == null) {
				initCamera();
			}
			this.showCamera();

			PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
			r.setKeepCallback(true);
			callbackContext.sendPluginResult(r);

			return true;
		} else if (action.equals("hide")) {
			this.hideCamera();
		}
		return true;
	}

	// --------------------------------------------------------------------------
	// LOCAL METHODS
	// --------------------------------------------------------------------------

	private void cleanupCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
	        mCamera.release();
			if(this.mPreview!=null){
				mPreview.setCamera(null);
				this.cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						View v = (View) mPreview.getParent(); // gets camera container view
						((ViewGroup)v.getParent()).removeView(v);
						mPreview = null;
					};
				});
			}
			mCamera = null;
		}
	}

	private void takePicture() {
		if(mCamera==null) {
			this.callbackContext.success("");
			return;
		}
		// get an image from the camera
		mCamera.autoFocus(new AutoFocusCallback() {

			public void onAutoFocus(boolean success, Camera camera) {
				mCamera.takePicture(null, null, new PictureCallback() {
					public void onPictureTaken(byte[] data, Camera camera) {
						BackgroundCameraLauncher.this.onPictureTaken(data,
								camera);
					}
				});
			}
		});
	}

	protected void onPictureTaken(byte[] data, Camera camera) {
		Uri uri = Uri.fromFile(new File(getTempDirectoryPath(), System
				.currentTimeMillis() + ".jpg"));

		// If all this is true we shouldn't compress the image.
		// if (this.targetHeight == -1 && this.targetWidth == -1 &&
		// this.mQuality == 100 &&
		// !this.correctOrientation) {
		
		Bitmap bmp;
		bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
		Bitmap resized = Bitmap.createScaledBitmap(bmp, 800, 600, true);
		bmp = null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		resized.compress(Bitmap.CompressFormat.JPEG, 90, stream);
		byte[] byteArray = stream.toByteArray();
		
		try {
			writeUncompressedImage(byteArray, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byteArray = null;
		this.callbackContext.success(uri.toString());

	}

	private void initCamera() {

		// initialization
		if (mCamera != null){
			cleanupCamera();
		}
			mCamera = getCameraInstance();
		if(mCamera==null) return;
		Parameters params = mCamera.getParameters();
		// params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		// params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		// params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
		// params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
		// params.setExposureCompensation(0);
		params.setPictureFormat(ImageFormat.JPEG);
		params.setJpegQuality(90);
		params.setRotation(90);

		List<Size> sizes = params.getSupportedPictureSizes();
		Size smallest = null;
		for (Size size : sizes) {
			System.out.println("size: " + size.width + "x" + size.height);
			if(smallest==null || (size.width<smallest.width && size.width>=300)) smallest = size;
		}
		Camera.Size size = smallest;
		params.setPictureSize(size.width, size.height);

		mCamera.setParameters(params);
		mCamera.setDisplayOrientation(90);
		if (mPreview == null) {
			Log.d("UI thread", "I am the UI thread");
			Configuration configuration = BackgroundCameraLauncher.this.cordova.getActivity().getResources().getConfiguration();
			mPreview = new Preview(
					BackgroundCameraLauncher.this.cordova.getActivity(),(configuration.orientation==configuration.ORIENTATION_PORTRAIT? Orientation.PORTRAIT: Orientation.LANDSCAPE));
			mPreview.setCamera(mCamera);
			final LinearLayout cameraContainer = new LinearLayout(
					BackgroundCameraLauncher.this.cordova.getActivity());
			
			FrameLayout view = (FrameLayout) BackgroundCameraLauncher.this.cordova
					.getActivity().findViewById(android.R.id.content);
			
			cameraContainer.setPadding(
					locationLeft,
					locationTop,
					view.getMeasuredWidth()
							- BackgroundCameraLauncher.this.width
							- BackgroundCameraLauncher.this.locationLeft,
					0);
			mPreview.setLayoutParams(new LayoutParams(
					BackgroundCameraLauncher.this.width,
					BackgroundCameraLauncher.this.height));
			cameraContainer.addView(mPreview);
			// new LayoutParams(1, LayoutParams.WRAP_CONTENT);
			
			this.cordova.getActivity().runOnUiThread(new Runnable() {
				
				public void run() {
					FrameLayout view = (FrameLayout) BackgroundCameraLauncher.this.cordova
							.getActivity().findViewById(android.R.id.content);
					view.addView(cameraContainer);
					mPreview.setVisibility(View.GONE);
				}
			});
		}else{
		//	mPreview.switchCamera(mCamera);
		}
		// end of: initialization
	}

	private void hideCamera() {
		if(mCamera==null)
			return;
		this.cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				mCamera.stopPreview();
				mPreview.setVisibility(View.GONE);
				
			}
		});
	}

	private void showCamera() {
		if(mCamera!=null){
			this.cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					mCamera.startPreview();
					mPreview.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = openFrontFacingCameraGingerbread(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}
	
	private static Camera openFrontFacingCameraGingerbread() {
	    int cameraCount = 0;
	    Camera cam = null;
	    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    cameraCount = Camera.getNumberOfCameras();
	    for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
	        Camera.getCameraInfo( camIdx, cameraInfo );
	        if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT  ) {
	            try {
	                cam = Camera.open( camIdx );
	            } catch (RuntimeException e) {
	                Log.e("BackgroundCameraLuncher", "Camera failed to open: " + e.getLocalizedMessage());
	            }
	        }
	    }

	    return cam;
	}

	private String getTempDirectoryPath() {
		File cache = null;

		// SD Card Mounted
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			cache = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ "/Android/data/"
					+ cordova.getActivity().getPackageName() + "/cache/");
		}
		// Use internal storage
		else {
			cache = cordova.getActivity().getCacheDir();
		}

		// Create the cache directory if it doesn't exist
		cache.mkdirs();
		return cache.getAbsolutePath();
	}

	private void writeUncompressedImage(byte[] data, Uri uri) throws FileNotFoundException,
			IOException {
//		FileInputStream fis = new FileInputStream(
//				FileHelper.stripFileProtocol(imageUri.toString()));
		OutputStream os = this.cordova.getActivity().getContentResolver()
				.openOutputStream(uri);
//		byte[] buffer = new byte[4096];
//		int len;
//		while ((len = fis.read(buffer)) != -1) {
			os.write(data, 0, data.length);
//		}
		os.flush();
		os.close();
//		fis.close();
	}

}
