package net.programmierecke.radiodroid2;

import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;

import com.google.android.material.tabs.TabLayout;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.rustamg.filedialogs.FileDialog;
import com.rustamg.filedialogs.OpenFileDialog;
import com.rustamg.filedialogs.SaveFileDialog;

import net.programmierecke.radiodroid2.alarm.FragmentAlarm;
import net.programmierecke.radiodroid2.alarm.TimePickerFragment;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;
import net.programmierecke.radiodroid2.players.PlayState;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.players.mpd.MPDServersRepository;
import net.programmierecke.radiodroid2.players.PlayStationTask;
import net.programmierecke.radiodroid2.players.selector.PlayerType;
import net.programmierecke.radiodroid2.service.MediaSessionCallback;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.StationsFilter;

import java.io.File;
import java.util.Date;

import okhttp3.OkHttpClient;

import static net.programmierecke.radiodroid2.service.MediaSessionCallback.EXTRA_STATION_UUID;

public class ActivityMain extends AppCompatActivity implements SearchView.OnQueryTextListener, NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener, FileDialog.OnFileSelectedListener, TimePickerDialog.OnTimeSetListener, SearchPreferenceResultListener {

    public static final String EXTRA_SEARCH_TAG = "search_tag";

    public static final int LAUNCH_EQUALIZER_REQUEST = 1;

    public final static int MAX_DYNAMIC_LAUNCHER_SHORTCUTS = 4;

    public static final int FRAGMENT_FROM_BACKSTACK = 777;

    public static final String ACTION_SHOW_LOADING = "net.programmierecke.radiodroid2.show_loading";
    public static final String ACTION_HIDE_LOADING = "net.programmierecke.radiodroid2.hide_loading";

    private static final String TAG = "RadioDroid";

    private final String TAG_SEARCH_URL = "json/stations/bytagexact";
    private final String SAVE_LAST_MENU_ITEM = "LAST_MENU_ITEM";

    public static final int PERM_REQ_STORAGE_FAV_SAVE = 1;
    public static final int PERM_REQ_STORAGE_FAV_LOAD = 2;

    private SearchView mSearchView;

    private AppBarLayout appBarLayout;
    private TabLayout tabsView;

    DrawerLayout mDrawerLayout;
    NavigationView mNavigationView;
    BottomNavigationView mBottomNavigationView;
    FragmentManager mFragmentManager;

    private BottomSheetBehavior playerBottomSheet;

    private FragmentPlayerSmall smallPlayerFragment;
    private FragmentPlayerFull fullPlayerFragment;

    BroadcastReceiver broadcastReceiver;

    MenuItem menuItemSearch;
    MenuItem menuItemDelete;
    MenuItem menuItemSleepTimer;
    MenuItem menuItemSave;
    MenuItem menuItemLoad;
    MenuItem menuItemIconsView;
    MenuItem menuItemListView;
    MenuItem menuItemAddAlarm;
    MenuItem menuItemMpd;

    private SharedPreferences sharedPref;

    private int selectedMenuItem;

    private boolean instanceStateWasSaved;

    private Date lastExitTry;

    private AlertDialog meteredConnectionAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Iconics.init(this);

        super.onCreate(savedInstanceState);

