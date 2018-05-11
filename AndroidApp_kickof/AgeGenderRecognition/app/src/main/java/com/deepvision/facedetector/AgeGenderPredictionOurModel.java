package com.deepvision.facedetector;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.graphics.Bitmap.createScaledBitmap;

class AgeGenderPredictionOurModel extends AsyncTask<Bitmap[], Void, String[]> {
    // class fields


    private ProgressDialog pr;
    private OnResultsCallback callback;
    private AssetManager assets;
    private String modelNameAge;
    private String modelNameGender;

    AgeGenderPredictionOurModel(
            Context context,
            AssetManager assets,
            String modelNameAge,
            String modelNameGender,
            OnResultsCallback callback) {
        this.callback = callback;
        this.assets = assets;
        this.modelNameAge = modelNameAge;
        this.modelNameGender = modelNameGender;
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
        return predict(faces[0], modelNameAge, modelNameGender, assets);
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

    private String[] predict(Bitmap[] faces, String modelNameAge, String modelNameGender, AssetManager assets) {


        // Import the model
        TensorFlowInferenceInterface inferenceInterfaceAge = new TensorFlowInferenceInterface(assets, modelNameAge);
        TensorFlowInferenceInterface inferenceInterfaceGender = new TensorFlowInferenceInterface(assets, modelNameGender);


        int INPUT_SIZE = 227;
//        String MODEL_FILE = "opt_frozen_model.pb";
//        String IMAGE_FILE="file:///android_asset/1.jpg";
//        String INPUT_NAME = "batch_processing/Reshape:0";
//        String OUTPUT_NAME = "output/output:0";
        String INPUT_NAME = "batch_processing/Reshape:0";
        String OUTPUT_NAME = "output/output:0";
        String[] OUTPUT_NAMES = {OUTPUT_NAME};
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        float[] floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];

        String gender = "";
        float maxAge = 0;
        String age = "";

        String[] results = new String[faces.length];


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

            //predict age:
            inferenceInterfaceAge.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE, INPUT_SIZE, 3);
            inferenceInterfaceAge.run(OUTPUT_NAMES, false);
            float[] outputs_age = new float[11];
            inferenceInterfaceAge.fetch(OUTPUT_NAME, outputs_age);


            // predict gender:
            inferenceInterfaceGender.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE, INPUT_SIZE, 3);
            inferenceInterfaceGender.run(OUTPUT_NAMES, false);
            float[] outputs_gender = new float[2];
            inferenceInterfaceGender.fetch(OUTPUT_NAME, outputs_gender);


            //multiply each age by its probability:
//            for (int k = 0; k < outputs_age.length; k++)
//                outputs_age[k] *= k;

            gender = outputs_gender[0] < 0.5 ? "Female" : "Male";
//            gender = "T";


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

            switch ((int) maxAge) {
                case 0:
                    age = "[0,2]";
                    break;
                case 1:
                    age = "[3,7]";
                    break;
                case 2:
                    age = "[8,14]";
                    break;
                case 3:
                    age = "[15,19]";
                    break;
                case 4:
                    age = "[20,27]";
                    break;
                case 5:
                    age = "[28,36]";
                    break;
                case 6:
                    age = "[37,45]";
                    break;
                case 7:
                    age = "[46,52]";
                    break;
                case 8:
                    age = "[53,60]";
                    break;
                case 9:
                    age = "[61,70]";
                    break;
                case 10:
                    age = "[71,100]";
                    break;
            }

            results[i] = gender + ";" + age;
//            results[i] = "G" + ";" + "G";

        }

        return results;
    }
}

