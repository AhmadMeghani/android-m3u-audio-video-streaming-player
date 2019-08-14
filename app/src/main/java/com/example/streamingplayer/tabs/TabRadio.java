package com.example.streamingplayer.tabs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.example.streamingplayer.R;
import com.example.streamingplayer.interfaces.IFragmentListener;
import com.example.streamingplayer.interfaces.ISearch;
import com.example.streamingplayer.utils.GridAdapter;
import com.example.streamingplayer.utils.MusicService;
import com.example.streamingplayer.utils.Parser;

import io.github.lucasepe.m3u.models.Segment;

public class TabRadio extends Fragment implements ISearch {
    private static final String ARG_SEARCHTERM = "search_term";
    private String mSearchTerm = null;
    private Segment selectedChannel;
    private List<Segment> channels;
    private IFragmentListener mIFragmentListener = null;
    private ArrayAdapter<String> viewAdapter = null;
    private LinearLayout control;
    private ImageButton btnPlay, btnStop;
    private TextView radioChannel;

    private ServiceConnection musicConnection;
    public ProgressDialog progress;
    public Handler messageHandler;
    private ArrayList<String> channelNames;
    private MusicService musicSrv;
    private Intent playIntent;

    public TabRadio() { } //needed, otherwise app crashes, however, it does not allow anything inside

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_radio, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.grid_view_radio);
        control = (LinearLayout) view.findViewById(R.id.playback_control_layout);
        control.setVisibility(LinearLayout.GONE);
        radioChannel = (TextView) view.findViewById(R.id.radioChannel);
        btnPlay = (ImageButton) view.findViewById(R.id.audioStreamBtn);
        btnStop = (ImageButton) view.findViewById(R.id.audioStopBtn);

        Parser parser = new Parser(getActivity(), "streaming/radio.m3u8");
        channels = parser.getChannel();
        channelNames = parser.getChannelName();
        progress = new ProgressDialog(getActivity());
        messageHandler = new MessageHandler(getContext());
        channels = parser.getChannel();
        channelNames = parser.getChannelName();
        viewAdapter = new GridAdapter(getActivity(), channelNames);
        gridView.setAdapter(viewAdapter);

        if (getArguments() != null)
            mSearchTerm = (String) getArguments().get(ARG_SEARCHTERM);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                for (Segment tmp : channels)
                    if (tmp.getTitle() == viewAdapter.getItem(i).toString())
                        selectedChannel = tmp;
                radioChannel.setText(selectedChannel.getTitle());

                if (playIntent == null) {
                    playIntent = new Intent(getContext(), MusicService.class);
                    playIntent.putExtra("MESSENGER", new Messenger(messageHandler));

                    musicConnection = new ServiceConnection() {
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                            musicSrv = binder.getService();
                            musicSrv.playChannel(selectedChannel);
                            control.setVisibility(View.VISIBLE);
                            btnPlay.setBackground(getResources().getDrawable(R.drawable.pausecontrol));
                        }
                        public void onServiceDisconnected(ComponentName name) { }
                    };

                    getActivity().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
                    getActivity().startService(playIntent);
                } else {
                    musicSrv.playChannel(selectedChannel);
                    control.setVisibility(View.VISIBLE);
                    btnPlay.setBackground(getResources().getDrawable(R.drawable.pausecontrol));
                }
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (musicSrv.isPlaying() == false) {
                    start();
                } else {
                    pause();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().stopService(playIntent);
                getActivity().unbindService(musicConnection);
                playIntent = null;
                control.setVisibility(View.GONE);
            }
        });

        return view;
    }

    public static TabRadio newInstance(String searchTerm) {
        TabRadio fragment = new TabRadio();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SEARCHTERM, searchTerm);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void pause() {
        btnPlay.setBackground(getResources().getDrawable(R.drawable.playcontrol));
        musicSrv.pausePlayer();
    }

    public void start() {
        btnPlay.setBackground(getResources().getDrawable(R.drawable.pausecontrol));
        musicSrv.startPlayer();
    }

    public void onResume() {
        super.onResume();
        if (null != mSearchTerm) {
            onTextQuery(mSearchTerm);
        }
    }

    public void onTextQuery(String text) {
        viewAdapter.getFilter().filter(text);
        viewAdapter.notifyDataSetChanged();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        mIFragmentListener = (IFragmentListener) context;
        mIFragmentListener.addiSearch(TabRadio.this);
    }

    public void onDetach() {
        super.onDetach();
        if (null != mIFragmentListener)
            mIFragmentListener.removeISearch(TabRadio.this);
    }

    public void onDestroy() {
        if(playIntent != null) {
            getActivity().stopService(playIntent);
            musicSrv = null;
        }
        super.onDestroy();
    }

    public class MessageHandler extends Handler {
        Context context;

        public MessageHandler(Context pContext) {
            context = pContext;
        }

        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case 4: //close app because stop gets pressed
                    ((Activity) context).finish();
                    break;
                case 3: //playback paused
                    pause();
                    break;
                case 2: //playback started
                    start();
                    break;
                case 1: //Buffering finished
                    if (progress.isShowing()) {
                        progress.cancel();
                    }
                    break;
                case 0: //Buffering started
                    progress.setMessage("Buffering..."); progress.show();
                    break;
            }
        }
    }
}