        if (sharedPref == null) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        }
        setTheme(Utils.getThemeResId(this));
        setContentView(R.layout.layout_main);

        try {
            File dir = new File(getFilesDir().getAbsolutePath());
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    if (BuildConfig.DEBUG) {
                        Log.d("MAIN", "delete file:" + aChildren);
                    }
                    try {
                        new File(dir, aChildren).delete();
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }

        final Toolbar myToolbar = findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(myToolbar);

        PlayerServiceUtil.bind(this);

        selectedMenuItem = sharedPref.getInt("last_selectedMenuItem", -1);
        instanceStateWasSaved = savedInstanceState != null;
        mFragmentManager = getSupportFragmentManager();

        appBarLayout = findViewById(R.id.app_bar_layout);
        tabsView = findViewById(R.id.tabs);
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mNavigationView = findViewById(R.id.my_navigation_view);
        mBottomNavigationView = findViewById(R.id.bottom_navigation);

        if (Utils.bottomNavigationEnabled(this)) {
            mBottomNavigationView.setOnNavigationItemSelectedListener(this);
            mNavigationView.setVisibility(View.GONE);
            mNavigationView.getLayoutParams().width = 0;
        } else {
            mNavigationView.setNavigationItemSelectedListener(this);
            mBottomNavigationView.setVisibility(View.GONE);

            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);
            mDrawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        smallPlayerFragment = (FragmentPlayerSmall) mFragmentManager.findFragmentById(R.id.fragment_player_small);
        fullPlayerFragment = (FragmentPlayerFull) mFragmentManager.findFragmentById(R.id.fragment_player_full);

        if (smallPlayerFragment == null || fullPlayerFragment == null) {
            smallPlayerFragment = new FragmentPlayerSmall();
            fullPlayerFragment = new FragmentPlayerFull();

            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            // Hide it at start to make .onHiddenChanged be called on first show
            fragmentTransaction.hide(fullPlayerFragment);
            fragmentTransaction.replace(R.id.fragment_player_small, smallPlayerFragment);
            fragmentTransaction.replace(R.id.fragment_player_full, fullPlayerFragment);
            fragmentTransaction.commit();
        }

        smallPlayerFragment.setCallback(new FragmentPlayerSmall.Callback() {
            @Override
            public void onToggle() {
                toggleBottomSheetState();
            }
        });
        fullPlayerFragment.setTouchInterceptListener(new FragmentPlayerFull.TouchInterceptListener() {
            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                findViewById(R.id.bottom_sheet).getParent().requestDisallowInterceptTouchEvent(disallow);
            }
        });

        // Disable ability of ToolBar to follow bottom sheet because it doesn't work well with
        // our custom RecyclerAwareNestedScrollView
        CoordinatorLayout.LayoutParams coordinatorLayoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior appBarLayoutBehavior = new AppBarLayout.Behavior() {
            @Override
            public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
                return playerBottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED;
            }
        };

        coordinatorLayoutParams.setBehavior(appBarLayoutBehavior);

        playerBottomSheet = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        playerBottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private int oldState = BottomSheetBehavior.STATE_COLLAPSED;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Prevent bottom sheet from minimizing if its content isn't scrolled to the top
                // Essentially this is a cheap hack to prevent bottom sheet from being dragged by non-scrolling elements.
                if (newState == BottomSheetBehavior.STATE_DRAGGING && oldState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (fullPlayerFragment.isScrolled()) {
                        playerBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                        return;
                    }
                }

                // Small player should serve as header if full screen player is expanded.
                // Hide full screen player's fragment if it is not visible to reduce resource usage.

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (smallPlayerFragment.getContext() == null)
                        return;

                    appBarLayout.setExpanded(false);
                    smallPlayerFragment.setRole(FragmentPlayerSmall.Role.HEADER);

                    FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.hide(mFragmentManager.findFragmentById(R.id.containerView));
                    fragmentTransaction.commit();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    appBarLayout.setExpanded(true);
                    smallPlayerFragment.setRole(FragmentPlayerSmall.Role.PLAYER);
                    fullPlayerFragment.resetScroll();

                    FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.hide(fullPlayerFragment);
                    fragmentTransaction.commit();
                }

                if (oldState == BottomSheetBehavior.STATE_EXPANDED && newState != BottomSheetBehavior.STATE_EXPANDED) {
                    FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.show(mFragmentManager.findFragmentById(R.id.containerView));
                    fragmentTransaction.commit();
                }

                if (oldState == BottomSheetBehavior.STATE_COLLAPSED && newState != oldState) {
                    fullPlayerFragment.init();

                    FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.show(fullPlayerFragment);
                    fragmentTransaction.commit();
                }

                oldState = newState;
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        CastHandler.onCreate(this);

        setupStartUpFragment();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        // If menuItem == null method was executed manually
        if (menuItem != null)
            selectedMenuItem = menuItem.getItemId();

        if (playerBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            playerBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        if (mSearchView != null) {
            mSearchView.clearFocus();
        }

        mDrawerLayout.closeDrawers();
        Fragment f = null;
        String backStackTag = String.valueOf(selectedMenuItem);

        switch (selectedMenuItem) {
            case R.id.nav_item_stations:
                f = new FragmentTabs();
                break;
            case R.id.nav_item_starred:
                f = new FragmentStarred();
                break;
            case R.id.nav_item_history:
                f = new FragmentHistory();
                break;
            case R.id.nav_item_alarm:
                f = new FragmentAlarm();
                break;
            case R.id.nav_item_settings:
                f = new FragmentSettings();
                break;
            default:
        }

        // Without "Immediate", "Settings" fragment may become forever stuck in limbo receiving onResume.
        // I'm not sure why.
        mFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        if (Utils.bottomNavigationEnabled(this))
            fragmentTransaction.replace(R.id.containerView, f).commit();
        else
            fragmentTransaction.replace(R.id.containerView, f).addToBackStack(backStackTag).commit();

        // User selected a menuItem. Let's hide progressBar
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
        invalidateOptionsMenu();
        checkMenuItems();

        appBarLayout.setExpanded(true);

        return false;
    }

    @Override
    public void onBackPressed() {
        if (playerBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            playerBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        int backStackCount = mFragmentManager.getBackStackEntryCount();
        FragmentManager.BackStackEntry backStackEntry;

        if (backStackCount > 0) {
            // FRAGMENT_FROM_BACKSTACK value added as a backstack name for non-root fragments like Recordings, About, etc
            backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 1);
            if (backStackEntry.getName().equals("SearchPreferenceFragment")) {
                super.onBackPressed();
                return;
            }
            int parsedId = Integer.parseInt(backStackEntry.getName());
            if (parsedId == FRAGMENT_FROM_BACKSTACK) {
                super.onBackPressed();
                invalidateOptionsMenu();
                return;
            }
        }

        // Don't support backstack with BottomNavigationView
        if (Utils.bottomNavigationEnabled(this)) {
            // I'm giving 3 seconds on making a choice
            if (lastExitTry != null && new Date().getTime() < lastExitTry.getTime() + 3 * 1000) {
                PlayerServiceUtil.shutdownService();
                finish();
            } else {
                Toast.makeText(this, R.string.alert_press_back_to_exit, Toast.LENGTH_SHORT).show();
                lastExitTry = new Date();
                return;
            }
        }

        if (backStackCount > 1) {
            backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 2);

            selectedMenuItem = Integer.parseInt(backStackEntry.getName());

            if (!Utils.bottomNavigationEnabled(this)) {
                mNavigationView.setCheckedItem(selectedMenuItem);
            }
            invalidateOptionsMenu();

        } else {
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "on request permissions result:" + requestCode);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERM_REQ_STORAGE_FAV_LOAD: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LoadFavourites();
                }
                return;
            }
            case PERM_REQ_STORAGE_FAV_SAVE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SaveFavourites();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PlayerServiceUtil.unBind(this);
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor ed = sharedPref.edit();
        ed.putInt("last_selectedMenuItem", selectedMenuItem);
        ed.apply();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PAUSED");
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        super.onPause();

        if (PlayerServiceUtil.getPlayerState() == PlayState.Idle) {
            PlayerServiceUtil.shutdownService();
        } else if (PlayerServiceUtil.isPlaying()) {
            CastHandler.onPause();
        }
    }

    private void handleIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        if (MediaSessionCallback.ACTION_PLAY_STATION_BY_UUID.equals(action)) {
            final Context context = getApplicationContext();
            final String stationUUID = extras.getString(EXTRA_STATION_UUID);
            if (TextUtils.isEmpty(stationUUID))
                return;
            intent.removeExtra(EXTRA_STATION_UUID); // mark intent as consumed
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            final OkHttpClient httpClient = radioDroidApp.getHttpClient();

            new AsyncTask<Void, Void, DataRadioStation>() {
                @Override
                protected DataRadioStation doInBackground(Void... params) {
                    return Utils.getStationByUuid(httpClient, context, stationUUID);
                }

                @Override
                protected void onPostExecute(DataRadioStation station) {
                    if (!isFinishing()) {
                        if (station != null) {
                            Utils.showPlaySelection(radioDroidApp, station, getSupportFragmentManager());

                            Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 1);
                            if (currentFragment instanceof FragmentHistory) {
                                ((FragmentHistory) currentFragment).RefreshListGui();
                            }
                        }
                    }
                }
            }.execute();
        } else {
            final String searchTag = extras.getString(EXTRA_SEARCH_TAG);
            Log.d("MAIN","received search request for tag 1: "+searchTag);
            if (searchTag != null) {
                Log.d("MAIN","received search request for tag 2: "+searchTag);
                Search(StationsFilter.SearchStyle.ByTagExact, searchTag);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "RESUMED");
        }

        setupBroadcastReceiver();

        PlayerServiceUtil.bind(this);
        CastHandler.onResume();

        if (playerBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            appBarLayout.setExpanded(false);
        }

        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
            setIntent(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        menuItemSleepTimer = menu.findItem(R.id.action_set_sleep_timer);
        menuItemSearch = menu.findItem(R.id.action_search);
        menuItemDelete = menu.findItem(R.id.action_delete);
        menuItemSave = menu.findItem(R.id.action_save);
        menuItemLoad = menu.findItem(R.id.action_load);
        menuItemListView = menu.findItem(R.id.action_list_view);
        menuItemIconsView = menu.findItem(R.id.action_icons_view);
        menuItemAddAlarm = menu.findItem(R.id.action_add_alarm);
        menuItemMpd = menu.findItem(R.id.action_mpd);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItemSearch);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            private int prevTabsVisibility = View.GONE;

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (Utils.bottomNavigationEnabled(ActivityMain.this)) {
                    mBottomNavigationView.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
                }

                if (hasFocus) {
                    prevTabsVisibility = tabsView.getVisibility();
                    tabsView.setVisibility(View.GONE);
                } else {
                    tabsView.setVisibility(prevTabsVisibility);
                }

            }
        });

        menuItemSleepTimer.setVisible(false);
        menuItemSearch.setVisible(false);
        menuItemDelete.setVisible(false);
        menuItemSave.setVisible(false);
        menuItemLoad.setVisible(false);
        menuItemListView.setVisible(false);
        menuItemIconsView.setVisible(false);
        menuItemAddAlarm.setVisible(false);

        boolean mpd_is_visible = false;
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        if (radioDroidApp != null) {
            MPDClient mpdClient = radioDroidApp.getMpdClient();
            if (mpdClient != null) {
                MPDServersRepository repository = mpdClient.getMpdServersRepository();
                mpd_is_visible = !repository.isEmpty();
            }
        }
        menuItemMpd.setVisible(mpd_is_visible);

        switch (selectedMenuItem) {
            case R.id.nav_item_stations: {
                menuItemSleepTimer.setVisible(true);
                menuItemSearch.setVisible(true);
                myToolbar.setTitle(R.string.nav_item_stations);
                break;
            }
            case R.id.nav_item_starred: {
                menuItemSleepTimer.setVisible(true);
                //menuItemSearch.setVisible(true);
                menuItemSave.setVisible(true);
                menuItemLoad.setVisible(true);
                menuItemSave.setTitle(R.string.nav_item_save_playlist);

                if (sharedPref.getBoolean("icons_only_favorites_style", false)) {
                    menuItemListView.setVisible(true);
                } else if (sharedPref.getBoolean("load_icons", false)) {
                    menuItemIconsView.setVisible(true);
                }
                if (radioDroidApp.getFavouriteManager().isEmpty()) {
                    menuItemDelete.setVisible(false);
                } else {
                    menuItemDelete.setVisible(true).setTitle(R.string.action_delete_favorites);
                }
                myToolbar.setTitle(R.string.nav_item_starred);
                break;
            }
            case R.id.nav_item_history: {
                menuItemSleepTimer.setVisible(true);
                //menuItemSearch.setVisible(true);
                menuItemSave.setVisible(true);
                menuItemSave.setTitle(R.string.nav_item_save_history_playlist);

                if (!radioDroidApp.getHistoryManager().isEmpty()) {
                    menuItemDelete.setVisible(true).setTitle(R.string.action_delete_history);
                }
                myToolbar.setTitle(R.string.nav_item_history);
                break;
            }
            case R.id.nav_item_alarm: {
                menuItemAddAlarm.setVisible(true);
                myToolbar.setTitle(R.string.nav_item_alarm);
                break;
            }
 /* settings fragment sets the toolbar title depending on the current preference screen
            case R.id.nav_item_settings: {
                myToolbar.setTitle(R.string.nav_item_settings);
                break;
            }
 */
        }

        MenuItem mediaRouteMenuItem = CastHandler.getRouteItem(getApplicationContext(), menu);
        return true;
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            favouriteManager.SaveM3U(filePath, "radiodroid.m3u");
        }
    }*/

    @Override
    public void onFileSelected(FileDialog dialog, File file) {
        try {
            Log.i("MAIN", "save to " + file.getParent() + "/" + file.getName());
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            HistoryManager historyManager = radioDroidApp.getHistoryManager();

            if (dialog instanceof SaveFileDialog) {
                if (selectedMenuItem == R.id.nav_item_starred) {
                    favouriteManager.SaveM3U(file.getParent(), file.getName());
                }else if (selectedMenuItem == R.id.nav_item_history) {
                    historyManager.SaveM3U(file.getParent(), file.getName());
                }
            } else if (dialog instanceof OpenFileDialog) {
                favouriteManager.LoadM3U(file.getParent(), file.getName());
            }
        } catch (Exception e) {
            Log.e("MAIN", e.toString());
        }
    }

    void SaveFavourites() {
        SaveFileDialog dialog = new SaveFileDialog();
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, Utils.getThemeResId(this));
        Bundle args = new Bundle();
        args.putString(FileDialog.EXTENSION, ".m3u"); // file extension is optional
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), SaveFileDialog.class.getName());
    }

    void LoadFavourites() {
        OpenFileDialog dialogOpen = new OpenFileDialog();
        dialogOpen.setStyle(DialogFragment.STYLE_NO_TITLE, Utils.getThemeResId(this));
        Bundle argsOpen = new Bundle();
        argsOpen.putString(FileDialog.EXTENSION, ".m3u"); // file extension is optional
        dialogOpen.setArguments(argsOpen);
        dialogOpen.show(getSupportFragmentManager(), OpenFileDialog.class.getName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
                return true;
            case R.id.action_save:
                try {
                    if (Utils.verifyStoragePermissions(this, PERM_REQ_STORAGE_FAV_SAVE)) {
                        SaveFavourites();
                    }
                } catch (Exception e) {
                    Log.e("MAIN", e.toString());
                }

                return true;
            case R.id.action_load:
                try {
                    if (Utils.verifyStoragePermissions(this, PERM_REQ_STORAGE_FAV_LOAD)) {
                        LoadFavourites();
                    }
                } catch (Exception e) {
                    Log.e("MAIN", e.toString());
                }
                return true;
            case R.id.action_set_sleep_timer:
                changeTimer();
                return true;
            case R.id.action_mpd:
                selectMPDServer();
                return true;
            case R.id.action_delete:
                if (selectedMenuItem == R.id.nav_item_history) {
                    new AlertDialog.Builder(this)
                            .setMessage(this.getString(R.string.alert_delete_history))
                            .setCancelable(true)
                            .setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                                    HistoryManager historyManager = radioDroidApp.getHistoryManager();

                                    historyManager.clear();

                                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.notify_deleted_history), Toast.LENGTH_SHORT);
                                    toast.show();
                                    recreate();
                                }
                            })
                            .setNegativeButton(this.getString(R.string.no), null)
                            .show();
                }
                if (selectedMenuItem == R.id.nav_item_starred) {
                    new AlertDialog.Builder(this)
                            .setMessage(this.getString(R.string.alert_delete_favorites))
                            .setCancelable(true)
                            .setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                                    FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();

                                    favouriteManager.clear();

                                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.notify_deleted_favorites), Toast.LENGTH_SHORT);
                                    toast.show();
                                    recreate();
                                }
                            })
                            .setNegativeButton(this.getString(R.string.no), null)
                            .show();
                }
                return true;
            case R.id.action_list_view:
                sharedPref.edit().putBoolean("icons_only_favorites_style", false).apply();
                recreate();
                return true;
            case R.id.action_icons_view:
                sharedPref.edit().putBoolean("icons_only_favorites_style", true).apply();
                recreate();
                return true;
            case R.id.action_add_alarm:
                TimePickerFragment newFragment = new TimePickerFragment();
                newFragment.setCallback(this);
                newFragment.show(getSupportFragmentManager(), "timePicker");
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void toggleBottomSheetState() {
        if (playerBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            playerBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            playerBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        HistoryManager historyManager = radioDroidApp.getHistoryManager();
        Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 2);
        if (historyManager.size() > 0 && currentFragment instanceof FragmentAlarm) {
            DataRadioStation station = historyManager.getList().get(0);
            ((FragmentAlarm) currentFragment).getRam().add(station, hourOfDay, minute);
        }
    }

    private void setupStartUpFragment() {
        // This will restore fragment that was shown before activity was recreated
        if (instanceStateWasSaved) {
            invalidateOptionsMenu();
            checkMenuItems();
            return;
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        HistoryManager hm = radioDroidApp.getHistoryManager();
        FavouriteManager fm = radioDroidApp.getFavouriteManager();

        final String startupAction = sharedPref.getString("startup_action", getResources().getString(R.string.startup_show_history));

        if (startupAction.equals(getResources().getString(R.string.startup_show_history)) && hm.isEmpty()) {
            selectMenuItem(R.id.nav_item_stations);
            return;
        }

        if (startupAction.equals(getResources().getString(R.string.startup_show_favorites)) && fm.isEmpty()) {
            selectMenuItem(R.id.nav_item_stations);
            return;
        }

        if (startupAction.equals(getResources().getString(R.string.startup_show_history))) {
            selectMenuItem(R.id.nav_item_history);
        } else if (startupAction.equals(getResources().getString(R.string.startup_show_favorites))) {
            selectMenuItem(R.id.nav_item_starred);
        } else if (startupAction.equals(getResources().getString(R.string.startup_show_all_stations)) || selectedMenuItem < 0) {
            selectMenuItem(R.id.nav_item_stations);
        } else {
            selectMenuItem(selectedMenuItem);
        }
    }

    private void selectMenuItem(int itemId) {
        MenuItem item;
        if (Utils.bottomNavigationEnabled(this))
            item = mBottomNavigationView.getMenu().findItem(itemId);
        else
            item = mNavigationView.getMenu().findItem(itemId);

        if (item != null) {
            onNavigationItemSelected(item);
        } else {
            selectedMenuItem = R.id.nav_item_stations;
            onNavigationItemSelected(null);
        }
    }

    private void checkMenuItems() {
        if (mBottomNavigationView.getMenu().findItem(selectedMenuItem) != null)
            mBottomNavigationView.getMenu().findItem(selectedMenuItem).setChecked(true);

        if (mNavigationView.getMenu().findItem(selectedMenuItem) != null)
            mNavigationView.getMenu().findItem(selectedMenuItem).setChecked(true);
    }

    public void Search(StationsFilter.SearchStyle searchStyle, String query) {
        Log.d("MAIN", "Search() searchstyle=" + searchStyle + " query=" + query);
        Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 1);
        if (currentFragment instanceof FragmentTabs) {
            ((FragmentTabs) currentFragment).Search(searchStyle, query);
        } else {
            String backStackTag = String.valueOf(R.id.nav_item_stations);
            FragmentTabs f = new FragmentTabs();
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            if (Utils.bottomNavigationEnabled(this)) {
                fragmentTransaction.replace(R.id.containerView, f).commit();
                mBottomNavigationView.getMenu().findItem(R.id.nav_item_stations).setChecked(true);
            } else {
                fragmentTransaction.replace(R.id.containerView, f).addToBackStack(backStackTag).commit();
                mNavigationView.getMenu().findItem(R.id.nav_item_stations).setChecked(true);
            }

            f.Search(searchStyle, query);
            selectedMenuItem = R.id.nav_item_stations;
            invalidateOptionsMenu();
        }

    }

    public void SearchStations(@NonNull String query) {
        Log.d("MAIN", "SearchStations() " + query);
        Fragment currentFragment = mFragmentManager.getFragments().get(mFragmentManager.getFragments().size() - 1);
        if (currentFragment instanceof IFragmentSearchable) {
            ((IFragmentSearchable) currentFragment).Search(StationsFilter.SearchStyle.ByName, query);
        }
    }

