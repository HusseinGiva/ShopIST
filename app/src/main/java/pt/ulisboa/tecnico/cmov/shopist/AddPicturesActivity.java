package pt.ulisboa.tecnico.cmov.shopist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AddPicturesActivity extends AppCompatActivity implements PicturesFragment.OnListFragmentInteractionListener {

    Button addNewPicture;
    Button browsePicturesButton;
    ActivityResultLauncher<Intent> cameraResultLauncher;
    ActivityResultLauncher<Intent> galleryResultLauncher;
    String currentPhotoPath;
    ArrayList<String> photoPaths = new ArrayList<>();
    private RecyclerView.Adapter recyclerViewAdapter;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pictures);
        Toolbar myToolbar = findViewById(R.id.addPicturesToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        photoPaths = getIntent().getStringArrayListExtra("PATHS");
        if (recyclerViewAdapter == null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.picturesFragment);
            recyclerView = (RecyclerView) currentFragment.getView();
            recyclerViewAdapter = recyclerView.getAdapter();
        }
        PictureContent.emptyList();
        recyclerViewAdapter.notifyDataSetChanged();
        if (!photoPaths.isEmpty()) {
            for (String s : photoPaths) {
                PictureContent.loadImage(new File(s));
                recyclerViewAdapter.notifyItemInserted(0);
            }
        }
        addNewPicture = findViewById(R.id.addNewPicture);
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
        browsePicturesButton = findViewById(R.id.browsePicturesButton);
        browsePicturesButton.setOnClickListener(v -> onClickBrowseButton());
        galleryResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri selectedImageUri = result.getData().getData();
                        try {
                            String filePath = getApplicationContext().getApplicationInfo().dataDir + File.separator
                                    + System.currentTimeMillis();
                            File file = new File(filePath);
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
    }

    void onClickBrowseButton() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryResultLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
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