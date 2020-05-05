package com.example.locationapi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button startLocationUpdatesButton, stoptLocationUpdatesButton;
    private TextView locationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdatesButton = findViewById(R.id.startLocationUpdatesButton);
        stoptLocationUpdatesButton = findViewById(R.id.stoptLocationUpdatesButton);
        locationTextView = findViewById(R.id.locationTextView);
    }
}
