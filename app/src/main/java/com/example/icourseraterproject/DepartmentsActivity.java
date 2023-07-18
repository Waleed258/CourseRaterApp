package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.HashMap;
import java.util.Map;

public class DepartmentsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    FirebaseUser user;
    FirebaseFirestore db; //TS member variable
    CollectionReference Departments;
    ListView listview;
    SimpleAdapter adapter;
    ArrayList<HashMap<String, String>> data;
    boolean departmentNotfication;
    SharedPreferences sharedpref;
    boolean isAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance(); //TS in onCreate
        setContentView(R.layout.activity_departments);
        isAdmin=getIntent().getBooleanExtra("usertype",false);
        listview=findViewById(R.id.lv_dept);
        listview.setOnItemClickListener(this);
        Departments = db.collection("Departments");
        Intent serviceIntent = new Intent(this, CourseRaterService.class);
        sharedpref=getSharedPreferences("Sharedpref",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpref.edit();
        editor.putString("userEmail", user.getEmail());
        editor.commit();
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAndDisplay();
    }
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
        if(isAdmin)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.admin_menu, menu);
            MenuItem add = menu.findItem(R.id.item_add);
            MenuItem delete = menu.findItem(R.id.item_delete);
            add.setTitle("Add New Department");
            delete.setTitle("Delete Department");
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

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Department");

            EditText et_departmentname = new EditText(this);
            builder.setView(et_departmentname);

            builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String departmentName = et_departmentname.getText().toString();
                    if (!departmentName.isEmpty())
                    {
                            Map<String, Object> department = new HashMap<>();
                            department.put("name", departmentName);
                            department.put("isNew",true);
                            department.put("checked",false);
                            department.put("notificationList", new ArrayList<>());
                            Departments.document(departmentName).set(department);

                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Please enter a department name", Toast.LENGTH_SHORT).show();
                    }
                    updateAndDisplay();
                }
            });
            builder.setNegativeButton("Cancel",null);
            builder.create().show();
            return true;
        }

        else if(itemId==R.id.item_delete)
        {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Delete Department");

                EditText et_departmentname = new EditText(this);
                builder.setView(et_departmentname);

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String departmentName = et_departmentname.getText().toString();
                        if (!departmentName.isEmpty()) {

                            db.collection("Departments")
                                    .whereEqualTo("name", departmentName)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                showConfirmationDialog(departmentName);
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Please enter an exisiting department name", Toast.LENGTH_SHORT).show();

                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(getApplicationContext(), "Please enter a department name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.create().show();
                return true;
        }
        else if(itemId==R.id.item_about1)
        {
            startActivity(new Intent(this, AboutActivity.class));
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

            DocumentReference docRef = db.collection("Users").document(user.getEmail());
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    boolean isTrue = documentSnapshot.getBoolean("departmentnotfication");
                    showNotficationDialog(isTrue);
                }
            });

        }
        return super.onOptionsItemSelected(item);
    }


    public void showConfirmationDialog(String departmentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation");
        builder.setMessage("Are you sure you want to delete the department: " + departmentName + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    deleteDepartment(departmentName);
                    updateAndDisplay();
            }
        });
        builder.setNegativeButton("No", null);
        builder.create().show();
    }
    public void showNotficationDialog(boolean isTrue) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notfication");
        if(isTrue)
        {
            builder.setMessage("Do you want to turn off notfications for Departments?");
        }
        else
        {
            builder.setMessage("Do you want to turn on notfications for Departments?");
        }

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(isTrue)
                {
                    db.collection("Users").document(user.getEmail()).update("departmentnotfication", false);
                }
                else
                {
                    db.collection("Users").document(user.getEmail()).update("departmentnotfication", true);
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

    public void deleteDepartment(String departmantName) {
        db.collection("Departments").document(departmantName).collection("Courses")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot courseDocument : task.getResult()) {
                                String id = courseDocument.getId();
                                deleteCourseReviews(departmantName, id);
                            }
                            db.collection("Departments").document(departmantName)
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("CMP345:", "Department successfully deleted!");

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("CMP354:", "Error deleting department document", e);
                                        }
                                    });
                        } else {
                            Log.d("CMP354:", "Error getting courses documents: ", task.getException());
                        }
                        updateAndDisplay();
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
                            deleteUserReview(departmentName);
                            deleteCourse(departmentName);
                        } else {
                            Log.d("CMP354:", "Error getting reviews documents: ", task.getException());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CMP354:", "Error deleting reviews subcollection", e);
                    }
                });
    }

    private void deleteUserReview(String departmentName) {
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
                                        .whereEqualTo("department", departmentName)
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
                                            }
                                        });
                            }
                        } else {
                            Log.d("CMP354:", "Error getting users documents: ", task.getException());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CMP354:", "Error deleting user review", e);
                    }
                });
    }

    public void deleteCourse(String departmentName)
    {
        db.collection("Departments").document(departmentName)
                .collection("Courses")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot courseDocument : task.getResult()) {
                                courseDocument.getReference().delete();
                            }
                        } else {
                            Log.d("CMP354:", "Error getting reviews : ", task.getException());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CMP354:", "Error deleting reviews ", e);
                    }
                });
    }
    public void updateAndDisplay() {
        db.collection("Departments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            data = new ArrayList<HashMap<String, String>>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                HashMap<String, String> map = new HashMap<String,String>();
                                Department department = document.toObject(Department.class);
                                String departmentName = department.getName();
                                map.put("name",departmentName);
                                data.add(map);
                            }
                            int resource = R.layout.list_item_dept;
                            String[] from = {"name"};
                            int[] to = {R.id.tv_deptname};
                            adapter =
                                    new SimpleAdapter(DepartmentsActivity.this, data, resource, from, to);
                            listview.setAdapter(adapter);

                        } else {
                            Log.d("CMP354:", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


        String departmentName = data.get(position).get("name");

        DocumentReference docRef = db.collection("Departments").document(departmentName);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Intent intent = new Intent(DepartmentsActivity.this, CoursesActivity.class);
                        intent.putExtra("departmentName", departmentName);
                        intent.putExtra("usertype",isAdmin);
                        startActivity(intent);
                    } else {
                        Toast.makeText(DepartmentsActivity.this, "Department no longer exists", Toast.LENGTH_SHORT).show();
                        updateAndDisplay();
                    }
                } else {
                    Toast.makeText(DepartmentsActivity.this, "Failed to retrieve Department details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}