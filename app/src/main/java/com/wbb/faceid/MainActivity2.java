package com.wbb.faceid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Size;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.wbb.faceid.util.FaceHelper;
import com.wbb.faceid.util.ImageUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;


/**
 * Android 原生自带人脸检测
 */
public class MainActivity2 extends AppCompatActivity implements CameraXConfig.Provider {


    private PreviewView viewFinder;
    private ProcessCameraProvider cameraProvider;
    int cameraId = CameraSelector.LENS_FACING_FRONT;
    private FaceHelper mFaceHelper;
    private HandlerThread mFaceHandleThread;
    private Handler mFaceHandle;
    private boolean isFirst;
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        viewFinder = findViewById(R.id.preview);
        Button change = findViewById(R.id.change);
        change.setOnClickListener(v -> {
            if (cameraId == CameraSelector.LENS_FACING_FRONT) {
                cameraId = CameraSelector.LENS_FACING_BACK;
            } else {
                cameraId = CameraSelector.LENS_FACING_FRONT;
            }
            startCamera();
        });
        mFaceHelper = FaceHelper.getInstance();
        mFaceHelper.setZoom(1);
        mFaceHelper.setRectFlagOffset(2.0f);
        mFaceHandleThread = new HandlerThread("face");
        mFaceHandleThread.start();
        mFaceHandle = new Handler(mFaceHandleThread.getLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraId)
                .build();
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        // 启用非阻塞模式。在此模式下，执行程序在调用 analyze() 方法时会从相机接收最新的可用帧。
                        // 如果此方法所用的时间超过单帧在当前帧速率下的延迟时间，它可能会跳过某些帧，
                        // 以便 analyze() 在下一次接收数据时获取相机流水线中的最新可用帧。
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//缩短照片拍摄的延迟时间
                //.setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)//优化照片质量
                //.setTargetRotation(preview_view.display.rotation)//设置输出图像的所需旋转度。
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                // 处理图片
//                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                // insert your code here.
                Bitmap bitmap = ImageUtils.toBitmap(image.getImage());
                bitmap = rotaingImageView(270, bitmap);
                FaceDetector.Face[] faces = mFaceHelper.findFaces(bitmap);
//                    Logger.i(TAG + logMsg);
                FaceDetector.Face facePostion = null;
                int index = 0;
                if (faces != null) {
                    for (FaceDetector.Face face : faces) {
                        if (face == null) {
                            type = 0;
                            break;
                        } else {
                            type = 1;
                            break;
                        }
                    }
                }
                bitmap.recycle();
                bitmap = null;
                Message message = mHandler.obtainMessage(1);
                Bundle bun = new Bundle();
                bun.putInt("type", type);
//                bun.putString("msg", logMsg);
                message.setData(bun);
                mHandler.sendMessage(message);
                image.close();
            }
        });
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        Camera camera = (Camera) cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, imageAnalysis, preview);

    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = buffer.array();
        buffer.get(bytes);
        BitmapFactory.Options options = new BitmapFactory.Options();
//               options.inSampleSize =2;
        options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isFirst = true;
        if (mFaceHandleThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mFaceHandleThread.quitSafely();
            }
            try {
                mFaceHandleThread.join();
                mFaceHandleThread = null;
                mFaceHandle = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bun = msg.getData();
            switch (msg.what) {
                case 1:
                    int type = bun.getInt("type");
                    if (type == 1) {
//                        faceStatus.setText("识别到人脸");
                        Toast.makeText(MainActivity2.this, "识别到人脸", Toast.LENGTH_LONG).show();
                    } else {
//                        faceStatus.setText("没有识别到人脸");
                        Toast.makeText(MainActivity2.this, "没有识别到人脸", Toast.LENGTH_LONG).show();
                    }
//                    log.setText(bun.getString("msg") + ",act_in:" + index);
                    break;
            }
        }
    };

    private Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
    }
}