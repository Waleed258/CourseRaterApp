package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CoursesActivity extends AppCompatActivity  implements AdapterView.OnItemClickListener, TextWatcher {
    String departmentName;
    FirebaseUser user;
    FirebaseFirestore db; //TS member variable
    CollectionReference Courses;
    CollectionReference Departments;
    ListView lv_course;
    SimpleAdapter adapter;
    ArrayList<HashMap<String, String>> data;
    boolean isAdmin;

    EditText et_search;
    Timer coursetimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance(); //TS in onCreate
        et_search=findViewById(R.id.et_serach);
        lv_course = findViewById(R.id.lv_course);
        lv_course.setOnItemClickListener(this);
        departmentName=getIntent().getStringExtra("departmentName");
        isAdmin = getIntent().getBooleanExtra("usertype",false);
        Departments=db.collection("Departments");
        Courses= db.collection("Courses");
        et_search.addTextChangedListener(this);

    }
    @Override
    protected void onResume() {
        super.onResume();
        updateAndDisplay();
    }
    public boolean onCreateOptionsMenu(Menu menu) {

        if(isAdmin)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.admin_menu, menu);
            MenuItem add = menu.findItem(R.id.item_add);
            MenuItem delete = menu.findItem(R.id.item_delete);
            add.setTitle("Add New Course");
            delete.setTitle("Delete Course");
        }
        else {

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.user_menu, menu);

        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int itemId = item.getItemId();
        if (itemId == R.id.item_add) {

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
            EditText et_coursename = dialogView.findViewById(R.id.et_coursename);
            EditText et_coursecode = dialogView.findViewById(R.id.et_coursecode);
            EditText et_coursedescription = dialogView.findViewById(R.id.et_coursedescription);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Course")
                    .setView(dialogView)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String courseName = et_coursename.getText().toString();
                            String courseCode = et_coursecode.getText().toString();
                            String courseDescription = et_coursedescription.getText().toString();

                            if (!courseName.isEmpty() && !courseCode.isEmpty() && !courseDescription.isEmpty()) {

                                    HashMap<String, Object> course = new HashMap<>();
                                    course.put("name", courseName);
                                    course.put("code", courseCode);
                                    course.put("description", courseDescription);
                                    course.put("isNew", true);
                                    course.put("notificationList", new ArrayList<>());
                                    Courses = Departments.document(departmentName).collection("Courses");
                                    Courses.document(courseCode).set(course);
                                    ///
                                TimerTask coursetask = new TimerTask()
                                {

                                    @Override
                                    public void run() {

                                        db.collection("Departments").document(departmentName)
                                                .collection("Courses")
                                                .whereEqualTo("isNew", true)
                                                .get()
                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                                Log.d("Testing...:", "this works!");
                                                                document.getReference().update("isNew",false);

                                                            }

                                                        } else {
                                                            Log.d("CMP354:", "Error getting documents: ",
                                                                    task.getException());
                                                        }

                                                    }
                                                });
                                        coursetimer.cancel();

