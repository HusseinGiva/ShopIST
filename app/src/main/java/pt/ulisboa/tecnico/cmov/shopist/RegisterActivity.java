package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {


    private static final String TAG = "REGISTER";
    private EditText email;
    private EditText firstName;
    private EditText lastName;
    private EditText password;
    private EditText confirmPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar myToolbar = findViewById(R.id.registerToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        email = findViewById(R.id.registerEmailTextBox);
        firstName = findViewById(R.id.firstNameTextBox);
        lastName = findViewById(R.id.lastNameTextBox);
        password = findViewById(R.id.registerPasswordTextBox);
        confirmPassword = findViewById(R.id.confirmPasswordTextBox);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void OnClickRegister(View view) {

        String emailText = email.getText().toString();
        String firstNameText = firstName.getText().toString();
        String lastNameText = lastName.getText().toString();
        String passwordText = password.getText().toString();
        String confirmPasswordText = confirmPassword.getText().toString();
        String language = "auto";
        SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("language", language);
        editor.commit();

        //Check if name fields are empty
        if (firstNameText.trim().isEmpty() || lastNameText.trim().isEmpty()) {
            Toast.makeText(this, R.string.invalidName, Toast.LENGTH_SHORT).show();
            return;
        }

        //Check if email is valid
        if (!isValidEmail(emailText)) {
            Toast.makeText(this, R.string.invalidEmail, Toast.LENGTH_SHORT).show();
            return;
        }

        //Check if password field is empty
        if (passwordText.trim().isEmpty()) {
            Toast.makeText(this, R.string.invalidPassword, Toast.LENGTH_SHORT).show();
            return;
        }

        //Check if passwords match
        if (!passwordText.equals(confirmPasswordText)) {
            Toast.makeText(this, R.string.passwordDontMatch, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        //Brand new user
        if (currentUser == null) {
            mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            addUserToFirestore(user, firstNameText, lastNameText, language);
                            updateUI(user, false);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null, false);
                        }
                    });
        }
        //Connect anonymous account to account with credentials
        else {
            AuthCredential credential = EmailAuthProvider.getCredential(emailText, passwordText);

            mAuth.getCurrentUser().linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "linkWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            addUserToFirestore(user, firstNameText, lastNameText, language);
                            updateUI(user, true);
                        } else {
                            Log.w(TAG, "linkWithCredential:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null, true);
                        }
                    });
        }


    }

    private void addUserToFirestore(FirebaseUser user, String firstNameText, String lastNameText, String language) {

        Map<String, String> userInfo = new HashMap<>();

        userInfo.put("firstName", firstNameText);
        userInfo.put("lastName", lastNameText);
        userInfo.put("language", language);

        db.collection("user").document(user.getUid())
                .set(userInfo)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
    }

    private void updateUI(FirebaseUser user, boolean isAnonymousConnect) {
        if (user != null) {

            if (isAnonymousConnect) {
                finish();
            } else {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }


        }
    }


}