package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.io.File;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;


public class ProfileFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "PROFILE";
    public String language;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView firstName;
    private TextView lastName;
    private Button btnCreateAccount;
    private Source source;

    public static boolean isConnected(Context getApplicationContext) {
        boolean status = false;

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null) {
            // connected to the internet
            status = true;
        }


        return status;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isConnected(getContext().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

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
        Button btnLogout = view.findViewById(R.id.buttonLogOut);

        btnCreateAccount.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup3);
        SharedPreferences sharedPref = getActivity().getSharedPreferences("language", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "en");
        if (language.equals("en")) {
            radioGroup.check(R.id.english);
        } else if (language.equals("pt")) {
            radioGroup.check(R.id.portuguese);
        } else {
            radioGroup.check(R.id.auto);
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == view.findViewById(R.id.english).getId()) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                db.collection("user").document(currentUser.getUid()).update("language", "en").addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getContext(), StartActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                });
            } else if (checkedId == view.findViewById(R.id.portuguese).getId()) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                db.collection("user").document(currentUser.getUid()).update("language", "pt").addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getContext(), StartActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                });
            } else if (checkedId == view.findViewById(R.id.auto).getId()) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                db.collection("user").document(currentUser.getUid()).update("language", "auto").addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getContext(), StartActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                });
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
            docRef.get(source).addOnCompleteListener(task -> {
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

                                FirebaseUser user = mAuth.getCurrentUser();

                                db.collection("user").document(user.getUid()).delete();

                                db.collection("PantryList").whereArrayContains("users", user.getUid()).get(source).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            PantryList pantry = document.toObject(PantryList.class);
                                            if (pantry.users.size() == 1) {

                                                db.collection("PantryItem").whereEqualTo("pantryId", document.getId()).get(source).addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document2 : task1.getResult()) {
                                                            db.collection("PantryItem").document(document2.getId()).delete();
                                                        }
                                                    }
                                                });

                                                db.collection("PantryList").document(document.getId()).delete();
                                            } else {
                                                pantry.users.remove(user.getUid());
                                                db.collection("PantryList").document(document.getId()).update("users", pantry.users);
                                            }
                                        }
                                    } else {
                                        Log.d(TAG, "Error getting documents: ", task.getException());

                                    }
                                });

                                db.collection("Item").whereEqualTo("barcode", "").get(source).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Item i = document.toObject(Item.class);

                                            if (i.users.containsKey(user.getUid()) && i.users.size() == 1)
                                                db.collection("Item").document(document.getId()).delete();
                                            else if (i.users.containsKey(user.getUid())) {
                                                i.users.remove(user.getUid());

                                                db.collection("Item").document(document.getId()).update("users", i.users);


                                            }
                                        }
                                    }
                                });


                                user.delete();
                                mAuth.signOut();

                                File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                deleteRecursive(storageDir);
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
                    File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    deleteRecursive(storageDir);
                    intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                break;
        }

    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

}

