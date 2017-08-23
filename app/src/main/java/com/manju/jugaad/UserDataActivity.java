package com.manju.jugaad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class UserDataActivity extends AppCompatActivity {

    private FirebaseUser user;
    private String uid;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private EditText fname, lname, email, city, address;
    private Button Submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        fname = (EditText) findViewById(R.id.firstName);
        lname = (EditText) findViewById(R.id.lastName);
        email = (EditText) findViewById(R.id.email);
        city = (EditText) findViewById(R.id.city);
        address = (EditText) findViewById(R.id.address);
        Submit = (Button) findViewById(R.id.submit_userdetails);

        mAuth = FirebaseAuth.getInstance();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {
                    startActivity(new Intent(UserDataActivity.this, LoginActivity.class));
                }
            }
        };

        user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user.getUid();
        Log.d("UID", ""+uid);
        DatabaseReference userRef = rootRef.child(uid);
        userRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                startActivity(new Intent(UserDataActivity.this, MainActivity.class));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                UserData value = dataSnapshot.getValue(UserData.class);

                Log.d("Data", "recieved");
                Log.d("city", ""+value.city);
                if(value == null){

                }
                else
                {
                    startActivity(new Intent(UserDataActivity.this, MainActivity.class));
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName, lastName, cityString, emailString, addressString;
                emailString = email.getText().toString();
                firstName = fname.getText().toString();
                lastName = lname.getText().toString();
                cityString = city.getText().toString();
                addressString = address.getText().toString();


                if (TextUtils.isEmpty(emailString)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(firstName)) {
                    Toast.makeText(getApplicationContext(), "Enter First Name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(lastName)) {
                    Toast.makeText(getApplicationContext(), "Enter Last Name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(cityString)) {
                    Toast.makeText(getApplicationContext(), "Enter City Location!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(addressString)) {
                    Toast.makeText(getApplicationContext(), "Enter City Location!", Toast.LENGTH_SHORT).show();
                    return;
                }


                UserData userData = new UserData();
                userData.city = cityString;
                userData.email = emailString;
                userData.fname = firstName;
                userData.lname = lastName;
                userData.address = addressString;
                userData.uid = uid;
                DatabaseReference mRef = FirebaseDatabase.getInstance().getReference().child("Users");
                mRef.child(uid).setValue(userData);
                startActivity(new Intent(UserDataActivity.this, MainActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }
}
