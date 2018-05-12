package com.deepvision.facedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class GridImagesAdapter extends BaseAdapter {
    private Context mContext;

    // all Images in array
    private Bitmap[] images;
    private String[] results;

    // Constructor
    public GridImagesAdapter(Context c) {
        mContext = c;
    }

    public GridImagesAdapter(Context c, Bitmap[] images, String[] results) {
        this.mContext = c;
        this.images = images;
        this.results = results;
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        View gridView = convertView;

        if (gridView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            gridView = inflater.inflate(R.layout.custom_image_layout, null);
        }

        ImageView imageView = gridView.findViewById(R.id.grid_image);
        imageView.setImageBitmap(images[position]);

        TextView ageTV = gridView.findViewById(R.id.grid_text_age);
        TextView genderTV = gridView.findViewById(R.id.grid_text_gender);
        String[] splitResults = results[position].split(";");
        Log.v("OUTPUTC", results[position]);
        String gender = splitResults[0];
        String maxAge = splitResults[1];
        ageTV.setText("Age: " + maxAge);
        genderTV.setText("Gender: " + gender);

//        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//        imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return gridView;
    }
}
