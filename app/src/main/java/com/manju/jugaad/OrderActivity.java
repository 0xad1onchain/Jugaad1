package com.manju.jugaad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OrderActivity extends AppCompatActivity {

    private RadioGroup mop, deliveryTime, orderDetails;
    private Button placeOrder;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private FirebaseUser user;
    public UserData userData;
    public OrderData orderData;
    public EditText quantity, time;
    public String uid = "NA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        auth = FirebaseAuth.getInstance();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(OrderActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
        user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user.getUid();
        orderData = new OrderData();
        orderData.uid = uid;

        final DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference().child("Orders");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(UserData.class);
                orderData.address = userData.getZipcode();
                orderData.name = userData.fname + " " + userData.lname;
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mop = (RadioGroup) findViewById(R.id.mop);
        deliveryTime = (RadioGroup) findViewById(R.id.delivery_time);
        orderDetails = (RadioGroup) findViewById(R.id.pack_select);
        placeOrder = (Button) findViewById(R.id.place_order);
        quantity = (EditText) findViewById(R.id.quantityText);
        time = (EditText) findViewById(R.id.timeText);

        placeOrder.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {

                String QuantityString = quantity.getText().toString();
                if (TextUtils.isEmpty(QuantityString)) {
                    Toast.makeText(getApplicationContext(), "Enter Quantity!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mop.getCheckedRadioButtonId() == -1)
                {
                    Toast.makeText(getApplicationContext(), "Select Payment", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (orderDetails.getCheckedRadioButtonId() == -1)
                {
                    Toast.makeText(getApplicationContext(), "Select Can / Bottle", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (deliveryTime.getCheckedRadioButtonId() == -1)
                {
                    Toast.makeText(getApplicationContext(), "Select Delivery Schedule", Toast.LENGTH_SHORT).show();
                    return;
                }

                if( mop.getCheckedRadioButtonId() == R.id.cod )
                    orderData.paymentMode = "Cash On Delivery";
                if( mop.getCheckedRadioButtonId() == R.id.paytm )
                    orderData.paymentMode = "PayTM";

                if( orderDetails.getCheckedRadioButtonId() == R.id.Can )
                    orderData.packing = "Can";
                if( orderDetails.getCheckedRadioButtonId() == R.id.Bottle)
                    orderData.packing = "Bottle";

                if(deliveryTime.getCheckedRadioButtonId() == R.id.now) {
                    orderData.deliverySchedule = "Now";
                    orderData.deliveryTime = "NA";
                }
                if(deliveryTime.getCheckedRadioButtonId() == R.id.later) {
                    orderData.deliverySchedule = "Later";
                    String timeString = time.getText().toString();
                    if (TextUtils.isEmpty(timeString)) {
                        Toast.makeText(getApplicationContext(), "Enter Time!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    orderData.deliveryTime = time.getText().toString();
                }

                orderData.quantity = quantity.getText().toString();
                orderData.number = uid;

                orderRef.push().setValue(orderData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Order Placed", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(OrderActivity.this, MainActivity.class));
                    }
                });

            }
        });

    }
}
