package com.chesak.adam.boilermake2017recipeapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Camera request code
    private final static int CAMERA_RQ = 2017;
    private final static int RESULT_LOAD_IMG_CODE = 1738;
    public static RecipeList recipeList = null;
    public static IO io = null;
    public Bitmap pic;

    public String imageString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (io == null) {
            io = new IO();
            recipeList = io.readData(MainActivity.this);
        }

        // Make sure the data directory exists
        File saveDir = new File(Environment.getExternalStorageDirectory(), "BoilerMake2017RecipeApp");
        saveDir.mkdirs();

        // Take a new picture
        Button takePictureButton = (Button) findViewById(R.id.main_take_picture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialCamera(MainActivity.this)
                        .stillShot()
                        .start(CAMERA_RQ);
            }
        });

        //get picture from gallery
        Button getPictureButton = (Button) findViewById(R.id.main_use_existing);
        getPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create intent to Open Image applications like Gallery, Google Photos
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG_CODE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Context context = getApplicationContext();

        try{
            // Image selected
            if (requestCode == RESULT_LOAD_IMG_CODE) {
                if (resultCode == RESULT_OK) {
                    //tell the user how things are going
                    Toast.makeText(this, "Getting text from image...",
                            Toast.LENGTH_LONG).show();

                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();


                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);




                    //get the text of the image
                    String text = OCR.getText(MainActivity.this, bitmap);

                    // Build a new recipe
                    //Recipe recipe = Recipe.parseRecipe(value);
                    Recipe recipe = Demo.getDemoRecipe(context, bitmap);
                    MainActivity.recipeList.add(recipe);
                    MainActivity.io.saveData(MainActivity.this);

                    // Go to the recipe screen
                    //Pass in the picked up text into that activity
                    Intent dataIntent = new Intent(MainActivity.this, RecipeActivity.class);
                    dataIntent.putExtra("recipe", recipe);
                    dataIntent.putExtra("text", text);
                    startActivity(dataIntent);

                }else {
                        Toast.makeText(this, "Please select an image",
                                Toast.LENGTH_LONG).show();
                }
            }



            // Received recording or error from MaterialCamera
            if  (requestCode == CAMERA_RQ) {
                if (resultCode == RESULT_OK) {
                    // Load the bitmap
                    final File file = new File(data.getData().getPath());
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                    // OCR: get the text of the image
                    String text = OCR.getText(MainActivity.this, bitmap);

                    // Build a new recipe
                    //Recipe recipe = Recipe.parseRecipe(value);
                    Recipe recipe = Demo.getDemoRecipe(context, bitmap);
                    MainActivity.recipeList.add(recipe);
                    MainActivity.io.saveData(MainActivity.this);

                    // Go to the recipe screen
                    //Pass in the picked up text into that activity
                    Intent dataIntent = new Intent(MainActivity.this, RecipeActivity.class);
                    dataIntent.putExtra("recipe", recipe);
                    dataIntent.putExtra("text", text);
                    startActivity(dataIntent);

                } else if (data != null) {

                    // Error here! Ruh Roh...
                    Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e ){
            Toast.makeText(this, "Error: " + e, Toast.LENGTH_LONG)
                    .show();
        }
    }
}
