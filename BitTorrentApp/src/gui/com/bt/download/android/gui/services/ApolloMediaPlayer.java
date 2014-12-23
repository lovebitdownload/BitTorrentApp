package com.bt.download.android.gui.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.os.RemoteException;

import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.core.player.CoreMediaPlayer;
import com.bt.download.android.core.player.Playlist;
import com.bt.download.android.core.player.PlaylistItem;

public class ApolloMediaPlayer implements CoreMediaPlayer {

    private final Service service;

    private Playlist playlist;
    private Map<Long, FileDescriptor> idMap = new HashMap<Long, FileDescriptor>();

    public ApolloMediaPlayer(Service service) {
        this.service = service;
    }

    @Override
    public void play(Playlist playlist) {
        this.playlist = playlist;

        List<PlaylistItem> items = playlist.getItems();

        idMap.clear();
        long[] list = new long[items.size()];
        int position = 0;

        PlaylistItem currentItem = playlist.getCurrentItem();

        for (int i = 0; i < items.size(); i++) {
            PlaylistItem item = items.get(i);
            list[i] = item.getFD().id;
            idMap.put((long) item.getFD().id, item.getFD());
            if (currentItem != null && currentItem.getFD().id == item.getFD().id) {
                position = i;
            }
        }

        MusicUtils.playAll(service, list, position, false);
    }

    @Override
    public void playPrevious() {
    }

    @Override
    public void playNext() {
    }

    @Override
    public void togglePause() {
    }

    @Override
    public void stop() {
        try {
            MusicUtils.mService.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        try {
            MusicUtils.mService.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPlaying() {
        return MusicUtils.isPlaying();
    }

    @Override
    public void seekTo(int position) {
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public FileDescriptor getCurrentFD() {
        try {
            long audioId = MusicUtils.mService.getAudioId();
            return idMap.get(audioId);
        } catch (Throwable e) {
        }

        return null;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @Override
    public void start() {
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }
}
