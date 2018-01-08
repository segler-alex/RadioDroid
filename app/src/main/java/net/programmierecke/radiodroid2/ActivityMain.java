package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.MPDServer;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ActivityMain extends AppCompatActivity implements SearchView.OnQueryTextListener, IMPDClientStatusChange, NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_SEARCH_TAG = "search_tag";

    public static final int LAUNCH_EQUALIZER_REQUEST = 1;

    private static final String TAG = "RadioDroid";

    private final String TAG_SEARCH_URL = "https://www.radio-browser.info/webservice/json/stations/bytagexact";
    private final String SAVE_LAST_MENU_ITEM = "LAST_MENU_ITEM";

    private SearchView mSearchView;

    DrawerLayout mDrawerLayout;
    NavigationView mNavigationView;
    FragmentManager mFragmentManager;

    IFragmentRefreshable fragRefreshable = null;
    IFragmentSearchable fragSearchable = null;

    MenuItem menuItemSearch;
    MenuItem menuItemRefresh;
    MenuItem menuItemDelete;
    MenuItem menuItemAlarm;

    private boolean fromBackStack = false;

    private SharedPreferences sharedPref;

    private int selectedMenuItem;

    @Override
    public void changed() {
        Handler mainHandler = new Handler(getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        };
        mainHandler.post(myRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(myToolbar);

        PlayerServiceUtil.bind(this);

        selectedMenuItem = sharedPref.getInt("last_selectedMenuItem", -1);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mFragmentManager = getSupportFragmentManager();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mNavigationView = (NavigationView) findViewById(R.id.my_navigation_view);

        mNavigationView.setNavigationItemSelectedListener(this);

        FragmentPlayer f = new FragmentPlayer();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.playerView, f).commit();

        //myToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.main_toolbar);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        CastHandler.onCreate(this);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        mDrawerLayout.closeDrawers();
        android.support.v4.app.Fragment f = null;
        String backStackTag = String.valueOf(menuItem.getItemId());
        int menuItemId = menuItem.getItemId();

        switch (menuItemId) {
            case R.id.nav_item_stations:
                if(!fromBackStack)
                    f = new FragmentTabs();
                menuItemSearch.setVisible(true);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_stations);
                break;
            case R.id.nav_item_starred:
                if(!fromBackStack)
                    f = new FragmentStarred();
                menuItemSearch.setVisible(true);
                Context context = getApplication().getApplicationContext();
                FavouriteManager fm = new FavouriteManager(context);
                if (fm.isEmpty()) {
                    menuItemDelete.setVisible(false);
                } else {
                    menuItemDelete.setVisible(true).setTitle(R.string.action_delete_favorites);
                }
                myToolbar.setTitle(R.string.nav_item_starred);
                break;
            case R.id.nav_item_history:
                if(!fromBackStack)
                    f = new FragmentHistory();
                menuItemSearch.setVisible(true);
                HistoryManager hm = new HistoryManager(getApplication().getApplicationContext());
                if (hm.isEmpty()) {
                    menuItemDelete.setVisible(false);
                } else {
                    menuItemDelete.setVisible(true).setTitle(R.string.action_delete_history);
                }
                myToolbar.setTitle(R.string.nav_item_history);
                break;
            case R.id.nav_item_serverinfo:
                if(!fromBackStack)
                    f = new FragmentServerInfo();
                menuItemSearch.setVisible(false);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_statistics);
                break;
            case R.id.nav_item_recordings:
                if(!fromBackStack)
                    f = new FragmentRecordings();
                menuItemSearch.setVisible(false);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_recordings);
                break;
            case R.id.nav_item_alarm:
                if(!fromBackStack)
                    f = new FragmentAlarm();
                menuItemSearch.setVisible(false);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_alarm);
                break;
            case R.id.nav_item_settings:
                if(!fromBackStack)
                    f = new FragmentSettings();
                menuItemSearch.setVisible(false);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_settings);
                break;
            case R.id.nav_item_about:
                if(!fromBackStack)
                    f = new FragmentAbout();
                menuItemSearch.setVisible(false);
                menuItemDelete.setVisible(false);
                myToolbar.setTitle(R.string.nav_item_about);
                break;
            default:
        }

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        if(!fromBackStack)
            fragmentTransaction.replace(R.id.containerView, f).addToBackStack(backStackTag).commit();

        fragRefreshable = null;
        fragSearchable = null;
        if (f instanceof IFragmentRefreshable) {
            fragRefreshable = (IFragmentRefreshable) f;
        }
        if (f instanceof IFragmentSearchable) {
            fragSearchable = (IFragmentSearchable) f;
        }
        menuItemRefresh.setVisible(fragRefreshable != null);

        // Remove this line if you want to see refresh button
        menuItemRefresh.setVisible(false);
        menuItemAlarm.setVisible(menuItemId == R.id.nav_item_history
                || menuItemId == R.id.nav_item_starred
                || menuItemId == R.id.nav_item_stations);

        menuItem.setChecked(true);
        selectedMenuItem = menuItemId;
        fromBackStack = false;
        return false;
    }
    
    @Override
    public void onBackPressed() {
        int backStackCount = mFragmentManager.getBackStackEntryCount();
        fromBackStack = true;
        if(backStackCount > 1) {
            FragmentManager.BackStackEntry backStackEntry;
            backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount()-2);
            MenuItem menuItem = mNavigationView.getMenu().findItem(Integer.parseInt(backStackEntry.getName()));
            if(menuItem != null)
                onNavigationItemSelected(menuItem);
        }
        else {
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        final String searchTag = extras.getString(EXTRA_SEARCH_TAG);
        if (searchTag != null) {
            try {
                String queryEncoded = URLEncoder.encode(searchTag, "utf-8");
                queryEncoded = queryEncoded.replace("+", "%20");
                Search(TAG_SEARCH_URL + "/" + queryEncoded);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "on request permissions result:" + requestCode);
        }
        switch (requestCode) {
            case Utils.REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (fragRefreshable != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "REFRESH VIEW");
                        }
                        fragRefreshable.Refresh();
                    }
                } else {
                    Toast toast = Toast.makeText(this, getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
                    toast.show();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PlayerServiceUtil.unBind(this);
        MPDClient.StopDiscovery(this);
    }

    @Override
    protected void onPause() {

        SharedPreferences.Editor ed = sharedPref.edit();
        ed.putInt("last_selectedMenuItem", selectedMenuItem);
        ed.apply();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PAUSED");
        }

        super.onPause();

        if (!PlayerServiceUtil.isPlaying()) {
            PlayerServiceUtil.shutdownService();
        } else {
            CastHandler.onPause();
        }

        MPDClient.StopDiscovery(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "RESUMED");
        }

        CastHandler.onResume();
        MPDClient.StartDiscovery(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menuItemSearch = menu.findItem(R.id.action_search);
        menuItemDelete = menu.findItem(R.id.action_delete);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItemSearch);
        mSearchView.setOnQueryTextListener(this);

        menuItemRefresh = menu.findItem(R.id.action_refresh);
        menuItemAlarm = menu.findItem(R.id.action_set_alarm);
        MenuItem menuItemMPDNok = menu.findItem(R.id.action_mpd_nok);
        MenuItem menuItemMPDOK = menu.findItem(R.id.action_mpd_ok);

        if (fragSearchable == null) {
            menuItemSearch.setVisible(false);
        }

        if (fragRefreshable == null) {
            menuItemRefresh.setVisible(false);
        }

        // Remove this line if you want to see refresh button
        menuItemRefresh.setVisible(false);
        menuItemMPDOK.setVisible(MPDClient.Discovered() && MPDClient.Connected());
        menuItemMPDNok.setVisible(MPDClient.Discovered() && !MPDClient.Connected());

        MenuItem mediaRouteMenuItem = CastHandler.getRouteItem(getApplicationContext(), menu);

        if(mFragmentManager.getBackStackEntryCount() > 0) {
            // We need it when calling getActivity.recreate() from settings or when orientation changes
            fromBackStack = true;
            FragmentManager.BackStackEntry backStackEntry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount()-1);
            MenuItem menuItem = mNavigationView.getMenu().findItem(Integer.parseInt(backStackEntry.getName()));
            if(menuItem != null)
                onNavigationItemSelected(menuItem);
            return true;
        }
        // Context context = getApplication().getApplicationContext();
        HistoryManager hm = new HistoryManager(this);
        FavouriteManager fm = new FavouriteManager(this);

        final String startupAction = sharedPref.getString("startup_action", getResources().getString(R.string.startup_show_history));

        if (startupAction.equals(getResources().getString(R.string.startup_show_history)) && hm.isEmpty()) {
            selectMenuItem(R.id.nav_item_stations);
            return true;
        }

        if (startupAction.equals(getResources().getString(R.string.startup_show_favorites)) && fm.isEmpty()) {
            selectMenuItem(R.id.nav_item_stations);
            return true;
        }

        if (startupAction.equals(getResources().getString(R.string.startup_show_history))) {
            selectMenuItem(R.id.nav_item_history);
        } else if (startupAction.equals(getResources().getString(R.string.startup_show_favorites))) {
            selectMenuItem(R.id.nav_item_starred);
        } else {
            // user preference: startup with the last view
            if (selectedMenuItem < 0) {
                selectMenuItem(R.id.nav_item_stations);
            } else {
                selectMenuItem(selectedMenuItem);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
                return true;
            case R.id.action_refresh:
                if (fragRefreshable != null) {
                    fragRefreshable.Refresh();
                }
                return true;
            case R.id.action_set_alarm:
                changeTimer();
                return true;
            case R.id.action_mpd_nok:
                selectMPDServer();
                return true;
            case R.id.action_mpd_ok:
                MPDClient.Disconnect(this, this);
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
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void Search(String query) {
        selectMenuItem(R.id.nav_item_stations);

        if (fragSearchable != null) {
            fragSearchable.Search(query);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        String queryEncoded;
        try {
            mSearchView.setQuery("", false);
            mSearchView.clearFocus();
            mSearchView.setIconified(true);
            queryEncoded = URLEncoder.encode(query, "utf-8");
            queryEncoded = queryEncoded.replace("+", "%20");
            Search("https://www.radio-browser.info/webservice/json/stations/byname/" + queryEncoded);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(MPDClient.connected && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP))
            return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        String mpd_hostname = "";
        int mpd_port = 0;
        for (MPDServer server: Utils.getMPDServers(this)) {
            if(server.selected) {
                mpd_hostname = server.hostname.trim();
                mpd_port = server.port;
                break;
            }
        }
        if(mpd_port == 0 || !MPDClient.connected)
            return super.onKeyUp(keyCode, event);

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                MPDClient.SetVolume(mpd_hostname, mpd_port, -5);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                MPDClient.SetVolume(mpd_hostname, mpd_port, 5);
                return true;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void selectMenuItem(int itemId) {
        mNavigationView.setCheckedItem(itemId);
        mNavigationView.getMenu().performIdentifierAction(itemId, 0);
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

        long currentTimer = PlayerServiceUtil.getTimerSeconds() > 60? Math.abs(PlayerServiceUtil.getTimerSeconds() / 60) : 1;
        seekBar.setProgress((int) currentTimer);
        seekDialog.setPositiveButton(R.string.sleep_timer_apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PlayerServiceUtil.clearTimer();
                PlayerServiceUtil.addTimer(seekBar.getProgress() * 60);
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
        final List<MPDServer> servers = Utils.getMPDServers(this);
        final List<MPDServer> connectedServers = new ArrayList<>();
        List<String> serversNames = new ArrayList<>();
        for (MPDServer server : servers) {
            server.selected = false;
            if(server.connected) {
                serversNames.add(server.name);
                connectedServers.add(server);
            }
        }
        if(connectedServers.size() == 1)
        {
            MPDServer selectedServer = servers.get(connectedServers.get(0).id);
            selectedServer.selected = true;
            Utils.saveMPDServers(servers, getApplicationContext());
            MPDClient.Connect(this);

            // Since we connected to one server but have many user should know server's name
            if(servers.size() > 1)
                Toast.makeText(this, getString(R.string.action_mpd_connected, selectedServer.name), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setItems(serversNames.toArray(new String[serversNames.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (MPDServer server : servers) {
                            server.selected = false;
                        }
                        MPDServer selectedServer = servers.get(connectedServers.get(which).id);
                        selectedServer.selected = true;
                        Utils.saveMPDServers(servers, getApplicationContext());
                        MPDClient.Connect(ActivityMain.this);
                    }
                })
                .setTitle(R.string.alert_select_mpd_server)
                .show();
    }
}
