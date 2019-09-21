package net.programmierecke.radiodroid2.players.mpd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CastHandler;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDChangeVolumeTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDPauseTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDPlayTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDResumeTask;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MPDServersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface ActionListener {
        void editServer(@NonNull MPDServerData mpdServerData);

        void removeServer(@NonNull MPDServerData mpdServerData);
    }

    private enum PlayerType {
        MPD_SERVER(0),
        RADIODROID(1),
        EXTERNAL(2),
        CAST(3);

        private final int value;

        PlayerType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private class MPDServerItemViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgConnectionStatus;
        final TextView textViewServerName;
        final ImageButton btnPlay;
        final ImageButton btnMore;
        final TextView textViewNoConnection;
        final AppCompatImageButton btnDecreaseVolume;
        final AppCompatImageButton btnIncreaseVolume;
        final TextView textViewCurrentVolume;

        MPDServerData mpdServerData;

        private MPDServerItemViewHolder(@NonNull View itemView) {
            super(itemView);

            imgConnectionStatus = itemView.findViewById(R.id.imgConnectionStatus);
            textViewServerName = itemView.findViewById(R.id.textViewMPDName);
            btnPlay = itemView.findViewById(R.id.buttonPlay);
            btnMore = itemView.findViewById(R.id.buttonMore);
            textViewNoConnection = itemView.findViewById(R.id.textViewNoConnection);
            btnDecreaseVolume = itemView.findViewById(R.id.buttonMPDDecreaseVolume);
            textViewCurrentVolume = itemView.findViewById(R.id.textViewMPDVolume);
            btnIncreaseVolume = itemView.findViewById(R.id.buttonMPDIncreaseVolume);
        }
    }

    /* Represents either "Play in RadioDroid" or "Play in external player" */
    private class PlayerItemViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewDescription;
        final ImageButton btnPlay;

        public PlayerItemViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            btnPlay = itemView.findViewById(R.id.buttonPlay);
        }
    }

    static class PlayStationTask extends AsyncTask<Void, Void, String> {
        private PlayerType playerType;
        private MPDClient mpdClient;
        private MPDServerData mpdServerData;
        private DataRadioStation stationToPlay;
        private WeakReference<Context> contextWeakReference;

        public PlayStationTask(PlayerType playerType, @Nullable MPDClient mpdClient, @Nullable MPDServerData mpdServerData, @NonNull DataRadioStation stationToPlay, @NonNull Context ctx) {
            this.playerType = playerType;
            this.mpdClient = mpdClient;
            this.mpdServerData = mpdServerData;
            this.stationToPlay = stationToPlay;
            this.contextWeakReference = new WeakReference<>(ctx);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Context ctx = contextWeakReference.get();
            if (ctx != null) {
                ctx.sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            Context ctx = contextWeakReference.get();
            if (ctx != null) {
                RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
                return Utils.getRealStationLink(radioDroidApp.getHttpClient(), ctx.getApplicationContext(), stationToPlay.StationUuid);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Context ctx = contextWeakReference.get();
            if (ctx == null) {
                return;
            }

            ctx.sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

            if (result != null) {
                stationToPlay.playableUrl = result;

                switch (playerType) {
                    case MPD_SERVER:
                        mpdClient.enqueueTask(mpdServerData, new MPDPlayTask(result, null));
                        break;
                    case RADIODROID:
                        // Nothing
                        break;
                    case EXTERNAL:
                        Intent share = new Intent(Intent.ACTION_VIEW);
                        share.setDataAndType(Uri.parse(result), "audio/*");
                        ctx.startActivity(share);
                        break;
                    case CAST:
                        CastHandler.PlayRemote(stationToPlay.Name, result, stationToPlay.IconUrl);
                        break;
                }
            } else {
                Toast toast = Toast.makeText(ctx.getApplicationContext(),
                        ctx.getResources()
                                .getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                toast.show();
            }
            super.onPostExecute(result);
        }
    }

    private final LayoutInflater inflater;
    private final Context context;

    private final boolean showPlayInExternal;
    private int fixedViewsCount;

    private List<Integer> viewTypes = new ArrayList<>();

    private DataRadioStation stationToPlay;

    private ActionListener actionListener;

    private MPDClient mpdClient;
    private List<MPDServerData> mpdServers;

    protected MPDServersAdapter(@NonNull Context context, @Nullable DataRadioStation stationToPlay) {
        //super(DIFF_CALLBACK);

        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.mpdClient = ((RadioDroidApp) context.getApplicationContext()).getMpdClient();
        this.stationToPlay = stationToPlay;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        showPlayInExternal = sharedPref.getBoolean("play_external", false) && stationToPlay != null;

        fixedViewsCount = 0;
        if (stationToPlay != null) {
            fixedViewsCount++;
            viewTypes.add(PlayerType.RADIODROID.getValue());
        }

        if (showPlayInExternal) {
            fixedViewsCount++;
            viewTypes.add(PlayerType.EXTERNAL.getValue());
        }

        if (CastHandler.isCastSessionAvailable()) {
            fixedViewsCount++;
            viewTypes.add(PlayerType.CAST.getValue());
        }
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType != PlayerType.MPD_SERVER.getValue()) {
            View itemView = inflater.inflate(R.layout.list_item_play_in, parent, false);
            return new PlayerItemViewHolder(itemView);
        }

        View itemView = inflater.inflate(R.layout.list_item_mpd_server, parent, false);
        return new MPDServerItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == PlayerType.MPD_SERVER.getValue()) {
            bindViewHolder((MPDServerItemViewHolder) holder, position);
        } else {
            bindViewHolder((PlayerItemViewHolder) holder, position);
        }
    }

    private void bindViewHolder(@NonNull final PlayerItemViewHolder holder, int position) {
        if (holder.getItemViewType() == PlayerType.RADIODROID.getValue()) {
            holder.textViewDescription.setText(R.string.app_name);

            if (PlayerServiceUtil.isPlaying()) {
                holder.btnPlay.setImageResource(R.drawable.ic_pause_circle);
                holder.btnPlay.setContentDescription(context.getResources().getString(R.string.detail_pause));
            } else {
                holder.btnPlay.setImageResource(R.drawable.ic_play_circle);
                holder.btnPlay.setContentDescription(context.getString(R.string.detail_play));
            }

            holder.btnPlay.setOnClickListener(view -> {
                if (PlayerServiceUtil.isPlaying()) {
                    if (PlayerServiceUtil.isRecording()) {
                        PlayerServiceUtil.stopRecording();
                    }

                    PlayerServiceUtil.stop();

                    holder.btnPlay.setImageResource(R.drawable.ic_play_circle);
                    holder.btnPlay.setContentDescription(context.getString(R.string.detail_play));
                } else {
                    Utils.Play((RadioDroidApp) context.getApplicationContext(), stationToPlay);

                    holder.btnPlay.setImageResource(R.drawable.ic_pause_circle);
                    holder.btnPlay.setContentDescription(context.getResources().getString(R.string.detail_pause));
                }
            });
        } else if (holder.getItemViewType() == PlayerType.EXTERNAL.getValue()) {
            holder.textViewDescription.setText(R.string.action_play_in_external);

            holder.btnPlay.setOnClickListener(view -> new PlayStationTask(PlayerType.EXTERNAL, null, null, stationToPlay, context).execute());
        } else if (holder.getItemViewType() == PlayerType.CAST.getValue()) {
            holder.textViewDescription.setText(R.string.media_route_menu_title);

            holder.btnPlay.setOnClickListener(view -> new PlayStationTask(PlayerType.CAST, null, null, stationToPlay, context).execute());
        }
    }

    private void bindViewHolder(@NonNull final MPDServerItemViewHolder holder, int position) {
        final MPDServerData mpdServerData = mpdServers.get(translatePosition(position));

        holder.mpdServerData = mpdServerData;

        holder.textViewServerName.setText(mpdServerData.name);

        if (holder.mpdServerData.connected) {
            holder.btnPlay.setVisibility(View.VISIBLE);
            holder.textViewNoConnection.setVisibility(View.GONE);
            holder.textViewCurrentVolume.setText(Integer.toString(mpdServerData.volume));
            holder.textViewCurrentVolume.setVisibility(View.VISIBLE);
            holder.imgConnectionStatus.setImageResource(R.drawable.ic_mpd_connected_24dp);
        } else {
            holder.btnPlay.setVisibility(View.GONE);
            holder.textViewCurrentVolume.setVisibility(View.GONE);
            holder.textViewNoConnection.setVisibility(View.VISIBLE);
            holder.imgConnectionStatus.setImageResource(R.drawable.ic_mpd_disconnected_24dp);
        }

        if (holder.mpdServerData.connected && stationToPlay == null && holder.mpdServerData.status != MPDServerData.Status.Playing) {
            holder.btnPlay.setVisibility(View.GONE);
        }

        if (holder.mpdServerData.connected && holder.mpdServerData.status != MPDServerData.Status.Idle) {
            holder.btnDecreaseVolume.setVisibility(View.VISIBLE);
            holder.btnIncreaseVolume.setVisibility(View.VISIBLE);

            holder.btnDecreaseVolume.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDChangeVolumeTask(-10, null)));

            holder.btnIncreaseVolume.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDChangeVolumeTask(10, null)));
        } else {
            holder.btnDecreaseVolume.setVisibility(View.INVISIBLE);
            holder.btnIncreaseVolume.setVisibility(View.INVISIBLE);
        }


        holder.btnMore.setOnClickListener(view -> {
            final PopupMenu dropDownMenu = new PopupMenu(context, holder.btnMore);
            dropDownMenu.getMenuInflater().inflate(R.menu.menu_mpd_server, dropDownMenu.getMenu());

            if (stationToPlay == null) {
                dropDownMenu.getMenu().findItem(R.id.action_play).setVisible(false);
                dropDownMenu.getMenu().findItem(R.id.action_pause).setVisible(false);
            } else {
                if (holder.mpdServerData.status != MPDServerData.Status.Playing) {
                    dropDownMenu.getMenu().findItem(R.id.action_pause).setVisible(false);
                } else {
                    dropDownMenu.getMenu().findItem(R.id.action_play).setVisible(false);
                }
            }

            dropDownMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.action_edit: {
                        if (actionListener != null) {
                            actionListener.editServer(mpdServerData);
                        }
                        break;
                    }
                    case R.id.action_remove: {
                        if (actionListener != null) {
                            actionListener.removeServer(mpdServerData);
                        }
                        break;
                    }
                    case R.id.action_play: {
                        new PlayStationTask(PlayerType.MPD_SERVER, mpdClient, mpdServerData, stationToPlay, context).execute();
                        break;
                    }
                    case R.id.action_pause: {
                        mpdClient.enqueueTask(mpdServerData, new MPDPauseTask(null));
                        break;
                    }
                }

                return true;
            });

            dropDownMenu.show();
        });

        if (holder.mpdServerData.connected) {
            if (stationToPlay != null) {
                holder.btnPlay.setContentDescription(context.getResources().getString(R.string.detail_play));
                holder.btnPlay.setImageResource(R.drawable.ic_play_circle);

                if (mpdServerData.status != MPDServerData.Status.Playing) {
                    holder.btnPlay.setOnClickListener(view -> new PlayStationTask(PlayerType.MPD_SERVER, mpdClient, mpdServerData, stationToPlay, context).execute());
                } else {
                    holder.btnPlay.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDResumeTask(null)));
                }
            }

            if (mpdServerData.status == MPDServerData.Status.Playing) {
                holder.btnPlay.setContentDescription(context.getResources().getString(R.string.detail_pause));
                holder.btnPlay.setImageResource(R.drawable.ic_pause_circle);

                holder.btnPlay.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDPauseTask(null)));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= fixedViewsCount) {
            return PlayerType.MPD_SERVER.getValue();
        }

        return viewTypes.get(position);
    }


    void setEntries(List<MPDServerData> mpdServers) {
        this.mpdServers = mpdServers;

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mpdServers.size() + fixedViewsCount;
    }

    private int translatePosition(int position) {
        return position - fixedViewsCount;
    }

    private static DiffUtil.ItemCallback<MPDServerData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MPDServerData>() {
                @Override
                public boolean areItemsTheSame(MPDServerData oldEntry, MPDServerData newEntry) {
                    return oldEntry.id == newEntry.id;
                }

                @Override
                public boolean areContentsTheSame(MPDServerData oldEntry,
                                                  @NonNull MPDServerData newEntry) {
                    return oldEntry.contentEquals(newEntry);
                }
            };
}