/// end of timertask
                                    }
                                };

                                coursetimer = new Timer(true);
                                int delay = 1000*15;//1000 * 60 * 60;      // 1 hour
                                int interval = 1000*15;//1000 * 60 * 60;   // 1 hour
                                coursetimer.schedule(coursetask, delay, interval);
                                ////


                            } else {
                                Toast.makeText(getApplicationContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show();
                            }
                            updateAndDisplay();
                        }
                    })
                    .setNegativeButton("Cancel", null);

            builder.create().show();
            return true;
        }

        else if(itemId==R.id.item_delete)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Write the Course Code to Delete");

            EditText et_coursecode = new EditText(this);
            builder.setView(et_coursecode);

            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String courseCode = et_coursecode.getText().toString();
                    if (!courseCode.isEmpty()) {
                        ////
                        db.collection("Departments").document(departmentName).collection("Courses")
                                .whereEqualTo("code", courseCode)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                            showConfirmationDialog(courseCode);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Please enter an exisiting Course code", Toast.LENGTH_SHORT).show();

                                        }
                                        ;
                                    }
                                });
                        /////
                    } else {
                        Toast.makeText(getApplicationContext(), "Please enter a Course code", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.create().show();
            return true;
        }
        else if(itemId==R.id.item_logout)
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
                    .collection("CoursesNotfications").document(departmentName);
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if(documentSnapshot.getBoolean("coursesnotfication")!=null)
                    {
                        boolean isTrue=documentSnapshot.getBoolean("coursesnotfication");
                        showNotificationDialog(isTrue);
                    }
                    else {
                        Map<String, Object> notficationdata = new HashMap<>();
                        notficationdata.put("coursesnotfication", false);
                        docRef.set(notficationdata); //”LA” is called the document path (think of it as a primary key)
                        showNotificationDialog(false);
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

    public void showConfirmationDialog(String courseCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation");
        builder.setMessage("Are you sure you want to delete the Course: " + courseCode + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               deleteCourse(courseCode);
               updateAndDisplay();
            }
        });

        builder.setNegativeButton("No", null);
        builder.create().show();
    }
    public void showNotificationDialog(boolean isTrue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notification");

        if (isTrue) {
            builder.setMessage("Do you want to turn off notifications for Courses in the " + departmentName + " department?");
        } else {
            builder.setMessage("Do you want to turn on notifications for Courses in the " + departmentName + " department?");
        }

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(isTrue)
                {

                    db.collection("Departments").document(departmentName)
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
                                        db.collection("Departments").document(departmentName).update("notificationList",list2);
                                        db.collection("Users").document(user.getEmail()).
                                                collection("CoursesNotfications").document(departmentName).update("coursesnotfication",false);
                                    }
                                }
                            });
                }
                else
                {
                    db.collection("Departments").document(departmentName)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful())
                                    {
                                        DocumentSnapshot document = task.getResult();
                                        List<String> list =  (List<String>) document.get("notificationList");
                                        list.add(user.getEmail());
                                        db.collection("Departments").document(departmentName).update("notificationList",list);
                                        db.collection("Users").document(user.getEmail()).
                                                collection("CoursesNotfications").document(departmentName).update("coursesnotfication",true);
                                    }
                                }
                            });
                    //db.collection("Users").document(user.getEmail()).update("departmentnotfication", true);
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
    public void deleteCourse(String courseCode) {

        deleteCourseReviews(departmentName,courseCode);
        db.collection("Departments").document(departmentName)
                .collection("Courses").document(courseCode)
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
    }
    private void deleteCourseReviews(String departmentName, String courseId) {
        // Delete the reviews subcollection for the specified course
        db.collection("Departments").document(departmentName)
                .collection("Courses").document(courseId)
                .collection("Reviews")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot reviewDocument : task.getResult()) {
                                reviewDocument.getReference().delete();
                            }
                            deleteUserReview(courseId);
                            deleteCourse(departmentName);
                        } else {
                            Log.d("CMP354:", "Error getting reviews documents: ", task.getException());
                        }
                        ;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CMP354:", "Error deleting reviews subcollection", e);
                    }
                });
    }

    private void deleteUserReview(String courseCode) {
        db.collection("Users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot userDocument : task.getResult()) {
                                String userId = userDocument.getId();
                                db.collection("Users").document(userId)
                                        .collection("myReviews")
                                        .whereEqualTo("course", courseCode)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot reviewDocument : task.getResult()) {
                                                        reviewDocument.getReference().delete();
                                                    }
                                                } else {
                                                    Log.d("CMP354:", "Error getting documents: ", task.getException());
                                                }
                                                ;
                                            }
                                        });
                            }
                        } else {
                            Log.d("CMP354:", "Error getting users documents: ", task.getException());
                        }
                        ;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CMP354:", "Error deleting user review", e);
                    }
                });
    }


    public void updateAndDisplay() {
        Departments.document(departmentName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {

                                db.collection("Departments").document(departmentName).collection("Courses")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    data = new ArrayList<HashMap<String, String>>();
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        HashMap<String, String> map = new HashMap<String,String>();
                                                        Course course = document.toObject(Course.class);
                                                        String courseName = course.getName();
                                                        String coursecode = course.getCode();
                                                        if((coursecode.toLowerCase().contains(et_search.getText().toString().toLowerCase()) || courseName.toLowerCase().contains(et_search.getText().toString().toLowerCase())))
                                                        {
                                                            String courseDescription = course.getDescription();
                                                            map.put("name",courseName);
                                                            map.put("code",coursecode);
                                                            map.put("description",courseDescription);
                                                            data.add(map);
                                                        }
                                                    }

                                                    int resource = R.layout.list_item_course;
                                                    String[] from = {"code","name"};
                                                    int[] to = {R.id.tv_crsCode,R.id.tv_crsName};
                                                    // create and set the adapter
                                                    adapter =
                                                            new SimpleAdapter(CoursesActivity.this, data, resource, from, to);
                                                    lv_course.setAdapter(adapter);


                                                } else {
                                                    Log.d("CMP354:", "Error getting documents: ", task.getException());
                                                }
                                                ;
                                            }
                                        });

                            } else {
                                Toast.makeText(CoursesActivity.this, "This Department has been deleted.", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Log.d("CMP354:", "Error getting course document: ", task.getException());
                        }

                    }
                });
        //////
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String courseCode = data.get(position).get("code");

        DocumentReference docRef = db.collection("Departments").document(departmentName)
                .collection("Courses").document(courseCode);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String courseName = data.get(position).get("name");
                        String courseDescription = data.get(position).get("description");
                        Intent intent = new Intent(CoursesActivity.this, CourseActivity.class);
                        intent.putExtra("courseName", courseName);
                        intent.putExtra("courseCode", courseCode);
                        intent.putExtra("courseDescription", courseDescription);
                        intent.putExtra("departmentName", departmentName);
                        intent.putExtra("isAdmin", isAdmin);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CoursesActivity.this, "Course no longer exists", Toast.LENGTH_SHORT).show();
                        updateAndDisplay();
                    }
                } else {
                    Toast.makeText(CoursesActivity.this, "Failed to retrieve course details", Toast.LENGTH_SHORT).show();
                }
               
            }
        });


    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        updateAndDisplay();
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}

