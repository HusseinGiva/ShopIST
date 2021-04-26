package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import pt.ulisboa.tecnico.cmov.shopist.persistence.AppDatabase;
import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "PROFILE";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView firstName;
    private TextView lastName;
    private Button btnCreateAccount;
    private Button btnLogout;

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
                    builder.setTitle(new String(Character.toChars(0x26A0)) + " WARNING");
                    builder.setMessage("If you confirm this action, all your lists and items will be permanently deleted! Are you sure you want to continue?");
                    builder.setPositiveButton("Confirm",
                            (dialog, which) -> {
                                FirebaseAuth.getInstance().signOut();

                                new Thread(() -> {
                                    AppDatabase db_phone = Room.databaseBuilder(getActivity().getApplicationContext(),
                                            AppDatabase.class, "database-name").build();
                                    db_phone.clearAllTables();

                                    GlobalClass globalVariable = (GlobalClass) getActivity().getApplicationContext();
                                    globalVariable.clearData();
                                }).start();


                                Intent intent1 = new Intent(getActivity(), MainActivity.class);
                                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent1);
                            });
                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                } else {
                    FirebaseAuth.getInstance().signOut();

                    intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                break;
        }

    }

}

