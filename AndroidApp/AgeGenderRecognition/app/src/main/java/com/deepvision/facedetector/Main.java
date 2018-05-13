package com.deepvision.facedetector;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


public class Main extends AppCompatActivity {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    final int CAMERA_REQUEST_CODE = 3;
    final int STORAGE_REQUEST_CODE = 4;
    // initialize global fields of the app.
    ImageView viewImage;
    ImageView viewImageFacesDetected;
    Button selectImageButton;
    Button detectFaceButton;
    Bitmap img;

    String imageName;
    Button predButtonDex;
    Button predButtonOurs;

    // rotate bitmap image to a given angle.
    static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // define some fields.
        viewImage = findViewById(R.id.viewImage);
        viewImageFacesDetected = findViewById(R.id.viewImageDetectedFaces);
        selectImageButton = findViewById(R.id.btnSelectPhoto);
        detectFaceButton = findViewById(R.id.dFaces);

        predButtonDex = findViewById(R.id.pred_disabled_button_dex);
        predButtonDex.setVisibility(View.VISIBLE);
        predButtonDex.setEnabled(false);

        predButtonOurs = findViewById(R.id.pred_disabled_button_ours);
        predButtonOurs.setVisibility(View.VISIBLE);
        predButtonOurs.setEnabled(false);

        // enable the button only when there is image on the image view.
        detectFaceButton.setEnabled(viewImage.getDrawable() != null);


