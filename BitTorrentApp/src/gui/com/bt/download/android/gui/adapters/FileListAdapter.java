/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bt.download.android.gui.adapters;

import com.bt.download.android.R;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.Peer;
import com.bt.download.android.gui.adapters.FileListAdapter.FileDescriptorItem;
import com.bt.download.android.gui.adapters.menu.DeleteFileMenuAction;
import com.bt.download.android.gui.adapters.menu.DownloadCheckedMenuAction;
import com.bt.download.android.gui.adapters.menu.DownloadMenuAction;
import com.bt.download.android.gui.adapters.menu.OpenMenuAction;
import com.bt.download.android.gui.adapters.menu.RenameFileMenuAction;
import com.bt.download.android.gui.adapters.menu.SendFileMenuAction;
import com.bt.download.android.gui.adapters.menu.SetAsRingtoneMenuAction;
import com.bt.download.android.gui.adapters.menu.SetAsWallpaperMenuAction;
import com.bt.download.android.gui.adapters.menu.SetSharedStateFileGrainedMenuAction;
import com.bt.download.android.gui.adapters.menu.ToggleFileGrainedSharingMenuAction;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.transfers.DownloadTransfer;
import com.bt.download.android.gui.transfers.ExistingDownload;
import com.bt.download.android.gui.transfers.TransferManager;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.AbstractListAdapter;
import com.bt.download.android.gui.views.BrowseThumbnailImageButton;
import com.bt.download.android.gui.views.BrowseThumbnailImageButton.OverlayState;
import com.bt.download.android.gui.views.ListAdapterFilter;
import com.bt.download.android.gui.views.MenuAction;
import com.bt.download.android.gui.views.MenuAdapter;
import com.bt.download.android.gui.views.MenuBuilder;
import com.bt.download.android.util.ImageUtils;
import com.bt.download.android.util.SystemUtils;
import com.frostwire.util.Condition;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class FileListAdapter extends AbstractListAdapter<FileDescriptorItem> {

    private final Peer peer;
    private final boolean local;
    private final byte fileType;

    private final PadLockClickListener padLockClickListener;
    private final DownloadButtonClickListener downloadButtonClickListener;

    public static final int FILE_LIST_FILTER_SHOW_ALL = 0;
    public static final int FILE_LIST_FILTER_SHOW_SHARED = 1;
    public static final int FILE_LIST_FILTER_SHOW_UNSHARED = 2;

    private FileListFilter fileListFilter;
    private Context context;

    public FileListAdapter(Context context, List<FileDescriptor> files, Peer peer, boolean local, byte fileType) {
        super(context, getViewItemId(local, fileType), convertFiles(files));

        setShowMenuOnClick(true);

        fileListFilter = new FileListFilter();
        setAdapterFilter(fileListFilter);

        this.context = context;
        this.peer = peer;
        this.local = local;
        this.fileType = fileType;

        this.padLockClickListener = new PadLockClickListener();
        this.downloadButtonClickListener = new DownloadButtonClickListener();

        checkSDStatus();
    }

    public byte getFileType() {
        return fileType;
    }

    /**
     * @param sharedState FILE_LIST_FILTER_SHOW_ALL, FILE_LIST_FILTER_SHOW_SHARED, FILE_LIST_FILTER_SHOW_UNSHARED
     */
    public void setFileVisibilityBySharedState(int sharedState) {
        fileListFilter.filterBySharedState(sharedState);
    }

    public int getFileVisibilityBySharedState() {
        return fileListFilter.getCurrentSharedStateShown();
    }

    @Override
    protected final void populateView(View view, FileDescriptorItem item) {
        if (getViewItemId() == R.layout.view_browse_thumbnail_peer_list_item) {
            populateViewThumbnail(view, item);
        } else {
            populateViewPlain(view, item);
        }
    }

    @Override
    protected MenuAdapter getMenuAdapter(View view) {
        Context context = getContext();

        List<MenuAction> items = new ArrayList<MenuAction>();

        // due to long click generic handle
        FileDescriptor fd = null;

        if (view.getTag() instanceof FileDescriptorItem) {
            FileDescriptorItem item = (FileDescriptorItem) view.getTag();
            fd = item.fd;
        } else if (view.getTag() instanceof FileDescriptor) {
            fd = (FileDescriptor) view.getTag();
        }

        if (local && checkIfNotExists(fd)) {
            return null;
        }

        List<FileDescriptor> checked = convertItems(getChecked());
        int numChecked = checked.size();

        boolean showSingleOptions = showSingleOptions(checked, fd);

        if (local) {
            if (showSingleOptions) {
                items.add(new OpenMenuAction(context, fd.filePath, fd.mime));

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new SendFileMenuAction(context, fd)); //applications cause a force close with GMail
                }

                if (fd.fileType == Constants.FILE_TYPE_RINGTONES && numChecked <= 1) {
                    items.add(new SetAsRingtoneMenuAction(context, fd));
                }

                if (fd.fileType == Constants.FILE_TYPE_PICTURES && numChecked <= 1) {
                    items.add(new SetAsWallpaperMenuAction(context, fd));
                }

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new RenameFileMenuAction(context, this, fd));
                }
            }

            List<FileDescriptor> list = checked;
            if (list.size() == 0) {
                list = Arrays.asList(fd);
            }

            //Share Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, true));

            //Unshare Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, false));

            //Toogle Shared States
            items.add(new ToggleFileGrainedSharingMenuAction(context, this, list));

            if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS) {
                items.add(new DeleteFileMenuAction(context, this, list));
            }
        } else {
            if (0 < numChecked && numChecked <= Constants.MAX_NUM_DOWNLOAD_CHECKED) {
                items.add(new DownloadCheckedMenuAction(context, this, checked, peer));
            }

            items.add(new DownloadMenuAction(context, this, peer, fd));
        }

        return new MenuAdapter(context, fd.title, items);
    }

    protected void onLocalPlay() {
    }

    private void localPlay(FileDescriptor fd) {
        if (fd == null) {
            return;
        }

        onLocalPlay();

        if (fd.mime != null && fd.mime.contains("audio")) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                Engine.instance().getMediaPlayer().stop();
            } else {
                try {
                    UIUtils.playEphemeralPlaylist(fd);
                    UXStats.instance().log(UXAction.LIBRARY_PLAY_AUDIO_FROM_FILE);
                } catch (RuntimeException re) {
                    UIUtils.showShortMessage(getContext(), R.string.media_player_failed);
                }
            }
            notifyDataSetChanged();
        } else {
            if (fd.filePath != null && fd.mime != null) {
                UIUtils.openFile(getContext(), fd.filePath, fd.mime);
            }
        }
    }

    /**
     * Start a transfer
     */
    private DownloadTransfer startDownload(FileDescriptor fd) {
        DownloadTransfer download = TransferManager.instance().download(peer, fd);
        notifyDataSetChanged();
        return download;
    }

    private void populateViewThumbnail(View view, FileDescriptorItem item) {
        FileDescriptor fd = item.fd;

        BrowseThumbnailImageButton fileThumbnail = findView(view, R.id.view_browse_peer_list_item_file_thumbnail);
        fileThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (local && fileType == Constants.FILE_TYPE_APPLICATIONS) {
            Uri uri = Uri.withAppendedPath(ImageUtils.APPLICATION_THUMBNAILS_URI, fd.album);
            ImageUtils.load(context, uri, fileThumbnail);
        } else {
            if (Condition.in(fileType, Constants.FILE_TYPE_AUDIO, Constants.FILE_TYPE_VIDEOS, Constants.FILE_TYPE_RINGTONES)) {
                if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                    fileThumbnail.setOverlayState(OverlayState.STOP);
                } else {
                    fileThumbnail.setOverlayState(OverlayState.PLAY);
                }
            }

            if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
                Uri uri = ContentUris.withAppendedId(ImageUtils.ALBUM_THUMBNAILS_URI, fd.albumId);
                ImageUtils.load(context, uri, fileThumbnail);
            } else if (fd.fileType == Constants.FILE_TYPE_VIDEOS) {
                Uri uri = ContentUris.withAppendedId(ImageUtils.VIDEO_THUMBNAILS_URI, fd.id);
                ImageUtils.load(context, uri, fileThumbnail);
            } else if (fd.fileType == Constants.FILE_TYPE_PICTURES) {
                Uri uri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, fd.id);
                ImageUtils.load(context, uri, fileThumbnail);
            }
        }

        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        fileThumbnail.setTag(fd);
        fileThumbnail.setOnClickListener(downloadButtonClickListener);

        populateSDState(view, item);
    }

    /**
     * Same factors are considered to show the padlock icon state and color.
     * 
     * When the file is not local and it's been marked for download the text color appears as blue.
     * 
     * @param fd
     * @param padlock
     * @param title
     */
    private void populatePadlockAppearance(FileDescriptor fd, ImageButton padlock, TextView title) {
        if (local) {
            padlock.setVisibility(View.VISIBLE);
            padlock.setTag(fd);
            padlock.setOnClickListener(padLockClickListener);

            if (fd.shared) {
                padlock.setImageResource(R.drawable.browse_peer_padlock_unlocked_icon);
            } else {
                padlock.setImageResource(R.drawable.browse_peer_padlock_locked_icon);
            }
        } else {
            padlock.setVisibility(View.GONE);
        }
    }

    private void populateViewPlain(View view, FileDescriptorItem item) {
        FileDescriptor fd = item.fd;

        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);
        populateContainerAction(view);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        BrowseThumbnailImageButton downloadButton = findView(view, R.id.view_browse_peer_list_item_download);

        if (local) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                downloadButton.setOverlayState(OverlayState.STOP);
            } else {
                downloadButton.setOverlayState(OverlayState.PLAY);
            }
        } else {
            downloadButton.setImageResource(R.drawable.download_icon);
        }

        downloadButton.setTag(fd);
        downloadButton.setOnClickListener(downloadButtonClickListener);

        populateSDState(view, item);
    }

    private void populateSDState(View v, FileDescriptorItem item) {
        ImageView img = findView(v, R.id.view_browse_peer_list_item_sd);
        ImageView lock = findView(v, R.id.view_browse_peer_list_item_lock_toggle);

        if (item.inSD) {
            if (item.mounted) {
                v.setBackgroundResource(R.drawable.listview_item_background_selector);
                setNormalTextColors(v);
                img.setVisibility(View.GONE);
            } else {
                v.setBackgroundResource(R.drawable.browse_peer_listview_item_inactive_background);
                setInactiveTextColors(v);
                img.setVisibility(View.VISIBLE);

                if (item.fd.shared) {
                    lock.setImageResource(R.drawable.browse_peer_padlock_unlocked_icon_inactive);
                } else {
                    lock.setImageResource(R.drawable.browse_peer_padlock_locked_icon_inactive);
                }
            }
        } else {
            v.setBackgroundResource(R.drawable.listview_item_background_selector);
            setNormalTextColors(v);
            img.setVisibility(View.GONE);
        }
    }

    private void setNormalTextColors(View v) {
        TextView title = findView(v, R.id.view_browse_peer_list_item_file_title);
        TextView text = findView(v, R.id.view_browse_peer_list_item_extra_text);
        TextView size = findView(v, R.id.view_browse_peer_list_item_file_size);

        Resources res = getContext().getResources();

        title.setTextColor(res.getColor(R.color.browse_peer_listview_item_foreground));
        text.setTextColor(res.getColor(R.color.browse_peer_listview_item_foreground));
        size.setTextColor(res.getColor(R.color.app_highlight_text));
    }

    private void setInactiveTextColors(View v) {
        TextView title = findView(v, R.id.view_browse_peer_list_item_file_title);
        TextView text = findView(v, R.id.view_browse_peer_list_item_extra_text);
        TextView size = findView(v, R.id.view_browse_peer_list_item_file_size);

        Resources res = getContext().getResources();

        title.setTextColor(res.getColor(R.color.browse_peer_listview_item_inactive_foreground));
        text.setTextColor(res.getColor(R.color.browse_peer_listview_item_inactive_foreground));
        size.setTextColor(res.getColor(R.color.browse_peer_listview_item_inactive_foreground));
    }

    private void populateContainerAction(View view) {
        ImageButton preview = findView(view, R.id.view_browse_peer_list_item_button_preview);

        if (local) {
            preview.setVisibility(View.GONE);
        } else {
            // just for now
            preview.setVisibility(View.GONE);
        }
    }

    private boolean showSingleOptions(List<FileDescriptor> checked, FileDescriptor fd) {
        if (checked.size() > 1) {
            return false;
        }
        if (checked.size() == 1) {
            return checked.get(0).equals(fd);
        }
        return true;
    }

    private static int getViewItemId(boolean local, byte fileType) {
        if (local && (fileType == Constants.FILE_TYPE_PICTURES || fileType == Constants.FILE_TYPE_VIDEOS || fileType == Constants.FILE_TYPE_APPLICATIONS || fileType == Constants.FILE_TYPE_AUDIO)) {
            return R.layout.view_browse_thumbnail_peer_list_item;
        } else {
            return R.layout.view_browse_peer_list_item;
        }
    }

    private static ArrayList<FileDescriptor> convertItems(Collection<FileDescriptorItem> items) {
        if (items == null) {
            return new ArrayList<FileDescriptor>();
        }

        ArrayList<FileDescriptor> list = new ArrayList<FileDescriptor>(items.size());

        for (FileDescriptorItem item : items) {
            list.add(item.fd);
        }

        return list;
    }

    private static ArrayList<FileDescriptorItem> convertFiles(Collection<FileDescriptor> fds) {
        if (fds == null) {
            return new ArrayList<FileDescriptorItem>();
        }

        ArrayList<FileDescriptorItem> list = new ArrayList<FileDescriptorItem>(fds.size());

        for (FileDescriptor fd : fds) {
            FileDescriptorItem item = new FileDescriptorItem();
            item.fd = fd;
            list.add(item);
        }

        return list;
    }

    public void deleteItem(FileDescriptor fd) {
        FileDescriptorItem item = new FileDescriptorItem();
        item.fd = fd;
        super.deleteItem(item);
    }

    private void checkSDStatus() {
        Map<String, Boolean> sds = new HashMap<String, Boolean>();

        String privateSubpath = "Android" + File.separator + "data";

        File[] externalDirs = SystemUtils.getExternalFilesDirs(getContext());
        for (int i = 1; i < externalDirs.length; i++) {
            File path = externalDirs[i];
            String absolutePath = path.getAbsolutePath();
            boolean isSecondaryExternalStorageMounted = SystemUtils.isSecondaryExternalStorageMounted(path);

            sds.put(absolutePath, isSecondaryExternalStorageMounted);

            if (absolutePath.contains(privateSubpath)) {
                String prefix = absolutePath.substring(0, absolutePath.indexOf(privateSubpath)-1);
                sds.put(prefix, isSecondaryExternalStorageMounted);
            }
        }

        if (sds.isEmpty()) {
            return; // yes, fast return (for now)
        }

        for (FileDescriptorItem item : getList()) {
            item.inSD = false;
            for (Entry<String, Boolean> e : sds.entrySet()) {
                if (item.fd.filePath.contains(e.getKey())) {
                    item.inSD = true;
                    item.mounted = e.getValue();
                }
            }
            item.exists = true;
        }
    }

    private boolean checkIfNotExists(FileDescriptor fd) {
        if (fd == null) {
            return true;
        }

        File f = new File(fd.filePath);

        if (!f.exists()) {
            if (SystemUtils.isSecondaryExternalStorageMounted(f.getAbsoluteFile())) {
                UIUtils.showShortMessage(getContext(), R.string.file_descriptor_sd_mounted);
                Librarian.instance().deleteFiles(fileType, Arrays.asList(fd));
                deleteItem(fd);
            } else {
                UIUtils.showShortMessage(getContext(), R.string.file_descriptor_sd_unmounted);
            }

            return true;
        } else {
            return false;
        }
    }

    private static class FileListFilter implements ListAdapterFilter<FileDescriptorItem> {

        private int visibleFiles;

        public void filterBySharedState(int state) {
            this.visibleFiles = state;
        }

        public int getCurrentSharedStateShown() {
            return visibleFiles;
        }

        @Override
        public boolean accept(FileDescriptorItem obj, CharSequence constraint) {
            if (visibleFiles != FILE_LIST_FILTER_SHOW_ALL && ((obj.fd.shared && visibleFiles == FILE_LIST_FILTER_SHOW_UNSHARED) || (!obj.fd.shared && visibleFiles == FILE_LIST_FILTER_SHOW_SHARED))) {
                return false;
            }

            String keywords = constraint.toString();

            if (keywords == null || keywords.length() == 0) {
                return true;
            }

            keywords = keywords.toLowerCase(Locale.US);

            FileDescriptor fd = obj.fd;

            if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
                return fd.album.trim().toLowerCase(Locale.US).contains(keywords) || fd.artist.trim().toLowerCase(Locale.US).contains(keywords) || fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            } else {
                return fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            }
        }
    }

    private final class PadLockClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            if (checkIfNotExists(fd)) {
                return;
            }

            fd.shared = !fd.shared;

            UXStats.instance().log(fd.shared ? UXAction.WIFI_SHARING_SHARED : UXAction.WIFI_SHARING_UNSHARED);

            notifyDataSetChanged();
            Librarian.instance().updateSharedStates(fileType, Arrays.asList(fd));
        }
    }

    private final class DownloadButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            if (local && checkIfNotExists(fd)) {
                return;
            }

            if (local) {
                localPlay(fd);
            } else {

                List<FileDescriptor> list = convertItems(getChecked());

                if (list == null || list.size() == 0) {
                    // if no files are selected, they want to download this one.
                    if (!(startDownload(fd) instanceof ExistingDownload)) {
                        UIUtils.showLongMessage(getContext(), R.string.download_added_to_queue);
                        UIUtils.showTransfersOnDownloadStart(getContext());
                    }
                } else {

                    // if many are selected... do they want to download many
                    // or just this one?
                    List<MenuAction> items = new ArrayList<MenuAction>(2);

                    items.add(new DownloadCheckedMenuAction(getContext(), FileListAdapter.this, list, peer));
                    items.add(new DownloadMenuAction(getContext(), FileListAdapter.this, peer, fd));

                    MenuAdapter menuAdapter = new MenuAdapter(getContext(), R.string.wanna_download_question, items);

                    trackDialog(new MenuBuilder(menuAdapter).show());
                }
            }
        }
    }

    public static class FileDescriptorItem {

        public FileDescriptor fd;
        public boolean inSD;
        public boolean mounted;
        public boolean exists;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileDescriptorItem)) {
                return false;
            }

            return fd.equals(((FileDescriptorItem) o).fd);
        }
    }
}