package com.amupys.maps_and_more;

import static android.content.Context.BIND_AUTO_CREATE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsAndMore {

    public static final int NUM_ACC = 20;
    public static final int MEDIUM_ACCIDENT = 15;

    private LocationManager locationManager;
    private Activity activity;
    public static int small_icon;
    private ServiceLocation serviceLocation;
    public static ArrayList<LocationItem> places, userArray;

    private boolean mLocationPermission = false;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng latLng;
    private LinearLayout view;

    private final String[] url = {"https://us-central1-roadsafety-d90af.cloudfunctions.net/app/api/read",
            "https://us-central1-roadsafety-d90af.cloudfunctions.net/app/useraddtoapi/read"};

    public MapsAndMore(Activity a, int icon, LinearLayout v){
        activity = a;
        small_icon = icon;
        view = v;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if(hasInternetConnection()){
            initList();
        }else
            Toast.makeText(activity, "Oops! No Internet Connection", Toast.LENGTH_SHORT).show();
    }

//    to be called from onResume
    public void configure(){
        Intent intent = new Intent(activity, ServiceLocation.class);
        activity.bindService(intent, (ServiceConnection) activity, BIND_AUTO_CREATE);

//        getLocationPermission();
    }

//    called when the app gets location permission and from onRequestPermission
    public void passPermission(boolean bool){
        mLocationPermission = bool;
        if(bool){
            isLocationEnabled();
        }
    }

    public ArrayList<LocationItem> getPlaces(){
        if (places != null)
            return places;

        return null;
    }

    public boolean start(){
        if(mLocationPermission){
            if(!serviceLocation.isRunning()){
//                btnStart.setText("Stop service");
                Log.e("button", "starting");
                try {
                    Bundle bundle = new Bundle();
                    Intent intent = new Intent(activity, ServiceLocation.class);
                    bundle.putDouble("Lat", latLng.latitude);
                    bundle.putDouble("Long", latLng.longitude);
                    intent.putExtras(bundle);
                    activity.startService(intent);

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(activity, "Error setting location", Toast.LENGTH_SHORT).show();
                }
            }else {
                Log.e("button", "stopped");
//                btnStart.setText("Start");
                serviceLocation.stopService();
            }
        }
        return false;
    }

    public LatLng getLocation(Context context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        try {
            if (mLocationPermission) {
                Task<Location> location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Location location1 = (Location) task.getResult();
                            if (location1 != null) {
                                latLng = new  LatLng(location1.getLatitude(), location1.getLongitude());
//                                moveCamera(latLng, ZOOM, "My Location");
                                String string = String.valueOf(latLng.latitude+" "+latLng.longitude);
                                Log.e("Location", string);
                            } else {
                                Toast.makeText(activity, "Device Location is disabled", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
        }
        return latLng;
    }

    private boolean hasInternetConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
        } catch (Exception e) {
        }

        return haveConnectedWifi || haveConnectedMobile;
    }

    private void initList() {
        if(places != null && places.size() != 0){
        }else {
            places = new ArrayList<>();

            final ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("Please wait..");
            progressDialog.setCancelable(true);
            progressDialog.show();

            StringRequest request = new StringRequest(Request.Method.GET ,url[0], new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(response != null){
                        progressDialog.dismiss();
                        try{
                            JSONArray jsonArray = new JSONArray(response);
                            for(int i=0;i<jsonArray.length();i++){
                                JSONObject object = jsonArray.getJSONObject(i);
                                LocationItem location = new LocationItem(null, object.getString("name"), object.getString("description")
                                        , object.getDouble("lat"), object.getDouble("long"), object.getInt("Acc_per_year")
                                        , object.getInt("Speed_limit"));

                                places.add(location);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(activity, error.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            RequestQueue queue = Volley.newRequestQueue(activity);

            queue.add(request);
        }

        if(userArray != null && userArray.size() != 0){}else {
            userArray = new ArrayList<>();

            final ProgressDialog progressDialogU = new ProgressDialog(activity);
            progressDialogU.setMessage("Please wait..");
            progressDialogU.setCancelable(true);
            progressDialogU.show();

            StringRequest request = new StringRequest(Request.Method.GET ,url[1], new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(response != null){
                        progressDialogU.dismiss();
                        try{
                            JSONArray jsonArray = new JSONArray(response);
                            for(int i=0;i<jsonArray.length();i++){
                                JSONObject object = jsonArray.getJSONObject(i);
                                LocationItem location = new LocationItem(null, object.getString("name"), object.getString("description")
                                        , object.getDouble("lat"), object.getDouble("long"), object.getInt("Acc_per_year")
                                        , object.getInt("Speed_limit"));

                                location.setTimeOfAcc(object.getLong("time"));
                                userArray.add(location);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(activity, error.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            RequestQueue queue = Volley.newRequestQueue(activity);

            queue.add(request);
        }
    }

    private void isLocationEnabled() {
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder alertDialog=new AlertDialog.Builder(activity);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(intent);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            AlertDialog alert=alertDialog.create();
            alert.show();
        }else {
            getLocation(activity);
        }
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder){
        ServiceLocation.MyBinder myBinder = (ServiceLocation.MyBinder) iBinder;
        serviceLocation = myBinder.getService();
        serviceLocation.configure(activity, view);
    }

    public void onServiceDisconnected(ComponentName componentName) {
        serviceLocation = null;
    }
}
