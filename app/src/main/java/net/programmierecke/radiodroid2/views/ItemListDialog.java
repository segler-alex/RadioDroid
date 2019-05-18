package net.programmierecke.radiodroid2.views;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import net.programmierecke.radiodroid2.R;


public class ItemListDialog {

    public interface Callback {
        void onItemSelected(int resourceId);
    }

    public static BottomSheetDialog create(@NonNull Activity activity, @NonNull int[] resourceIds, @NonNull final Callback callback) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);

        final LayoutInflater inflater = activity.getLayoutInflater();

        final View sheetView = inflater.inflate(R.layout.dialog_generic_item_list, null);
        final ViewGroup viewItemsList = sheetView.findViewById(R.id.layout_items_list);

        for (final int resourceId : resourceIds) {
            final View itemView = inflater.inflate(R.layout.dialog_generic_item, null);

            TextView textView = itemView.findViewById(R.id.text);
            textView.setText(resourceId);
            textView.setClickable(true);
            textView.setOnClickListener(view -> {
                callback.onItemSelected(resourceId);
                bottomSheetDialog.hide();
            });

            viewItemsList.addView(textView);
        }

        bottomSheetDialog.setContentView(sheetView);
        return bottomSheetDialog;
    }
}
