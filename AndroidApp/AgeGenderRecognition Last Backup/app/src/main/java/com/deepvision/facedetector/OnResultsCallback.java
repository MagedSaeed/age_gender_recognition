package com.deepvision.facedetector;

import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;

interface OnResultsCallback {
    void faceDetectionResults(SparseArray<Face> face, float angle, float incrementer);

    void predictionResults(String[] results);
}
