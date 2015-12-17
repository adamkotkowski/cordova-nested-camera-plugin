package com.akotkowski.camera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

class Preview extends ViewGroup implements SurfaceHolder.Callback {
	private final String TAG = "Preview";

	public enum Orientation {
		LANDSCAPE, PORTRAIT
	};

	Orientation displayOrientation;
	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;
	Camera mCamera;

	Preview(Context context, Orientation orientation) {
		super(context);
		this.displayOrientation = orientation;

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);

		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			requestLayout();
		}
	}

	public void switchCamera(Camera camera) {
		setCamera(camera);
		try {
			camera.setPreviewDisplay(mHolder);
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		requestLayout();

		camera.setParameters(parameters);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
					height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				if (displayOrientation == Orientation.PORTRAIT) {
					previewWidth = mPreviewSize.height;
					previewHeight = mPreviewSize.width;
				} else {
					previewWidth = mPreviewSize.width;
					previewHeight = mPreviewSize.height;
				}

			}

			double ratio = (double) width / (double) height;
			double desiredRatio = (double) previewWidth
					/ (double) previewHeight;
			if (desiredRatio > ratio) {
				// we need to cut set padding on top and bottom
				int newHeight = (int) ((double) width / desiredRatio);
				child.layout(0, (height - newHeight) / 2, width,
						(height + newHeight) / 2);
			} else {
				// we need to cut set padding on left and right
				int newWidth = (int) ((double) height * desiredRatio);
				child.layout((width - newWidth) / 2, 0, (width + newWidth) / 2,
						height);
			}
			// if (width * previewHeight > height * previewWidth) {
			// final int scaledChildWidth = previewWidth * height /
			// previewHeight;
			// child.layout((width - scaledChildWidth) / 2, 0,
			// (width + scaledChildWidth) / 2, height);
			// } else {
			// final int scaledChildHeight = previewHeight * width /
			// previewWidth;
			// child.layout(0, (height - scaledChildHeight) / 2,
			// width, (height + scaledChildHeight) / 2);
			// }
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			// mCamera.stopPreview();
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (displayOrientation == Orientation.PORTRAIT) {
			targetRatio = (double) h / w;
		}
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			if (mPreviewSize != null) {
				parameters.setPreviewSize(mPreviewSize.width,
						mPreviewSize.height);
				Log.w("CameraPreview", "mPreviewSize = null");
			}
			requestLayout();

			mCamera.setParameters(parameters);
			mCamera.startPreview();
		}
	}
}