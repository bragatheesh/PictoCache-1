package com.ece150.bw.ece150251homework2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Camera theCamera;
    public Bitmap somebmp;
    Matrix matrix = new Matrix();
    FrameLayout preview;
    ImageView picCaptured;
    CameraPreview viewFinder;

    GPSLocation gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of Camera
        theCamera = getCameraInstance();

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

                            gps = new GPSLocation(MainActivity.this);
                            if (gps.canGetLocation()){
                                double latitude = gps.getLatitude();
                                double longitude = gps.getLongitude();

                                Toast.makeText(getApplicationContext(), "Latitude: " + latitude + "; Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                            }
                            else{ gps.showSettingsAlert(); }

                            captureButton.setText("Go Back");
                        }

                        //change to Preview mode
                        else if(captureButton.getText() == "Go Back") {
                            finish();
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

    @Override
    protected void onPause() {
        // release the camera immediately on pause event for other applications
        super.onPause();
        if (theCamera != null){
            theCamera.stopPreview();
            theCamera.release();
            theCamera = null;
        }
        gps.stopUsingGPS();
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
