package org.opencv.samples.colorblobdetect;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ndnnga on 10/24/2017.
 */

public class DetectImage extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "DetectImage_Demo";
    private ImageView imageView1, imageView2, imageView3;
    private Button btClose;
    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;
    private  Bitmap imgSrc, imgTmp;
    private  Mat imgSrcMap;
    private CameraBridgeViewBase mOpenCvCameraView;

    String infile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/filmTemp.jpg";
    String outfile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/out_shelf.jpg";
    String tempfile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/filmSrc.jpg";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.compare_activity);
        imageView1 = (ImageView) findViewById(R.id.imgView);
        imgSrc = BitmapFactory.decodeResource(getResources(), R.drawable.film);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //imgTmp = BitmapFactory.decodeResource(getResources(), R.drawable.keyboard1);

        mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    if (detector == null) {
                        detector = FeatureDetector.create(FeatureDetector.ORB);
                    }
                    if (extractor == null) {
                        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                    }
                    if (matcher == null) {
                        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                    }
                    imgSrcMap = new Mat();
                    Utils.bitmapToMat(imgSrc, imgSrcMap);
                    // find an image in image
                    //findMatch(Imgproc.TM_SQDIFF_NORMED); //ok
                    //                    findMatch(infile,tempfile,outfile, Imgproc.TM_CCOEFF);
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
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }


    public void findMatch( int match_method) {
        System.out.println("\nRunning Template Matching");

        Mat img = Highgui.imread(infile);
        Mat templ = Highgui.imread(tempfile);
       /*Mat img = new Mat(imgSrc.getWidth(), imgSrc.getHeight(),  CvType.CV_8UC1);
       Mat templ = new Mat(imgTmp.getWidth(), imgTmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(imgSrc, img);
        Utils.bitmapToMat(imgTmp, templ);*/
        // / Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);



        Imgproc.matchTemplate(img, templ,result,Imgproc.TM_CCOEFF_NORMED);//Template Matching
        Imgproc.threshold(result, result, 0.1, 1, Imgproc.THRESH_TOZERO);
        double threshold = 0.95;
        double maxval;
        Mat dst;
        while(true)
        {
            Core.MinMaxLocResult maxr = Core.minMaxLoc(result);
            Point maxp = maxr.maxLoc;
            maxval = maxr.maxVal;
            Point maxop = new Point(maxp.x + templ.width(), maxp.y + templ.height());
            dst = img.clone();
            if(maxval >= threshold)
            {
                System.out.println("Template Matches with input image");

                Core.rectangle(img, maxp, new Point(maxp.x + templ.cols(),
                        maxp.y + templ.rows()), new Scalar(0, 255, 0),5);
                Core.rectangle(result, maxp, new Point(maxp.x + templ.cols(),
                        maxp.y + templ.rows()), new Scalar(0, 255, 0),-1);
            }else{
                break;
            }
        }
       // Highgui.imwrite(outFile, dst);//save image

       // Bitmap bitmap = BitmapFactory.decodeFile(outFile);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
        Bitmap bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bmp);
        imageView1.setImageBitmap(bmp);
    }

    public Mat akaze(Mat rgba, Mat rgba2) {

        //MatOfDMatch matches = new MatOfDMatch();
        Mat descriptors = new Mat();
        Mat descriptors2 = new Mat();

        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints2 = new MatOfKeyPoint();
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(rgba2, rgba2, Imgproc.COLOR_BGR2RGB);
       /* if (detector == null) {
            detector = FeatureDetector.create(FeatureDetector.ORB);
        }*/
        detector.detect(rgba, keyPoints);
        extractor.compute(rgba, keyPoints, descriptors);

        detector.detect(rgba2, keyPoints2);
        extractor.compute(rgba2, keyPoints2, descriptors2);
        MatOfDMatch good_matches = new MatOfDMatch();
        LinkedList<MatOfDMatch> dmatchesListOfMat = new LinkedList<>();
        if (descriptors.cols() == descriptors2.cols() && descriptors.type() == descriptors2.type()) {
            matcher.knnMatch(descriptors, descriptors2, dmatchesListOfMat, 2);
        }
        LinkedList<DMatch> good_matchesList = new LinkedList<>();
        double ratio = 0.8;
        /*for (int matchIndx = 0; matchIndx < dmatchesListOfMat.size(); matchIndx++) {

            if (dmatchesListOfMat.get(matchIndx).toArray()[0].distance < ratio * dmatchesListOfMat.get(matchIndx).toArray()[1].distance) {
                good_matchesList.addLast(dmatchesListOfMat.get(matchIndx).toArray()[0]);
            }
        }*/
        for (int i = 0; i < dmatchesListOfMat.size(); i++) {
            MatOfDMatch matofDMatch = dmatchesListOfMat.get(i);
            DMatch[] dmatcharray = matofDMatch.toArray();
            DMatch m1 = dmatcharray[0];
            DMatch m2 = dmatcharray[1];

            if (m1.distance <= m2.distance * ratio) {
                good_matchesList.addLast(m1);

            }
        }
        good_matches.fromList(good_matchesList);
        Log.i(TAG, "good_matches: " + good_matches.toArray().length + ":" + good_matchesList.isEmpty());
        //feature and connection colors
        Scalar RED = new Scalar(255, 0, 0);
        Scalar GREEN = new Scalar(0, 255, 0);
        //output image
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        Features2d.drawMatches(rgba, keyPoints, rgba2, keyPoints2, new MatOfDMatch(),
                outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        //Core.putText(outputImg, "FRAME", new Point(rgba.width() / 2, 30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 255), 2);
        if (good_matches.toArray().length < 25) {

            Core.putText(outputImg, "NG", new Point(rgba.width() + rgba2.width() / 2, 30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0), 2);
        } else {

            List<KeyPoint> objKeypointlist = keyPoints.toList();
            List<KeyPoint> scnKeypointlist = keyPoints2.toList();

            LinkedList<Point> objectPoints = new LinkedList<>();
            LinkedList<Point> scenePoints = new LinkedList<>();

            for (int i = 0; i < good_matchesList.size(); i++) {
                objectPoints.addLast(objKeypointlist.get(good_matchesList.get(i).queryIdx).pt);
                scenePoints.addLast(scnKeypointlist.get(good_matchesList.get(i).trainIdx).pt);
            }

            MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
            objMatOfPoint2f.fromList(objectPoints);
            MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
            scnMatOfPoint2f.fromList(scenePoints);

            Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

            Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
            Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

            obj_corners.put(0, 0, new double[]{0, 0});
            obj_corners.put(1, 0, new double[]{rgba.cols(), 0});
            obj_corners.put(2, 0, new double[]{rgba.cols(), rgba.rows()});
            obj_corners.put(3, 0, new double[]{0, rgba.rows()});

            System.out.println("Transforming object corners to scene corners...");
            Core.perspectiveTransform(obj_corners, scene_corners, homography);

            //Mat img = new Mat();
            //Imgproc.cvtColor(rgba2, img, Imgproc.COLOR_BGR2RGB);
            Core.line(rgba, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
            Core.line(rgba, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
            Core.line(rgba, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
            Core.line(rgba, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);



            //Core.putText(outputImg, "OK", new Point(rgba.width() + rgba2.width() / 2, 30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0), 2);
        }
        //Imgproc.cvtColor(outputImg, outputImg, Imgproc.COLOR_BGR2RGB);
        // Bitmap bmp = Bitmap.createBitmap(outputImg.cols(), outputImg.rows(), Bitmap.Config.ARGB_8888);
        //outBitMap = Bitmap.createBitmap(outputImg.cols(), outputImg.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(outputImg, outBitMap);
        return rgba;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        return akaze(inputFrame.rgba(), imgSrcMap  );
       // return inputFrame.rgba();
    }
}

