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

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Objects;

public class AddPicturesActivity extends AppCompatActivity implements PicturesFragment.OnListFragmentInteractionListener {

    Button addNewPicture;
    Button browsePicturesButton;
    ActivityResultLauncher<Intent> cameraResultLauncher;
    ActivityResultLauncher<Intent> galleryResultLauncher;
    String currentPhotoPath;
    long dirSize = 0;
    String lruLocation = "";
    FileTime lruFileTime;
    File storageDirGlobal;
    ArrayList<String> photoPaths = new ArrayList<>();
    private RecyclerView.Adapter recyclerViewAdapter;

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
            RecyclerView recyclerView = (RecyclerView) Objects.requireNonNull(currentFragment).getView();
            recyclerViewAdapter = Objects.requireNonNull(recyclerView).getAdapter();
        }
        PictureContent.emptyList();
        recyclerViewAdapter.notifyDataSetChanged();
        storageDirGlobal = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        getDirSize(storageDirGlobal);
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
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imagesRef = storageRef.child(id);
            imagesRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference item : listResult.getItems()) {
                            boolean exists = false;
                            for (File f : Objects.requireNonNull(storageDir.listFiles())) {
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
                                item.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                                    PictureContent.loadImage(new File(currentPhotoPath));
                                    recyclerViewAdapter.notifyItemInserted(0);
                                    while (dirSize > 10000) {
                                        try {
                                            findLRU(storageDirGlobal);
                                            deleteLRU(lruLocation);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        getDirSize(storageDirGlobal);
                                    }
                                });
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
                            while (dirSize > 10000) {
                                try {
                                    findLRU(storageDirGlobal);
                                    deleteLRU(lruLocation);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                getDirSize(storageDirGlobal);
                            }
                        }
                    });
            browsePicturesButton.setOnClickListener(v -> onClickBrowseButton());
            galleryResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri selectedImageUri = Objects.requireNonNull(result.getData()).getData();
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
                                while (dirSize > 10000) {
                                    try {
                                        findLRU(storageDirGlobal);
                                        deleteLRU(lruLocation);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    getDirSize(storageDirGlobal);
                                }
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

    void getDirSize(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                getDirSize(child);

        dirSize += (fileOrDirectory.length() / 1024);
    }

    void deleteLRU(String path) {
        if (!path.equals("")) {
            File file = new File(path);
            file.delete();
        }
    }

    void findLRU(File fileOrDirectory) throws IOException {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
                findLRU(child);
            }
        else {
            if (lruLocation.equals("") || Files.readAttributes(fileOrDirectory.toPath(),
                    BasicFileAttributes.class).lastAccessTime().compareTo(lruFileTime) < 0) {
                lruLocation = fileOrDirectory.getAbsolutePath();
                lruFileTime = Files.readAttributes(fileOrDirectory.toPath(),
                        BasicFileAttributes.class).lastAccessTime();
            }
        }
    }

    void onClickBrowseButton() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryResultLauncher.launch(intent);
    }

    private File createImageFile() {
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
            File photoFile;
            photoFile = createImageFile();
            // Continue only if the File was successfully created
            Uri photoURI = FileProvider.getUriForFile(this,
                    "pt.ulisboa.tecnico.cmov.shopist",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraResultLauncher.launch(takePictureIntent);
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();    //Call the back button's method
            return true;
        }// If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra("PATHS", photoPaths);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}