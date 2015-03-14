package com.ece150.bw.ece150251homework2;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by William Chen on 3/13/2015.
 */

public class CompareImages {
    private ImageView baseImg, compImg;
    private FrameLayout mFrame;
    private Button changeImg;
    int currImg = 0;
    private Bitmap baseBmp, compBmp, tempBmp;
    private Mat baseMat, compMat;
    private TextView text;
    private static int MIN_DIST = 10;
    private static int MIN_MATCHES = 750;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    public double compare() {
         /* Comparing Code */
        // Convert stuff to Bitmap
        baseBmp = BitmapFactory.decodeResource(getResources(), R.drawable.img1);
        if ((int) compImg.getTag() == R.drawable.img2)
            compBmp = BitmapFactory.decodeResource(getResources(), R.drawable.img2);
        else if ((int) compImg.getTag() == R.drawable.img3)
            compBmp = BitmapFactory.decodeResource(getResources(), R.drawable.img3);
        else if ((int) compImg.getTag() == R.drawable.img5)
            compBmp = BitmapFactory.decodeResource(getResources(), R.drawable.img5);
        else if ((int) compImg.getTag() == R.drawable.img6)
            compBmp = BitmapFactory.decodeResource(getResources(), R.drawable.img6);

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
            double result = Imgproc.compareHist(baseHist, compHist, Imgproc.CV_COMP_CHISQR);
            text.setText("Similarity: " + Double.toString(result));
            Log.d("ImgCompare", "Coefficient of Similarity: " + result);
            if (result == 0) {
                Toast.makeText(MainActivity.this, "The Pictures are exactly alike", Toast.LENGTH_SHORT).show();
            } else if (result > 0 && result < 20000) {
                Toast.makeText(MainActivity.this, "The Pictures are somewhat alike. Running more tests...", Toast.LENGTH_SHORT).show();
                new BetterComparison(MainActivity.this).execute();
            } else {
                Toast.makeText(MainActivity.this, "The Pictures are nothing alike.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (baseBmp == null) {
                Toast.makeText(MainActivity.this, "No base image.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No comparison image", Toast.LENGTH_SHORT).show();
            }
        }

        return result;
    }

    public class BetterComparison extends AsyncTask<Void, Void, Void> {
        private MainActivity BetterComparisonContext = null;
        private ProgressDialog prog;
        private FeatureDetector detector;
        private DescriptorExtractor extractor;
        private DescriptorMatcher matcher;
        private MatOfKeyPoint baseKeypoints, compKeypoints;
        private Mat baseDescriptors, compDescriptors, baseMat, compMat;
        private MatOfDMatch matches, final_matches;
        private boolean isMatch = false;

        @Override
        protected Void doInBackground(Void... params) {
            compare();
            return null;
        }

        public BetterComparison(MainActivity context) {
            BetterComparisonContext = context;
        }

        protected void onPreExecute() {
            // Set up the progress dialog box
            prog = new ProgressDialog(BetterComparisonContext);
            prog.setIndeterminate(true);
            prog.setCancelable(true);
            prog.setCanceledOnTouchOutside(false);
            prog.setMessage("Working...");
            prog.show();
        }

        void compare() {
            try {
                // Set up the Mats and images again
                baseBmp = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
                compBmp = compBmp.copy(Bitmap.Config.ARGB_8888, true);
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
        }

        protected void onPostExecute(Void result) {
            // What happens after the compare function.
            try {
                Mat newMat = new Mat();
                MatOfByte drawnMatches = new MatOfByte();
                Features2d.drawMatches(baseMat, baseKeypoints, compMat, compKeypoints, matches, newMat, new Scalar(0, 255, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
                Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGR2RGB);
                tempBmp = Bitmap.createBitmap(newMat.cols(), newMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(newMat, tempBmp);
                List<DMatch> final_matches_list = matches.toList();
                final int numMatches = final_matches_list.size();
                if (final_matches_list.size() > MIN_MATCHES) {
                    text.setText(final_matches_list.size() + " matches. It's probably a match");
                    isMatch = true;
                }
                else {
                    text.setText(final_matches_list.size() + " matches. It's probably NOT a match");
                    isMatch = false;
                }
                prog.dismiss();
            }
            catch (Exception e) {
                Log.e("After Feature Detection", "ERROR: Something went wrong");
                e.printStackTrace();
            }
        }
    }

}