        // to ask for permission at runtime:
        int cameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            Log.i("Camera Permissions:", "Permission to record denied");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        }

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Log.i("Storage Permission", "Storage Permission not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }


        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        detectFaceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (img != null)
                    detectAndPredictFaces(img);
                else
                    Toast.makeText(Main.this, "There is no image!!", Toast.LENGTH_LONG).show();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds options to the action bar if it is present.

//        getMenuInflater().inflate(R.menu.main, menu);

        return true;

    }

    private void selectImage() {

        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //save image on a temp file.
                    // The image name is "temp[current timestamp].jpg".
                    Date d = new Date();
                    imageName = "temp" + d.getTime() + ".jpg";
                    File f = new File(Environment.getExternalStorageDirectory(), imageName);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, 1);
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // enable detectFaceButton
            detectFaceButton.setEnabled(true);


            if (requestCode == 1) {
                // get image from camera
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals(imageName)) {
                        f = temp;
                        break;
                    }
                }
                try {
                    final Bitmap bitmap;
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            bitmapOptions);
                    img = fixBitmapOrientation(bitmap, f.getAbsolutePath());
                    viewImage.setImageBitmap(img);

                    // delete temp image.
                    f.delete();


                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (requestCode == 2) {

                // get image from gallery
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                assert selectedImage != null;
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                assert c != null;
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                img = fixBitmapOrientation(thumbnail, picturePath);
                Log.w("Image Path from Gallery", picturePath + "");
                viewImage.setImageBitmap(img);
            }
        }
    }

    private Bitmap fixBitmapOrientation(Bitmap thumbnail, String picturePath) {
        // code to rotate image if it is not rotated properly when it is taken from camera.
        try {
            ExifInterface exif = new ExifInterface(picturePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true); // rotating bitmap
        } catch (Exception ignored) {

        }

        return thumbnail;

    }

    // method to detect faces.
    // the face detection api is google vision.

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i("Permissions", "Permission has been denied by user");
                } else {
                    Log.i("Permissions", "Permission has been granted by user");
                }
                return;
            }

            case STORAGE_REQUEST_CODE: {
                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED)

                    Log.i("Permissions", "Permission has been denied by user");

                else
                    Log.i("Permissions", "Permission has been granted by user");
            }
        }
    }

    private void detectAndPredictFaces(final Bitmap img) {
        // face detector class coming from the google library.
        final FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        // check if the class is working, if not show a toast and, automatically, return.
        if (!faceDetector.isOperational())
            Toast.makeText(Main.this, "Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();

        else {

            // This class is added to provide multithreading and progress bar.
            // the progress bar will not be working outside a thread.
            // AsyncTask is a light threading strategy created by android developers.

            FaceDetectionTask detector = new FaceDetectionTask(faceDetector, Main.this, 90, new OnResultsCallback() {
                // this callback will be triggered when the detection task completes.
                @Override
                public void faceDetectionResults(SparseArray<Face> facesArray, float angle, float incrementer) {

                    // get the painter ready to draw rectangles on the image.

                    final Paint rectPaint = new Paint();
                    Bitmap tempBitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(tempBitmap);
                    canvas.drawBitmap(img, 0, 0, null);
                    rectPaint.setStrokeWidth(5);
                    rectPaint.setColor(Color.RED);
                    rectPaint.setStyle(Paint.Style.STROKE);

                    Log.v("OUTPUTC", facesArray.size() + "");// log cat the number of detected faces.

                    // create array of bitmaps to hold the faces.
                    final Bitmap faces[] = new Bitmap[facesArray.size()];


                    if (facesArray.size() != 0) {
                        // for each face, draw a rectangle
                        for (int i = 0; i < facesArray.size(); i++) {
                            Face face = facesArray.valueAt(i);
                            float x1 = face.getPosition().x >= 0 ? face.getPosition().x : 0;
                            float y1 = face.getPosition().y >= 0 ? face.getPosition().y : 0;
                            float x2 = (x1 + face.getWidth()) >= img.getWidth() ? img.getWidth() : (x1 + face.getWidth());
                            float y2 = (y1 + face.getHeight()) >= img.getHeight() ? img.getHeight() : (y1 + face.getHeight());
                            RectF rectF = new RectF(x1, y1, x2, y2);
                            faces[i] = Bitmap.createBitmap(img, (int) x1, (int) y1,
                                    (int) (face.getWidth()), (int) (face.getHeight()));
                            canvas.drawRoundRect(rectF, 1, 1, rectPaint);
                        }

                        // restore the bitmap to their original orientation before rotations and display on the image view.
                        tempBitmap = rotateBitmap(tempBitmap, -(angle - incrementer));

                        // same thing is applied to faces
                        for (int i = 0; i < faces.length; i++)
                            faces[i] = rotateBitmap(faces[i], -(angle - incrementer));

                        // show the image with faces detected on the screen.
                        viewImageFacesDetected.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
//                        viewImageFacesDetected.setImageDrawable(new BitmapDrawable(getResources(), faces[0]));


                        // predict using the model.
                        final AgeGenderPredictionDexModel dexPredictor = new AgeGenderPredictionDexModel(Main.this, getAssets(), "model.pb", new OnResultsCallback() {
                            @Override
                            public void faceDetectionResults(SparseArray<Face> face, float angle, float incrementer) {
                                // silence is golden
                            }

                            @Override
                            public void predictionResults(String[] results) {

                                ExpandableHeightGridView gridViewDex = findViewById(R.id.grid_view_dex);
                                gridViewDex.setExpanded(true);

                                Bitmap tempBitmaps[] = new Bitmap[results.length];

                                for (int i = 0; i < results.length; i++) {

                                    //draw predictions on the faces:
                                    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

                                    //canvas.drawText("text", 0, y, textPaint);
                                    //textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
                                    tempBitmaps[i] = Bitmap.createBitmap(faces[i].getWidth(), faces[i].getHeight(), Bitmap.Config.RGB_565);
                                    Canvas canvas = new Canvas(tempBitmaps[i]);
                                    canvas.drawBitmap(faces[i], 0, 0, textPaint);

                                }
                                gridViewDex.setAdapter(new GridImagesAdapter(Main.this, tempBitmaps, results));

                            }

                        });

                        final AgeGenderPredictionOurModel ourPredictor = new AgeGenderPredictionOurModel(Main.this, getAssets(), "frozen_model_a_wiki.pb", "frozen_model_g.pb", new OnResultsCallback() {
                            @Override
                            public void faceDetectionResults(SparseArray<Face> face, float angle, float incrementer) {

                            }

                            @Override
                            public void predictionResults(String[] results) {

                                ExpandableHeightGridView gridViewOurs = findViewById(R.id.grid_view_ours);
                                gridViewOurs.setExpanded(true);

                                Bitmap tempBitmaps[] = new Bitmap[results.length];

                                for (int i = 0; i < results.length; i++) {

                                    //draw predictions on the faces:
                                    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
                                    tempBitmaps[i] = Bitmap.createBitmap(faces[i].getWidth(), faces[i].getHeight(), Bitmap.Config.RGB_565);
                                    Canvas canvas = new Canvas(tempBitmaps[i]);
                                    canvas.drawBitmap(faces[i], 0, 0, textPaint);


                                }
                                gridViewOurs.setAdapter(new GridImagesAdapter(Main.this, tempBitmaps, results));

                            }

                        });


                        predButtonDex.setVisibility(View.VISIBLE);
                        predButtonDex.setEnabled(true);

                        predButtonDex.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                dexPredictor.execute(faces);
                            }
                        });

                        predButtonOurs.setVisibility(View.VISIBLE);
                        predButtonOurs.setEnabled(true);
                        predButtonOurs.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                ourPredictor.execute(faces);
                            }
                        });

                    }
                    // if there is no faces detected, get "no-faces-deteceted.png" from the assets folder.
                    else {
                        AssetManager assetManager = getAssets();
                        try {
                            InputStream ims = assetManager.open("no-face-detected.png");
                            Drawable no_face_detected_image = Drawable.createFromStream(ims, null);
                            viewImageFacesDetected.setImageDrawable(no_face_detected_image);
                            Log.v("OUTPUTC", getResources().getIdentifier("no-face-detected.png", "drawable", getPackageName()) + "");

                        } catch (IOException ex) {
                            Toast.makeText(Main.this, "some problems occurred. Anyway, there is no face detected", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void predictionResults(String[] results) {
                    // silence is golden
                }
            });

            detector.execute(img);

        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}