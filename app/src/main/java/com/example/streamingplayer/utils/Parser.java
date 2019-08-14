package com.example.streamingplayer.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.github.lucasepe.m3u.M3UParser;
import io.github.lucasepe.m3u.models.Playlist;
import io.github.lucasepe.m3u.models.Segment;

public class Parser {
    private List<Segment> channels;
    private ArrayList<String> channelName;

    public Parser(Context context, String filename) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream file = assetManager.open(filename);
            M3UParser parser = new M3UParser();
            Playlist playlist = parser.parse(file);
            channels = playlist.getSegments();
            channelName = new ArrayList<String>();
            for (Segment channel : channels)
                channelName.add(channel.getTitle());
        } catch (Exception e) { }
    }

    /* returns List of Segments. Each Segment contains the streaming url and the name */
    public List<Segment> getChannel() {
        return channels;
    }

    /* just returns a list of the names of the segments */
    public ArrayList<String> getChannelName() {
        return channelName;
    }
}