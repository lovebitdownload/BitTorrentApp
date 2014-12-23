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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import org.apache.commons.io.FilenameUtils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.MediaType;
import com.bt.download.android.gui.NetworkManager;
import com.bt.download.android.gui.adapters.menu.BrowsePeerMenuAction;
import com.bt.download.android.gui.adapters.menu.CancelMenuAction;
import com.bt.download.android.gui.adapters.menu.OpenMenuAction;
import com.bt.download.android.gui.adapters.menu.PauseDownloadMenuAction;
import com.bt.download.android.gui.adapters.menu.ResumeDownloadMenuAction;
import com.bt.download.android.gui.transfers.BittorrentDownload;
import com.bt.download.android.gui.transfers.DownloadTransfer;
import com.bt.download.android.gui.transfers.HttpDownload;
import com.bt.download.android.gui.transfers.PeerHttpDownload;
import com.bt.download.android.gui.transfers.PeerHttpUpload;
import com.bt.download.android.gui.transfers.SoundcloudDownload;
import com.bt.download.android.gui.transfers.TorrentFetcherDownload;
import com.bt.download.android.gui.transfers.Transfer;
import com.bt.download.android.gui.transfers.YouTubeDownload;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.MenuAction;
import com.bt.download.android.gui.views.MenuAdapter;
import com.bt.download.android.gui.views.MenuBuilder;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransferListAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "FW.TransferListAdapter";

    private final WeakReference<Context> context;

    private final OnClickListener viewOnClickListener;
    private final ViewOnLongClickListener viewOnLongClickListener;
    private final OpenOnClickListener playOnClickListener;

    /** Keep track of all dialogs ever opened so we dismiss when we leave to avoid memleaks */
    private final List<Dialog> dialogs;

    private List<Transfer> list;

    private final Map<String,String> TRANSFER_STATE_STRING_MAP = new Hashtable<String,String>();

    public TransferListAdapter(Context context, List<Transfer> list) {
        this.context = new WeakReference<Context>(context);

        this.viewOnClickListener = new ViewOnClickListener();
        this.viewOnLongClickListener = new ViewOnLongClickListener();
        this.playOnClickListener = new OpenOnClickListener();

        this.dialogs = new ArrayList<Dialog>();

        this.list = list.equals(Collections.emptyList()) ? new ArrayList<Transfer>() : list;

        initTransferStateStringMap();
    }

    private void initTransferStateStringMap() {
        Context c = context.get();
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.QUEUED_FOR_CHECKING), c.getString(R.string.queued_for_checking));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.CHECKING), c.getString(R.string.checking_ellipsis));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.DOWNLOADING_METADATA), c.getString(R.string.downloading_metadata));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.DOWNLOADING_TORRENT), c.getString(R.string.torrent_fetcher_download_status_downloading_torrent));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.DOWNLOADING), c.getString(R.string.azureus_manager_item_downloading));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.FINISHED), c.getString(R.string.azureus_peer_manager_status_finished));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.SEEDING), c.getString(R.string.azureus_manager_item_seeding));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ALLOCATING), c.getString(R.string.azureus_manager_item_allocating));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.PAUSED), c.getString(R.string.azureus_manager_item_paused));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ERROR), c.getString(R.string.azureus_manager_item_error));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ERROR_MOVING_INCOMPLETE), c.getString(R.string.error_moving_incomplete));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ERROR_HASH_MD5), c.getString(R.string.error_wrong_md5_hash));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ERROR_SIGNATURE), c.getString(R.string.error_wrong_signature));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.ERROR_NOT_ENOUGH_PEERS), c.getString(R.string.error_not_enough_peers));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.STOPPED), c.getString(R.string.azureus_manager_item_stopped));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.PAUSING), c.getString(R.string.pausing));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.CANCELING), c.getString(R.string.canceling));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.CANCELED), c.getString(R.string.torrent_fetcher_download_status_canceled));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.WAITING), c.getString(R.string.waiting));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.COMPLETE), c.getString(R.string.peer_http_download_status_complete));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.UPLOADING), c.getString(R.string.peer_http_upload_status_uploading));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.UNCOMPRESSING), c.getString(R.string.http_download_status_uncompressing));
        TRANSFER_STATE_STRING_MAP.put(String.valueOf(TransferState.DEMUXING), c.getString(R.string.transfer_status_demuxing));
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return list.get(groupPosition).getItems().get(childPosition);
    }

    public TransferItem getChildItem(int groupPosition, int childPosition) {
        return list.get(groupPosition).getItems().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        TransferItem item = getChildItem(groupPosition, childPosition);

        if (convertView == null) {
            convertView = View.inflate(context.get(), R.layout.view_transfer_item_list_item, null);

            convertView.setOnClickListener(viewOnClickListener);
            convertView.setOnLongClickListener(viewOnLongClickListener);
        }

        try {

            initTouchFeedback(convertView, item);

            populateChildView(convertView, item);

        } catch (Throwable e) {
            Log.e(TAG, "Fatal error getting view: " + e.getMessage());
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int size = list.get(groupPosition).getItems().size();
        return size <= 1 ? 0 : size;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return list.get(groupPosition);
    }

    public Transfer getGroupItem(int groupPosition) {
        return list.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return list.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Transfer item = getGroupItem(groupPosition);

        if (convertView == null || convertView instanceof TextView) {
            // convertView could be a dummy view due to an issue with the slide menu layout request order
            try {
                convertView = View.inflate(context.get(), R.layout.view_transfer_list_item, null);
            } catch (Throwable e) {
                // creating a dummy view to avoid a force close due to a NPE
                // next time the "if" will try to recover the actual layout
                convertView = new TextView(context.get());
                ((TextView) convertView).setText("Rendering error");
            }
        }

        try {
            boolean clickable = item.getItems().size() == 0;
            convertView.setOnClickListener(clickable ? viewOnClickListener : null);
            convertView.setOnLongClickListener(clickable ? viewOnLongClickListener : null);

            convertView.setClickable(clickable);
            convertView.setLongClickable(clickable);

            setupGroupIndicator(convertView, isExpanded, item);

            convertView.setTag(item);
            populateGroupView(convertView, item);
        } catch (Throwable e) {
            Log.e(TAG, "Fatal error getting the group view: " + e.getMessage(), e);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void updateList(List<Transfer> g) {
        list = g;
        notifyDataSetChanged();
    }

    public void dismissDialogs() {
        for (Dialog dialog : dialogs) {
            try {
                dialog.dismiss();
            } catch (Throwable e) {
                Log.w(TAG, "Error dismissing dialog", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <TView extends View> TView findView(View view, int id) {
        return (TView) view.findViewById(id);
    }

    protected void populateGroupView(View view, Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            populateBittorrentDownload(view, (BittorrentDownload) transfer);
        } else if (transfer instanceof PeerHttpDownload) {
            populatePeerDownload(view, (PeerHttpDownload) transfer);
        } else if (transfer instanceof PeerHttpUpload) {
            populatePeerUpload(view, (PeerHttpUpload) transfer);
        } else if (transfer instanceof HttpDownload) {
            populateHttpDownload(view, (HttpDownload) transfer);
        } else if (transfer instanceof YouTubeDownload) {
            populateYouTubeDownload(view, (YouTubeDownload) transfer);
        } else if (transfer instanceof SoundcloudDownload) {
            populateSoundcloudDownload(view, (SoundcloudDownload) transfer);
        }
    }

    protected void populateChildView(View view, TransferItem item) {
        populateBittorrentDownloadItem(view, item);
    }

    protected MenuAdapter getMenuAdapter(View view) {
        Object tag = view.getTag();
        String title = "";
        List<MenuAction> items = new ArrayList<MenuAction>();

        if (tag instanceof BittorrentDownload) {
            BittorrentDownload download = (BittorrentDownload) tag;
            title = download.getDisplayName();

            //If it's a torrent download with a single file, we should be able to open it.
            if (download.isComplete()) {
                TransferItem transferItem = download.getItems().get(0);
                String path = transferItem.getFile().getAbsolutePath();
                String mimeType = UIUtils.getMimeType(path);
                items.add(new OpenMenuAction(context.get(), path, mimeType));
            }

            if (!download.isComplete() || ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS)) {
                if (download.isPausable()) {
                    items.add(new PauseDownloadMenuAction(context.get(), download));
                } else if (download.isResumable()) {
                    boolean wifiIsUp = NetworkManager.instance().isDataWIFIUp();
                    boolean bittorrentOnMobileData = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);

                    if (wifiIsUp || (!wifiIsUp && bittorrentOnMobileData)) {
                        if (!download.isComplete()) {
                            items.add(new ResumeDownloadMenuAction(context.get(), download, R.string.resume_torrent_menu_action));
                        } else {
                            //let's see if we can seed...
                            boolean seedTorrents = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
                            boolean seedTorrentsOnWifiOnly = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
                            if ((seedTorrents && !seedTorrentsOnWifiOnly) ||
                                (seedTorrents && seedTorrentsOnWifiOnly && wifiIsUp)) {
                                items.add(new ResumeDownloadMenuAction(context.get(), download, R.string.seed));
                            }
                        }
                    }
                }
            }

            items.add(new CancelMenuAction(context.get(), download, !download.isComplete()));
        } else if (tag instanceof DownloadTransfer) {
            DownloadTransfer download = (DownloadTransfer) tag;
            title = download.getDisplayName();

            boolean errored = download.getStatus() != null && getStatusFromResId(download.getStatus()).contains("Error");

            boolean openMenu = false;
            openMenu |= !errored && download.isComplete() && (tag instanceof HttpDownload || tag instanceof PeerHttpDownload || tag instanceof YouTubeDownload || tag instanceof SoundcloudDownload);

            if (openMenu) {
                items.add(new OpenMenuAction(context.get(), download.getDisplayName(), download.getSavePath().getAbsolutePath(), extractMime(download)));
            }

            if (download instanceof PeerHttpDownload) {
                PeerHttpDownload pdownload = (PeerHttpDownload) download;
                items.add(new BrowsePeerMenuAction(context.get(), pdownload.getPeer()));
            }

            items.add(new CancelMenuAction(context.get(), download, !openMenu));

        } else if (tag instanceof PeerHttpUpload) {
            PeerHttpUpload upload = (PeerHttpUpload) tag;
            title = upload.getDisplayName();

            items.add(new CancelMenuAction(context.get(), upload, false));
        }

        return items.size() > 0 ? new MenuAdapter(context.get(), title, items) : null;
    }

    protected String extractMime(DownloadTransfer download) {
        if (download instanceof PeerHttpDownload) {
            return ((PeerHttpDownload) download).getFD().mime;
        } else {
            return UIUtils.getMimeType(download.getSavePath().getAbsolutePath());
        }
    }

    protected Dialog trackDialog(Dialog dialog) {
        dialogs.add(dialog);
        return dialog;
    }

    private void setupGroupIndicator(View view, boolean expanded, Transfer item) {
        ImageView groupIndicator = findView(view, R.id.view_transfer_list_item_group_indicator);

        if (groupIndicator != null) {
            if (item.getItems().size() <= 1) {
                //show the file type for the only file there is
                String extension = null;
                String path = null;

                if (item instanceof BittorrentDownload) {
                    BittorrentDownload bItem = (BittorrentDownload) item;
                    if (bItem.getItems().size() > 0) {
                        TransferItem transferItem = bItem.getItems().get(0);
                        path = transferItem.getFile().getAbsolutePath();
                        extension = FilenameUtils.getExtension(path);
                    }
                } else if (item instanceof DownloadTransfer) {
                    DownloadTransfer transferItem = (DownloadTransfer) item;
                    if (transferItem.getSavePath() != null) {
                        path = transferItem.getSavePath().getAbsolutePath();
                        extension = FilenameUtils.getExtension(path);
                    }
                } else if (item instanceof PeerHttpUpload) {
                    PeerHttpUpload transferItem = (PeerHttpUpload) item;
                    path = transferItem.getFD().filePath;
                    extension = FilenameUtils.getExtension(path);
                }

                if (extension != null && extension.equals("apk")) {
                    try {
                        //Apk apk = new Apk(context,path);

                        //TODO: Get the APK Icon so we can show the APK icon on the transfer manager once
                        //it's finished downloading, or as it's uploading to another peer.
                        //apk.getDrawable(id);

                        //in the meantime, just hardcode it
                        groupIndicator.setImageResource(R.drawable.browse_peer_application_icon_selector_menu);
                    } catch (Throwable e) {
                        groupIndicator.setImageResource(R.drawable.browse_peer_application_icon_selector_menu);
                    }
                } else {
                    groupIndicator.setImageResource(getFileTypeIconId(extension));
                }
            } else {
                groupIndicator.setImageResource(expanded ? R.drawable.transfer_menuitem_minus : R.drawable.transfer_menuitem_plus);
            }
        }
    }

    private void initTouchFeedback(View v, TransferItem item) {
        v.setOnClickListener(viewOnClickListener);
        v.setOnLongClickListener(viewOnLongClickListener);
        v.setTag(item);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = vg.getChildAt(i);
                initTouchFeedback(child, item);
            }
        }
    }

    private void populateBittorrentDownload(View view, BittorrentDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);

        seeds.setText(context.get().getString(R.string.seeds_n, download.getSeeds()));
        peers.setText(context.get().getString(R.string.peers_n, download.getPeers()));

        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());

        status.setText(TRANSFER_STATE_STRING_MAP.get(download.getStatus()));

        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private void populatePeerDownload(View view, PeerHttpDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(getStatusFromResId(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private void populatePeerUpload(View view, PeerHttpUpload upload) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(upload.getDisplayName());
        progress.setProgress(upload.getProgress());
        status.setText(getStatusFromResId(upload.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(upload.getUploadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(upload.getSize()));

        buttonAction.setTag(upload);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private void populateHttpDownload(View view, HttpDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(getStatusFromResId(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private void populateBittorrentDownloadItem(View view, TransferItem item) {
        ImageView icon = findView(view, R.id.view_transfer_item_list_item_icon);
        TextView title = findView(view, R.id.view_transfer_item_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_item_list_item_progress);
        TextView size = findView(view, R.id.view_transfer_item_list_item_size);
        ImageButton buttonPlay = findView(view, R.id.view_transfer_item_list_item_button_play);

        icon.setImageResource(getFileTypeIconId(FilenameUtils.getExtension(item.getFile().getAbsolutePath())));
        title.setText(item.getDisplayName());
        progress.setProgress(item.getProgress());
        size.setText(UIUtils.getBytesInHuman(item.getSize()));

        buttonPlay.setTag(item);
        buttonPlay.setVisibility(item.isComplete() ? View.VISIBLE : View.GONE);
        buttonPlay.setOnClickListener(playOnClickListener);
    }

    private void populateYouTubeDownload(View view, YouTubeDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(getStatusFromResId(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private void populateSoundcloudDownload(View view, SoundcloudDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(getStatusFromResId(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(viewOnClickListener);
    }

    private String getStatusFromResId(String str) {
        String s = "";
        try {
            s = context.get().getString(Integer.parseInt(str));
        } catch (Throwable e) {
            // ignore
        }
        return s;
    }

    private static int getFileTypeIconId(String ext) {
        MediaType mt = MediaType.getMediaTypeForExtension(ext);
        if (mt == null) {
            return R.drawable.question_mark;
        }
        if (mt.equals(MediaType.getApplicationsMediaType())) {
            return R.drawable.browse_peer_application_icon_selector_menu;
        } else if (mt.equals(MediaType.getAudioMediaType())) {
            return R.drawable.browse_peer_audio_icon_selector_menu;
        } else if (mt.equals(MediaType.getDocumentMediaType())) {
            return R.drawable.browse_peer_document_icon_selector_menu;
        } else if (mt.equals(MediaType.getImageMediaType())) {
            return R.drawable.browse_peer_picture_icon_selector_menu;
        } else if (mt.equals(MediaType.getVideoMediaType())) {
            return R.drawable.browse_peer_video_icon_selector_menu;
        } else if (mt.equals(MediaType.getTorrentMediaType())) {
            return R.drawable.browse_peer_torrent_icon_selector_menu;
        } else {
            return R.drawable.question_mark;
        }
    }

    private final class ViewOnClickListener implements OnClickListener {
        public void onClick(View v) {
            try {
                MenuAdapter adapter = getMenuAdapter(v);
                if (adapter != null) {
                    trackDialog(new MenuBuilder(adapter).show());
                    return;
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create the menu", e);
            }
        }
    }

    private final class ViewOnLongClickListener implements OnLongClickListener {
        public boolean onLongClick(View v) {
            try {
                MenuAdapter adapter = getMenuAdapter(v);
                if (adapter != null) {
                    trackDialog(new MenuBuilder(adapter).show());
                    return true;
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create the menu");
            }
            return false;
        }
    }

    private final class OpenOnClickListener implements OnClickListener {
        public void onClick(View v) {
            TransferItem item = (TransferItem) v.getTag();

            boolean canOpen = false;
            canOpen |= item.isComplete();

            if (canOpen) {
                File savePath = item.getFile();

                if (savePath != null) {
                    if (savePath.exists()) {
                        UIUtils.openFile(context.get(), savePath);
                    } else {
                        UIUtils.showShortMessage(context.get(), R.string.cant_open_file_does_not_exist, savePath.getName());
                    }
                }
            }
        }
    }
}