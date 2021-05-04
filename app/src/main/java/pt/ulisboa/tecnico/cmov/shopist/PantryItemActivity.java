package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.firestore.Source;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;

public class PantryItemActivity extends AppCompatActivity {

    public String id;
    public EditText itemPantryQuantity;
    public EditText itemTargetQuantity;
    public EditText barcodeNumber;
    public Item item;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_item);
        Toolbar myToolbar = findViewById(R.id.pantryItemToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        id = getIntent().getStringExtra("ID");
        itemPantryQuantity = findViewById(R.id.itemStoreQuantity);
        itemTargetQuantity = findViewById(R.id.itemTargetQuantity);
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);

        if(isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

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
                                db.collection("PantryItem").whereEqualTo("itemId", id).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                        if (task2.isSuccessful()) {
                                            for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                PantryItem pi = document2.toObject(PantryItem.class);
                                                itemPantryQuantity.setText(String.valueOf(pi.quantity));
                                                itemTargetQuantity.setText(String.valueOf(pi.idealQuantity));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
    }

    public void onClickViewImages(View view) {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putExtra("MODE", "read");
        if (barcodeNumber.getText().toString().equals("")) {
            intent.putExtra("ID", id);
        }
        else {
            intent.putExtra("ID", barcodeNumber.getText().toString());
        }
        startActivity(intent);
    }

    public void onClickViewStores(View view) {
        Intent intent = new Intent(this, AddStoresActivity.class);
        intent.putExtra("MODE", "read");
        if (barcodeNumber.getText().toString().equals("")) {
            intent.putExtra("ID", id);
        }
        else {
            intent.putExtra("ID", barcodeNumber.getText().toString());
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_share_flag, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shareItem:
                String message = "Text I want to share.";
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, message);

                startActivity(Intent.createChooser(share, "Title of the dialog the system will open"));
                return true;
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
    public void onBackPressed() {
        finish();
    }

    public static boolean isConnected(Context getApplicationContext) {
        boolean status = false;

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null) {
            // connected to the internet
            status = true;
        }


        return status;
    }
}