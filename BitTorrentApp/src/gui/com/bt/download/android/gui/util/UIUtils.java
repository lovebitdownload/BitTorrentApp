/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.bt.download.android.gui.util;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.InputType;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.activities.MainActivity;
import com.bt.download.android.gui.services.Engine;
import com.frostwire.util.MimeDetector;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class UIUtils {

    private static final String TAG = "FW.UIUtils";

    /**
     * Localizable Number Format constant for the current default locale.
     */
    private static NumberFormat NUMBER_FORMAT0; // localized "#,##0"
    private static NumberFormat NUMBER_FORMAT1; // localized "#,##0.0"

    private static String[] BYTE_UNITS = new String[] { "b", "KB", "Mb", "Gb", "Tb" };

    public static String GENERAL_UNIT_KBPSEC = "KB/s";

    static {
        NUMBER_FORMAT0 = NumberFormat.getNumberInstance(Locale.getDefault());
        NUMBER_FORMAT0.setMaximumFractionDigits(0);
        NUMBER_FORMAT0.setMinimumFractionDigits(0);
        NUMBER_FORMAT0.setGroupingUsed(true);

        NUMBER_FORMAT1 = NumberFormat.getNumberInstance(Locale.getDefault());
        NUMBER_FORMAT1.setMaximumFractionDigits(1);
        NUMBER_FORMAT1.setMinimumFractionDigits(1);
        NUMBER_FORMAT1.setGroupingUsed(true);
    }

    public static void showToastMessage(Context context, String message, int duration, int gravity, int xOffset, int yOffset) {
        if (context != null && message != null) {
            Toast toast = Toast.makeText(context, message, duration);
            if (gravity != (Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM)) {
                toast.setGravity(gravity, xOffset, yOffset);
            }
            toast.show();
        }
    }
    
    public static void showToastMessage(Context context, String message, int duration) {
        showToastMessage(context, message, duration, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    }

    public static void showShortMessage(Context context, String message) {
        showToastMessage(context, message, Toast.LENGTH_SHORT);
    }

    public static void showLongMessage(Context context, String message) {
        showToastMessage(context, message, Toast.LENGTH_LONG);
    }

    public static void showShortMessage(Context context, int resId) {
        showShortMessage(context, context.getString(resId));
    }

    public static void showLongMessage(Context context, int resId) {
        showLongMessage(context, context.getString(resId));
    }

    public static void showShortMessage(Context context, int resId, Object... formatArgs) {
        showShortMessage(context, context.getString(resId, formatArgs));
    }

    /**
     * 
     * @param context
     * @param messageId
     * @param titleId
     * @param positiveListener
     */
    public static Dialog showYesNoDialog(Context context, int messageId, int titleId, OnClickListener positiveListener) {
        return showYesNoDialog(context, messageId, titleId, positiveListener, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    public static void showYesNoDialog(Context context, int iconId, String message, int titleId, OnClickListener positiveListener) {
        showYesNoDialog(context, iconId, message, titleId, positiveListener, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    /**
     * 
     * @param context
     * @param messageId
     * @param titleId
     * @param positiveListener
     * @param negativeListener
     */
    public static Dialog showYesNoDialog(Context context, int messageId, int titleId, OnClickListener positiveListener, OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(messageId).setTitle(titleId).setCancelable(false).setPositiveButton(android.R.string.yes, positiveListener).setNegativeButton(android.R.string.no, negativeListener);
        Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static void showYesNoDialog(Context context, int iconId, int messageId, int titleId, OnClickListener positiveListener, OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setIcon(iconId).setMessage(messageId).setTitle(titleId).setCancelable(false).setPositiveButton(android.R.string.yes, positiveListener).setNegativeButton(android.R.string.no, negativeListener);

        builder.create().show();
    }

    public static void showYesNoDialog(Context context, String message, int titleId, OnClickListener positiveListener, OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(message).setTitle(titleId).setCancelable(false).setPositiveButton(android.R.string.yes, positiveListener).setNegativeButton(android.R.string.no, negativeListener);

        builder.create().show();
    }

    public static void showYesNoDialog(Context context, int iconId, String message, int titleId, OnClickListener positiveListener, OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setIcon(iconId).setMessage(message).setTitle(titleId).setCancelable(false).setPositiveButton(android.R.string.yes, positiveListener).setNegativeButton(android.R.string.no, negativeListener);

        builder.create().show();
    }

    public static void showOkCancelDialog(Context context, View view, int titleId, OnClickListener okListener, OnClickListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(view).setTitle(titleId).setPositiveButton(android.R.string.ok, okListener).setNegativeButton(android.R.string.cancel, cancelListener);

        builder.create().show();
    }

    public static void showOkCancelDialog(Context context, View view, int titleId, OnClickListener okListener) {
        showOkCancelDialog(context, view, titleId, okListener, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    public static EditText buildEditTextWithType(Context context, KeyListener keyListener, String text) {
        EditText editText = new EditText(context);
        editText.setKeyListener(keyListener);
        editText.setText(text);
        return editText;
    }

    public static EditText buildNumericEditText(Context context, String text) {
        return buildEditTextWithType(context, new NumberKeyListener() {
            @Override
            public int getInputType() {
                return InputType.TYPE_CLASS_NUMBER;
            }

            @Override
            protected char[] getAcceptedChars() {
                return new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
            }
        }, text);
    }

    public static String getBytesInHuman(float size) {
        int i = 0;
        for (i = 0; size > 1024; i++) {
            size /= 1024f;
        }
        return String.format(Locale.US, "%.2f %s", size, BYTE_UNITS[i]);
    }

    /**
     * Converts an rate into a human readable and localized KB/s speed.
     */
    public static String rate2speed(double rate) {
        return NUMBER_FORMAT0.format(rate) + " " + GENERAL_UNIT_KBPSEC;
    }

    public static String getFileTypeAsString(Resources resources, byte fileType) {
        switch (fileType) {
        case Constants.FILE_TYPE_APPLICATIONS:
            return resources.getString(R.string.applications);
        case Constants.FILE_TYPE_AUDIO:
            return resources.getString(R.string.audio);
        case Constants.FILE_TYPE_DOCUMENTS:
            return resources.getString(R.string.documents);
        case Constants.FILE_TYPE_PICTURES:
            return resources.getString(R.string.pictures);
        case Constants.FILE_TYPE_RINGTONES:
            return resources.getString(R.string.ringtones);
        case Constants.FILE_TYPE_VIDEOS:
            return resources.getString(R.string.video);
        case Constants.FILE_TYPE_TORRENTS:
            return resources.getString(R.string.media_type_torrents);
        default:
            return resources.getString(R.string.unknown);
        }
    }

    public static int getFileTypeIconId(byte fileType) {
        switch (fileType) {
        case Constants.FILE_TYPE_APPLICATIONS:
            return R.drawable.browse_peer_application_icon_selector_off;
        case Constants.FILE_TYPE_AUDIO:
            return R.drawable.browse_peer_audio_icon_selector_off;
        case Constants.FILE_TYPE_DOCUMENTS:
            return R.drawable.browse_peer_document_icon_selector_off;
        case Constants.FILE_TYPE_PICTURES:
            return R.drawable.browse_peer_picture_icon_selector_off;
        case Constants.FILE_TYPE_RINGTONES:
            return R.drawable.browse_peer_ringtone_icon_selector_off;
        case Constants.FILE_TYPE_VIDEOS:
            return R.drawable.browse_peer_video_icon_selector_off;
        default:
            return R.drawable.question_mark;
        }
    }

    /**
     * Opens the given file with the default Android activity for that File and
     * mime type.
     * 
     * @param filePath
     * @param mime
     */
    public static void openFile(Context context, String filePath, String mime) {
        try {
            if (!openAudioInternal(filePath)) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(new File(filePath)), mime);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);

                if (mime != null && mime.contains("video")) {
                    if (MusicUtils.isPlaying()) {
                        MusicUtils.playOrPause();
                    }
                    UXStats.instance().log(UXAction.LIBRARY_VIDEO_PLAY);
                }
            }
        } catch (Throwable e) {
            UIUtils.showShortMessage(context, R.string.cant_open_file);
            Log.e(TAG, "Failed to open file: " + filePath, e);
        }
    }

    public static void openFile(Context context, File file) {
        openFile(context, file.getAbsolutePath(), getMimeType(file.getAbsolutePath()));
    }

    public static void openURL(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    public static String getMimeType(String filePath) {
        try {
            return MimeDetector.getMimeType(FilenameUtils.getExtension(filePath));
        } catch (Throwable e) {
            Log.e(TAG, "Failed to read mime type for: " + filePath);
            return MimeDetector.UNKNOWN;
        }
    }

    /**
     * Create an ephemeral playlist with the files of the same type that live on the folder of the given file descriptor and play it.
     * @param fd
     */
    public static void playEphemeralPlaylist(FileDescriptor fd) {
        Engine.instance().getMediaPlayer().play(Librarian.instance().createEphemeralPlaylist(fd));
    }

    private static boolean openAudioInternal(String filePath) {
        try {
            UXStats.instance().log(UXAction.LIBRARY_PLAY_AUDIO_FROM_FILE);

            List<FileDescriptor> fds = Librarian.instance().getFiles(filePath, true);

            if (fds.size() == 1 && fds.get(0).fileType == Constants.FILE_TYPE_AUDIO) {
                playEphemeralPlaylist(fds.get(0));
                return true;
            } else {
                return false;
            }
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * This method sets up the visibility of the support frostwire control (@see {@link DonationsView})
     * depending on remote configuration parameters and local configuration preferences.
     * @param supportFrostWireView
     */
    public static void supportFrostWire(View supportFrostWireView) {
        //remote kill switch
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE_THRESHOLD)) {
            supportFrostWireView.setVisibility(View.GONE);
            Log.v(TAG, "Hiding support, above threshold.");
        } else if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE)) {
            supportFrostWireView.setVisibility(View.VISIBLE);

            if (supportFrostWireView.getLayoutParams() != null) {
                supportFrostWireView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
            }
        }
    }

    /**
     * Android devices with SDK below target=11 do not support textView.setAlpha().
     * This is a work around. 
     * @param v - the text view
     * @param alpha - a value from 0 to 255. (0=transparent, 255=fully visible)
     */
    public static void setTextViewAlpha(TextView v, int alpha) {
        v.setTextColor(v.getTextColors().withAlpha(alpha));
        v.setHintTextColor(v.getHintTextColors().withAlpha(alpha));
        v.setLinkTextColor(v.getLinkTextColors().withAlpha(alpha));

        Drawable[] compoundDrawables = v.getCompoundDrawables();
        for (int i = 0; i < compoundDrawables.length; i++) {
            Drawable d = compoundDrawables[i];
            if (d != null) {
                d.setAlpha(alpha);
            }
        }

    }

    /**
     * Checks setting to show or not the transfers window right after a download has started.
     * This should probably be moved elsewhere (similar to GUIMediator on the desktop)
     * @param activity
     */
    public static void showTransfersOnDownloadStart(Context context) {
        if (ConfigurationManager.instance().showTransfersOnDownloadStart() && context != null) {
            Intent i = new Intent(context, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(i);
        }
    }
    
    public static void showKeyboard(Context context, View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
    
    public static void hideKeyboardFromActivity(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}