package com.example.locationapi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CHECK_SETTINGS_CODE = 111;

    private Button startLocationUpdatesButton, stoptLocationUpdatesButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;
    private String locationUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdatesButton = findViewById(R.id.startLocationUpdatesButton);
        stoptLocationUpdatesButton = findViewById(R.id.stoptLocationUpdatesButton);
        locationTextView = findViewById(R.id.locationTextView);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdate();
            }
        });

        buildLocationRequest();
        buildLocationCallback();
        buildlocationSettingsRequest();
    }

    private void startLocationUpdate() {
        isLocationUpdatesActive = true;
        startLocationUpdatesButton.setEnabled(false);
        stoptLocationUpdatesButton.setEnabled(true);

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(
                            LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                        );

                        updateLocationUi();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCode = ((ApiException) e).getStatusCode();

                        switch (statusCode){
                            case LocationSettingsStatusCodes
                                .RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(
                                            MainActivity.this,
                                            CHECK_SETTINGS_CODE
                                    );
                                } catch (IntentSender.SendIntentException sie) {
                                    sie.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes
                                    .SETTINGS_CHANGE_UNAVAILABLE:
                                String message =
                                        "Adjust location settings on your device";
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                                isLocationUpdatesActive = false;
                                startLocationUpdatesButton.setEnabled(true);
                                stoptLocationUpdatesButton.setEnabled(false);
                        }
                        updateLocationUi();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CHECK_SETTINGS_CODE:

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.d("MainActivity", "User has agreed to change location"
                                + "settings");
                        startLocationUpdate();
                        break;

                    case Activity.RESULT_CANCELED:
                        Log.d("MainActivity", "User has not agreed to change location"
                                + "settings");
                        isLocationUpdatesActive = false;
                        startLocationUpdatesButton.setEnabled(true);
                        stoptLocationUpdatesButton.setEnabled(false);
                        updateLocationUi();
                        break;
                }
                break;
        }
    }

    private void buildlocationSettingsRequest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUi();
            }
        };
    }

    private void updateLocationUi() {
        locationTextView.setText("" +
                currentLocation.getLatitude() + "/" +
                currentLocation.getLongitude()
        );
        locationUpdateTimeTextView.setText(
                DateFormat.getTimeInstance().format(new Date())
        );
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()){
            startLocationUpdate();
        }else if (!checkLocationPermission()){
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {

        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (shouldProvideRationale){

            showSnackBar();
        }
    }

    private void showSnackBar() {
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
