package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CourseActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    String departmentName;
    String courseName;
    String courseCode;
    String courseDescription;
    FirebaseUser user;
    FirebaseFirestore db; //TS member variable
    CollectionReference Users;
    CollectionReference Departments;
    CollectionReference Reviews;
    TextView tv_crsname,tv_crsdescription,tv_rating;
    Button btn_addreview,btn_viewall;
    ListView lv_review;
    ArrayList<HashMap<String, String>> data;
    String userName;
    String imageURL;
    SimpleAdapter adapter;
    LinearLayout ll_title,ll_description,ll_rating;
    float rating=0;
    int count =0;
    RatingBar rbar_course;
    boolean isAdmin;
    Timer reviewtimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance(); //TS in onCreate
        Users= db.collection("Users").document(user.getEmail()).collection("myReviews");
        Departments=db.collection("Departments");
        rbar_course=findViewById(R.id.rbar_course);
        ll_title=findViewById(R.id.ll_title);
        ll_description=findViewById(R.id.ll_description);
        ll_rating=findViewById(R.id.ll_rating);
        tv_crsname=findViewById(R.id.tv_crsname);
        lv_review=findViewById(R.id.lv_review);
        tv_crsdescription=findViewById(R.id.tv_crsdescription);
        tv_rating=findViewById(R.id.tv_rating);
        btn_addreview=findViewById(R.id.btn_addreview);
        btn_viewall=findViewById(R.id.btn_viewall);
        btn_addreview.setOnClickListener(this);
        btn_viewall.setOnClickListener(this);
        courseName=getIntent().getStringExtra("courseName");
        courseCode=getIntent().getStringExtra("courseCode");
        courseDescription=getIntent().getStringExtra("courseDescription");
        departmentName = getIntent().getStringExtra("departmentName");
        tv_crsname.setText(courseCode+"-"+courseName);
        tv_crsdescription.setText(courseDescription);
        Reviews = Departments.document(departmentName).collection("Courses")
                .document(courseCode).collection("Reviews");
        lv_review.setOnItemClickListener(this);
        isAdmin=getIntent().getBooleanExtra("isAdmin",isAdmin);
        if (isAdmin)
        {
            btn_addreview.setVisibility(View.GONE);
        }
        else {
            btn_addreview.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAndDisplay();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.admin_menu, menu);
        MenuItem add = menu.findItem(R.id.item_add);
        MenuItem delete = menu.findItem(R.id.item_delete);
        add.setVisible(false);
        delete.setVisible(false);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if(itemId==R.id.item_logout)
        {
            showLogoutDialog();
        }
        else if(itemId==R.id.item_refresh)
        {
            updateAndDisplay();
        }
        else if(itemId==R.id.item_notfication)
        {
            DocumentReference docRef = db.collection("Users").document(user.getEmail())
                    .collection("ReviewsNotfications").document(courseCode);
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if(documentSnapshot.getBoolean("reviewnotfication")!=null)
                    {
                        boolean isTrue=documentSnapshot.getBoolean("reviewnotfication");
                        showNotficationDialog(isTrue);
                    }
                    else {
                        Map<String, Object> notficationdata = new HashMap<>();
                        notficationdata.put("reviewnotfication", false);
                        docRef.set(notficationdata); //”LA” is called the document path (think of it as a primary key)
                        showNotficationDialog(false);
                    }
                }
            });
        }
        else if(itemId==R.id.item_about1)
        {
            startActivity(new Intent(this, AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public void showNotficationDialog(boolean isTrue) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notfication");
        if(isTrue)
        {
            builder.setMessage("Do you want to turn off notfications for Reviews in "+courseCode+ "||"+courseName+ "?");
        }
        else
        {
            builder.setMessage("Do you want to turn on notfications for Reviews in "+courseCode+ "||"+courseName+ "?");
        }

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(isTrue)
                {
                    //////////////////
                    /////////////////
                    db.collection("Departments").document(departmentName)
                            .collection("Courses").document(courseCode)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful())
                                    {
                                        DocumentSnapshot document = task.getResult();
                                        List<String> list =  (List<String>) document.get("notificationList");
                                        List<String> list2= new ArrayList<>();
                                        for(String l:list)
                                        {
                                            if(!(l.equals(user.getEmail())))
                                            {
                                                list2.add(l);
                                            }
                                        }
                                        db.collection("Departments").document(departmentName)
                                                .collection("Courses").document(courseCode).update("notificationList",list2);
                                        db.collection("Users").document(user.getEmail()).
                                                collection("CoursesNotfications").document(departmentName).update("coursesnotfication",false);
                                    }
                                }
                            });
                    db.collection("Users").document(user.getEmail())
                            .collection("ReviewsNotfications").document(courseCode)
                            .update("reviewnotfication",false);

                }
                else
                {
                    /////////
                    db.collection("Departments").document(departmentName)
                            .collection("Courses").document(courseCode)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful())
                                    {
                                        DocumentSnapshot document = task.getResult();
                                        List<String> list =  (List<String>) document.get("notificationList");
                                        list.add(user.getEmail());
                                        db.collection("Departments").document(departmentName)
                                                .collection("Courses").document(courseCode).update("notificationList",list);
                                        db.collection("Users").document(user.getEmail()).
                                                collection("ReviewsNotfications").document(courseCode).update("reviewnotfication",true);
                                    }
                                }
                            });
                    //////////////////////////
                }
            }
        });
        builder.setNegativeButton("No", null);
        builder.create().show();
    }

    public void showLogoutDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Out");

        builder.setMessage("Are you sure you want to log out?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopService(new Intent(getApplicationContext(),CourseRaterService.class));
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });
        builder.setNegativeButton("No", null);
        builder.create().show();
    }





    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_viewall) {

            if (btn_viewall.getText().toString().equals("VIEW ALL REVIEWS")) {
                ll_title.setVisibility(View.GONE);
                ll_description.setVisibility(View.GONE);
                ll_rating.setVisibility(View.GONE);
                btn_viewall.setText("Back to Course");
            } else {
                ll_title.setVisibility(View.VISIBLE);
                ll_description.setVisibility(View.VISIBLE);
                ll_rating.setVisibility(View.VISIBLE);
                btn_viewall.setText("VIEW ALL REVIEWS");
            }
            updateAndDisplay();

        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Review");
            View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
            builder.setView(view);

            EditText etReview = view.findViewById(R.id.et_review);
            RatingBar ratingBar = view.findViewById(R.id.rating_bar);
            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {



                    String description = etReview.getText().toString();
                    float rating = ratingBar.getRating();
                    if (rating == 0 || description.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Please fill all the blanks", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("Users")
                            .document(user.getEmail())
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            userName = document.getString("name");
                                            imageURL = document.getString("imageURL");
                                            Log.d("CMP354:", "User name: " + userName);


                                            LocalDateTime currentTime = LocalDateTime.now();
                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                                            String dateTimeString = currentTime.format(formatter);

                                            HashMap<String, Object> reviewdataforCourse = new HashMap<>();
                                            reviewdataforCourse.put("description", description);
                                            reviewdataforCourse.put("rating", rating);
                                            reviewdataforCourse.put("email", user.getEmail());
                                            reviewdataforCourse.put("name", userName);
                                            reviewdataforCourse.put("imageURL", imageURL);
                                            reviewdataforCourse.put("time", dateTimeString);
                                            reviewdataforCourse.put("isNew", true);

                                            HashMap<String, Object> reviewdataforUser = new HashMap<>();
                                            reviewdataforUser.put("review", description);
                                            reviewdataforUser.put("rating", rating);
                                            reviewdataforUser.put("course", courseCode);
                                            reviewdataforUser.put("time", dateTimeString);
                                            reviewdataforUser.put("department",departmentName);
                                            Reviews.document(dateTimeString + "| " + user.getEmail()).set(reviewdataforCourse);
                                            Reviews = Departments.document(departmentName).collection("Courses")
                                                    .document(courseCode).collection("Reviews");
                                            /////
                                            TimerTask reviewtask = new TimerTask()
                                            {

                                                @Override
                                                public void run() {

                                                    db.collection("Departments").document(departmentName)
                                                            .collection("Courses").document(courseCode)
                                                            .collection("Reviews")
                                                            .whereEqualTo("isNew", true)
                                                            .get()
                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if (task.isSuccessful()) {
                                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                                            document.getReference().update("isNew",false);

                                                                        }
                                                                        reviewtimer.cancel();

                                                                    } else {
                                                                        Log.d("CMP354:", "Error getting documents: ",
                                                                                task.getException());
                                                                    }

                                                                }

                                                            });

/// end of timertask
                                                }
                                            };

                                            reviewtimer = new Timer(true);
                                            int delay = 1000*15;//1000 * 60 * 60;      // 1 hour
                                            int interval = 1000*15;//1000 * 60 * 60;   // 1 hour
                                            reviewtimer.schedule(reviewtask, delay, interval);
                                            ////
                                            Users.document(dateTimeString + courseCode).set(reviewdataforUser);

                                            updateAndDisplay();

                                            // Load the image with Picasso in the ListView adapter
                                            adapter.notifyDataSetChanged();
                                        } else {
                                            Log.d("CMP354:", "No such document");
                                        }
                                    } else {
                                        Log.d("CMP354:", "Error getting document: " + task.getException());
                                    }
                                    ;
                                }
                            });
                }

            });

            builder.setNegativeButton("Cancel", null);
            builder.create().show();
        }
    }

    public void updateAndDisplay() {
        Departments.document(departmentName)
                .collection("Courses")
                .document(courseCode)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                rating = 0;
                                count = 0;
                                Departments.document(departmentName).collection("Courses")
                                        .document(courseCode).collection("Reviews")
                                        .orderBy("time", Query.Direction.DESCENDING)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    data = new ArrayList<HashMap<String, String>>();
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        HashMap<String, String> map = new HashMap<String, String>();
                                                        Review review = document.toObject(Review.class);
                                                        String reviewDescription = review.getDescription();
                                                        String reviewName = review.getName();
                                                        String reviewRating = review.getRating() + "";
                                                        String reviewImage = review.getImageURL();
                                                        String reviewTime = review.getTime();
                                                        String reviewEmail = review.getEmail();
                                                        map.put("name", reviewName);
                                                        map.put("rating", reviewRating);
                                                        map.put("description", reviewDescription);
                                                        map.put("imageURL", reviewImage);
                                                        map.put("time", reviewTime);
                                                        map.put("email", reviewEmail);
                                                        data.add(map);
                                                        rating = Float.parseFloat(reviewRating) + rating;
                                                        count++;
                                                    }
                                                    rating = rating / count;
                                                    DecimalFormat decimalFormat = new DecimalFormat("#.#");
                                                    String formattedRating = decimalFormat.format(rating);
                                                    rbar_course.setRating(rating);
                                                    tv_rating.setText("Average Rating: " + formattedRating);

                                                    if (count > 0) {
                                                        rbar_course.setVisibility(View.VISIBLE);
                                                        tv_rating.setVisibility(View.VISIBLE);
                                                        btn_viewall.setVisibility(View.VISIBLE);
                                                    } else {
                                                        rbar_course.setVisibility(View.GONE);
                                                        tv_rating.setVisibility(View.GONE);
                                                        btn_viewall.setVisibility(View.GONE);
                                                    }

                                                    Collections.sort(data, new Comparator<HashMap<String, String>>() {
                                                        @Override
                                                        public int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
                                                            String time1 = o1.get("time");
                                                            String time2 = o2.get("time");
                                                            LocalDateTime dateTime1 = LocalDateTime.parse(time1, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                                                            LocalDateTime dateTime2 = LocalDateTime.parse(time2, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                                                            return dateTime2.compareTo(dateTime1);
                                                        }
                                                    });

                                                    int resource = R.layout.list_item_review;
                                                    String[] from = {"name", "rating", "description", "imageURL", "time"};
                                                    int[] to = {R.id.tv_username, R.id.tv_userrating, R.id.tv_userdescription, R.id.img_userprofile, R.id.tv_usertime};
                                                    adapter = new SimpleAdapter(CourseActivity.this, data, resource, from, to) {
                                                        @Override
                                                        public View getView(int position, View convertView, ViewGroup parent) {
                                                            View view = super.getView(position, convertView, parent);
                                                            ImageView imgUserProfile = view.findViewById(R.id.img_userprofile);
                                                            String imageURL = data.get(position).get("imageURL");
                                                            Picasso.get().load(imageURL).into(imgUserProfile);
                                                            return view;
                                                        }
                                                    };
                                                    lv_review.setAdapter(adapter);
                                                } else {
                                                    Log.d("CMP354:", "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(CourseActivity.this, "This course has been deleted.", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Log.d("CMP354:", "Error getting course document: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //
        String reviewTime =  data.get(position).get("time");
        String reviewEmail =  data.get(position).get("email");
        DocumentReference docRef = db.collection("Departments").document(departmentName)
                .collection("Courses").document(courseCode)
                .collection("Reviews").document(reviewTime+"| "+reviewEmail);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String reviewDescription = data.get(position).get("description");
                        String reviewName =  data.get(position).get("name");
                        String reviewRating =  data.get(position).get("rating");
                        String reviewImage =  data.get(position).get("imageURL");
                        Intent intent = new Intent(CourseActivity.this,ReviewActivity.class);
                        intent.putExtra("reviewDescription", reviewDescription);
                        intent.putExtra("reviewRating", reviewRating);
                        intent.putExtra("reviewImage", reviewImage);
                        intent.putExtra("reviewTime", reviewTime);
                        intent.putExtra("reviewName", reviewName);
                        intent.putExtra("reviewEmail", reviewEmail);
                        intent.putExtra("courseCode",courseCode);
                        intent.putExtra("departmentName",departmentName);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CourseActivity.this, "Review no longer exists", Toast.LENGTH_SHORT).show();
                        updateAndDisplay();
                    }
                } else {
                    Toast.makeText(CourseActivity.this, "Failed to retrieve course details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}