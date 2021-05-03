package pt.ulisboa.tecnico.cmov.shopist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class AddPicturesActivity extends AppCompatActivity implements PicturesFragment.OnListFragmentInteractionListener {

    Button addNewPicture;
    Button browsePicturesButton;
    ActivityResultLauncher<Intent> cameraResultLauncher;
    ActivityResultLauncher<Intent> galleryResultLauncher;
    String currentPhotoPath;
    ArrayList<String> photoPaths = new ArrayList<>();
    private RecyclerView.Adapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pictures);
        Toolbar myToolbar = findViewById(R.id.addPicturesToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        addNewPicture = findViewById(R.id.addNewPicture);
        browsePicturesButton = findViewById(R.id.browsePicturesButton);
        if (recyclerViewAdapter == null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.picturesFragment);
            recyclerView = (RecyclerView) currentFragment.getView();
            recyclerViewAdapter = recyclerView.getAdapter();
        }
        PictureContent.emptyList();
        recyclerViewAdapter.notifyDataSetChanged();
        if (getIntent().getStringExtra("MODE").equals("read")) {
            addNewPicture.setVisibility(View.INVISIBLE);
            browsePicturesButton.setVisibility(View.INVISIBLE);
            RecyclerView pictureFragment = findViewById(R.id.picturesFragment);
            ConstraintLayout.LayoutParams newLayoutParams = (ConstraintLayout.LayoutParams) pictureFragment.getLayoutParams();
            newLayoutParams.topMargin = 150;
            pictureFragment.setLayoutParams(newLayoutParams);
            getSupportActionBar().setTitle(R.string.viewPictures);
            String id = getIntent().getStringExtra("ID");
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id);
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference();
            StorageReference imagesRef = storageRef.child(id);
            imagesRef.listAll()
                    .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                        @Override
                        public void onSuccess(ListResult listResult) {
                            for (StorageReference item : listResult.getItems()) {
                                Boolean exists = false;
                                for (File f : storageDir.listFiles()) {
                                    if (f.getName().equals(item.getName())) {
                                        currentPhotoPath = f.getAbsolutePath();
                                        photoPaths.add(currentPhotoPath);
                                        PictureContent.loadImage(new File(currentPhotoPath));
                                        recyclerViewAdapter.notifyItemInserted(0);
                                        exists = true;
                                    }
                                }
                                if (!exists) {
                                    File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id).getAbsolutePath() + "/" + item.getName());
                                    currentPhotoPath = localFile.getAbsolutePath();
                                    photoPaths.add(currentPhotoPath);
                                    item.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            PictureContent.loadImage(new File(currentPhotoPath));
                                            recyclerViewAdapter.notifyItemInserted(0);
                                        }
                                    });
                                }
                            }
                        }
                    });
        } else if (getIntent().getStringExtra("MODE").equals("add")) {
            photoPaths = getIntent().getStringArrayListExtra("PATHS");
            addNewPicture.setOnClickListener(v -> dispatchTakePictureIntent());
            cameraResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            photoPaths.add(currentPhotoPath);
                            PictureContent.loadImage(new File(currentPhotoPath));
                            recyclerViewAdapter.notifyItemInserted(0);
                        }
                    });
            browsePicturesButton.setOnClickListener(v -> onClickBrowseButton());
            galleryResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri selectedImageUri = result.getData().getData();
                            try {
                                String id = getIntent().getStringExtra("ID");
                                File file;
                                if ((id != null) && (!id.equals(""))) {
                                    file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id).getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
                                } else {
                                    file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
                                }
                                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(selectedImageUri);
                                OutputStream outputStream = new FileOutputStream(file);
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = inputStream.read(buf)) > 0)
                                    outputStream.write(buf, 0, len);
                                outputStream.close();
                                inputStream.close();
                                photoPaths.add(file.getAbsolutePath());
                                PictureContent.loadImage(file);
                                recyclerViewAdapter.notifyItemInserted(0);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            if (!photoPaths.isEmpty()) {
                for (String s : photoPaths) {
                    PictureContent.loadImage(new File(s));
                    recyclerViewAdapter.notifyItemInserted(0);
                }
            }
        }
    }

    void onClickBrowseButton() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryResultLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String id = getIntent().getStringExtra("ID");
        File file;
        if ((id != null) && (!id.equals(""))) {
            file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id).getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        } else {
            file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        }
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = file.getAbsolutePath();
        return file;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "pt.ulisboa.tecnico.cmov.shopist",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraResultLauncher.launch(takePictureIntent);
            }
        }
    }

    @Override
    public void onListFragmentInteraction(Uri mItem) {
        Intent intent = new Intent(this, ViewPicturesActivity.class);
        intent.putExtra("URI", mItem.toString());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra("PATHS", photoPaths);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}