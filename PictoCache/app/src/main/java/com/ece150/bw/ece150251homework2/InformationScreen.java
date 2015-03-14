package com.ece150.bw.ece150251homework2;

        import android.app.Activity;
        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;

/**
 * Created by Bragatheesh on 3/6/2015.
 */
public class InformationScreen extends Activity {
    EditText name = (EditText) findViewById(R.id.edit_name);
    EditText landmark = (EditText) findViewById(R.id.edit_landmark);
    EditText comments = (EditText) findViewById(R.id.edit_comments);
    Button done = (Button) findViewById(R.id.button_done);
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_screen);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle dataBundle = new Bundle();
                dataBundle.putString("name",name.getText().toString());
                dataBundle.putString("landmark", landmark.getText().toString());
                dataBundle.putString("comments", comments.getText().toString());
                Intent intent = getIntent();
                intent.putExtras(dataBundle);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }


}
