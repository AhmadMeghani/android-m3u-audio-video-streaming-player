package com.example.streamingplayer.tabs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.example.streamingplayer.R;
import com.example.streamingplayer.activities.VideoActivity;
import com.example.streamingplayer.interfaces.IFragmentListener;
import com.example.streamingplayer.interfaces.ISearch;
import com.example.streamingplayer.utils.GridAdapter;
import com.example.streamingplayer.utils.Parser;

import io.github.lucasepe.m3u.models.Segment;

import static com.example.streamingplayer.activities.MainActivity.PREFS_NAME;

public class TabTV extends Fragment implements ISearch {
    private static final String ARG_SEARCHTERM = "search_term";
    private String mSearchTerm = null;
    private SharedPreferences prefs;
    private ArrayList<String> channelNames;
    private List<Segment> channels;
    private IFragmentListener mIFragmentListener = null;
    private ArrayAdapter<String> viewAdapter = null;

    public TabTV() { } //needed, otherwise app crashes, however, it does not allow anything inside

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_tv, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view_tv);

        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Parser parser = new Parser(getActivity(), "streaming/tv.m3u8");
        channels = parser.getChannel();
        channelNames = parser.getChannelName();
        viewAdapter = new GridAdapter(getActivity(), channelNames);
        gridView.setAdapter(viewAdapter);

        if (getArguments() != null)
            mSearchTerm = (String) getArguments().get(ARG_SEARCHTERM);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Segment selectedChannel = null;
                for (Segment tmp : channels)
                    if (tmp.getTitle() == viewAdapter.getItem(i).toString())
                        selectedChannel = tmp;

                String url = selectedChannel.getUri().toString();
                String title = selectedChannel.getTitle().toString();
                Toast.makeText(getActivity().getApplicationContext(), title, Toast.LENGTH_SHORT).show();

                Intent myIntent = new Intent(getActivity(), VideoActivity.class);
                myIntent.putExtra("title", title).putExtra("URL", url);
                startActivity(myIntent);
            }
        });

        return view;
    }

    public static TabTV newInstance(String searchTerm) {
        TabTV fragment = new TabTV();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SEARCHTERM, searchTerm);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void onTextQuery(String text) {
        viewAdapter.getFilter().filter(text);
        viewAdapter.notifyDataSetChanged();
    }

    public void onResume() {
        super.onResume();

        if (null != mSearchTerm)
            onTextQuery(mSearchTerm);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        mIFragmentListener = (IFragmentListener) context;
        mIFragmentListener.addiSearch(TabTV.this);
    }

    public void onDetach() {
        super.onDetach();
        if (null != mIFragmentListener)
            mIFragmentListener.removeISearch(TabTV.this);
    }
}