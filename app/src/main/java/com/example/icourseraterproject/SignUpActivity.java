package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseFirestore db; // Firebase Firestore instance
    private static final int REQUEST_IMAGE_PICKER = 1;
    CollectionReference Users;
    TextView tv_name, tv_email, tv_pass;
    EditText et_name, et_email, et_pass,et_confirm;
    Button btn_signup2, btn_cancel, btn_profile;
    RadioGroup rg;
    RadioButton rb_admin, rb_user;
    private FirebaseAuth mAuth;
    ImageView img_profile;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        tv_name = findViewById(R.id.tv_name);
        tv_email = findViewById(R.id.tv_email);
        tv_pass = findViewById(R.id.tv_pass);
        et_name = findViewById(R.id.et_name);
        et_email = findViewById(R.id.et_email);
        et_pass = findViewById(R.id.et_pass);
        et_confirm = findViewById(R.id.et_confirm);
        img_profile = findViewById(R.id.img_profile);
        btn_profile = findViewById(R.id.btn_profile);
        btn_signup2 = findViewById(R.id.btn_signup2);
        btn_cancel = findViewById(R.id.btn_cancel);
        rg = findViewById(R.id.rg);
        rb_admin = findViewById(R.id.rb_admin);
        rb_user = findViewById(R.id.rb_user);
        rb_user.setChecked(true);
        btn_signup2.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_profile.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        Users = db.collection("Users");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.item_about1) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICKER);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICKER && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            img_profile.setImageURI(imageUri);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_profile) {
            openImagePicker();
        }
        else if (v.getId() == R.id.btn_cancel) {
            finish();
        }
        else
            {
            if (et_name.getText().toString().isEmpty() && et_email.getText().toString().isEmpty() && et_pass.getText().toString().isEmpty()
            && et_confirm.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!(et_confirm.getText().toString().equals(et_pass.getText().toString())))
            {
                Toast.makeText(this, "Confrim password is not correct", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.createUserWithEmailAndPassword(et_email.getText().toString(), et_pass.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                saveUserDataToFirestore(firebaseUser);
                                uploadProfilePicture(firebaseUser);
                                finish();
                            } else {
                                Log.d("FB Signup", "createUserWithEmail:failure: " +
                                        task.getException().toString());
                                Toast.makeText(getApplicationContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            ;
                        }
                    });
        }
    }

    private void saveUserDataToFirestore(FirebaseUser firebaseUser) {
        String userEmail = firebaseUser.getEmail();
        String userName = et_name.getText().toString();

        Map<String, Object> user = new HashMap<>();
        user.put("admin", rb_admin.isChecked());
        user.put("name", userName);
        user.put("email",userEmail);
        user.put("departmentnotfication",false);

        Users.document(userEmail).set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                       // Toast.makeText(getApplicationContext(), "User data saved successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfilePicture(FirebaseUser firebaseUser) {
        String userEmail = firebaseUser.getEmail();

        if (imageUri == null) {
            uploadDefaultProfilePicture(userEmail, firebaseUser);
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("users/" + userEmail + ".jpg");

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageURL = uri.toString();
                                Users.document(userEmail).update("imageURL", imageURL)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getApplicationContext(), "Sign up Success", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getApplicationContext(), "Failed to update profile picture URL", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadDefaultProfilePicture(String userEmail, FirebaseUser firebaseUser) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("users/" + userEmail + ".jpg");


        int resourceId = getResources().getIdentifier("img", "drawable", getPackageName());


        Uri defaultProfilePictureUri = Uri.parse("android.resource://" + getPackageName() + "/" + resourceId);


        UploadTask uploadTask = imageRef.putFile(defaultProfilePictureUri);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageURL = uri.toString();
                                Users.document(userEmail).update("imageURL", imageURL)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getApplicationContext(), "Sign up Success", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getApplicationContext(), "Failed to update profile picture URL", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
