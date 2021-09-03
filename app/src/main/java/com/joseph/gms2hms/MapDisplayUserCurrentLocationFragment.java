package com.joseph.gms2hms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapDisplayUserCurrentLocationFragment extends Fragment {


    private static final int KEY_REQUEST_PERMISSION = 100;
    private final int[] mMapLayers = {
            GoogleMap.MAP_TYPE_NORMAL,
            // GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_HYBRID,
            // GoogleMap.MAP_TYPE_TERRAIN
    };
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mMap;
    private Marker selectedLocationMarker;
    private double mLat;
    private double mLng;
    private Context mContext;
    private Address mAddressObj;

    LocationManager mLocationManger;
    private int mMapCurrentLayer = 1;
    private View incProgressView;
    private final OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {

            mMap = googleMap;

            if (isLocationPermissionGranted()) {
                verifyGPSEnabled();
            } else {
                requestLocationPermission();
            }


            /** map types
             * roadmap : displays the default road map view. This is the default map type.
             * satellite : displays Google Earth satellite images.
             * hybrid : displays a mixture of normal and satellite views.
             * terrain : displays a physical map based on terrain information.
             */
            //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            changeMapLayer();

            googleMap.setOnMapClickListener(latLng -> {

                // if you want to enable user to select multiple location on map remove this if statement
                if (selectedLocationMarker != null) {
                    selectedLocationMarker.remove();
                }

                LatLng userSelectedLocation = new LatLng(latLng.latitude, latLng.longitude);
                getLocationName(userSelectedLocation.latitude, userSelectedLocation.longitude);


            });

        }
    };
    private View parentView;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        parentView = inflater.inflate(R.layout.fragment_maps, container, false);
        setHasOptionsMenu(true);
        mLocationManger = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        setupUi();

        return parentView;
    }

    private void setupUi() {
        incProgressView = parentView.findViewById(R.id.incProgressView);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


        FloatingActionButton fabMapLayer = parentView.findViewById(R.id.fabLayer);
        FloatingActionButton fabRelocateUserLocation = parentView.findViewById(R.id.fabLocate);
        Button btnContinue = parentView.findViewById(R.id.btnContinue);

        fabRelocateUserLocation.setOnClickListener(v -> {


            if (isLocationPermissionGranted()) {
                verifyGPSEnabled();
            } else {
                requestLocationPermission();
            }

        });

        fabMapLayer.setOnClickListener(v -> {
            changeMapLayer();
        });

        btnContinue.setOnClickListener(v -> {
            String addressText = mAddressObj.getLocality() + ", " + mAddressObj.getAdminArea() + ", " + mAddressObj.getCountryName();
            Toast.makeText(mContext, addressText, Toast.LENGTH_SHORT).show();
            Log.d("address", "full address: " + addressText);
            Log.d("address_latlng", "lat: " + mLat + " | lng: " + mLng);

        });
    }

    private void verifyGPSEnabled() {
        if (hasGPSEnabled()) {
            displayUserCurrentLocation();
        } else {
            Toast.makeText(mContext, "Please enable GPS!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasGPSEnabled() {
        return mLocationManger.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private void changeMapLayer() {

        /** map types
         * roadmap : displays the default road map view. This is the default map type.
         * satellite : displays Google Earth satellite images.
         * hybrid : displays a mixture of normal and satellite views.
         * terrain : displays a physical map based on terrain information.
         */
        if (mMapCurrentLayer == mMapLayers.length) {
            mMapCurrentLayer = 0;
        }
        mMap.setMapType(mMapLayers[mMapCurrentLayer]);
        mMapCurrentLayer++;


    }

    private void createMarker(double lat, double lng) {

        // LatLng sydney = new LatLng(-34, 151);
        LatLng userCurrentLocation = new LatLng(lat, lng);


        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
        }

        mLat = lat;
        mLng = lng;

        String addressText = mAddressObj.getLocality() + ", " + mAddressObj.getAdminArea() + ", " + mAddressObj.getCountryName();
        Toast.makeText(mContext, addressText, Toast.LENGTH_SHORT).show();


        incProgressView.setVisibility(View.GONE);

        selectedLocationMarker = mMap.addMarker(new MarkerOptions().position(userCurrentLocation).title(addressText));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userCurrentLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18f), 1000, null);

    }

    @SuppressLint("MissingPermission")
    private void displayUserCurrentLocation() {
        incProgressView.setVisibility(View.VISIBLE);
        mFusedLocationClient.getLastLocation().addOnCompleteListener(task -> {

            Location userLocation = task.getResult();// get user current location

            if (userLocation == null) {
                requestLocationData();
            } else {

                double lat = userLocation.getLatitude();
                double lng = userLocation.getLongitude();
                getLocationName(lat, lng);
            }

        });

    }


    @SuppressLint("MissingPermission")
    private void requestLocationData() {

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//get user location with high accuracy!
        request.setInterval(0);
        request.setFastestInterval(0);
        request.setNumUpdates(1);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        mFusedLocationClient.requestLocationUpdates(request, locationCallback(), Looper.myLooper());

    }

    private LocationCallback locationCallback() {

        LocationCallback callback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();

                double lat = location.getLatitude();
                double lng = location.getLongitude();
                getLocationName(lat, lng);
            }
        };


        return callback;
    }


    private void requestLocationPermission() {
        String[] requestPermissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(getActivity(), requestPermissions, KEY_REQUEST_PERMISSION);
    }

    private boolean isLocationPermissionGranted() {

        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void getLocationName(double lat, double lng) {


        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            // String fullAddress = addresses.get(0).getAddressLine(0);
            mAddressObj = addresses.get(0);
            createMarker(lat, lng);

        } catch (IOException e) {
            e.printStackTrace();
            incProgressView.setVisibility(View.GONE);
        }


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == KEY_REQUEST_PERMISSION) {

            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verifyGPSEnabled();
            } else {
                Toast.makeText(mContext, "Please give app permission!", Toast.LENGTH_SHORT).show();
            }

        }
    }


}