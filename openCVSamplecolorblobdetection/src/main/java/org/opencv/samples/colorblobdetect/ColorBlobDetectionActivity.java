package org.opencv.samples.colorblobdetect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "thole::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private ImageView imageView;

    private CameraBridgeViewBase mOpenCvCameraView;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;
    private Button camPic;
    private Bitmap inputImage;

    private FeatureDetector detector;//= FeatureDetector.create(FeatureDetector.SIFT);
    ///////////////////DESCRIPTORS
    private DescriptorExtractor extractor;
    /*Mat firstImgMatOfKeyPoints = new Mat();*/

    private DescriptorMatcher matcher;// = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                    if (detector == null) {
                        detector = FeatureDetector.create(FeatureDetector.ORB);
                    }
                    if (extractor == null) {
                        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                    }
                    if (matcher == null) {
                        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);
        imageView = (ImageView) findViewById(R.id.imgView);
        camPic = (Button) findViewById(R.id.btn_takePic);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }
    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
       /* int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Mat descriptors = new Mat();


        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        detector.detect(touchedRegionRgba, keyPoints);
        extractor.compute(touchedRegionRgba, keyPoints, descriptors);
        Log.i(TAG, "Touched rgba descriptors : "+ descriptors.size());

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();*/

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            //Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            //colorLabel.setTo(mBlobColorRgba);
            Log.e(TAG, "spectrumLabel:" + mRgba.rows() + ":" + mRgba.cols());
            if (mRgba.cols() <= mSpectrum.cols()) {
                Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
                mSpectrum.copyTo(spectrumLabel);
              }

            Scalar green = new Scalar(81, 190, 0);
            Mat tmp = new Mat();
            for (MatOfPoint contour: contours) {
                contour.convertTo(tmp, CvType.CV_32F );
                if ( Imgproc.contourArea(tmp) >= 500) {
                    RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
                    drawRotatedRect(mRgba, rotatedRect, green, 2);
                    double peri  = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                    MatOfPoint2f apporx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), apporx, 0.04*peri, true);
                    Log.i(TAG, "approxPoly:" + apporx.toArray().length);
                    if (apporx.toArray().length == 4) {
                        Imgproc.boundingRect(contour);
                        //Imgproc.CalibrationMatrixValues();
                    }
                }
            }
            List<Moments> mu = new ArrayList<Moments>(contours.size());
            for (int i = 0; i < contours.size(); i++) {
                mu.add(i, Imgproc.moments(contours.get(i), false));
                Moments p = mu.get(i);
                int x = (int) (p.get_m10() / p.get_m00());
                int y = (int) (p.get_m01() / p.get_m00());
                Core.circle(mRgba, new Point(x, y), 4, new Scalar(255,49,0,255));
            }
        }

        return mRgba;
    }

    public static void drawRotatedRect(Mat image, RotatedRect rotatedRect, Scalar color, int thickness) {
        Point[] vertices = new Point[4];
        rotatedRect.points(vertices);
        MatOfPoint points = new MatOfPoint(vertices);
        double angle = rotatedRect.angle;
        //Log.e(TAG, "angle: " + angle);

        Imgproc.drawContours(image, Arrays.asList(points), -1, color, thickness);
        /*Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);*/
        Log.i(TAG, "inputImage height: " +  image.height() + "width: " + image.width());
        if (angle == 0.0) {
            /*Log.e(TAG, "W1: " + vertices[0].x + "H: " + vertices[0].y);
            Log.e(TAG, "W2: " + vertices[1].x + "H: " + vertices[1].y);
            Log.e(TAG, "W3: " + vertices[2].x + "H: " + vertices[2].y);
            Log.e(TAG, "W4: " + vertices[3].x + "H: " + vertices[3].y);*/
            Core.putText(image, "W:" + (vertices[2].x - vertices[1].x), new Point(vertices[0].x + 100 , vertices[0].y), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,255),3);
            Core.putText(image, "H:" + (vertices[0].y - vertices[1].y), new Point(vertices[1].x , vertices[1].y + 100), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,255),3);

        }
        else if (angle == -90) {
            Core.putText(image, "W:" + (vertices[1].y - vertices[2].y), new Point(vertices[0].x + 100 , vertices[0].y), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,255),3);
            Core.putText(image, "H:" + (vertices[0].x - vertices[1].x), new Point(vertices[1].x , vertices[1].y + 100), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,255),3);
        }
    }
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public void takePicStandard(View v) {
        inputImage = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);//need to save bitmap
        Utils.matToBitmap(mRgba, inputImage);
        imageView.setImageBitmap(inputImage);

        //Mat tmp = new Mat();
        //tmp = mRgba.s
        //Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2BGRA);
        //Log.i(TAG, "inputImage height 11: " +  tmp.size().height + "width: " + tmp.size().width);
        getColorFromImage();
    }
    private  void getColorFromImage() {

       // Bitmap inputImage;
        /*Mat rgba = new Mat();
        Utils.bitmapToMat(inputImage, rgba);
        Log.e(TAG, rgba.size().toString());
        Bitmap imageMatched = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);//need to save bitmap*/
        Rect touchedRect = new Rect();

        touchedRect.width = inputImage.getWidth();
        touchedRect.height = inputImage.getHeight();

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar blobColorHsv = new Scalar(255);
        blobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < blobColorHsv.val.length; i++)
            blobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(blobColorHsv);

        Log.i(TAG, "Touched rgba color 111: (" + blobColorHsv.val[0] + ", " + blobColorHsv.val[1] +
                ", " + blobColorHsv.val[2] + ", " + blobColorHsv.val[3] + ")");

        int topLeftIndex = inputImage.getPixel(0, 0);int R1 = (topLeftIndex >> 16) & 0xff;

        int G1 = (topLeftIndex >> 8) & 0xff;
        int B1  = topLeftIndex  & 0xff;
        Log.i(TAG, "Touched rgba color: (" + topLeftIndex + ", " + R1 +
                ", " + G1 + ", " + B1 + ")");
       // Log.e("RGB", "R: " + R1 + "G:" + G1 + "B:" +B1);
        mDetector.setHsvColor(blobColorHsv);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;
    }
    private  Bitmap cropImage(Mat img) {
        Bitmap bmp = null;
        mDetector.process(img);
        List<MatOfPoint> contours = mDetector.getContours();
        Log.e(TAG, "Contours count: " + contours.size());
        Imgproc.drawContours(mRgba, contours, 0, CONTOUR_COLOR);

        Mat colorLabel = img.submat(4, 68, 4, 68);
        colorLabel.setTo(mBlobColorRgba);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        if (contours.size() >=  1) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(0).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);
            Mat selectedRegion = img.submat(rect);
            bmp = Bitmap.createBitmap(selectedRegion.cols(), selectedRegion.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(selectedRegion, bmp);


        }
        else {
            bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
        }
        return bmp;
    }
}
