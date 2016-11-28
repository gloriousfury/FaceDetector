package com.gloriousfury.facedetector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.R.attr.bitmap;
import static android.provider.CalendarContract.CalendarCache.URI;

import static com.gloriousfury.facedetector.R.id.imageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView pic;
   private Bitmap defaultBitmap, selectedBitmap;
    Bitmap temporaryBitmap;
    private Canvas canvas;
    private Paint rectPaint;
    private Bitmap eyePatchBitmap, moustacheBitmap;
    private String TAG ="Bitmap";
    String ImagePath;
    Uri URI;
    Button processButton, savePicture, getCamera, getGallery;
    private static final int SELECT_PHOTO = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pic = (ImageView) findViewById(imageView);
        processButton = (Button) findViewById(R.id.btnProcess);
        savePicture = (Button) findViewById(R.id.btnProcess);
        getCamera = (Button) findViewById(R.id.btnFromCamera);
        getGallery = (Button) findViewById(R.id.btnGallery);



        processButton.setOnClickListener(this);
        savePicture.setOnClickListener(this);
        getGallery.setOnClickListener(this);
        getCamera.setOnClickListener(this);






    }

    private void processImage() {

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;

        InitializeBitMaps(bitmapOptions);
        createRectanglePaint();

        canvas = new Canvas(temporaryBitmap);
        canvas.drawBitmap(defaultBitmap, 0, 0, null);


        FaceDetector faceDetector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        if (!faceDetector.isOperational()) {
            new AlertDialog.Builder(this)
                    .setMessage("Face Detector could not be set up on your device :(")
                    .show();
        } else {

            Frame frame = new Frame.Builder().setBitmap(defaultBitmap).build();
            //Major Key here
            SparseArray<Face> sparseArray = faceDetector.detect(frame);
            DetectFaces(sparseArray);
            pic.setImageDrawable(new BitmapDrawable(getResources(), temporaryBitmap));
            faceDetector.release();





            return;

        }


    }


    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.btnProcess:
                processImage();

                break;

            case R.id.btnGallery:

                fromGallery();

                break;


            case R.id.btnSaveImage:

                if(temporaryBitmap != null) {

                    saveImageToExternalStorage(temporaryBitmap);

                }
                break;

            case R.id.btnFromCamera:



                break;


        }




    }


    public void fromGallery(){

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();

                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    selectedBitmap = BitmapFactory.decodeStream(imageStream);

                    pic.setImageDrawable(new BitmapDrawable(getResources(), selectedBitmap));
                }
        }


    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }


    public void InitializeBitMaps(BitmapFactory.Options bitmapOptions) {

        if(selectedBitmap != null){
            defaultBitmap = selectedBitmap;
        }else {
            defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.friends,
                    bitmapOptions);
        }

        temporaryBitmap = Bitmap.createBitmap(defaultBitmap.getWidth(),
                defaultBitmap.getHeight(), Bitmap.Config.RGB_565);
        eyePatchBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eye_patch,
                bitmapOptions);

        moustacheBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.moustache,
                bitmapOptions);

    }


    public void createRectanglePaint(){

        rectPaint = new Paint();
        rectPaint.setStrokeWidth(7);
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);



    }


    public void DetectFaces(SparseArray<Face> sparseArray) {

        for (int i = 0; i < sparseArray.size(); i++) {
            Face face = sparseArray.valueAt(i);

            float left = face.getPosition().x;
            float top = face.getPosition().y;
            float right = left + face.getWidth();
            float bottom = right + face.getHeight();
            float cornerRadius = 2.0f;

            RectF rectF = new RectF(left, top, right, bottom);

            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, rectPaint);
            detectLandmarks(face);
        }
    }



    private void detectLandmarks(Face face) {
        for (Landmark landmark : face.getLandmarks()) {

            int cx = (int) (landmark.getPosition().x);
            int cy = (int) (landmark.getPosition().y);

//            canvas.drawCircle(cx, cy, 10, rectPaint);

//            drawLandmarkType(landmark.getType(), cx, cy);
            drawEyePatchBitmap(landmark.getType(), cx, cy);
            drawMoustache(landmark.getType(), cx, cy);

        }
    }

    private void drawEyePatchBitmap(int landmarkType, float cx, float cy) {

        if (landmarkType ==  4) {
            // TODO: Optimize so that this calculation is not done for every face
            int scaledWidth = eyePatchBitmap.getScaledWidth(canvas);
            int scaledHeight = eyePatchBitmap.getScaledHeight(canvas);
            canvas.drawBitmap(eyePatchBitmap, cx - (scaledWidth / 2), cy - (scaledHeight / 2), null);
        }
    }

    private void drawMoustache(int landmarkType, float cx, float cy) {

        if (landmarkType ==  6) {
            // TODO: Optimize so that this calculation is not done for every face
            int scaledWidth = moustacheBitmap.getScaledWidth(canvas);
            int scaledHeight = moustacheBitmap.getScaledHeight(canvas);
            canvas.drawBitmap(moustacheBitmap, cx - (scaledWidth / 2), cy - (scaledHeight / 2), null);

        }
    }






        private void drawLandmarkType(int landmarkType, float cx, float cy) {
            String type = String.valueOf(landmarkType);
            rectPaint.setTextSize(50);
            canvas.drawText(type, cx, cy, rectPaint);
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

    private void storeImage(Bitmap image) {
        ImagePath = MediaStore.Images.Media.insertImage(getContentResolver(),
                temporaryBitmap,
                "demo_image",
                "demo_image"
        );

        URI = Uri.parse(ImagePath);

        Toast.makeText(MainActivity.this, "Image Saved Successfully", Toast.LENGTH_LONG).show();

    }


    public final static String APP_PATH_SD_CARD = "/FaceDetector/";
    public final static String APP_THUMBNAIL_PATH_SD_CARD = "thumbnails";

    public boolean saveImageToExternalStorage(Bitmap image) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD + APP_THUMBNAIL_PATH_SD_CARD;

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            File file = new File(fullPath, "desiredFilename.png");
            file.createNewFile();
            fOut = new FileOutputStream(file);

// 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            Toast saved = Toast.makeText(this, "Saved successfully", Toast.LENGTH_LONG);
            saved.show();
            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }


}