//    public void togglePlayer() {
//        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
//        if (smallPlayerFragment.isDetached()) {
//            fragmentTransaction.attach(smallPlayerFragment);
//            fragmentTransaction.detach(fullPlayerFragment);
//        } else {
//            fragmentTransaction.attach(fullPlayerFragment);
//            fragmentTransaction.detach(smallPlayerFragment);
//        }
//
//        fragmentTransaction.commit();
//    }

    @Override
    public boolean onQueryTextSubmit(String query) {
//        String queryEncoded;
//        try {
//            mSearchView.setQuery("", false);
//            mSearchView.clearFocus();
//            mSearchView.setIconified(true);
//            queryEncoded = URLEncoder.encode(query, "utf-8");
//            queryEncoded = queryEncoded.replace("+", "%20");
//            SearchStations(query);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        SearchStations(newText);
        return true;
    }

    private void showMeteredConnectionDialog(@NonNull Runnable playFunc) {
        Resources res = this.getResources();
        String title = res.getString(R.string.alert_metered_connection_title);
        String text = res.getString(R.string.alert_metered_connection_message);
        meteredConnectionAlertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> playFunc.run())
                .setOnDismissListener(dialog -> meteredConnectionAlertDialog = null)
                .create();

        meteredConnectionAlertDialog.show();
    }

    private void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HIDE_LOADING);
        filter.addAction(ACTION_SHOW_LOADING);
        filter.addAction(PlayerService.PLAYER_SERVICE_STATE_CHANGE);
        filter.addAction(PlayerService.PLAYER_SERVICE_METERED_CONNECTION);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_HIDE_LOADING)) {
                    hideLoadingIcon();
                } else if (intent.getAction().equals(ACTION_SHOW_LOADING)) {
                    showLoadingIcon();
                } else if (intent.getAction().equals(PlayerService.PLAYER_SERVICE_METERED_CONNECTION)) {
                    if (meteredConnectionAlertDialog != null) {
                        meteredConnectionAlertDialog.cancel();
                        meteredConnectionAlertDialog = null;
                    }

                    PlayerType playerType = intent.getParcelableExtra(PlayerService.PLAYER_SERVICE_METERED_CONNECTION_PLAYER_TYPE);

                    switch (playerType) {
                        case RADIODROID:
                            showMeteredConnectionDialog(() -> Utils.play((RadioDroidApp) getApplication(), PlayerServiceUtil.getCurrentStation()));
                            break;
                        case EXTERNAL:
                            DataRadioStation currentStation = PlayerServiceUtil.getCurrentStation();
                            if (currentStation != null) {
                                showMeteredConnectionDialog(() -> PlayStationTask.playExternal(currentStation, ActivityMain.this).execute());
                            }
                            break;
                        default:
                            Log.e(TAG, String.format("broadcastReceiver unexpected PlayerType '%s'", playerType.toString()));
                    }
                } else if (intent.getAction().equals(PlayerService.PLAYER_SERVICE_STATE_CHANGE)) {
                    if (PlayerServiceUtil.isPlaying()) {
                        if (meteredConnectionAlertDialog != null) {
                            meteredConnectionAlertDialog.cancel();
                            meteredConnectionAlertDialog = null;
                        }
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    // Loading listener
    private void showLoadingIcon() {
        findViewById(R.id.progressBarLoading).setVisibility(View.VISIBLE);
    }

    private void hideLoadingIcon() {
        findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
    }

    private void changeTimer() {
        final AlertDialog.Builder seekDialog = new AlertDialog.Builder(this);
        View seekView = View.inflate(this, R.layout.layout_timer_chooser, null);

        seekDialog.setTitle(R.string.sleep_timer_title);
        seekDialog.setView(seekView);

        final TextView seekTextView = (TextView) seekView.findViewById(R.id.timerTextView);
        final SeekBar seekBar = (SeekBar) seekView.findViewById(R.id.timerSeekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        long currenTimerSeconds = PlayerServiceUtil.getTimerSeconds();
        long currentTimer;
        if (currenTimerSeconds <= 0) {
            currentTimer = sharedPref.getInt("sleep_timer_default_minutes", 10);
        } else if (currenTimerSeconds < 60) {
            currentTimer = 1;
        } else {
            currentTimer = currenTimerSeconds / 60;
        }
        seekBar.setProgress((int) currentTimer);
        seekDialog.setPositiveButton(R.string.sleep_timer_apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PlayerServiceUtil.clearTimer();
                PlayerServiceUtil.addTimer(seekBar.getProgress() * 60);
                sharedPref.edit().putInt("sleep_timer_default_minutes", seekBar.getProgress()).apply();
            }
        });

        seekDialog.setNegativeButton(R.string.sleep_timer_clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PlayerServiceUtil.clearTimer();
            }
        });

        seekDialog.create();
        seekDialog.show();
    }

    private void selectMPDServer() {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        Utils.showMpdServersDialog(radioDroidApp, getSupportFragmentManager(), null);
    }

    public final Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.my_awesome_toolbar);
    }

    @Override
    public void onSearchResultClicked(SearchPreferenceResult result) {
        result.closeSearchPage(this);
        getSupportFragmentManager().popBackStack();
        FragmentSettings f = FragmentSettings.openNewSettingsSubFragment(this, result.getScreen());
        result.highlight(f, Utils.getAccentColor(this));
    }
}
