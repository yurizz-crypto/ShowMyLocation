package com.itsd83.showmylocation;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener {
    private GoogleMap member_map;
    private FusedLocationProviderClient fused_location_client;
    private Marker user_current_location;
    private LocationCallback remember_location;
    private Location last_known_location;
    private static final int LOCATION_PERMISSION_REQUEST = 99;
    private static final String PREFS_NAME = "MarkerPrefs";
    private static final String MARKERS_KEY = "customMarkers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fused_location_client = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            check_location_permission();
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
                    on_location_changed(location);
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

        load_markers();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                member_map.setMyLocationEnabled(true);
                start_location_updates();
            }
        } else {
            member_map.setMyLocationEnabled(true);
            start_location_updates();
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

            save_marker(markerTitle, latLng.latitude, latLng.longitude);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        LatLng pos = marker.getPosition();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(marker.getTitle());

        String message = "What would you like to do?";
        if (last_known_location != null && !"My Position".equals(marker.getTitle())) {
            float[] results = new float[1];
            Location.distanceBetween(
                    last_known_location.getLatitude(),
                    last_known_location.getLongitude(),
                    pos.latitude,
                    pos.longitude,
                    results
            );
            double distanceKm = results[0] / 1000.0;
            message = String.format(Locale.getDefault(), "Distance from you: %.2f km\n\nWhat would you like to do?", distanceKm);
        }
        builder.setMessage(message);

        builder.setPositiveButton("Open in Maps", (dialog, which) -> {
            String uri = "geo:" + pos.latitude + "," + pos.longitude + "?q=" + pos.latitude + "," + pos.longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                String webUrl = "http://maps.google.com/?q=" + pos.latitude + "," + pos.longitude;
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                startActivity(webIntent);
            }
        });

        if (!"My Position".equals(marker.getTitle())) {
            builder.setNegativeButton("Remove", (dialog, which) -> {
                remove_marker(marker.getTitle(), pos.latitude, pos.longitude);
                marker.remove();
                Toast.makeText(MapsActivity.this, "Pinned location removed", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNeutralButton("Cancel", null);
        builder.show();

        return true;
    }

    private void start_location_updates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateDistanceMeters(10)
                .build();
        fused_location_client.requestLocationUpdates(locationRequest, remember_location, null);
    }

    private void on_location_changed(Location location) {
        last_known_location = location;

        if (user_current_location != null) {
            user_current_location.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("My Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        user_current_location = member_map.addMarker(markerOptions);

        if (!member_map.getCameraPosition().target.equals(latLng)) {
            member_map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            member_map.animateCamera(CameraUpdateFactory.zoomTo(11));
            fused_location_client.removeLocationUpdates(remember_location);
        }
    }

    private void save_marker(String title, double lat, double lng) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> existingMarkers = prefs.getStringSet(MARKERS_KEY, new HashSet<>());

        Set<String> mutableMarkers = new HashSet<>(existingMarkers);

        String newMarker = title + "|" + lat + "|" + lng;
        mutableMarkers.add(newMarker);

        editor.putStringSet(MARKERS_KEY, mutableMarkers);
        editor.apply();
    }

    private void load_markers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> markers = prefs.getStringSet(MARKERS_KEY, new HashSet<>());

        for (String markerString : markers) {
            String[] parts = markerString.split("\\|");

            if (parts.length == 3) {
                try {
                    String title = parts[0];
                    double lat = Double.parseDouble(parts[1]);
                    double lng = Double.parseDouble(parts[2]);

                    LatLng latLng = new LatLng(lat, lng);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(title);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    member_map.addMarker(markerOptions);

                } catch (NumberFormatException e) {
                    android.util.Log.e("MapsActivity", "Corrupted marker data: " + markerString, e);
                }
            }
        }
    }

    private void remove_marker(String title, double lat, double lng) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> existingMarkers = prefs.getStringSet(MARKERS_KEY, new HashSet<>());
        Set<String> mutableMarkers = new HashSet<>(existingMarkers);

        String markerToRemove = title + "|" + lat + "|" + lng;

        if (mutableMarkers.contains(markerToRemove)) {
            mutableMarkers.remove(markerToRemove);
            editor.putStringSet(MARKERS_KEY, mutableMarkers);
            editor.apply();
        }
    }


    private boolean check_location_permission() {
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
                        start_location_updates();
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