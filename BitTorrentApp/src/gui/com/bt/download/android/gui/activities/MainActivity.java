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

package com.bt.download.android.gui.activities;

import static com.andrew.apollo.utils.MusicUtils.mService;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.PeerManager;
import com.bt.download.android.gui.SoftwareUpdater;
import com.bt.download.android.gui.activities.internal.MainController;
import com.bt.download.android.gui.activities.internal.MainMenuAdapter;
import com.bt.download.android.gui.dialogs.YesNoDialog;
import com.bt.download.android.gui.fragments.AboutFragment;
import com.bt.download.android.gui.fragments.BrowsePeerFragment;
import com.bt.download.android.gui.fragments.BrowsePeersDisabledFragment;
import com.bt.download.android.gui.fragments.BrowsePeersFragment;
import com.bt.download.android.gui.fragments.MainFragment;
import com.bt.download.android.gui.fragments.SearchFragment;
import com.bt.download.android.gui.fragments.TransfersFragment;
import com.bt.download.android.gui.fragments.TransfersFragment.TransferStatus;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.transfers.TransferManager;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.AbstractActivity;
import com.bt.download.android.gui.views.AbstractDialog;
import com.bt.download.android.gui.views.AbstractDialog.OnDialogClickListener;
import com.bt.download.android.gui.views.PlayerMenuItemView;
import com.bt.download.android.gui.views.TimerObserver;
import com.bt.download.android.gui.views.TimerService;
import com.bt.download.android.gui.views.TimerSubscription;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;
import com.umeng.analytics.MobclickAgent;
import com.wandoujia.ads.sdk.Ads;
import com.wandoujia.ads.sdk.loader.Fetcher;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Stack;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractActivity implements  OnDialogClickListener, ServiceConnection {

    private static final Logger LOG = Logger.getLogger(MainActivity.class);

    private static final String FRAGMENTS_STACK_KEY = "fragments_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String DUR_TOKEN_KEY = "dur_token";
    private static final String APPIA_STARTED_KEY = "appia_started";

    private static final String LAST_BACK_DIALOG_ID = "last_back_dialog";
    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";

    private static boolean firstTime = true;

    private MainController controller;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private View leftDrawer;
    private ListView listMenu;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private BrowsePeersDisabledFragment peersDisabled;
    private AboutFragment about;

    private Fragment currentFragment;
    private final Stack<Integer> fragmentsStack;

    private PlayerMenuItemView playerItem;

    private TimerSubscription playerSubscription;

    private BroadcastReceiver mainBroadcastReceiver;

    public MainActivity() {
        super(R.layout.activity_main);
        this.controller = new MainController(this);
        this.fragmentsStack = new Stack<Integer>();
    }

    public void showMyFiles() {
        controller.showMyFiles();
    }

    public void switchFragment(int itemId) {
        controller.switchFragment(itemId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!(getCurrentFragment() instanceof SearchFragment)) {
                controller.switchFragment(R.id.menu_main_search);
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleDrawer();
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (fragmentsStack.size() > 1) {
            try {
                fragmentsStack.pop();
                int id = fragmentsStack.peek();
                Fragment fragment = getFragmentManager().findFragmentById(id);
                switchContent(fragment, false);
            } catch (Throwable e) {
                // don't break the app
                handleLastBackPressed();
            }
        } else {
            handleLastBackPressed();
        }

        syncSlideMenu();
        updateHeader(getCurrentFragment());
    }

    private boolean isShutdown(Intent intent) {
        if (intent == null) {
            return false;
        }

        if (intent.getBooleanExtra("shutdown-" + ConfigurationManager.instance().getUUIDString(), false)) {
            finish();
            Engine.instance().shutdown();
            return true;
        }

        return false;
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {

        if (isShutdown(getIntent())) {
            return;
        }

        drawerLayout = findView(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                refreshPlayerItem();
                syncSlideMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }
        });

        leftDrawer = findView(R.id.activity_main_left_drawer);
        listMenu = findView(R.id.left_drawer);

        playerItem = findView(R.id.slidemenu_player_menuitem);
        playerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.launchPlayerActivity();
            }
        });

        setupFragments();

        setupMenuItems();

        setupInitialFragment(savedInstanceState);

        playerSubscription = TimerService.subscribe((TimerObserver) findView(R.id.activity_main_player_notifier), 1);

        onNewIntent(getIntent());

        setupActionBar();
        setupDrawer();

        //PlaybackService.get(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (isShutdown(intent)) {
            return;
        }

        String action = intent.getAction();
        //onResumeFragments();

        if (action != null && action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
            if (Ads.isLoaded(Fetcher.AdFormat.interstitial, Constants.TAG_INTERSTITIAL_WIDGET)) {
                Ads.showAppWidget(this, null, Constants.TAG_INTERSTITIAL_WIDGET,
                        Ads.ShowMode.FULL_SCREEN);
            }

            controller.showTransfers(TransferStatus.ALL);
        } else if (action != null && action.equals(Constants.ACTION_OPEN_TORRENT_URL)) {
            //Open a Torrent from a URL or from a local file :), say from Astro File Manager.
            /**
             * TODO: Ask @aldenml the best way to plug in NewTransferDialog.
             * I've refactored this dialog so that it is forced (no matter if the setting
             * to not show it again has been used) and when that happens the checkbox is hidden.
             * 
             * However that dialog requires some data about the download, data which is not
             * obtained until we have instantiated the Torrent object.
             * 
             * I'm thinking that we can either:
             * a) Pass a parameter to the transfer manager, but this would probably
             * not be cool since the transfer manager (I think) should work independently from
             * the UI thread.
             * 
             * b) Pass a "listener" to the transfer manager, once the transfer manager has the torrent
             * it can notify us and wait for the user to decide wether or not to continue with the transfer
             * 
             * c) Forget about showing that dialog, and just start the download, the user can cancel it.
             */

            //Show me the transfer tab
            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

            //go!
            TransferManager.instance().downloadTorrent(intent.getDataString());
        }
        // When another application wants to "Share" a file and has chosen FrostWire to do so.
        // We make the file "Shared" so it's visible for other FrostWire devices on the local network.
        else if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {
            controller.handleSendAction(intent);
        }

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            controller.showTransfers(TransferStatus.COMPLETED);
            TransferManager.instance().clearDownloadsToReview();
            try {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED);
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH)) {
                    File file = new File(extras.getString(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH));
                    if (file.isFile()) {
                        UIUtils.openFile(this, file.getAbsoluteFile());
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Error handling download complete notification", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshPeersFragment();

        mainResume();

        registerMainBroadcastReceiver();

        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mainBroadcastReceiver != null) {
            unregisterReceiver(mainBroadcastReceiver);
        }

        MobclickAgent.onPause(this);
    }

    private void registerMainBroadcastReceiver() {
        mainBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.ACTION_NOTIFY_SDCARD_MOUNTED.equals(intent.getAction())) {
                    onNotifySdCardMounted();
                }
            }
        };

        IntentFilter bf = new IntentFilter(Constants.ACTION_NOTIFY_SDCARD_MOUNTED);
        registerReceiver(mainBroadcastReceiver, bf);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLastFragment(outState);
        saveFragmentsStack(outState);
    }

    private ServiceToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToken = MusicUtils.bindToService(this, this);

        try {
            Ads.init(MainActivity.this, Constants.ADS_APP_ID, Constants.ADS_SECRET_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Ads.preLoad(this, Fetcher.AdFormat.interstitial, Constants.TAG_INTERSTITIAL_WIDGET);
        Ads.preLoad(this, Fetcher.AdFormat.banner, Constants.TAG_BANNER);
    }

    @Override
    public void onStart() {
        if (currentFragment instanceof TransfersFragment && transfers.adBanner != null) {
            transfers.adBanner.startAutoScroll();
        }
        if (currentFragment instanceof SearchFragment && search.adBanner != null) {
            search.adBanner.startAutoScroll();
        }

        super.onStart();
    }

    @Override
    public void onStop() {
        if (transfers.adBanner != null) {
            transfers.adBanner.stopAutoScroll();
        }

        if (search.adBanner != null) {
            search.adBanner.stopAutoScroll();
        }

        super.onStop();
    }

    private void onNotifySdCardMounted() {
        transfers.initStorageRelatedRichNotifications(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerSubscription != null) {
            playerSubscription.unsubscribe();
        }

        //avoid memory leaks when the device is tilted and the menu gets recreated.
        SoftwareUpdater.instance().removeConfigurationUpdateListener(this);

        if (playerItem != null) {
            playerItem.unbindDrawables();
        }

        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void mainResume() {
        syncSlideMenu();

        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }

    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(leftDrawer)) {
            drawerLayout.closeDrawer(leftDrawer);
        } else {
            drawerLayout.openDrawer(leftDrawer);
        }

        updateHeader(getCurrentFragment());
    }

    private Fragment getWifiSharingFragment() {
        return Engine.instance().isStarted() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP) ? peers : peersDisabled;
    }

    private void handleLastBackPressed() {
        YesNoDialog dlg = YesNoDialog.newInstance(LAST_BACK_DIALOG_ID, R.string.minimize_frostwire, R.string.are_you_sure_you_wanna_leave);
        dlg.show(getFragmentManager());
    }

    private void handleShutdownRequest() {
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message);
        dlg.show(getFragmentManager());
    }

    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(LAST_BACK_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            finish();
        } else if (tag.equals(SHUTDOWN_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            controller.shutdown();
        }
    }

    private void syncSlideMenu() {
        Fragment fragment = getCurrentFragment();

        if (fragment instanceof SearchFragment) {
            setSelectedItem(R.id.menu_main_search);
        } else if (fragment instanceof BrowsePeerFragment) {
            setSelectedItem(R.id.menu_main_library);
        } else if (fragment instanceof TransfersFragment) {
            setSelectedItem(R.id.menu_main_transfers);
        } else if (fragment instanceof BrowsePeersFragment || fragment instanceof BrowsePeersDisabledFragment) {
            setSelectedItem(R.id.menu_main_peers);
        } else if (fragment instanceof AboutFragment) {
            setSelectedItem(R.id.menu_main_about);
        }

        updateHeader(getCurrentFragment());
    }

    private void setSelectedItem(int id) {
        try {
            int position = 0;
            MainMenuAdapter adapter = (MainMenuAdapter) listMenu.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItemId(i) == id) {
                    position = i;
                }
            }
            listMenu.setItemChecked(position, true);
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error setting slide menu item selected", e);
        }
    }

    private void refreshPlayerItem() {
        if (playerItem != null) {
            playerItem.refresh();
        }
    }

    private void setupMenuItems() {
        listMenu.setAdapter(new MainMenuAdapter(this));
        listMenu.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                controller.closeSlideMenu();

                try {
                    if (id == R.id.menu_main_preferences) {
                        controller.showPreferences();
                    }  else if (id == R.id.menu_main_shutdown) {
                        handleShutdownRequest();
                    } else {
                        listMenu.setItemChecked(position, true);
                        controller.switchFragment((int) id);
                    }
                } catch (Throwable e) { // protecting from weird android UI engine issues
                    LOG.error("Error clicking slide menu item", e);
                }
            }
        });
    }

    private void setupFragments() {
        search = (SearchFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_search);
        library = (BrowsePeerFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_browse_peer);
        transfers = (TransfersFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_transfers);
        peers = (BrowsePeersFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_browse_peers);
        peersDisabled = (BrowsePeersDisabledFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_browse_peers_disabled);
        about = (AboutFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_about);

        hideFragments(getFragmentManager().beginTransaction()).commit();

        library.setPeer(PeerManager.instance().getLocalPeer());
    }

    private FragmentTransaction hideFragments(FragmentTransaction ts) {
        return ts.hide(search).hide(library).hide(transfers).hide(peers).hide(peersDisabled).hide(about);
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;

        if (savedInstanceState != null) {
            fragment = getFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
            restoreFragmentsStack(savedInstanceState);
        }
        if (fragment == null) {
            fragment = search;
            setSelectedItem(R.id.menu_main_search);
        }

        switchContent(fragment);
    }

    private void saveFragmentsStack(Bundle outState) {
        int[] stack = new int[fragmentsStack.size()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = fragmentsStack.get(i);
        }
        outState.putIntArray(FRAGMENTS_STACK_KEY, stack);
    }

    private void restoreFragmentsStack(Bundle savedInstanceState) {
        try {
            int[] stack = savedInstanceState.getIntArray(FRAGMENTS_STACK_KEY);
            for (int id : stack) {
                fragmentsStack.push(id);
            }
        } catch (Throwable e) {
            // silent recovering, stack is't not really important
        }
    }

    private void updateHeader(Fragment fragment) {
        try {
            RelativeLayout placeholder = (RelativeLayout) getActionBar().getCustomView();//findView(R.id.activity_main_layout_header_placeholder);
            if (placeholder != null && placeholder.getChildCount() > 0) {
                placeholder.removeAllViews();
            }

            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (placeholder != null && header != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    placeholder.addView(header, params);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    private void refreshPeersFragment() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof BrowsePeersFragment || fragment instanceof BrowsePeersDisabledFragment) {
            controller.switchFragment(R.id.menu_main_peers);
        }
        PeerManager.instance().updateLocalPeer();
    }

    private void switchContent(Fragment fragment, boolean addToStack) {
        hideFragments(getFragmentManager().beginTransaction()).show(fragment).commitAllowingStateLoss();
        if (addToStack && (fragmentsStack.isEmpty() || fragmentsStack.peek() != fragment.getId())) {
            fragmentsStack.push(fragment.getId());
        }

        if (fragment instanceof TransfersFragment) {
            if (transfers.adBanner != null) {
                transfers.adBanner.startAutoScroll();
            }
        } else {
            if (transfers.adBanner != null) {
                transfers.adBanner.stopAutoScroll();
            }
        }

        if (fragment instanceof SearchFragment) {
            if (search.adBanner != null) {
                search.adBanner.startAutoScroll();
            }
        } else {
            if (search.adBanner != null) {
                search.adBanner.stopAutoScroll();
            }
        }

        currentFragment = fragment;
        updateHeader(fragment);
    }

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByMenuId(int id) {
        switch (id) {
            case R.id.menu_main_search:
                return search;
            case R.id.menu_main_library:
                return library;
            case R.id.menu_main_transfers:
                return transfers;
            case R.id.menu_main_peers:
                return getWifiSharingFragment();
            case R.id.menu_main_about:
                return about;
            default:
                return null;
        }
    }

    public void switchContent(Fragment fragment) {
        switchContent(fragment, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public void closeSlideMenu() {
        drawerLayout.closeDrawer(leftDrawer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();

        bar.setCustomView(R.layout.view_custom_actionbar);

        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
    }

    private void setupDrawer() {
        drawerToggle = new MenuDrawerToggle(this, drawerLayout);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    private static final class MenuDrawerToggle extends ActionBarDrawerToggle {

        private final WeakReference<MainActivity> activityRef;

        public MenuDrawerToggle(MainActivity activity, DrawerLayout drawerLayout) {
            super(activity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

            // aldenml: even if the parent class hold a strong reference, I decided to keep a weak one
            this.activityRef = Ref.weak(activity);
        }

        @Override
        public void onDrawerClosed(View view) {
            if (Ref.alive(activityRef)) {
                activityRef.get().invalidateOptionsMenu();
            }
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (Ref.alive(activityRef)) {
                UIUtils.hideKeyboardFromActivity(activityRef.get());
                activityRef.get().invalidateOptionsMenu();
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (Ref.alive(activityRef)) {
                MainActivity activity = activityRef.get();
                activity.refreshPlayerItem();
                activity.syncSlideMenu();
            }
        }
    }

    public void performYTSearch(String ytUrl) {
        SearchFragment searchFragment = (SearchFragment) getFragmentByMenuId(R.id.menu_main_search);
        searchFragment.performYTSearch(ytUrl);
        switchContent(searchFragment);
    }

}
