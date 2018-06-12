package com.ocr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ocr.common.LoadingDialog;
import com.ocr.youtu.Youtu;
import com.pcitc.opencvdemo.BitmapUtils;
import com.pcitc.opencvdemo.DetectionBasedTracker;
import com.pcitc.opencvdemo.EyeUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class FaceOpenCVActivity2 extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "FaceOpenCVActivity";

    // 人脸检测view
    private JavaCameraView mOpenCvCameraView;
    //显示实时消息的textview
    private TextView message;

    private LoadingDialog mLoadingDialog;

    // rgb图像
    private Mat mRgba;
    // 灰度图像
    private Mat mGray;
    private DetectionBasedTracker mNativeDetector;

    //眨眼检测器
    private CascadeClassifier mEyeJavaDetector;
    // 图像人脸小于高度的多少就不检测
    private int mAbsoluteFaceSize = 0;
    // 标记是否超时
    private boolean overTime = false;
    private WindowManager manager;
    private int eyeCheckSuccessCount = 0;


    public static final String APP_ID = "10008768";                                 // 替换APP_ID
    public static final String SECRET_ID = "AKIDN8lBPUYSHuNdkCAjhVhnhQwISHyumQvd";  // 替换SECRET_ID
    public static final String SECRET_KEY = "jV6rTt782nU4hgaN3bkkXBbzGNI1a0oS";     // 替换SECRET_KEY

    private static String personId;

    /**
     * 倒计时，总计20秒，每次减少1秒
     */
    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 20, 1) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            overTime = true;
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    System.loadLibrary("OpenCV");
                    try {
                        InputStream is = getResources().openRawResource(com.pcitc.opencvdemo.R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                        cascadeDir.delete();

                        // 眨眼检测器
                        InputStream eyeIs = getResources().openRawResource(com.pcitc.opencvdemo.R.raw.haarcascade_eye);
                        File eyeDir = getDir("eyedir", Context.MODE_APPEND);
                        File eyeFile = new File(eyeDir, "haarcascade_eye.xml");
                        FileOutputStream eyeOs = new FileOutputStream(eyeFile);
                        byte[] bufferEye = new byte[4096];
                        int byetesReadEye;
                        while ((byetesReadEye = eyeIs.read(bufferEye)) != -1) {
                            eyeOs.write(bufferEye, 0, byetesReadEye);
                        }
                        eyeIs.close();
                        eyeOs.close();
                        mEyeJavaDetector = new CascadeClassifier(eyeFile.getAbsolutePath());
                        if (mEyeJavaDetector.empty()) {
                            Log.d(TAG, "眨眼识别器加载失败");
                        }
                        eyeFile.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "摄像头启动失败", Toast.LENGTH_SHORT).show();
                        FaceOpenCVActivity2.this.finish();
                    }
                    mOpenCvCameraView.enableView();
                    countDownTimer.start();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setContentView(R.layout.activity_opencv_face_detction2);
        message = findViewById(R.id.message);

        mOpenCvCameraView = findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCameraIndex(JavaCameraView.CAMERA_ID_FRONT);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {// 加载java库
            Toast.makeText(getApplicationContext(), "打开摄像头失败", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    /**
     * 获取camera回调的每一针的图像
     *
     * @param inputFrame
     * @return
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        // 检查是否超时
        if (overTime) {
            runOnUiThread(() -> showTipDialog("检测人脸超时，请重试"));
            return mRgba;
        }
        if (mOpenCvCameraView.getCameraIndex() == JavaCameraView.CAMERA_ID_FRONT) {
            // 原始opencv只识别手机横向的图像，此处是将max顺时针旋转90度
            if (mGray != null) {
                Mat mRgbaT = mGray.t();
                Core.flip(mRgbaT, mRgbaT, -1);
                mGray = mRgbaT;
            }
        } else if (mOpenCvCameraView.getCameraIndex() == JavaCameraView.CAMERA_ID_BACK) {
            if (mGray != null) {
                Mat mRgbaT = mGray.t();
                Core.flip(mRgbaT, mRgbaT, 1);
                mGray = mRgbaT;
            }
        }
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            float relativeFaceSize = 0.2f;
            if (Math.round(height * relativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * relativeFaceSize);
            }
            // 设置能检测的最小的人脸的尺寸
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }
        MatOfRect faces = new MatOfRect();
        if (mNativeDetector != null) {
            mNativeDetector.detect(mGray, faces);
        }
        final Rect[] facesArray = faces.toArray();
        dealEyeCheck(facesArray);
        return mRgba;
    }


    /**
     * 眨眼检测，活体检测
     */
    private void dealEyeCheck(Rect[] facesArray) {
        if (facesArray.length == 1) {
            Rect r = facesArray[0];
            Rect eyearea = new Rect((int) (r.x + r.width * 0.12f), (int) (r.y + (r.height * 0.17f)), (int) (r.width * 0.76f), (int) (r.height * 0.4f));
            Mat eyeMat = new Mat(mGray, eyearea);
            MatOfRect eyes = new MatOfRect();
            if (mEyeJavaDetector != null) {
                mEyeJavaDetector.detectMultiScale(eyeMat, eyes, 1.2f, 5, 2,
                        new Size(eyearea.width * 0.2f, eyearea.width * 0.2f),
                        new Size(eyearea.width * 0.5f, eyearea.height * 0.7f));
                Rect[] rects = eyes.toArray();
                int size = rects.length;
                EyeUtils.put(size);
                boolean success = EyeUtils.check();
                if (success) {
                    eyeCheckSuccessCount++;
                    EyeUtils.clearEyeCount();
                    // 连续两次眨眼成功认为检测成功，可以设置更大的值，保证检验正确率，但会增加检测难度
                    eyeCheckSuccessCount = 0;
                    setMessage("检测到人脸");
                    dealWithEyeCheckSuccess();
                } else {
                    setMessage("请眨眼");
                }
            }
        } else if (facesArray.length == 0) {
            EyeUtils.clearEyeCount();
            eyeCheckSuccessCount = 0;
            setMessage("未检测到人脸");
        } else {
            EyeUtils.clearEyeCount();
            eyeCheckSuccessCount = 0;
            setMessage("请保证只有一张人脸");
        }
    }


    /**
     * 眨眼检测成功后进行之后的处理
     */
    private void dealWithEyeCheckSuccess() {
        final Bitmap bitmap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.ARGB_8888);
        try {
            Utils.matToBitmap(mRgba, bitmap);
        } catch (Exception e) {
            setMessage("检测失败，请重试");
            EyeUtils.clearEyeCount();
            return;
        }
        runOnUiThread(() -> {
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.disableView();
            }
            int rotation = manager.getDefaultDisplay().getRotation();
            int degrees = 0;
            if (mOpenCvCameraView.getCameraIndex() == JavaCameraView.CAMERA_ID_FRONT) {
                switch (rotation) {
                    case Surface.ROTATION_0:
                        degrees = 270;
                        break;
                    case Surface.ROTATION_90:
                        degrees = 180;
                        break;
                    case Surface.ROTATION_180:
                        degrees = 90;
                        break;
                    case Surface.ROTATION_270:
                        degrees = 0;
                        break;
                    default:
                        break;
                }
            } else if (mOpenCvCameraView.getCameraIndex() == JavaCameraView.CAMERA_ID_BACK) {
                switch (rotation) {
                    case Surface.ROTATION_0:
                        degrees = 90;
                        break;
                    case Surface.ROTATION_90:
                        degrees = 0;
                        break;
                    case Surface.ROTATION_180:
                        degrees = 270;
                        break;
                    case Surface.ROTATION_270:
                        degrees = 180;
                        break;
                    default:
                        break;
                }
            }
            Matrix matrix = new Matrix();
            // 旋转
            matrix.postRotate(degrees);
            if (mOpenCvCameraView.getCameraIndex() == JavaCameraView.CAMERA_ID_FRONT) {
                // 水平翻转
                matrix.postScale(-1, 1);
            }
            final Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            // 裁剪
            int rotateBitmapWidth = rotateBitmap.getWidth();
            int rotateBitmapHeight = rotateBitmap.getHeight();
            int squareLength = Math.min(rotateBitmapWidth, rotateBitmapHeight);
            int startLenth = Math.max(rotateBitmapWidth, rotateBitmapHeight) - squareLength;
            final Bitmap squareBitmap = Bitmap.createBitmap(rotateBitmap, 0, startLenth / 2, squareLength, squareLength);
            rotateBitmap.recycle();
            Bitmap zoomBitmap = BitmapUtils.getZoomImage(squareBitmap, 1024);
            dealWtihZoomBitmap(zoomBitmap);
        });

    }

    /**
     * 处理压缩后的bitmap
     *
     * @param zoomBitmap
     */
    private void dealWtihZoomBitmap(final Bitmap zoomBitmap) {
        Youtu faceYoutu = new Youtu(APP_ID, SECRET_ID, SECRET_KEY);
        showLoading();
        new Thread(() -> {
            try {
                if (personId == null) {
                    String person_Id = "person_id" + System.currentTimeMillis();
                    List<String> group_ids = new ArrayList<>();
                    group_ids.add("group_id");
                    JSONObject jsonObject = faceYoutu.NewPerson(zoomBitmap, person_Id, group_ids);
                    runOnUiThread(() -> {
                        dismissLoading();
                        try {
                            boolean isSuccess = jsonObject.get("errorcode").toString().equals("0");
                            if (isSuccess) personId = person_Id;
                            showTipDialog("保存结果：" + isSuccess);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    JSONObject respose = faceYoutu.FaceVerify(zoomBitmap, personId);
                    runOnUiThread(() -> {
                        dismissLoading();
                        try {
                            showTipDialog("验证结果：" + respose.get("ismatch"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });

                }
            } catch (Exception e) {
                dismissLoading();
                runOnUiThread(() -> showTipDialog("验证失败"));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 显示超时的对话框
     */
    private void showTipDialog(String msg) {
        mOpenCvCameraView.disableView();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(msg);
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
            setResult(RESULT_CANCELED);
            finish();
        });
        builder.setPositiveButton("确定", (dialog, which) -> {
            dialog.dismiss();
            setResult(RESULT_CANCELED);
            finish();
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * 在主线程设置textview的文字
     *
     * @param msg
     */
    private void setMessage(final String msg) {
        runOnUiThread(() -> message.setText(msg));
    }


    private void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this);
        }
        mLoadingDialog.show();
    }

    private void dismissLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }
}
