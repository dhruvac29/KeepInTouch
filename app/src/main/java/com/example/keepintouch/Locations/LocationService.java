package com.example.keepintouch.Locations;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.keepintouch.MainActivity;
import com.example.keepintouch.Model.GroupItem;
import com.example.keepintouch.Model.MyLocation;
import com.example.keepintouch.Model.Zone;
import com.example.keepintouch.Model.sUser;
import com.example.keepintouch.Notification.ApiInterface;
import com.example.keepintouch.Notification.Client;
import com.example.keepintouch.Notification.Data;
import com.example.keepintouch.Notification.MyNotification;
import com.example.keepintouch.Notification.NotificationHelper;
import com.example.keepintouch.Notification.NotificationSender;
import com.example.keepintouch.R;
import com.example.keepintouch.ui.GroupsFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.keepintouch.Locations.App.LOCATION_SERVICE_CHANNEL_ID;

public class LocationService extends Service {
    private FirebaseFirestore mFirebaseFirestore = FirebaseFirestore.getInstance();
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private static final String TAG = "lct_src_tager";
    private FusedLocationProviderClient mLocationClient;
    private LocationCallback mLocationCallback;
    private ApiInterface apiService;
    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private ArrayList<String> groupIdList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        apiService = Client.getClient("https://fcm.googleapis.com/").create(ApiInterface.class);///////////

        getcodes();

        mLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                List<Location> locationList = locationResult.getLocations();

//                for (Location l : locationList) {
                Location l=locationList.get(0);
                    String lati = Double.toString(l.getLatitude());
                    String longi = Double.toString(l.getLongitude());
                    Date date = new Date(l.getTime());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                    String time = dateFormat.format(date);
                    Log.d(TAG, l.toString());
                    mFirebaseFirestore.collection("Users").document(mFirebaseAuth.getCurrentUser().getUid()).update("latitude", lati, "logitude", longi, "time", time);
                    String cid = GroupsFragment.getGroupsActivityInstance().getCurrentgroupid();


                    for (String gid : groupIdList) {
                        mFirebaseFirestore.collection("Zone").document(gid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    Zone currentzone = task.getResult().toObject(Zone.class);

                                    mFirebaseFirestore.collection("Zone").document(gid).collection("memberList").document(mFirebaseAuth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                 MyLocation prevMyLocation = task.getResult().toObject(MyLocation.class);
                                                boolean prevFlag = prevMyLocation.isSafe();
                                                boolean cureFlag = calculateDistance(currentzone,l,prevFlag);
                                                Log.d(TAG,prevFlag + "_---"+cureFlag);
                                                MyLocation myLocation = new MyLocation(mFirebaseAuth.getCurrentUser().getUid(), l, cureFlag);
                                                mFirebaseFirestore.collection("Zone").document(gid).collection("memberList").document(mFirebaseAuth.getCurrentUser().getUid()).set(myLocation);
                                                Log.d(TAG, "new Change success");
                                            }
                                        }
                                    });

                                }
                            }
                        });
                    }
                    //================


