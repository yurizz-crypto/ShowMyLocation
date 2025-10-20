package com.itsd83.showmylocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener {
    private GoogleMap member_map;
    private FusedLocationProviderClient fused_location_client;
    private Marker user_current_location;
    private LocationCallback remember_location;
    private static final int LOCATION_PERMISSION_REQUEST = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fused_location_client = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        remember_location = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    onLocationChanged(location);
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        member_map = googleMap;
        member_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        member_map.setOnMapClickListener(this);
        member_map.setOnMarkerClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                member_map.setMyLocationEnabled(true);
                startLocationUpdates();
            }
        } else {
            member_map.setMyLocationEnabled(true);
            startLocationUpdates();
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name this Location");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter a name");

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Add Pin", (dialog, which) -> {
            String markerTitle = input.getText().toString();

            if (markerTitle.trim().isEmpty()) {
                markerTitle = "Pinned Location";
            }

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(markerTitle);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            member_map.addMarker(markerOptions);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        LatLng pos = marker.getPosition();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(marker.getTitle())
                .setMessage("What would you like to do?");

        builder.setPositiveButton("Open in Maps", (dialog, which) -> {
            String uri = "geo:" + pos.latitude + "," + pos.longitude + "?q=" + pos.latitude + "," + pos.longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                String webUrl = "https://maps.google.com/?q=" + pos.latitude + "," + pos.longitude;
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                startActivity(webIntent);
            }
        });

        if (!"My Position".equals(marker.getTitle())) {
            builder.setNegativeButton("Remove", (dialog, which) -> {
                marker.remove();
                Toast.makeText(MapsActivity.this, "Pinned location removed", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNeutralButton("Cancel", null);
        builder.show();

        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .build();
        fused_location_client.requestLocationUpdates(locationRequest, remember_location, null);
    }

    private void onLocationChanged(Location location) {
        if (user_current_location != null) {
            user_current_location.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("My Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        user_current_location = member_map.addMarker(markerOptions);

        member_map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        member_map.animateCamera(CameraUpdateFactory.zoomTo(11));

        fused_location_client.removeLocationUpdates(remember_location);
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (member_map != null) {
                        member_map.setMyLocationEnabled(true);
                        startLocationUpdates();
                    }
                }
            } else {
                Toast.makeText(this, "Location permission denied. App functionality limited.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fused_location_client != null && remember_location != null) {
            fused_location_client.removeLocationUpdates(remember_location);
        }
    }
}