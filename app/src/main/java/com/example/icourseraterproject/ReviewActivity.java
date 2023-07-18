package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class ReviewActivity extends AppCompatActivity {

    String departmentName;
    String courseCode;
    String reviewDescription;
    String reviewName;
    String reviewRating;
    String reviewImage;
    String reviewTime;
    TextView tv_thisreviewname,tv_thisreviewdescription,tv_thisreviewrating,tv_thisreviewntime;
    ImageView img_thisreviewimg;
    String reviewEmail;
    FirebaseUser user;
    FirebaseFirestore db; //TS member variable
    CollectionReference Users;
    CollectionReference Departments;
    CollectionReference Reviews;
    RatingBar rb_myreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        reviewEmail=getIntent().getStringExtra("reviewEmail");
        reviewDescription=getIntent().getStringExtra("reviewDescription");
        reviewName=getIntent().getStringExtra("reviewName");
        reviewRating=getIntent().getStringExtra("reviewRating");
        reviewImage=getIntent().getStringExtra("reviewImage");
        reviewTime=getIntent().getStringExtra("reviewTime");
        courseCode=getIntent().getStringExtra("courseCode");
        departmentName=getIntent().getStringExtra("departmentName");
        img_thisreviewimg=findViewById(R.id.img_thisreviewimg);
        tv_thisreviewdescription=findViewById(R.id.tv_thisreviewdescription);
        tv_thisreviewname=findViewById(R.id.tv_thisreviewname);
        tv_thisreviewrating=findViewById(R.id.tv_thisreviewrating);
        tv_thisreviewntime=findViewById(R.id.tv_thisreviewtime);
        rb_myreview=findViewById(R.id.rbar_myreview);
        rb_myreview.setRating(Float. parseFloat(reviewRating));
        // Use a library like Picasso or Glide to load the image from the URL
        Picasso.get().load(reviewImage).into(img_thisreviewimg);
        tv_thisreviewname.setText(reviewName);
        tv_thisreviewdescription.setText(reviewDescription);
        tv_thisreviewrating.setText("Rating: "+reviewRating);
        tv_thisreviewntime.setText(reviewTime);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance(); //TS in onCreate
        Reviews= db.collection("Departments").document(departmentName).
                collection("Courses").document(courseCode).
                collection("Reviews");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reviewmenu, menu);
        MenuItem delete = menu.findItem(R.id.delete_review);
        if(user.getEmail().equals(reviewEmail))
        {
            delete.setVisible(true);
            return true;
        }
        delete.setVisible(false);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        int itemId = item.getItemId();
        if (itemId == R.id.delete_review) {
            showConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation");
        builder.setMessage("Are you sure you want to delete this Review:?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform the deletion here
                db.collection("Departments").document(departmentName).collection("Courses").document(courseCode)
                        .collection("Reviews").document(reviewTime+"| "+reviewEmail)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("CMP345:", "Document BJ-City successfully deleted!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("CMP354:", "Error deleting document", e);
                            }
                        });

                db.collection("Users").document(reviewEmail)
                        .collection("myReviews").document(reviewTime +courseCode)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("CMP345:", "Document BJ-City successfully deleted!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("CMP354:", "Error deleting document", e);
                            }
                        });
                finish();
            }
        });

        builder.setNegativeButton("No", null);
        builder.create().show();
    }
}