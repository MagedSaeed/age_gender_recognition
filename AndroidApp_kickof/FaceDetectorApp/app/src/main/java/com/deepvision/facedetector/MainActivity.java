package com.deepvision.facedetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.graphics.Bitmap.createBitmap;
import static android.graphics.Bitmap.createScaledBitmap;


public class MainActivity extends AppCompatActivity {
    // initialize global fields of the app.
    ImageView viewImage;
    ImageView viewImageFacesDetected;
    Button selectImageButton;
    Button detectFaceButton;
    Bitmap img;
    TensorFlowInferenceInterface inferenceInterface;
    String imageName;
    Button predButton;

    final int CAMERA_REQUEST_CODE = 3;
    final int STORAGE_REQUEST_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // define some fields.
        viewImage = findViewById(R.id.viewImage);
        viewImageFacesDetected = findViewById(R.id.viewImageDetectedFaces);
        selectImageButton = findViewById(R.id.btnSelectPhoto);
        detectFaceButton = findViewById(R.id.dFaces);
        predButton = findViewById(R.id.pred_disabled_button);
        predButton.setVisibility(View.INVISIBLE);

        // enable the button only when there is image on the image view.
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

        // import the model
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), "model.pb");
        detectFaceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(img !=null)
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
                    //save image on a temp file.
                    // The image name is "temp[current timestamp].jpg".
                    Date d = new Date();
                    imageName = "temp"+d.getTime()+".jpg";
                    File f = new File(Environment.getExternalStorageDirectory(), imageName);
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



                    // this code might be added in case of saving the image after manipulation.
                    // I GUESS !! I GUESS !! I GUESS !! I GUESS !!

//                    String path = android.os.Environment
//                            .getExternalStorageDirectory()
//                            + File.separator
//                            + "Phoenix" + File.separator + "default";
//
//                    OutputStream outFile;
//
//                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
//                    try {
//                        outFile = new FileOutputStream(file);
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
//                        outFile.flush();
//                        outFile.close();
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (requestCode == 2) {

                // get image from gallery
                Uri selectedImage = data.getData();
                String[] filePath = { MediaStore.Images.Media.DATA };
                assert selectedImage != null;
                Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
                assert c != null;
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                img = fixBitmapOrientation(thumbnail, picturePath);
                Log.w("Image Path from Gallery", picturePath+"");
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
            }
            else if (orientation == 3) {
                matrix.postRotate(180);
            }
            else if (orientation == 8) {
                matrix.postRotate(270);
            }
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true); // rotating bitmap
        }
        catch (Exception ignored) {

        }

        // reduce the image size to 25% if it is greater than 100 in width or to 70% it it is less.
        int height,width;
        if(thumbnail.getHeight()<1000 || thumbnail.getWidth() <1000) {
            height = (int) (thumbnail.getHeight());
            width = (int) (thumbnail.getWidth());
        }
        else{
            height = (int) (thumbnail.getHeight()*0.30);
            width = (int) (thumbnail.getWidth()*0.30);
        }

        return createScaledBitmap(thumbnail, width, height, true);


//        // new height and width for the image to be suitable for the face detection algorithm.
//           It destructs the image though.

