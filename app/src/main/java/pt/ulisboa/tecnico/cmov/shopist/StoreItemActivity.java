package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;

public class StoreItemActivity extends AppCompatActivity {

    public String id;
    public EditText itemStoreQuantity;
    public EditText barcodeNumber;
    public Item item;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_item);
        Toolbar myToolbar = findViewById(R.id.storeItemToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        id = getIntent().getStringExtra("ID");
        //itemStoreQuantity = findViewById(R.id.itemStoreQuantity);
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db.collection("Item").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                item = document.toObject(Item.class);
                                getSupportActionBar().setTitle(item.users.get(mAuth.getCurrentUser().getUid()));
                                barcodeNumber.setText(item.barcode);
                                db.collection("StoreItem").whereEqualTo("itemId", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                        if (task2.isSuccessful()) {
                                            for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                PantryItem pi = document2.toObject(PantryItem.class);
                                                itemStoreQuantity.setText(String.valueOf(pi.quantity));
                                                //itemTargetQuantity.setText(String.valueOf(pi.idealQuantity));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_share_flag, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed () {
        finish();
    }
}