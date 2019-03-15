package com.appbusso.mapsapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.constraint.Placeholder;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;



import com.appbusso.mapsapp.models.PlaceInfo;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import cameo.code.placeautocomplete.PlaceAutoCompleteFragment;
import cameo.code.placeautocomplete.PlaceModel;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, PlaceAutoCompleteFragment.onPlaceSelectedListener{

    private GoogleMap mMap;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    ArrayList<Points> points = new ArrayList<>();
    private DatabaseReference mDatabase;
    List<LatLng> list;
    private LatLng currentLatLng;
    private Location location;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private LocationManager locationManager;
    private LocationListener myLocationListener;
    private GoogleApiClient mGoogleApiClient;
    private FloatingSearchView mSearchView;

    private BottomSheetBehavior mBottomSheetBehavior;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    int[] colors = {
            Color.rgb(102, 225, 0), // green
            Color.rgb(255, 0, 0)    // red
    };

    float[] startPoints = {
            0.2f, 0.3f
    };

    private PlaceInfo mPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        LinearLayout llMain = (LinearLayout) findViewById(R.id.llbottomsheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(llMain);


        mBottomSheetBehavior.setPeekHeight(300);
        mBottomSheetBehavior.setHideable(false);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_DRAGGING);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    Toast.makeText(getApplicationContext(), "STATE_EXPANDED", Toast.LENGTH_LONG).show();
                    mMap.setPadding(0, 0, 0, bottomSheet.getHeight());
//                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(8.0f));
                 //   clusterMarker();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    Toast.makeText(getApplicationContext(), "STATE_COLLAPSED", Toast.LENGTH_LONG).show();
                    mMap.setPadding(0, 0, 0, 310);
//                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(10.0f));
                  //  clusterMarker();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Permission already Granted
            //Do your work here
            //Perform operations here only which requires permission
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        GPSTracker gpsTracker = new GPSTracker(this);
        if (gpsTracker.getIsGPSTrackingEnabled())
        {
            currentLng = gpsTracker.longitude;
            currentLat = gpsTracker.latitude;
            updateDatabase();
        }
        else
        {
            gpsTracker.showSettingsAlert();
        }

    }

    public void getAdapter(){

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();


        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS,null);
        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {

                //get suggestions based on newQuery

                //pass them on to the search view

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_in_night));
        checkLocation();
        // Setting a click event handler for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                //markerOptions.title(latLng.latitude + " : " + latLng.longitude);

                Geocoder geocoder=new Geocoder(getApplicationContext());

                try {
                    List<Address> addressList=geocoder.getFromLocation(latLng.latitude,latLng.longitude,10);

                    for(Address address:addressList){
                        Log.d("TAG",address.getAddressLine(0));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Clears the previously touched position
                mMap.clear();

                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);


            }
        });
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        list = null;
        getData();
    }


    public void getGPS(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling

            return;
        }
        mMap.setMyLocationEnabled(true);
        LatLng getCurrent = new LatLng(currentLat, currentLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(getCurrent));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mMap.setMyLocationEnabled(true);


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission Granted
                //Do your work here
                //Perform operations here only which requires permission
                checkLocation();
            }
        }
    }

    public void updateDatabase(){
        mDatabase.child(FirebaseAuth.getInstance().getUid())
                .child("lat")
                .setValue(currentLat);
        mDatabase.child(FirebaseAuth.getInstance().getUid())
                .child("lng")
                .setValue(currentLng);
    }

    public void checkLocation() {

        String serviceString = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) getSystemService(serviceString);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                updateDatabase();
                getGPS();
            }
        }

        myLocationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location locationListener) {

                if (isGPSEnabled(MapsActivity.this)) {
                    if (locationListener != null) {
                        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                currentLat = location.getLatitude();
                                currentLng = location.getLongitude();
                                updateDatabase();
                            }
                        }
                    }
                } else if (isInternetConnected(MapsActivity.this)) {
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLng = location.getLongitude();
                            updateDatabase();
                        }
                    }
                }
            }

            public void onProviderDisabled(String provider) {

            }

            public void onProviderEnabled(String provider) {

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, myLocationListener);
    }

    public static boolean isInternetConnected(Context ctx) {
        ConnectivityManager connectivityMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        // Check if wifi or mobile network is available or not. If any of them is
        // available or connected then it will return true, otherwise false;
        if (wifi != null) {
            if (wifi.isConnected()) {
                return true;
            }
        }
        if (mobile != null) {
            if (mobile.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void addHeatMap() {


        Gradient gradient = new Gradient(colors, startPoints);
// Create the tile provider.
        Log.d("los valores son: ", String.valueOf(list.toString()));
        mProvider = new HeatmapTileProvider.Builder()
                .data(list)
                //.gradient(gradient)
               // .opacity(100)
                .build();
        mProvider.setRadius(50);

// Add the tile overlay to the map.
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    private ArrayList<LatLng> readItems(int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }
        return list;
    }
    private void getData(){
        points.clear();
        mDatabase.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot sn: dataSnapshot.getChildren()){
                    Points p = new Points();
                    for (DataSnapshot postSnapshot: sn.getChildren()){
                      //  p.setLat(postSnapshot.getV);
                        if (postSnapshot.getKey().equals("lat")){
                            p.setLat(Double.parseDouble(postSnapshot.getValue().toString()));
                        }else if (postSnapshot.getKey().equals("lng")) {
                            p.setLng(Double.parseDouble(postSnapshot.getValue().toString()));
                        }
                    }
                    points.add(p);
                }
                Log.d("los valores son: ", String.valueOf(points.get(0).getLat()));

                try {
                    list = showData(points);
                    addHeatMap();
                    Log.d("los valores son: ", String.valueOf(list.get(0).latitude));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // adapterGet();
                Log.d("los valores son: ", String.valueOf(list.toString()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
//        Log.d("los valores son: ", String.valueOf(list.toString()));
    }
    private ArrayList<LatLng> showData(ArrayList<Points> p)throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        String json = new Gson().toJson(p);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }

        return list;
    }

    @Override
    public void onPlaceSelected(PlaceModel placeModel) {
        LatLng selectedPlace = new LatLng(placeModel.getLat(), placeModel.getLng());

        mMap.addMarker(new MarkerOptions().position(selectedPlace).title("Marker in "+placeModel.getMainPlace()));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(selectedPlace));
    }

    @Override
    public String getWebApiKey() {
        return getString(R.string.google_maps_key);
    }
}
