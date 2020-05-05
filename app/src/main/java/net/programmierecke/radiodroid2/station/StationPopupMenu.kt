package net.programmierecke.radiodroid2.station

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenu
import com.github.zawadz88.materialpopupmenu.popupMenu
import net.programmierecke.radiodroid2.R
import net.programmierecke.radiodroid2.Utils

object StationPopupMenu {
    fun open(view: View, context: Context, activity: FragmentActivity, station: DataRadioStation, itemAdapterStation: ItemAdapterStation): MaterialPopupMenu {
        val rootView = view.rootView
        val popupMenu = popupMenu {
            dropdownGravity = Gravity.BOTTOM
            style = if (Utils.isDarkTheme(context)) R.style.Widget_MPM_Menu_Dark else R.style.Widget_MPM_Menu
            section {
                item {
                    labelRes = R.string.context_menu_weblinks
                    icon = R.drawable.ic_store_black_24dp
                    callback = {
                        StationActions.showWebLinks(activity, station)
                    }
                }
                item {
                    labelRes = R.string.context_menu_add_alarm
                    icon = R.drawable.ic_add_alarm_black_24dp
                    callback = {
                        StationActions.setAsAlarm(activity, station)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    item {
                        labelRes = R.string.context_menu_create_shortcut
                        icon = R.drawable.ic_back_arrow_24dp
                        callback = {
                            station.prepareShortcut(context, itemAdapterStation.CreatePinShortcutListener())
                        }
                    }
                }
                item {
                    labelRes = R.string.context_menu_delete
                    icon = R.drawable.ic_delete_black_24dp
                    callback = {
                        StationActions.removeFromFavourites(context, rootView, station)
                    }
                }
            }
        }
        popupMenu.show(context, view)
        return popupMenu
    }
}