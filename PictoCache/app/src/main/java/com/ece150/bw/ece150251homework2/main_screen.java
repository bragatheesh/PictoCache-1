package com.ece150.bw.ece150251homework2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * Created by William on 2/26/2015.
 */

public class main_screen extends Activity {
    final int result = 1;
    private RadioGroup checkCache;
    private RadioButton rb_storke, rb_csil;
    private boolean bool_csil, bool_storke;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        ImageButton btnTakePicture = (ImageButton) findViewById(R.id.btn_takePicture);
        ImageButton btnAddCache = (ImageButton) findViewById(R.id.btn_writeCache);

        configureTakePicture(btnTakePicture);
        configureAddCache(btnAddCache);

        checkCache = (RadioGroup) findViewById(R.id.rGroup);
        rb_csil = (RadioButton) findViewById(R.id.radioButton_csil);
        rb_storke = (RadioButton) findViewById(R.id.radioButton_storke);

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
                        bool_csil = rb_csil.isChecked();
                        bool_storke = rb_storke.isChecked();

                        Bundle whichCache = new Bundle();
                        whichCache.putBoolean("set_storke", bool_storke);
                        whichCache.putBoolean("set_csil", bool_csil);
                        Intent startMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        startMainActivity.putExtras(whichCache);
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
