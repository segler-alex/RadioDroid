package net.programmierecke.radiodroid2;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentAbout extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
