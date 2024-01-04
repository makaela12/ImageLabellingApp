
package com.example.imagelabellingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defineButtons();

    }

    public void defineButtons() {
        findViewById(R.id.newProject).setOnClickListener(buttonClickListener);
        findViewById(R.id.oldProject).setOnClickListener(buttonClickListener);
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.newProject:
                    Intent intent1 = new Intent(MainActivity.this, createProject.class);
                    startActivity(intent1);
                    break;
                case R.id.oldProject:
                    Intent intent2 = new Intent(MainActivity.this, selectProject.class);
                    startActivity(intent2);
                    break;
            }
        }
    };
    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Handle the result from createProject
            // You can add any specific logic here if needed
        }
    }*/

}