//        int height,width, orgHight, orgWidth;
//        width = 640; // targeted width of the image
//        orgHight = thumbnail.getHeight();
//        orgWidth = thumbnail.getWidth();
//        height = orgHight/(orgWidth/width); // targeted height.
//        Bitmap scaledImage;
//        if(orgWidth>width)
//            scaledImage = Bitmap.createScaledBitmap(thumbnail, width, height, true);
//        else
//            scaledImage = thumbnail;
//        return scaledImage;
    }

    // rotate bitmap image to a given angle.
    private Bitmap rotateBitmap(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


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

    // method to detect faces.
    // the face detection api is google vision.

    private void detectFaces(Bitmap img) {
        // face detector class coming from the google library.
        final FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        // check if the class is working, if not show a toast and, automatically, return.
        if(!faceDetector.isOperational())
            Toast.makeText(MainActivity.this, "Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();

        else {

            @SuppressLint("StaticFieldLeak")
            // This class is added to provide multithreading and progress bar.
                // the progress bar will not be working outside a thread.
                // AsyncTask is a light threading strategy created by android developers.

            class DetectFaces extends AsyncTask<Bitmap, Float, SparseArray<Face>> {
                // class fields
                private Frame frame;
                private SparseArray<Face> sparseArray;
                private ProgressDialog pr = new ProgressDialog(MainActivity.this);
                private Bitmap suitableImage;
                private float angle;
                private float incrementer = 90; // incrementer on the image rotation.

                @Override
                protected void onPreExecute() {
                    // show the progress bar on the pre-execution of the face detector.
                    pr.setTitle("Face Detection");
                    pr.setMessage("Detecting faces in the image, please wait!");
                    pr.show();
                }


                @Override
                protected SparseArray<Face> doInBackground(Bitmap... bitmaps) {
                    frame = new Frame.Builder().setBitmap(bitmaps[0]).build();
                    sparseArray = faceDetector.detect(frame);
                    angle = incrementer;
                    suitableImage = bitmaps[0];
                    while(sparseArray.size()==0 && angle <= 360) {
                        suitableImage = rotateBitmap(bitmaps[0], angle);
                        frame = new Frame.Builder().setBitmap(suitableImage).build();
                        sparseArray = faceDetector.detect(frame);
                        publishProgress(angle);
                        angle += incrementer;
                    }
                    return sparseArray;
                }

                @Override
                protected void onProgressUpdate(Float... values) {
                    super.onProgressUpdate(values);
                    pr.setMessage("Original Image does not contain any frontal faces." +
                            " Trying to rotate image" +
                            " to:" + values[0]+" and detect faces!!");
                }

                @Override
                protected void onPostExecute(SparseArray<Face> faceSparseArray) {
                    pr.setMessage("Faces are detected at angle: "+(angle-incrementer));
                    pr.dismiss();

                    // get the painter ready to draw rectangles on the image.
                    Paint rectPaint = new Paint();
                    Bitmap tempBitmap = Bitmap.createBitmap(suitableImage.getWidth(),suitableImage.getHeight(), Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(tempBitmap);
                    canvas.drawBitmap(suitableImage,0,0,null);
                    rectPaint.setStrokeWidth(5);
                    rectPaint.setColor(Color.RED);
                    rectPaint.setStyle(Paint.Style.STROKE);

                    Log.v("OUTPUTC", sparseArray.size()+"");// log cat the number of detected faces.

                    // create array of bitmaps to hold the faces.
                    Bitmap faces [] = new Bitmap[sparseArray.size()];

                    if(faceSparseArray.size() != 0) {
                        // for each face, draw a rectangle
                        for (int i = 0; i < sparseArray.size(); i++) {
                            Face face = sparseArray.valueAt(i);
                            float x1 = face.getPosition().x;
                            float y1 = face.getPosition().y;
                            float x2 = x1 + face.getWidth();
                            float y2 = y1 + face.getHeight();
                            RectF rectF = new RectF(x1, y1, x2, y2);
                            faces[i] = Bitmap.createBitmap(suitableImage, (int)x1, (int)y1,
                                            (int)face.getWidth(), (int)face.getHeight());
                            canvas.drawRoundRect(rectF, 1, 1, rectPaint);
//
                        }

                        // restore the bitmap to their original orientation before rotations and display on the image view.
                        tempBitmap = rotateBitmap(tempBitmap, -(angle-incrementer));

                        // same thing is applied to faces as well
                        for(int i=0; i<faces.length; i++)
                            faces[i] = rotateBitmap(faces[i], -(angle-incrementer));

                        // show the image with faces detected on the screen.
                        viewImageFacesDetected.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
//                        viewImageFacesDetected.setImageDrawable(new BitmapDrawable(getResources(), faces[0]));

                        // predict using the model.
                        predictAge(faces);
                    }
                    // if there is no faces detected, get "no-faces-deteceted.png" from the assets folder.
                    else{
                        AssetManager assetManager = getAssets();
                        try {
                            InputStream ims = assetManager.open("no-face-detected.png");
                            Drawable no_face_detected_image = Drawable.createFromStream(ims, null);
                            viewImageFacesDetected.setImageDrawable(no_face_detected_image);
                            Log.v("OUTPUTC", getResources().getIdentifier("no-face-detected.png","drawable",getPackageName())+"");

                        }
                        catch (IOException ex) {
                            Toast.makeText(MainActivity.this,"some problems occurred. Anyway, there is no face detected", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            new DetectFaces().execute(img);

        }
    }

    public void predictAge(Bitmap[] faces) {
        int INPUT_SIZE = 64;
//        String MODEL_FILE = "opt_frozen_model.pb";
//        String IMAGE_FILE="file:///android_asset/1.jpg";
//        String INPUT_NAME = "batch_processing/Reshape:0";
//        String OUTPUT_NAME = "output/output:0";
        String INPUT_NAME = "input_1";
        String OUTPUT_NAME_GENDER = "dense_1/Softmax";
        String OUTPUT_NAME_AGE = "dense_2/Softmax";
        String[] OUTPUT_NAMES = {OUTPUT_NAME_AGE, OUTPUT_NAME_GENDER};
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        float[] floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];
        Bitmap [] tempBitmaps = new Bitmap[faces.length];


        for(int i=0; i<faces.length; i++) {
            Bitmap bitmap = createScaledBitmap(faces[i], INPUT_SIZE, INPUT_SIZE, true);

            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            for (int j = 0; j < intValues.length; ++j) {
                final int val = intValues[j];

//            floatValues[i * 3 + 0] = ((intValues[i] >> 16) & 0xFF ) / 255.0f;
//            floatValues[i * 3 + 1] = ((intValues[i] >> 8) & 0xFF ) / 255.0f;
//            floatValues[i * 3 + 2] = (intValues[i] & 0xFF ) / 255.0f;

                floatValues[j * 3 + 0] = ((val >> 16) & 0xFF);
                floatValues[j * 3 + 1] = ((val >> 8) & 0xFF);
                floatValues[j * 3 + 2] = (val & 0xFF);

                floatValues[j * 3 + 2] = Color.red(val);
                floatValues[j * 3 + 1] = Color.green(val);
                floatValues[j * 3] = Color.blue(val);
            }

//        Log.v("OUTPUTC", "FLOAT_VALUES: "+Arrays.toString(floatValues));


            inferenceInterface.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE, INPUT_SIZE, 3);

            inferenceInterface.run(OUTPUT_NAMES, false);

            float[] outputs_gender = new float[2];
            inferenceInterface.fetch(OUTPUT_NAME_GENDER, outputs_gender);

            float[] outputs_age = new float[101];
            inferenceInterface.fetch(OUTPUT_NAME_AGE, outputs_age);

            //multiply each age by its probability:
            for(int k=0; k<outputs_age.length; k++)
                outputs_age[k] *= k;

            String gender = outputs_gender[0]>0.5?"F":"M";

            float maxAgeProb = outputs_age[0];
            for(int k=1; k<outputs_age.length; k++)
                if(outputs_age[k]>maxAgeProb)
                    maxAgeProb = outputs_age[k];

            float maxAge = 0;
            for(int k=1; k<outputs_age.length; k++) {
                if (outputs_age[k] == maxAgeProb) {
                    maxAge = k;
                    break;
                }
            }


            //draw predictions on the faces:
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            textPaint.setColor(Color.RED);
            textPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));
            textPaint.setTextAlign(Paint.Align.LEFT);
            Paint.FontMetrics metric = textPaint.getFontMetrics();
            int textHeight = (int) Math.ceil(metric.descent - metric.ascent);
            int y = (int)(textHeight - metric.descent);
//            canvas.drawText("text", 0, y, textPaint);
//            textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
            tempBitmaps[i] = Bitmap.createBitmap(faces[i].getWidth(),faces[i].getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(tempBitmaps[i]);
            canvas.drawBitmap(faces[i],0,0,textPaint);
            canvas.drawText(gender+", "+maxAge, 0, y, textPaint);


//        Log.v("OUTPUTC", Arrays.toString(outputs));

//        Arrays.sort(outputs_age);
//        Log.v("OUTPUTC", Arrays.toString(outputs_gender));
//        Log.v("OUTPUTC", Arrays.toString(outputs_age));

        }

        predButton.setVisibility(View.VISIBLE);
        predButton.setEnabled(false);
        GridView gridView = findViewById(R.id.grid_view);

        gridView.setAdapter(new ImageAdapter(MainActivity.this, tempBitmaps));
    }

    static {
        System.loadLibrary("tensorflow_inference");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}


class ImageAdapter extends BaseAdapter {
    private Context mContext;

    // all Images in array
    private Bitmap[] images;

    // Constructor
    public ImageAdapter(Context c){
        mContext = c;
    }

    public ImageAdapter(Context c, Bitmap[] images){
        this.mContext = c;
        this.images = images;
    }

    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public Object getItem(int position) {
        return images[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext);
        imageView.setImageBitmap(images[position]);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return imageView;
    }   }