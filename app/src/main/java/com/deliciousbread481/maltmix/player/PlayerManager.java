package com.deliciousbread481.maltmix.player;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    public interface PlaybackErrorListener {
        void onError(String message);
    }

    private static PlayerManager instance;
    private final ExoPlayer player;
    private PlaybackErrorListener errorListener;

    private PlayerManager(Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/120.0.0.0 Safari/537.36");

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()  
                .setDefaultRequestProperties(headers)  
                .setAllowCrossProtocolRedirects(true)  
                .setConnectTimeoutMs(10000)  
                .setReadTimeoutMs(15000);

        player = new ExoPlayer.Builder(context.getApplicationContext())
                .setMediaSourceFactory(new ProgressiveMediaSource.Factory(httpFactory))
                .build();

        player.addListener(new Player.Listener() {  
            private int retryCount = 0;  
  
            @Override  
            public void onPlaybackStateChanged(int playbackState) {  
                if (playbackState == Player.STATE_READY) {  
                    retryCount = 0;  
                }  
            }  
  
            @Override  
            public void onPlayerError(@NonNull PlaybackException error) {  
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED  
                        || error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE  
                        || error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {  
                    if (retryCount < 1 && player.getMediaItemCount() > 0) {  
                        retryCount++;  
                        player.prepare();  
                        player.play();  
                        return;  
                    }  
                }  
                if (errorListener != null) {  
                    errorListener.onError("ExoPlayer 播放错误\n"  
                            + "错误码: " + error.errorCode + "\n"  
                            + "信息: " + error.getMessage() + "\n"  
                            + "原因: " + error.getCause());  
                }  
            }  
        });
    }

    public static synchronized PlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerManager(context);
        }
        return instance;
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void setErrorListener(PlaybackErrorListener listener) {
        this.errorListener = listener;
    }

    public boolean hasMedia() {
        return player.getMediaItemCount() > 0;
    }

    public void playUrl(String url) {
        MediaItem item = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void stop() {  
        player.stop();  
        player.clearMediaItems();  
    }

    public void release() {
        if (player != null) {
            player.release();
        }
        instance = null;
    }
}