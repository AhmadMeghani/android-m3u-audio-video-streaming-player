package com.example.streamingplayer.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Arrays;

import com.example.streamingplayer.R;
import com.example.streamingplayer.activities.MainActivity;

import io.github.lucasepe.m3u.models.Segment

public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener, Player.EventListener {
	private final IBinder musicBind = new MusicBinder();
	private AudioManager audioManager; //to stop playback when someone is calling or other audio source wanna play
	private NotificationManager notificationManager; //to show a notification when music gets played
	private Messenger messageHandler; //to inform the GUI in TabRadio when music gets interrupted, stopped, played, whatever
	private BroadcastReceiver broadcastReceiver; //to receive Intents from Notification, e. g. pause/play/stop pressed from Notification Bar
	private SimpleExoPlayer player;
	private DataSource.Factory mediaDataSourceFactory;
	private DefaultTrackSelector trackSelector;
	private BandwidthMeter bandwidthMeter;
	private String title, url;

	/* Binder, needed that GUI and Background Service belongs to each other */
	public class MusicBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}

	/* Note, you don't have to initialize the player again when you only change the channel you wanna play*/
	public IBinder onBind(Intent intent) {
		if(player == null)
			initializePlayer();
		return musicBind;
	}

	/* called when app gets closed or you press "stop" button (not the pause button) */
	public boolean onUnbind(Intent intent){
		player.stop();
		player.release();
		player = null;
		return super.onUnbind(intent);
	}

	public void onCreate() {
		super.onCreate();
		bandwidthMeter = new DefaultBandwidthMeter();
		mediaDataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "mediaPlayerSample"), (TransferListener<? super DataSource>) bandwidthMeter);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		broadcastReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("action_close_app");
		filter.addAction("action_play_pause");
		registerReceiver(broadcastReceiver, filter);
	}

	//MessageHandler is part of the GUI, so when the GUI calls the Service, you can get the Handler from it. This is needed for the interaction between GUI and Service
	public int onStartCommand (Intent intent, int flags, int startId) {
		Bundle extras = intent.getExtras();
		messageHandler = (Messenger) extras.get("MESSENGER");
		return START_REDELIVER_INTENT;
	}

	private void initializePlayer() {
		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
		trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
		player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
		player.setPlayWhenReady(true);
		player.addListener(this);
	}

	public void playChannel(Segment channel) {
		audioManager.requestAudioFocus(this, audioManager.STREAM_MUSIC, audioManager.AUDIOFOCUS_GAIN);
		title = channel.getTitle(); //get title
		url = channel.getUri().toString();
		DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
		MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(url), mediaDataSourceFactory, extractorsFactory, null, null);
		player.prepare(mediaSource);
	}

	public boolean isPlaying() {
		return player.getPlayWhenReady();
	}

	public void startPlayer(){
		player.setPlayWhenReady(true);
		notification();
	}

	public void pausePlayer(){
		player.setPlayWhenReady(false);
		notification();
	}

	protected void releasePlayer() {
		if (player != null) {
			player.release();
			player = null;
			trackSelector = null;
		}
	}

	public void onLoadingChanged(boolean isLoading) {
		if(isLoading) {
			sendMessage(0); //notify GUI that Player is Buffering
		}
	}

	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if(playbackState == ExoPlayer.STATE_READY){
			sendMessage(1); //notify GUI that buffering is done, playback can start
			notification();
		}
	}

	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				sendMessage(2); //notify playback can be continued
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				sendMessage(3); //notify that playback has to be interrupted
				break;
		}
	}

	public void onDestroy() {
		releasePlayer();
		audioManager.abandonAudioFocus(this);
		audioManager = null;
		notificationManager.cancel(1);
		super.onDestroy();
	}

	private void notification() {
		Intent notIntentOpenApp = new Intent(this, MainActivity.class);
		notIntentOpenApp.addCategory(Intent.CATEGORY_LAUNCHER);
		notIntentOpenApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendIntOpenApp = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), notIntentOpenApp, 0);
		PendingIntent pendIntCloseApp = PendingIntent.getBroadcast(this, 0, new Intent("action_close_app"), 0);
		PendingIntent pendIntPlayPause = PendingIntent.getBroadcast(this, 0, new Intent("action_play_pause"), 0);
		NotificationCompat.Builder customNotification = new NotificationCompat.Builder(this, "CHANNEL_ID");
		RemoteViews notificationLayout_small = new RemoteViews(getPackageName(), R.layout.notification_small);
		RemoteViews notificationLayout_big = new RemoteViews(getPackageName(), R.layout.notification_big);
		notificationLayout_small.setTextViewText(R.id.notification_title_small, title);
		notificationLayout_small.setOnClickPendingIntent(R.id.audioStopBtnNotSmall, pendIntCloseApp);
		notificationLayout_small.setOnClickPendingIntent(R.id.audioStreamBtnNotSmall, pendIntPlayPause);
		notificationLayout_big.setTextViewText(R.id.notification_title_big, title);
		notificationLayout_big.setOnClickPendingIntent(R.id.audioStopBtnNotBig, pendIntCloseApp);
		notificationLayout_big.setOnClickPendingIntent(R.id.audioStreamBtnNotBig, pendIntPlayPause);

		int smallIcon;
		if(isPlaying()) {
			notificationLayout_small.setInt(R.id.audioStreamBtnNotSmall, "setBackgroundResource", R.drawable.pausecontrol_notification);
			notificationLayout_big.setInt(R.id.audioStreamBtnNotBig, "setBackgroundResource", R.drawable.pausecontrol_notification);
			smallIcon = R.drawable.playcontrol;
		} else {
			notificationLayout_small.setInt(R.id.audioStreamBtnNotSmall, "setBackgroundResource", R.drawable.playcontrol_notification);
			notificationLayout_big.setInt(R.id.audioStreamBtnNotBig, "setBackgroundResource", R.drawable.playcontrol_notification);
			smallIcon = R.drawable.pausecontrol;
		}

		customNotification.setSmallIcon(smallIcon).setStyle(new NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(notificationLayout_small).setCustomBigContentView(notificationLayout_big).setContentIntent(pendIntOpenApp);
		Notification not = customNotification.build();

		try {
			if(Arrays.asList(getResources().getAssets().list("logos")).contains(title.toLowerCase() + ".png")) {
				Picasso.get().load("file:///android_asset/logos/" + title.toLowerCase() + ".png").into(notificationLayout_big, R.id.notification_image, 1, not);
			} else {
				Picasso.get().load("file:///android_asset/logos/no_logo.png").into(notificationLayout_big, R.id.notification_image, 1, not);
			}
		} catch (IOException e) { }
		notificationManager.notify(1, not);
	}

	//simple Method to send message to MessageHandler (GUI). Be aware that states are predefined as assumed
	private void sendMessage(int state) {
		Message message = Message.obtain();
		message.arg1 = state;

		try {
			messageHandler.send(message);
		} catch (RemoteException e) { }
	}

	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) { }
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) { }
	public void onRepeatModeChanged(int repeatMode) { }
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) { }
	public void onPlayerError(ExoPlaybackException error) { }
	public void onPositionDiscontinuity(int reason) { }
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) { }
	public void onSeekProcessed() { }

	//Receives intent from Notification
	private class MyReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("action_close_app")) {
				player.stop();
				sendMessage(4);
				stopSelf();
			} else if (intent.getAction().equals("action_play_pause")) {
				if(player.getPlayWhenReady()) {
					sendMessage(3);
				} else {
					sendMessage(2);
				}
			}
		}
	}
}
