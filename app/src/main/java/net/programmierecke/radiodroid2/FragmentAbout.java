package net.programmierecke.radiodroid2;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentAbout extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_about,null);

        TextView aTextVersion = (TextView) view.findViewById(R.id.about_version);
        if (aTextVersion != null) {

            String version = BuildConfig.VERSION_NAME;
            String gitHash = getString(R.string.GIT_HASH);
            String buildDate = getString(R.string.BUILD_DATE);


            if (!gitHash.isEmpty()) {
                version += " (git " + gitHash + ")";
            }

            Resources resources = getResources();
            aTextVersion.setText(resources.getString(R.string.about_version, version+" "+buildDate));

        }

        return view;
    }
}
