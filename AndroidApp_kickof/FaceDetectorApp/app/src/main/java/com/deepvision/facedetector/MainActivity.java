package com.deepvision.facedetector;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    ImageView viewImage;
    ImageView viewImageFacesDetected;
    Button selectImageButton;
    Button editImageButton;
    Button detectFaceButton;
    Bitmap img;

    final int CAMERA_REQUEST_CODE = 3;
    final int STORAGE_REQUEST_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewImage = findViewById(R.id.viewImage);
        viewImageFacesDetected = findViewById(R.id.viewImageDetectedFaces);
        selectImageButton = findViewById(R.id.btnSelectPhoto);
//        editImageButton = findViewById(R.id.mImage);
        detectFaceButton = findViewById(R.id.dFaces);

//        editImageButton.setEnabled(viewImage.getDrawable()!=null);
        detectFaceButton.setEnabled(viewImage.getDrawable()!=null);

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

        if (storagePermission != PackageManager.PERMISSION_GRANTED){
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
                // Perform action on click
                if(img !=null)
//                    goToDetectFaceActivity(img);
                    detectFaces(img);

                else
                    Toast.makeText(MainActivity.this, "There is no image!!", Toast.LENGTH_LONG).show();

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

        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, 1);
                }

                else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                }

                else if (options[item].equals("Cancel")) {
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
//            editImageButton.setEnabled(true);
            detectFaceButton.setEnabled(true);
            if (requestCode == 1) {
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    final Bitmap bitmap;
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            bitmapOptions);
                    img = rotateBitmap(bitmap, f.getAbsolutePath());
                    viewImage.setImageBitmap(img);
                    String path = android.os.Environment
                            .getExternalStorageDirectory()
                            + File.separator
                            + "Phoenix" + File.separator + "default";
                    f.delete();
                    OutputStream outFile = null;
                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
                    try {
                        outFile = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                        outFile.flush();
                        outFile.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (requestCode == 2) {

                Uri selectedImage = data.getData();
                String[] filePath = { MediaStore.Images.Media.DATA };
                Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                img = rotateBitmap(thumbnail, picturePath);
                Log.w("Image Path from Gallery", picturePath+"");
                viewImage.setImageBitmap(img);

            }
        }
    }

    private Bitmap rotateBitmap(Bitmap thumbnail, String picturePath) {
        // code to rotate image if it is not rotated properly.
        try {
            ExifInterface exif = new ExifInterface(picturePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            }
            else if (orientation == 3) {
                matrix.postRotate(180);
            }
            else if (orientation == 8) {
                matrix.postRotate(270);
            }
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true); // rotating bitmap
        }
        catch (Exception e) {

        }


//        int height = (int) ( thumbnail.getHeight() * (640 / thumbnail.getWidth()) );
        // new height and width for the image to be suitable for the face detection algorithm.
        int height,width;
        if(thumbnail.getHeight()<1000 || thumbnail.getWidth() <1000) {
            height = (int) (thumbnail.getHeight()*0.75);
            width = (int) (thumbnail.getWidth()*0.75);
        }
        else{
            height = (int) (thumbnail.getHeight()*0.30);
            width = (int) (thumbnail.getWidth()*0.30);
        }


        Bitmap scaled = Bitmap.createScaledBitmap(thumbnail, width, height, true);
        return scaled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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

    private void detectFaces(Bitmap img) {
        Paint rectPaint = new Paint();
        final Bitmap tempBitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(img,0,0,null);

        rectPaint.setStrokeWidth(5);
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);

        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        if(!faceDetector.isOperational())
        {
            Toast.makeText(MainActivity.this, "Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            Frame frame = new Frame.Builder().setBitmap(img).build();
            SparseArray<Face> sparseArray = faceDetector.detect(frame);

            for (int i = 0; i < sparseArray.size(); i++) {
                Face face = sparseArray.valueAt(i);
                float x1 = face.getPosition().x;
                float y1 = face.getPosition().y;
                float x2 = x1 + face.getWidth();
                float y2 = y1 + face.getHeight();
                RectF rectF = new RectF(x1, y1, x2, y2);
                canvas.drawRoundRect(rectF, 2, 2, rectPaint);
            }

            viewImageFacesDetected.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
