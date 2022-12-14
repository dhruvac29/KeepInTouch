package com.example.keepintouch.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;

import com.example.keepintouch.Model.MyLocation;
import com.example.keepintouch.Model.User;
import com.example.keepintouch.Model.Zone;
import com.example.keepintouch.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "map_act_tager";
    private GoogleMap mMap;
    private String currentGroupId;
    private FirebaseFirestore mFirebaseFirestore = FirebaseFirestore.getInstance();
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private CardView cardView;
    private EditText radiusTextView;
    private SeekBar seekBar;
    private Button setRadiusBtn;
    private Boolean flag = false;
    private Integer Radius = 0;
    public Double Latitude = 0.0;
    public Double Longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        cardView = findViewById(R.id.card_view);
        seekBar = findViewById(R.id.radius_seek_bar);
        radiusTextView = findViewById(R.id.radius_edit_text);
        setRadiusBtn = findViewById(R.id.set_radius_btn);
        if (savedInstanceState == null) {
            Bundle extra = getIntent().getExtras();
            if (extra == null) {
                currentGroupId = null;
                System.out.println("didn't get current group id");
            } else {
                currentGroupId = extra.getString("currentGroupId");
                flag = extra.getBoolean("flag");

            }
        } else {
            currentGroupId = (String) savedInstanceState.getSerializable("currentGroupId");
            flag = savedInstanceState.getBoolean("flag");

        }
        //Log.d(TAG,flag+"");
        if (flag == true) {
//            Log.d(TAG,"Can Set Radius! ");
            Toast.makeText(this, "Can Set Radius! ", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "Can Not Set Radius! ", Toast.LENGTH_SHORT).show();
            //  Log.d(TAG,"Can Not Set Radius! ");
            cardView.setVisibility(View.GONE);
            cardView.setMinimumHeight(0);
            cardView.setMinimumWidth(0);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //radiusTextView.setText(String.valueOf(val[0]) +"", TextView.BufferType.EDITABLE);
            }
        });
        radiusTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                seekBar.setProgress(i * 10);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                seekBar.setProgress(i * 10);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        setRadiusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRadiusLatiLong();

            }

        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void setRadiusLatiLong() {
        Radius = Integer.parseInt(radiusTextView.getText().toString());
        String AdminId = firebaseAuth.getCurrentUser().getUid();
        final User[] Admin = {null};
        String lati, longi;
        mFirebaseFirestore.collection("Users").document(AdminId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Admin[0] = task.getResult().toObject(User.class);
                String lati = Admin[0].getLatitude();
                String longi = Admin[0].getLogitude();
                mFirebaseFirestore.collection("Zone").document(currentGroupId).update("radius", Radius.toString(), "latitude", lati, "longitude", longi);
                mFirebaseFirestore.collection("Groups").document(currentGroupId).update("radius", Radius.toString());
                Latitude = Double.parseDouble(lati);
                Longitude = Double.parseDouble(longi);
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(Double.parseDouble(lati), Double.parseDouble(longi)))
                        .radius(Radius)
                        .strokeColor(Color.RED)
                        .fillColor(Color.BLUE));
                Log.d(TAG, "Circle Drawan!!");
            }
        });


        Toast.makeText(MapsActivity.this, "Radius Set : " + Radius, Toast.LENGTH_LONG).show();
    }

    private void DrawCircle(String lati, String longi, Integer radius) {

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Map<String, String> IdName = new HashMap<>();
        // Log.d(TAG,firebaseAuth.getCurrentUser().getDisplayName()+"||");
        mFirebaseFirestore.collection("Zone").document(currentGroupId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                IdName.clear();
                Zone zone = value.toObject(Zone.class);
                Latitude = Double.parseDouble(zone.getLatitude());
                Longitude = Double.parseDouble(zone.getLongitude());
                Radius = Integer.parseInt(zone.getRadius());
                ArrayList<String> mIdList = zone.getMemberList();
                mFirebaseFirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<User> uList = task.getResult().toObjects(User.class);
                            for (User u : uList) {
                                if (mIdList.contains(u.getUserId())) {
                                    IdName.put(u.getUserId(), u.getName());
                                }
                            }
                        }

                        // Add a marker in Sydney and move the camera
                        ArrayList<MyLocation> markerList = new ArrayList<MyLocation>();
                        mFirebaseFirestore.collection("Zone").document(currentGroupId).collection("memberList").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot task, @Nullable FirebaseFirestoreException error) {

                            //}
                        //})
                      ///  mFirebaseFirestore.collection("Zone").document(currentGroupId).collection("memberList").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                         //   @Override
                         //   public void onComplete(@NonNull Task<QuerySnapshot> task) {
                               // if (task.isSuccessful()) {

                                    List<DocumentSnapshot> dlist = task.getDocuments();//getResult()
                                    markerList.clear();
                                    for (DocumentSnapshot d : dlist) {
                                        markerList.add(d.toObject(MyLocation.class));
                                    }
                                    mMap.clear();

                                    Circle circle = mMap.addCircle(new CircleOptions()
                                            .center(new LatLng(Latitude, Longitude))
                                            .radius(Radius)
                                            .strokeColor(Color.BLUE)
                                            .fillColor(R.color.purple_Tr));
                                    for (MyLocation l : markerList) {

                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                                        String time = dateFormat.format(l.getTime());
                                        LatLng sydney = new LatLng(l.getLatitude(), l.getLongitude());
                                        String name = IdName.get(l.getUserId());
                                       //  Log.d(TAG,name + " map updated -------------------");

                                        mMap.addMarker(new MarkerOptions().position(sydney).title(name + " Last Updated:" + time));
                                    }
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(markerList.get(0).getLatitude(),markerList.get(0).getLongitude())));

                              //  }
                            }
                        });
                    }
                });
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Latitude, Longitude)));
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-34, 151)));
    }
}