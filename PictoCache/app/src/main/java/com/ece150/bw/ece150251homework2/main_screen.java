package com.ece150.bw.ece150251homework2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

/**
 * Created by William on 2/26/2015.
 */

public class main_screen extends Activity {
    final int result = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        ImageButton btnTakePicture = (ImageButton) findViewById(R.id.btn_writeCache);
        ImageButton btnAddCache = (ImageButton) findViewById(R.id.btn_takePicture);

        configureTakePicture(btnTakePicture);
        configureAddCache(btnAddCache);

    }

    private void configureTakePicture(ImageButton btn){
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent startMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        startActivityForResult(startMainActivity, result);
                    }
                }
        );
    }

    private void configureAddCache(ImageButton btn) {
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent startMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        startActivityForResult(startMainActivity, result);
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


}
