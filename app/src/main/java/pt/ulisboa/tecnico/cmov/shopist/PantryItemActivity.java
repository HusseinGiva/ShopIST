package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Map;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;

public class PantryItemActivity extends AppCompatActivity {

    public String id;
    public String pantryId;
    public EditText itemPantryQuantity;
    public EditText itemTargetQuantity;
    public EditText barcodeNumber;
    public EditText userRating;
    public TextView avgRating;
    public TextView rating_5;
    public TextView rating_4;
    public TextView rating_3;
    public TextView rating_2;
    public TextView rating_1;
    public Item item;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;
    public ActivityResultLauncher<Intent> editResultLauncher;

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
        pantryId = getIntent().getStringExtra("PantryId");
        itemPantryQuantity = findViewById(R.id.itemStoreQuantity);
        itemTargetQuantity = findViewById(R.id.itemTargetQuantity);
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);
        userRating = findViewById(R.id.yourRatingInput);
        avgRating = findViewById(R.id.avgRatingText);
        rating_5 = findViewById(R.id.star5Text);
        rating_4 = findViewById(R.id.star4Text);
        rating_3 = findViewById(R.id.star3Text);
        rating_2 = findViewById(R.id.star2Text);
        rating_1 = findViewById(R.id.star1Text);

        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        editResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent intent2 = getIntent();
                    finish();
                    startActivity(intent2);
                });

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

                                float[] votes = {0,0,0,0,0};
                                float totalRatings = 0;
                                float totalVotes = 0;

                                if(item.ratings.containsKey(mAuth.getCurrentUser().getUid()))
                                    userRating.setText(item.ratings.get(mAuth.getCurrentUser().getUid()).toString());

                                for(Map.Entry<String, Integer> entry : item.ratings.entrySet()){
                                    votes[entry.getValue() - 1]++;
                                    totalRatings += entry.getValue();
                                    totalVotes++;
                                }

                                if(totalVotes == 0)
                                    totalVotes = 1;

                                avgRating.setText(getResources().getString(R.string.averageRating) +  " : " +
                                        String.format( "%.1f", (totalRatings / totalVotes)));
                                rating_5.setText(getResources().getString(R.string.votes5star) + ": " + votes[4] + " (" +  Math.round( (votes[4] / totalVotes) * 100)  + "%)");
                                rating_4.setText(getResources().getString(R.string.votes4star) + ": " + votes[3] + " (" +  Math.round( (votes[3] / totalVotes) * 100)  + "%)");
                                rating_3.setText(getResources().getString(R.string.votes3star) + ": " + votes[2] + " (" +  Math.round( (votes[2] / totalVotes) * 100)  + "%)");
                                rating_2.setText(getResources().getString(R.string.votes2star) + ": " + votes[1] + " (" +  Math.round( (votes[1] / totalVotes) * 100)  + "%)");
                                rating_1.setText(getResources().getString(R.string.votes1star) + ": " + votes[0] + " (" +  Math.round( (votes[0] / totalVotes) * 100)  + "%)");

                                db.collection("PantryItem").whereEqualTo("itemId", id).whereEqualTo("pantryId", pantryId).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
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
        } else {
            intent.putExtra("ID", barcodeNumber.getText().toString());
        }
        startActivity(intent);
    }

    public void onClickViewStores(View view) {
        Intent intent = new Intent(this, AddStoresActivity.class);
        intent.putExtra("MODE", "read");
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    public void onClickSubmitRating(View view){
        String userRatingText = userRating.getText().toString();

        int userRatingNumber;

        try{
            userRatingNumber = Integer.parseInt(userRatingText);
        }catch (NumberFormatException e){
            Toast.makeText(this, "Invalid rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if(userRatingNumber < 1 || userRatingNumber > 5){
            Toast.makeText(this, "Rating must be between 1 and 5!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Item").document(id).update(
          "ratings." + mAuth.getCurrentUser().getUid(), userRatingNumber
        );

        db.collection("Item").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                item = document.toObject(Item.class);
                                float[] votes = {0,0,0,0,0};
                                float totalRatings = 0;
                                float totalVotes = 0;

                                if(item.ratings.containsKey(mAuth.getCurrentUser().getUid()))
                                    userRating.setText(item.ratings.get(mAuth.getCurrentUser().getUid()).toString());

                                for(Map.Entry<String, Integer> entry : item.ratings.entrySet()){
                                    votes[entry.getValue() - 1]++;
                                    totalRatings += entry.getValue();
                                    totalVotes++;
                                }

                                if(totalVotes == 0)
                                    totalVotes = 1;

                                avgRating.setText(getResources().getString(R.string.averageRating) +  " : " +
                                        String.format( "%.1f", (totalRatings / totalVotes)));
                                rating_5.setText(getResources().getString(R.string.votes5star) + ": " + votes[4] + " (" +  Math.round( (votes[4] / totalVotes) * 100)  + "%)");
                                rating_4.setText(getResources().getString(R.string.votes4star) + ": " + votes[3] + " (" +  Math.round( (votes[3] / totalVotes) * 100)  + "%)");
                                rating_3.setText(getResources().getString(R.string.votes3star) + ": " + votes[2] + " (" +  Math.round( (votes[2] / totalVotes) * 100)  + "%)");
                                rating_2.setText(getResources().getString(R.string.votes2star) + ": " + votes[1] + " (" +  Math.round( (votes[1] / totalVotes) * 100)  + "%)");
                                rating_1.setText(getResources().getString(R.string.votes1star) + ": " + votes[0] + " (" +  Math.round( (votes[0] / totalVotes) * 100)  + "%)");
                            }
                        }
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_share_flag, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item2) {
        switch (item2.getItemId()) {
            case R.id.shareItem:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                String shareable = getString(R.string.checkoutThisProduct) + item.users.get(mAuth.getCurrentUser().getUid());
                shareable += getString(R.string.itHasTheBarcode) + item.barcode;
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareable);
                File storageDir;
                FirebaseStorage storage;
                StorageReference storageRef;
                storage = FirebaseStorage.getInstance();
                storageRef = storage.getReference();
                StorageReference imagesRef;
                if (!barcodeNumber.getText().toString().equals("")) {
                    storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + barcodeNumber.getText().toString());
                    imagesRef = storageRef.child(barcodeNumber.getText().toString());
                } else {
                    storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id);
                    imagesRef = storageRef.child(id);
                }
                imagesRef.listAll()
                        .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                            @Override
                            public void onSuccess(ListResult listResult) {
                                if (listResult.getItems().size() == 0) {
                                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                } else {
                                    for (StorageReference item : listResult.getItems()) {
                                        Boolean exists = false;
                                        for (File f : storageDir.listFiles()) {
                                            if (f.getName().equals(item.getName())) {
                                                exists = true;
                                                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                Uri screenshotUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), f);
                                                sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                                                sharingIntent.setType("image/jpeg");
                                                startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id).getAbsolutePath() + "/" + item.getName());
                                            item.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                    Uri screenshotUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), localFile);
                                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                                                    sharingIntent.setType("image/jpeg");
                                                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                                }
                                            });
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                return true;
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            case R.id.editItem:
                Intent intent = new Intent(this, AddItemActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.pantry));
                intent.putExtra("ID", pantryId);
                intent.putExtra("ItemId", id);
                intent.putExtra("MODE", "update");
                editResultLauncher.launch(intent);
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item2);
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