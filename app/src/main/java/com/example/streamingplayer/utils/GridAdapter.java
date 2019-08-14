package com.example.streamingplayer.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import com.example.streamingplayer.R;

public class GridAdapter extends ArrayAdapter<String> {
    public GridAdapter(@NonNull Context context, ArrayList<String> list) {
        super(context, 0 , list);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    public int getCount() {
        return super.getCount();
    }

    public String getItem(final int position) {
        return super.getItem(position);
    }

    public long getItemId(final int position) {
        return super.getItemId(position);
    }

    public View getView(final int position, final View convertView, final ViewGroup parent) {
        //super.getView(position,convertView,parent);
        View view = convertView;
        if (view == null)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_layout, parent, false);
        TextView text = (TextView) view.findViewById(R.id.grid_item_label); // set value into textview
        String channelName = super.getItem(position);
        text.setText(channelName);

        final ImageView imageView = (ImageView) view.findViewById(R.id.grid_item_image);
        String logo = "file:///android_asset/logos/" + channelName.toLowerCase() + ".png";
        Picasso.get().load(logo).into(imageView, new Callback() {
            public void onSuccess() { }
            public void onError(Exception e) {
                Picasso.get().load("file:///android_asset/logos/no_logo.png").into(imageView);
            }
        });

        return view;
    }
}