//                    if (cid != null) {
//                        final boolean[] isSafe = {true};
//                        final Zone[] zone = new Zone[1];
//                        mFirebaseFirestore.collection("Zone").whereEqualTo("groupId", cid).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                if (task.isSuccessful()) {
//                                    List<DocumentSnapshot> documentSnapshot = task.getResult().getDocuments();
//                                    zone[0] = documentSnapshot.get(0).toObject(Zone.class);
//
//                                    isSafe[0] = calculateDistance(zone[0], l,true);
//                                    Log.d(TAG, zone[0].toString() + " " + isSafe);
//                                    MyLocation myLocation = new MyLocation(mFirebaseAuth.getCurrentUser().getUid(), l, isSafe[0]);
//                                    mFirebaseFirestore.collection("Zone").document(cid).collection("memberList").document(mFirebaseAuth.getCurrentUser().getUid()).set(myLocation);
//                                    Log.d(TAG, "new Change success");
//                                } else {
//                                    Log.d(TAG, "task unSucseess");
//                                }
//                            }
//                        });
//
//
//                    }


            }
        };
        // Log.d(TAG, "OnCreate:location");

    }

    private void getcodes() {
        mFirebaseFirestore.collection("Group'sCode").document(mFirebaseAuth.getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                HashMap<String, ArrayList<String>> maps = (HashMap<String, ArrayList<String>>) value.toObject(Object.class);
                ArrayList<String> codes = maps.get("CodeList");
                //Log.d(TAG,codes.toString());
                groupIdList.clear();
                for (String code : codes) {
                    mFirebaseFirestore.collection("Groups").whereEqualTo("code", code).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            List<DocumentSnapshot> document = task.getResult().getDocuments();
                            Log.d(TAG,"////////////////////////"+document.toString());

                            for (DocumentSnapshot d : document) {
                                groupIdList.add(d.toObject(GroupItem.class).getGroupId());
                                Log.d(TAG, d.toObject(GroupItem.class).getGroupId());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,
                notificationIntent,
                0);
        Notification notification = new NotificationCompat.Builder(this, LOCATION_SERVICE_CHANNEL_ID)
                .setContentTitle("Keep IN Touch")
                .setContentText("Location Service")
                .setSmallIcon(R.drawable.ic_android_official)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        getLocatoinUpdates();
        return START_STICKY;

    }

    private void getLocatoinUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(4000);
        locationRequest.setMaxWaitTime(15 * 1000);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Location Pemission Rqquired!!", Toast.LENGTH_SHORT).show();
            return;

        }
        mLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());


    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //  Log.d(TAG, "onDestroy");
        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean calculateDistance(Zone zone, Location location,boolean prevFlag) {
        boolean curFlag;

        if (Double.parseDouble(zone.getRadius()) == 0)
        {return true;}

        double lat1 = Double.parseDouble(zone.getLatitude());
        double lat2 = location.getLatitude();
        double lon1 = Double.parseDouble(zone.getLongitude());
        double lon2 = location.getLongitude();
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        Double result = (c * r * 1000);
        if (result <= Double.parseDouble(zone.getRadius())) {
            curFlag = true;
        }
        else  curFlag = false;
//        sendNotification("fims-qcNRDWV8wk1CuKKRz:APA91bGPHPNvh0I9-9DKKyVWWtPSUPvjlkiDqSMVzPVW66KrOUgShepFONSrhKLHINS6ETJqzF18d9BJNhIwQB733i348pysvnSDqun4G2AB6wfT2dn8A0udBh11HFTgdyNOSywBb_KA", "raja", "gajera");


        if(prevFlag==true && curFlag==false)
        {
            String groupName = zone.getGroupName();
            String adminId = zone.getAdminId();
            String currnetUserId = mFirebaseAuth.getCurrentUser().getUid();

             mFirebaseDatabase.getReference("Users").child(adminId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if(task.isSuccessful())
                    {
                        sUser s = task.getResult().getValue(sUser.class);

                        sendNotification(s.getToken(),groupName,"Your Group Member is Out Of Zone!!");
                        NotificationHelper.displayNotification(getApplicationContext(),"KeepInTouch",groupName+": You are Out Of Zone!!");
                    }
                }
            });

        }
        return  curFlag;

    }


    public void sendNotification(String userToken, String title, String message) {

        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(userToken, data, data);

        apiService.sendNotification(sender).enqueue(new Callback<MyNotification>() {
            @Override
            public void onResponse(Call<MyNotification> call, Response<MyNotification> response) {
                if (response.code() == 200)
                    if (response.body().success != 1) {
                        Toast.makeText(getApplicationContext(), "Doesn't send", Toast.LENGTH_SHORT).show();
                    }
                Log.d(TAG, response.code() + "/////////////////////////" + response.toString());

            }

            @Override
            public void onFailure(Call<MyNotification> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "failed Doesn't send", Toast.LENGTH_SHORT).show();

            }
        });

    }
}
