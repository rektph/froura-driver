package com.froura.develo4.driver.registration;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.froura.develo4.driver.LandingActivity;
import com.froura.develo4.driver.R;
import com.froura.develo4.driver.config.TaskConfig;
import com.froura.develo4.driver.utils.SuperTask;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class PhoneAuthentication extends AppCompatActivity implements SuperTask.TaskListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private ProgressDialog progressDialog;
    private TextView mobnum_txt_vw;
    private Button edit_btn;
    private TextInputEditText verification_code_et;
    private Button verifcation_btn;
    private TextView cntdwn_txt_vw;
    private Button resend_btn;

    private String mobnum;
    private String email;
    private String name;
    private String profpic = "default";
    private String database_id = "null";
    private String auth = "mobile";
    private boolean phoneReg;
    private CountDownTimer requestCodeTimer;

    private String TAG = "PhoneAuth";
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authentication);
        cntdwn_txt_vw = findViewById(R.id.cntdwn_txt_vw);
        verifcation_btn = findViewById(R.id.verifcation_btn);
        verification_code_et = findViewById(R.id.verification_code_et);
        mobnum_txt_vw = findViewById(R.id.mobnum_txt_vw);
        resend_btn = findViewById(R.id.resend_btn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Logging in with Mobile...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        mobnum = getIntent().getStringExtra("mobnum");
        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");
        phoneReg = getIntent().getBooleanExtra("phoneReg", false);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() != null) {
                    registerUser();
                    return;
                }
            }
        };

        verifcation_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mCode = verification_code_et.getText().toString();
                if(!mCode.isEmpty()) {
                    progressDialog.show();
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, mCode));
                    verification_code_et.setError(null);
                    verification_code_et.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
                } else {
                    verification_code_et.setError("Code is required.", getResources().getDrawable(R.drawable.ic_warning_red_24dp));
                }

            }
        });

        requestCodeTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                cntdwn_txt_vw.setText("Request a new code in 00:"+ (l < 10000 ? "0" + l/1000 : l/1000));
            }

            @Override
            public void onFinish() {
                cntdwn_txt_vw.setVisibility(View.GONE);
                resend_btn.setVisibility(View.VISIBLE);
            }
        };

        resend_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCode();
                requestCodeTimer.start();
                cntdwn_txt_vw.setVisibility(View.VISIBLE);
                resend_btn.setVisibility(View.GONE);
            }
        });

        if(mobnum.matches("^(09)\\d{9}$")) {
            mobnum_txt_vw.setText("+63 " + mobnum.substring(1));
            mobnum = "+63" + mobnum.substring(1);
        } else if(mobnum.matches("^(\\+639)\\d{9}$")) {
            mobnum_txt_vw.setText(mobnum = "+63 " + mobnum.substring(3));
            mobnum = "+63" + mobnum.substring(3);
        }

        if(phoneReg) {
            requestCode();
            requestCodeTimer.start();
            phoneReg = false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(PhoneAuthentication.this, PhoneRegistration.class);
        startActivity(intent);
        finish();
    }

    private void saveUserDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String JSON_DETAILS_KEY = "userDetails";
        String jsonDetails = "{\"name\" : \"" + WordUtils.capitalize(name.toLowerCase()) + "\", " +
                "\"email\" : \"" + email + "\", " +
                "\"mobnum\" : \"" + mobnum + "\", " +
                "\"profile_pic\" : \"" + profpic + "\", " +
                "\"auth\" : \"" + auth + "\", " +
                "\"database_id\": \""+ database_id +"\"}";
        editor.putString(JSON_DETAILS_KEY, jsonDetails);
        editor.apply();
        progressDialog.dismiss();
        Intent intent = new Intent(PhoneAuthentication.this, LandingActivity.class);
        startActivity(intent);
        finish();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) { }
                    }
                });
    }

    private void requestCode() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobnum,
                60,
                TimeUnit.SECONDS,
                PhoneAuthentication.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        progressDialog.show();
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed", e);
                        if (e instanceof FirebaseTooManyRequestsException) {
                            Toast.makeText(PhoneAuthentication.this, "Server Overload! A request has been sent.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        mVerificationId = s;
                    }
                });
    }

    private void registerUser() {
        final String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userFound = false;
                for(DataSnapshot users: dataSnapshot.getChildren()) {
                    if(users.getKey().equals("passenger")) {
                        for(DataSnapshot passenger : users.getChildren()) {
                            if(user_id.equals(passenger.getKey())) {
                                userFound = true;
                                progressDialog.dismiss();
                                mAuth.signOut();
                                Intent intent = new Intent(PhoneAuthentication.this, LoginActivity.class);
                                intent.putExtra("loginError", 1);
                                startActivity(intent);
                                finish();
                                break;
                            }
                        }
                    } else if(users.getKey().equals("driver")){
                        for(DataSnapshot driver : users.getChildren()) {
                            if(user_id.equals(driver.getKey())) {
                                userFound = true;
                                SuperTask.execute(PhoneAuthentication.this,
                                        TaskConfig.GET_DRIVER_DATA_URL,
                                        "get_driver_data");
                                break;
                            }
                        }
                    }
                    if(userFound) break;
                }
                if(!userFound) {
                    progressDialog.dismiss();
                    mAuth.signOut();
                    Intent intent = new Intent(PhoneAuthentication.this, LoginActivity.class);
                    intent.putExtra("loginError", 1);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
    public void onTaskRespond(String json, String id) {
        switch (id) {
            case "get_driver_data":
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String status = jsonObject.getString("status");
                    if(status.equals("success")) {
                        String user_id = "";
                        String plate = jsonObject.getString("plate");
                        database_id = jsonObject.getString("database_id");
                        email = jsonObject.getString("email");
                        name = jsonObject.getString("name");
                        mobnum = jsonObject.getString("contact");
                        auth = "mobile";
                        profpic = jsonObject.getString("img_path");
                        user_id = jsonObject.getString("uid");

                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(user_id);
                        dbRef.child("name").setValue(WordUtils.capitalize(name.toLowerCase()));
                        dbRef.child("email").setValue(email);
                        dbRef.child("mobnum").setValue(mobnum);
                        dbRef.child("auth").setValue(auth);
                        dbRef.child("profile_pic").setValue(profpic);
                        dbRef.child("plate").setValue(plate);

                        saveUserDetails();
                    } else {
                        progressDialog.dismiss();
                        mAuth.signOut();
                        Intent intent = new Intent(PhoneAuthentication.this, LoginActivity.class);
                        intent.putExtra("loginError", 1);
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) { }
                break;
        }
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues, String id) {
        switch (id) {
            case "get_driver_data":
                contentValues.put("android", 1);
                contentValues.put("firebase_uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                return contentValues;
            default:
                return null;
        }
    }
}
