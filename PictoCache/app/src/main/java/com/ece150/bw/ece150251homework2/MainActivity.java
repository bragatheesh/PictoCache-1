package com.ece150.bw.ece150251homework2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Camera theCamera;
    public Bitmap somebmp, storke, csil;
    Matrix matrix = new Matrix();
    FrameLayout preview;
    ImageView picCaptured, baseImage;
    CameraPreview viewFinder;
    private final static String STORETEXT = "coordinates.txt";

    boolean look_csil, look_storke;

    double latitude;
    double longitude;


    //GPSLocation gps;
    protected LocationManager locationManager;
    protected LocationListener LListener;

    // OpenCV stuff
    private Mat baseMat, compMat;
    private static int MIN_DIST = 10;
    private static int MIN_MATCHES = 400;
    double similarity, result;
    boolean isMatch = false;

    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;
    private MatOfKeyPoint baseKeypoints, compKeypoints;
    private Mat baseDescriptors, compDescriptors;
    private MatOfDMatch matches, final_matches;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of Camera
        theCamera = getCameraInstance();

        LListener = new MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, LListener);

        //auto focus
        Camera.Parameters params = theCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        theCamera.setParameters(params);

        // Make storke and csil a BMP

        storke = BitmapFactory.decodeResource(getResources(), R.drawable.storke);
        csil = BitmapFactory.decodeResource(getResources(), R.drawable.csil);

        // Create our Preview view and set it as the content of our activity.
        viewFinder = new CameraPreview(this, theCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(viewFinder);

        //set the text of the button
        final Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setText("Take Picture");

        try {
            Bundle extras = getIntent().getExtras();
            look_csil = extras.getBoolean("set_csil");
            look_storke = extras.getBoolean("set_storke");

            // Set up overlay
            baseImage = new ImageView(getApplicationContext());
            if (look_csil) {
                baseImage.setImageResource(R.drawable.csil);
            } else {
                baseImage.setImageResource(R.drawable.storke);
            }
            baseImage.setAlpha((float) .33);
            preview.addView(baseImage);
        }
        catch (Exception e) {

        }

        //fix image rotation
        matrix.postRotate(90);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //to Picture mode
                        if (captureButton.getText() == "Take Picture") {
                            // get an image from the camera
                            theCamera.takePicture(null, null, takenPicture);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            //get GPS coordinates
                            //gps = new GPSLocation(MainActivity.this);
                            //if (gps.canGetLocation()){
                            //get current coordinates
                            //double latitude = gps.getLatitude();
                            //double longitude = gps.getLongitude();

                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();

                                try {
                                    OutputStreamWriter out = new OutputStreamWriter(openFileOutput(STORETEXT, 0));

                                    //out.write(String.valueOf(latitude) + String.valueOf(longitude));
                                    out.close();
                                } catch (Throwable t) {
                                    Toast.makeText(getApplicationContext(), "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
                                }

                                double SPMinLat = 34.4118550;
                                double SPMaxLat = 34.4127620;
                                double SPMinLong = -119.8486510;
                                double SPMaxLong = -119.8478160;

                                double CSMinLat = 34.412946;
                                double CSMaxLat = 34.414008;
                                double CSMinLong = -119.842126;
                                double CSMaxLong = -119.840717;

                                if (somebmp == null) {
                                    Toast.makeText(getApplicationContext(), "someBMP is null", Toast.LENGTH_SHORT).show();
                                }

                                //storke tower
                                //check if within latitude bounds
                                if ((SPMinLat <= latitude) && (latitude <= SPMaxLat)) {
                                    //then check if within longitude bounds
                                    if ((SPMinLong <= longitude) && (longitude <= SPMaxLong)) {
                                        //if within lat and long bounds, toast coordinates
                                        Toast.makeText(getApplicationContext(), "STORKE TOWER!!\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                        similarity = compare(storke, somebmp, look_storke, look_csil);
                                        if (similarity >= 0 && similarity <= 20000) {
                                            isMatch = betterCompare(storke, somebmp, look_storke, look_csil);
                                        }
                                        if (isMatch) {
                                            startActivityForResult(new Intent(MainActivity.this, InformationScreen.class), 1);
                                        }
                                    }

                                    //if within latitude bounds, but not in longitude bounds
                                    else {
                                        //error message
                                        Toast.makeText(getApplicationContext(), "Not in Storke Plaza\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                //HFH CSIL
                                //check if within latitude bounds
                                else if ((CSMinLat < latitude) && (latitude < CSMaxLat)) {
                                    //then check if within longitude bounds
                                    if ((CSMinLong < longitude) && (longitude < CSMaxLong)) {
                                        //if within lat and long bounds, toast coordinates
                                        Toast.makeText(getApplicationContext(), "CSIL!!!\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                        similarity = compare(csil, somebmp, look_storke, look_csil);
                                        if (similarity >= 0 && similarity <= 20000) {
                                            isMatch = betterCompare(csil, somebmp, look_storke, look_csil);
                                        }
                                        if (isMatch) {
                                            startActivityForResult(new Intent(MainActivity.this, InformationScreen.class), 1);
                                        }
                                    }

                                    //if within latitude bounds, but not in longitude bounds
                                    else {
                                        //error message
                                        Toast.makeText(getApplicationContext(), "Not in CSIL\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                    }
                                } else if ((34.415202 < latitude) && (latitude < 34.415604)) {
                                    if ((-119.860501 < longitude) && (longitude < -119.859948)) {
                                        Toast.makeText(getApplicationContext(), "@HOME\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                        startActivityForResult(new Intent(MainActivity.this, InformationScreen.class), 1);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Kind of @HOME\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                //not within either latitude bounds
                                else {
                                    Toast.makeText(getApplicationContext(), "Not entering if statements\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String gpserror = "Can't get GPS";
                                Toast.makeText(getApplicationContext(), gpserror, Toast.LENGTH_SHORT).show();
                            }

                            captureButton.setText("Go Back");
                        }

                        //change to Preview mode
                        else if (captureButton.getText() == "Go Back") {
                            //finish();
                            theCamera.startPreview();
                            captureButton.setText("Take Picture");
                        }
                    }
                }
        );
    }

    PictureCallback takenPicture = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                BitmapFactory.Options bitmap_options = new BitmapFactory.Options();

                //rgb 565 bitmap required
                bitmap_options.inPreferredConfig = Bitmap.Config.RGB_565;

                //get picture
                somebmp = BitmapFactory.decodeByteArray(data, 0, data.length, bitmap_options);
                somebmp = Bitmap.createBitmap(somebmp, 0, 0, somebmp.getWidth(), somebmp.getHeight(), matrix, true);
                Log.i("takenPicture", somebmp.getConfig().toString());

                picCaptured.setImageBitmap(somebmp);
                preview.addView(picCaptured);

            } catch (Exception e) {
            }
        }
    };

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    @Override
    protected void onPause() {
        // release the camera immediately on pause event for other applications
        super.onPause();
        if (theCamera != null) {
            theCamera.stopPreview();
            theCamera.release();
            theCamera = null;
        }
        //gps.stopUsingGPS();
        locationManager.removeUpdates(LListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        csil = null;
        storke = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        csil = null;
        storke = null;
    }


    /* get an instance of the Camera object */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_OK && requestCode == 1)
        {
            Bundle extras = data.getExtras();
            try {
                OutputStreamWriter out = new OutputStreamWriter(openFileOutput(STORETEXT, 0));

                out.write("Name: " + extras.getString("name") + "\n" + "Landmark: " + extras.getString("landmark") + "\n" + "Comments: " + extras.getString("comments") +
                        "\n" + String.valueOf(latitude) + ", " + String.valueOf(longitude) );
                out.close();
            }
            catch (Throwable t) {

                Toast.makeText(getApplicationContext(), "Exception: "+t.toString(), Toast.LENGTH_LONG).show();

            }

        }
    }

    /* Image Comparison stuff */
    public double compare(Bitmap baseBmp, Bitmap compBmp, boolean isStorke, boolean isCsil) {
             /* Comparing Code */

        if (baseBmp == null) {
            Log.e("ImgCompare", "No base image, reassigning...");
            //Toast.makeText(MainActivity.this, "No base image.", Toast.LENGTH_SHORT).show();
            if (isStorke) {
                baseBmp = BitmapFactory.decodeResource(getResources(), R.drawable.storke);
            }
            else {
                baseBmp = BitmapFactory.decodeResource(getResources(), R.drawable.csil);
            }
        }

        if (baseBmp != null && compBmp != null) {
            // Scale the images and scale with different resolution cameras and resize for rough comparison
            if (baseBmp.getWidth() != compBmp.getWidth()) {
                compBmp = Bitmap.createScaledBitmap(compBmp, baseBmp.getWidth(), baseBmp.getHeight(), true);
            }
            //baseBmp = Bitmap.createScaledBitmap(baseBmp, 100, 100, true);
            //compBmp = Bitmap.createScaledBitmap(compBmp, 100, 100, true);

            // Convert BMPs to Mats
            baseMat = new Mat();
            compMat = new Mat();
            Utils.bitmapToMat(baseBmp, baseMat);
            Utils.bitmapToMat(compBmp, compMat);

            // Convert Color Space
            Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.cvtColor(compMat, compMat, Imgproc.COLOR_RGB2GRAY);
            baseMat.convertTo(baseMat, CvType.CV_32F);
            compMat.convertTo(compMat, CvType.CV_32F);

            // Do Histogram Based Comparisons
            Mat baseHist = new Mat();
            Mat compHist = new Mat();
            MatOfInt hSize = new MatOfInt(180);
            MatOfInt channels = new MatOfInt(0);
            ArrayList<Mat> baseBgrPlanes = new ArrayList<Mat>();
            ArrayList<Mat> compBgrPlanes = new ArrayList<Mat>();
            Core.split(baseMat, baseBgrPlanes);
            Core.split(compMat, compBgrPlanes);
            MatOfFloat hRanges = new MatOfFloat(0f, 180f);
            boolean accumulate = false;
            // Calculate and normalize the histograms
            Imgproc.calcHist(baseBgrPlanes, channels, new Mat(), baseHist, hSize, hRanges, accumulate);
            Core.normalize(baseHist, baseHist, 0, baseHist.rows(), Core.NORM_MINMAX, -1, new Mat());
            Imgproc.calcHist(compBgrPlanes, channels, new Mat(), compHist, hSize, hRanges, accumulate);
            Core.normalize(compHist, compHist, 0, compHist.rows(), Core.NORM_MINMAX, -1, new Mat());
            // Convert histograms to CV_32F for performance
            baseMat.convertTo(baseMat, CvType.CV_32F);
            compMat.convertTo(compMat, CvType.CV_32F);
            baseHist.convertTo(baseHist, CvType.CV_32F);
            compHist.convertTo(compHist, CvType.CV_32F);
            // Do the histogram comparing
            result = Imgproc.compareHist(baseHist, compHist, Imgproc.CV_COMP_CHISQR);
            //text.setText("Similarity: " + Double.toString(result));
            Log.d("ImgCompare", "Coefficient of Similarity: " + result);
            if (result == 0) {
                Toast.makeText(MainActivity.this, "The Pictures are exactly alike", Toast.LENGTH_SHORT).show();
            } else if (result > 0 && result < 20000) {
                Toast.makeText(MainActivity.this, "The Pictures are somewhat alike. Running more tests...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "The Pictures are nothing alike.", Toast.LENGTH_SHORT).show();
            }
        }

        else {
            if (baseBmp == null) {
                Toast.makeText(MainActivity.this, "No base image.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No comparison image", Toast.LENGTH_SHORT).show();
            }
        }


        return result;
    }
    public boolean betterCompare(Bitmap baseBmp, Bitmap compBmp, boolean isStorke, boolean isCsil) {
        try {
            if (baseBmp == null) {
                Log.e("ImgCompare", "No base image, reassigning...");
                //Toast.makeText(MainActivity.this, "No base image.", Toast.LENGTH_SHORT).show();
                if (isStorke) {
                    baseBmp = BitmapFactory.decodeResource(getResources(), R.drawable.storke);
                }
                else {
                    baseBmp = BitmapFactory.decodeResource(getResources(), R.drawable.csil);
                }
            }
            // Set up the Mats and images again
            //baseBmp = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
            //compBmp = compBmp.copy(Bitmap.Config.ARGB_8888, true);
            baseMat = new Mat();
            compMat = new Mat();
            Utils.bitmapToMat(baseBmp, baseMat);
            Utils.bitmapToMat(compBmp, compMat);
            Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_BGR2RGB);
            Imgproc.cvtColor(compMat, compMat, Imgproc.COLOR_BGR2RGB);

            // Set up the feature detection
            detector = FeatureDetector.create(FeatureDetector.PYRAMID_FAST);
            extractor = DescriptorExtractor.create(DescriptorExtractor.FREAK);
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            baseDescriptors = new Mat();
            compDescriptors = new Mat();
            baseKeypoints = new MatOfKeyPoint();
            compKeypoints = new MatOfKeyPoint();
            matches = new MatOfDMatch();

            // Detect the features
            detector.detect(baseMat, baseKeypoints);
            detector.detect(compMat, compKeypoints);
            Log.d("Feature Detection", "Base - Detected points: " + baseKeypoints.size());
            Log.d("Feature Detection", "Comp - Detected points: " + compKeypoints.size());

            // Get the Descriptors
            extractor.compute(baseMat, baseKeypoints, baseDescriptors);
            extractor.compute(compMat, compKeypoints, compDescriptors);
            Log.d("Feature Detection", "Base - Descriptors: " + baseDescriptors.size());
            Log.d("Feature Detection", "Comp - Descriptors: " + compDescriptors.size());

            // Match the Descriptors
            matcher.match(baseDescriptors, compDescriptors, matches);
            Log.d("Feature Detection", "Matches: " + matches.size());
            List<DMatch> matchList = matches.toList();
            List<DMatch> matchSemiFinals = new ArrayList<DMatch>();
            int i = 0;
            for (i = 0; i < matchList.size(); i++) {
                if (matchList.get(i).distance <= MIN_DIST) {
                    matchSemiFinals.add(matches.toList().get(i));
                }
            }
            final_matches = new MatOfDMatch();
            final_matches.fromList(matchSemiFinals);
        }
        catch (Exception e) {
            Log.e("Feature Detection", "ERROR: Something went wrong");
            e.printStackTrace();
        }

        // What happens after the compare function.
        try {
            /* In case of wanting to draw out the matches
            Mat newMat = new Mat();
            MatOfByte drawnMatches = new MatOfByte();
            Features2d.drawMatches(baseMat, baseKeypoints, compMat, compKeypoints, matches, newMat, new Scalar(0, 255, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
            Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGR2RGB);
            tempBmp = Bitmap.createBitmap(newMat.cols(), newMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(newMat, tempBmp);
            */
            List<DMatch> final_matches_list = matches.toList();
            final int numMatches = final_matches_list.size();
            if (final_matches_list.size() > MIN_MATCHES) {
                Toast.makeText(MainActivity.this, "There are " + final_matches_list.size() + " matches. Match = True", Toast.LENGTH_SHORT).show();
                //text.setText(final_matches_list.size() + " matches. It's probably a match");
                return true;
            }
            else {
                Toast.makeText(MainActivity.this, "There are " + final_matches_list.size() + " matches. Match = False", Toast.LENGTH_SHORT).show();

                //text.setText(final_matches_list.size() + " matches. It's probably NOT a match");
                return false;
            }
        }
        catch (Exception e) {
            Log.e("After Feature Detection", "ERROR: Something went wrong");
            e.printStackTrace();
        }
        return false;
    }
}