package com.amupys.maps_and_more;

import static com.amupys.maps_and_more.MapsAndMore.MEDIUM_ACCIDENT;
import static com.amupys.maps_and_more.MapsAndMore.NUM_ACC;
import static com.amupys.maps_and_more.MapsAndMore.places;
import static com.amupys.maps_and_more.MapsAndMore.small_icon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;

public class ServiceLocation extends Service {
    public static final int MIN_DISTANCE = 10;
    public static final int MIN_RESPONSE_DISTANCE = 30;
    IBinder mBinder = new MyBinder();
    private LatLng latLng;
    private boolean running = false;
    private LocationManager locationManager;
    private Activity activity;
    private LinearLayout view;

    public void configure(Activity activity, LinearLayout v){
        this.activity = activity;
        view = v;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

        locationManager=(LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                2000,
                10, locationListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            latLng = new LatLng(intent.getExtras().getDouble("Lat", 0),intent.getExtras().getDouble("Long", 0));
        }

        running = true;
        Log.e("Service", "Service started");

        startMyOwnForeground("Maps", "App is running");
        //do heavy work on a background thread

        return START_STICKY;
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            LatLng latLng1 = latLng, latLng2;
            latLng2 = new LatLng(location.getLatitude(), location.getLongitude());
            latLng = latLng2;
            trackUserLocation(latLng1, latLng2);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        ServiceLocation getService(){
            return ServiceLocation.this;
        }
    }

    public void trackUserLocation(LatLng latLng1, LatLng latLng2){
        if(latLng1 != null && latLng2 != null && distance(latLng1, latLng2) > MIN_DISTANCE ){
            LatLng latLng3;
            for(LocationItem l : places){
                latLng3 = new LatLng(l.getLatitude(), l.getLongitude());
                if(distance(latLng2, latLng3) < MIN_RESPONSE_DISTANCE){
                    try {
                        createToast(l, view);
                    }catch (Exception e){
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    try {
//                        MainActivity.getInstance().moveCamera(latLng3, 15f, l.getName());
                    }catch (Exception r){
                        Toast.makeText(this, r.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startMyOwnForeground(l.getName(), l.getDescription());
                    }
                }
            }
        }
    }

    private void createToast(LocationItem location, final LinearLayout view){
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, activity.<ViewGroup>findViewById(R.id.toast_lay_root));

        if(location.getNumAcc() >= NUM_ACC){
            layout.setBackgroundColor(activity.getResources().getColor(R.color.red));
        }else if(location.getNumAcc() < NUM_ACC && location.getNumAcc() > MEDIUM_ACCIDENT)
            layout.setBackgroundColor(activity.getResources().getColor(R.color.yellow));
        TextView text1 = (TextView) layout.findViewById(R.id.toast_loc);
        text1.setText(location.getName());
        TextView text = (TextView) layout.findViewById(R.id.toast_des);
        text.setText(location.getDescription());
        TextView speed = layout.findViewById(R.id.txt_speed), severe = layout.findViewById(R.id.txt_severity);
        speed.setText("Speed: "+location.getSpeedLim()+" Km/hr");
        int num = location.getNumAcc();
        if(num>50)
            severe.setText("High");
        else
            severe.setText("Moderate");


//        Toast toast = new Toast(getApplicationContext());
//        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//        toast.setDuration(Toast.LENGTH_LONG);
//        toast.setView(layout);
//        toast.show();

        view.removeAllViews();
        view.addView(layout);
        view.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.INVISIBLE);
            }
        }, 30000);
    }

    boolean isRunning(){
        return running;
    }

    void stopService(){
        running = false;
        stopForeground(true);
        stopSelf();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(String location, String message){
        Log.e("Notification", "done");
        Intent notificationIntent = new Intent(this, activity.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(false)
                .setSmallIcon(small_icon)
                .setContentTitle(location)
                .setContentText(message)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent);
        startForeground(1, notificationBuilder.build());
    }

    public static double distance(LatLng lat1, LatLng lat2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2.latitude
                - lat1.latitude);
        double lonDistance = Math.toRadians(lat2.longitude - lat1.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.latitude)) * Math.cos(Math.toRadians(lat2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = 0.0;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
