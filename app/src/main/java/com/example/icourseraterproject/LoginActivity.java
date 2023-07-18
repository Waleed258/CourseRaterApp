package com.example.icourseraterproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

///////////////////////////
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    EditText et_username,et_password;
    FirebaseFirestore db; //TS member variable
    CollectionReference Users;
    Button btn_login,btn_signup;
    Boolean isAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        et_username=findViewById(R.id.et_username);
        et_password=findViewById(R.id.et_password);
        btn_login=findViewById(R.id.btn_login);
        btn_signup=findViewById(R.id.btn_signup);
        btn_login.setOnClickListener(this);
        btn_signup.setOnClickListener(this);
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
    @Override
    protected void onResume() {
        super.onResume();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {

        if(v.getId()==R.id.btn_login)
        {
            if(et_username.getText().toString().isEmpty() || et_password.getText().toString().isEmpty())
            {
                Toast.makeText(this,"Please enter fill all the fields",Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(et_username.getText().toString(),
                            et_password.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("FB Login", "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                getUserinfo();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("FB Login", "signInWithEmail:failure", task.getException());
                                Toast.makeText(getApplicationContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            ;
                        }
                    });
        }
        else if (v.getId()==R.id.btn_signup)
        {
            startActivity(new Intent(this, SignUpActivity.class));
        }
    }

    public void getUserinfo() {
        DocumentReference docRef = db.collection("Users").document(et_username.getText()+"");
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                isAdmin = user.getAdmin();
                Intent intent = new Intent(LoginActivity.this,DepartmentsActivity.class);
                intent.putExtra("username", et_username.getText()+"");
                intent.putExtra("usertype",isAdmin);
                startActivity(intent);
            }
        });
    }
}
