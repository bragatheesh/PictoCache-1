package com.ece150.bw.ece150251homework2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.OutputStreamWriter;

public class MainActivity extends Activity {
    private Camera theCamera;
    public Bitmap somebmp;
    Matrix matrix = new Matrix();
    FrameLayout preview;
    ImageView picCaptured;
    CameraPreview viewFinder;
    private final static String STORETEXT="coordinates.txt";

    double latitude;
    double longitude;

    //GPSLocation gps;
    protected LocationManager locationManager;
    protected LocationListener LListener;

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


        // Create our Preview view and set it as the content of our activity.
        viewFinder = new CameraPreview(this, theCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(viewFinder);

        //set the text of the button
        final Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setText("Take Picture");

        //fix image rotation
        matrix.postRotate(90);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //to Picture mode
                        if(captureButton.getText()=="Take Picture") {
                            // get an image from the camera
                            theCamera.takePicture(null, null, takenPicture);

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

                                double CSMinLat = 34.413491;
                                double CSMaxLat = 34.413986;
                                double CSMinLong = -119.841958;
                                double CSMaxLong = -119.840821;

                                //storke tower
                                //check if within latitude bounds
                                if ((SPMinLat <= latitude) && (latitude <= SPMaxLat)) {
                                    //then check if within longitude bounds
                                    if ((SPMinLong <= longitude) && (longitude <= SPMaxLong)) {
                                        //if within lat and long bounds, toast coordinates
                                        Toast.makeText(getApplicationContext(), "STORKE TOWER!!\nLatitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                                        startActivityForResult(new Intent(MainActivity.this, InformationScreen.class), 1);
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
                                        startActivityForResult(new Intent(MainActivity.this, InformationScreen.class), 1);
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
                            }
                            else {
                                String gpserror = "Can't get GPS";
                                Toast.makeText(getApplicationContext(), gpserror, Toast.LENGTH_SHORT).show();
                            }

                            captureButton.setText("Go Back");
                        }

                        //change to Preview mode
                        else if(captureButton.getText() == "Go Back") {
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
            try{
                BitmapFactory.Options bitmap_options = new BitmapFactory.Options();

                //rgb 565 bitmap required
                bitmap_options.inPreferredConfig = Bitmap.Config.RGB_565;

                //get picture
                somebmp = BitmapFactory.decodeByteArray(data, 0, data.length, bitmap_options);
                somebmp = Bitmap.createBitmap(somebmp, 0, 0, somebmp.getWidth(), somebmp.getHeight(), matrix, true);
                Log.i("takenPicture", somebmp.getConfig().toString());

                picCaptured.setImageBitmap(somebmp);
                preview.addView(picCaptured);

            } catch (Exception e) {}
        }
    };

    private class MyLocationListener implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    }

    @Override
    protected void onPause() {
        // release the camera immediately on pause event for other applications
        super.onPause();
        if (theCamera != null){
            theCamera.stopPreview();
            theCamera.release();
            theCamera = null;
        }
        //gps.stopUsingGPS();
        locationManager.removeUpdates(LListener);
    }

    /* get an instance of the Camera object */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
}
