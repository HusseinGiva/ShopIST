package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class ProfileFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "PROFILE";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView firstName;
    private TextView lastName;
    private Button btnCreateAccount;
    private Button btnLogout;

    public String language;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);


        firstName = view.findViewById(R.id.textViewFirstName);
        lastName = view.findViewById(R.id.textViewLastName);
        btnCreateAccount = view.findViewById(R.id.buttonCreateAccount);
        btnLogout = view.findViewById(R.id.buttonLogOut);

        btnCreateAccount.setOnClickListener((View.OnClickListener) this);
        btnLogout.setOnClickListener((View.OnClickListener) this);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup3);
        SharedPreferences sharedPref = getActivity().getSharedPreferences("language", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "en");
        if (language.equals("en")) {
            radioGroup.check(R.id.english);
        }
        else if (language.equals("pt")) {
            radioGroup.check(R.id.portuguese);
        }
        else {
            radioGroup.check(R.id.auto);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == view.findViewById(R.id.english).getId()) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    db.collection("user").document(currentUser.getUid()).update("language", "en").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            /*SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("language", "en");
                            editor.commit();*/
                            Intent intent = new Intent(getContext(), StartActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    });
                }
                else if (checkedId == view.findViewById(R.id.portuguese).getId()) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    db.collection("user").document(currentUser.getUid()).update("language", "pt").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            /*SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("language", "pt");
                            editor.commit();*/
                            Intent intent = new Intent(getContext(), StartActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    });
                }
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (!currentUser.isAnonymous()) {

            DocumentReference docRef = db.collection("user").document(currentUser.getUid());
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        firstName.setText((CharSequence) document.getData().get("firstName"));
                        lastName.setText((CharSequence) document.getData().get("lastName"));
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            });

            btnCreateAccount.setVisibility(View.INVISIBLE);

        }
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.buttonCreateAccount:
                Intent intent = new Intent(getActivity(), RegisterActivity.class);
                startActivity(intent);
                break;
            case R.id.buttonLogOut:

                FirebaseUser currentUser = mAuth.getCurrentUser();

                if (currentUser.isAnonymous()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setCancelable(true);
                    builder.setTitle(new String(Character.toChars(0x26A0)) + getString(R.string.warning).toUpperCase());
                    builder.setMessage(R.string.logoutWarning);
                    builder.setPositiveButton(R.string.confirm,
                            (dialog, which) -> {
                                FirebaseAuth.getInstance().signOut();

                                new Thread(() -> {

                                    //TODO - Delete user data
                                }).start();


                                Intent intent1 = new Intent(getActivity(), LoginActivity.class);
                                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent1);
                            });
                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                } else {
                    FirebaseAuth.getInstance().signOut();

                    intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                break;
        }

    }

}

