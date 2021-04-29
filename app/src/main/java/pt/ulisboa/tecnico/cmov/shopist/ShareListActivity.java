package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShareListActivity extends AppCompatActivity {

    private static final String TAG = "SHARE_LIST";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);
        Toolbar myToolbar = findViewById(R.id.shareListToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        /*mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GlobalClass globalVariable = (GlobalClass) getApplicationContext();
        boolean isPantry = globalVariable.getTypeSelected().equals("PANTRY");

        if(isPantry){
            if(globalVariable.getPantryWithItems().get(globalVariable.getPositionSelected()).pantry.pantryIdServer != null){
                //Pantry already in server... Nothing to be done
            }
            else{

                for(Item i : globalVariable.getPantryWithItems().get(globalVariable.getPositionSelected()).items){
                    for(StoreWithItems s: globalVariable.getStoreWithItems()){
                        if(s.store.storeIdServer != null){
                            //Store already in server
                        }else{
                            if(s.items.contains(i)){
                                //Create Store in firestore
                                db.collection("store_shared").add(s.store).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        s.store.storeIdServer = documentReference.getId();
                                    }
                                })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ShareListActivity.this, e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    //if(i.)
                }

                String name = globalVariable.getPantryWithItems().get(globalVariable.getPositionSelected()).pantry.name;
                double latitude = globalVariable.getPantryWithItems().get(globalVariable.getPositionSelected()).pantry.latitude;
                double longitude = globalVariable.getPantryWithItems().get(globalVariable.getPositionSelected()).pantry.longitude;

                //Create Pantry in firestore
                Map<String, Object> pantry = new HashMap<>();
                pantry.put("name", name);
                pantry.put("location", new GeoPoint(latitude, longitude));
                pantry.put("users", Arrays.asList(mAuth.getCurrentUser().getUid()));


            }
        }*/
    }


}
