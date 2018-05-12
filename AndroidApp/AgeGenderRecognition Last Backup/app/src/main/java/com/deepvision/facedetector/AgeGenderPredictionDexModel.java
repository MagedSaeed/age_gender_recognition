package com.deepvision.facedetector;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.graphics.Bitmap.createScaledBitmap;

class AgeGenderPredictionDexModel extends AsyncTask<Bitmap[], Void, String[]> {
    // class fields


    private ProgressDialog pr;
    private OnResultsCallback callback;
    private AssetManager assets;
    private String modelName;

    AgeGenderPredictionDexModel(Context context, AssetManager assets, String modelName, OnResultsCallback callback) {
        this.callback = callback;
        this.assets = assets;
        this.modelName = modelName;
        pr = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        // show the progress bar on the pre-execution of the face detector.
        pr.setTitle("Age Gender Prediction Task");
        pr.setMessage("Prediction Task Starts. Please wait..,");
        pr.setCanceledOnTouchOutside(false);
        pr.show();
    }


    @Override
    protected String[] doInBackground(Bitmap[]... faces) {
        return predict(faces[0], modelName, assets);
    }

    @Override
    protected void onProgressUpdate(Void... nulls) {

    }

    @Override
    protected void onPostExecute(String[] results) {
        pr.setMessage("Prediction was successful");
        pr.dismiss();
        callback.predictionResults(results);

    }

    private String[] predict(Bitmap[] faces, String modelName, AssetManager assets) {


        // Import the model
        TensorFlowInferenceInterface inferenceInterface = new TensorFlowInferenceInterface(assets, modelName);


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

        String gender = "";
        float maxAge = 0;

        String[] resluts = new String[faces.length];


        for (int i = 0; i < faces.length; i++) {
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


            inferenceInterface.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE, INPUT_SIZE, 3);

            inferenceInterface.run(OUTPUT_NAMES, false);

            float[] outputs_gender = new float[2];
            inferenceInterface.fetch(OUTPUT_NAME_GENDER, outputs_gender);

            float[] outputs_age = new float[101];
            inferenceInterface.fetch(OUTPUT_NAME_AGE, outputs_age);

            //multiply each age by its probability:
            for (int k = 0; k < outputs_age.length; k++)
                outputs_age[k] *= k;

            gender = outputs_gender[0] > 0.5 ? "F" : "M";

            float maxAgeProb = outputs_age[0];
            for (int k = 1; k < outputs_age.length; k++)
                if (outputs_age[k] > maxAgeProb)
                    maxAgeProb = outputs_age[k];

            maxAge = 0;
            for (int k = 1; k < outputs_age.length; k++) {
                if (outputs_age[k] == maxAgeProb) {
                    maxAge = k;
                    break;
                }
            }

            resluts[i] = gender + ";" + maxAge;

        }

        return resluts;
    }
}

