package net.programmierecke.radiodroid2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;

import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.util.ArrayList;

import static java.lang.Math.min;

public class FavouriteManager extends StationSaveManager {
    @Override
    protected String getSaveId() {
        return "favourites";
    }

    public FavouriteManager(Context ctx) {
        super(ctx);
    }

    @Override
    public void add(DataRadioStation station) {
        if (!has(station.StationUuid)) {
            listStations.add(station);
            Save();

            notifyObservers();
        }
    }

    @Override
    void Load() {
        super.Load();
        updateShortcuts();
    }

    @Override
    void Save() {
        super.Save();
        updateShortcuts();
    }

    public void updateShortcuts() {
        if (Build.VERSION.SDK_INT >= 25) {
            int number = min(listStations.size(), ActivityMain.MAX_DYNAMIC_LAUNCHER_SHORTCUTS);
            SetDynamicAppLauncherShortcuts setDynamicAppLauncherShortcuts = new SetDynamicAppLauncherShortcuts(number);
            for (int i = 0; i < number; i++) {
                listStations.get(i).prepareShortcut(context, setDynamicAppLauncherShortcuts);
            }
        }
    }

    @TargetApi(25)
    class SetDynamicAppLauncherShortcuts implements DataRadioStation.ShortcutReadyListener {
        ArrayList<ShortcutInfo> shortcuts;
        int expectedNumber;

        SetDynamicAppLauncherShortcuts(int expectedNumber) {
            this.expectedNumber = expectedNumber;
            shortcuts = new ArrayList<ShortcutInfo>(expectedNumber);
        }

        @Override
        public void onShortcutReadyListener(ShortcutInfo shortcut) {
            shortcuts.add(shortcut);
            if (shortcuts.size() >= expectedNumber) {
                ShortcutManager shortcutManager = context.getApplicationContext().getSystemService(ShortcutManager.class);
                shortcutManager.removeAllDynamicShortcuts();
                shortcutManager.setDynamicShortcuts(shortcuts);
            }
        }
    }
}

