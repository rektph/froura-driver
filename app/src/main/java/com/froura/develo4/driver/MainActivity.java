package com.froura.develo4.driver;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.froura.develo4.driver.libraries.DialogCreator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements DialogCreator.DialogActionListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.loader);

        Glide.with(this).load(getImage("loader")).into(imageView);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user == null) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this, LandingActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }, 10);
                } else {
                    if(!haveNetworkConnection()) {
                        DialogCreator.create(MainActivity.this, "internetDisabled")
                                .setTitle("No Internet Connection")
                                .setMessage("This application needs internet connection.")
                                .setPositiveButton("EXIT")
                                .setCancelable(false)
                                .show();
                        return;
                    }
                    final Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    DatabaseReference mPassengerDB = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    mPassengerDB.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                            intent.putExtra("name", map.get("name").toString());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });

                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public int getImage(String imageName) {
        int drawableResourceId = this.getResources()
                .getIdentifier(imageName, "drawable", this.getPackageName());

        return drawableResourceId;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        switch (actionId) {
            case "internetDisabled":
                finish();
                break;
        }
    }

    @Override
    public void onClickNegativeButton(String actionId) {

    }

    @Override
    public void onClickNeutralButton(String actionId) {

    }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) {

    }

    @Override
    public void onCreateDialogView(String actionId, View view) {

    }
}