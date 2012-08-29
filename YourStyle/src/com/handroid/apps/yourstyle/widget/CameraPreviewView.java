package com.handroid.apps.yourstyle.widget;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {

    public CameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    SurfaceHolder mHolder;
    Camera mCamera;
    Context mContext;

    public CameraPreviewView(Context context) {
        super(context);
        init(context);
    }
    
    private void init(Context context) {
        mContext = context;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera camera) {
            if (imageData != null) {
                storeByteImage(mContext, imageData, 100,
                        "ImageName");
            }
        };
    };
    
    public /*static*/ boolean storeByteImage(Context mContext, byte[] imageData,
            int quality, String expName) {

        File sdImageMainDirectory = new File("/sdcard");
        FileOutputStream fileOutputStream = null;
        try {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 5;
            
            Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
                    imageData.length,options);

            fileOutputStream = new FileOutputStream(
                    sdImageMainDirectory.toString() +"/image.jpg");
                            
            BufferedOutputStream bos = new BufferedOutputStream(
                    fileOutputStream);

            myImage.compress(CompressFormat.JPEG, quality, bos);

            bos.flush();
            bos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
    
    public void photoCapture() {
        mCamera.takePicture(null, mPictureCallback, mPictureCallback);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mCamera = Camera.open();
        try {
           mCamera.setPreviewDisplay(holder);
           Parameters param = mCamera.getParameters();
           param.setRotation(180);
           mCamera.setParameters(param);
           
           // support auto focus
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
                    public void onShutter() {
                        // Play your sound here.
                    }
                };

                public void onAutoFocus(boolean success, Camera camera) {
                    camera.takePicture(shutterCallback, null, mPictureCallback);
                }
            });
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
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
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}