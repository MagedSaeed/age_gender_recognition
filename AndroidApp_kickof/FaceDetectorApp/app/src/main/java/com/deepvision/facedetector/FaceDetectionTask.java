package com.deepvision.facedetector;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

class FaceDetectionTask extends AsyncTask<Bitmap, Float, SparseArray<Face>> {
    // class fields
    private Frame frame;
    private SparseArray<Face> sparseArray;
    private ProgressDialog pr;
    private Bitmap img;
    private float angle;
    private float incrementer; // incrementer on the image rotation.
    private OnResultsCallback callback;
    private FaceDetector faceDetector;

    FaceDetectionTask(FaceDetector faceDetector, Context context, float angleIncrementer,
                      OnResultsCallback callback) {
        this.faceDetector = faceDetector;
        this.incrementer = angleIncrementer;
        this.callback = callback;
        pr = new ProgressDialog(context);
    }

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
        img = bitmaps[0];
        while (sparseArray.size() == 0 && angle <= 360) {
            img = Main.rotateBitmap(bitmaps[0], angle);
            frame = new Frame.Builder().setBitmap(img).build();
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
                " to:" + values[0] + " and detect faces!!");
    }

    @Override
    protected void onPostExecute(SparseArray<Face> faceSparseArray) {
        pr.setMessage("Faces are detected at angle: " + (angle - incrementer));
        pr.dismiss();


        callback.faceDetectionResults(faceSparseArray, angle, incrementer);


    }
}