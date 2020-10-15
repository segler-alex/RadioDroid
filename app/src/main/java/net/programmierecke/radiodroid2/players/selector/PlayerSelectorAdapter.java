package net.programmierecke.radiodroid2.players.selector;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.programmierecke.radiodroid2.CastHandler;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.PlayStationTask;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.players.mpd.MPDServerData;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDChangeVolumeTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDPauseTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDResumeTask;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDStopTask;
import net.programmierecke.radiodroid2.service.PauseReason;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.util.ArrayList;
import java.util.List;

public class PlayerSelectorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface ActionListener {
        void editServer(@NonNull MPDServerData mpdServerData);

        void removeServer(@NonNull MPDServerData mpdServerData);
    }

    private class MPDServerItemViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgConnectionStatus;
        final TextView textViewServerName;
        final ImageButton btnPlay;
        final ImageButton btnStop;
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
            btnStop = itemView.findViewById(R.id.buttonStop);
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


    private final LayoutInflater inflater;
    private final Context context;

    private final boolean showPlayInExternal;
    private final boolean warnOnMeteredConnection;

    private int fixedViewsCount;

    private List<Integer> viewTypes = new ArrayList<>();

    private DataRadioStation stationToPlay;

    private ActionListener actionListener;

    private MPDClient mpdClient;
    private List<MPDServerData> mpdServers;

    protected PlayerSelectorAdapter(@NonNull Context context, @Nullable DataRadioStation stationToPlay) {
        //super(DIFF_CALLBACK);

        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.mpdClient = ((RadioDroidApp) context.getApplicationContext()).getMpdClient();
        this.stationToPlay = stationToPlay;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        showPlayInExternal = sharedPref.getBoolean("play_external", false) && stationToPlay != null;
        warnOnMeteredConnection = sharedPref.getBoolean(PlayerService.METERED_CONNECTION_WARNING_KEY, false);

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

    public void notifyRadioDroidPlaybackStateChanged() {
        if (stationToPlay != null) {
            int pos = viewTypes.indexOf(PlayerType.RADIODROID.getValue());
            if (pos != -1) {
                notifyItemChanged(pos);
            }
        }
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

                    PlayerServiceUtil.pause(PauseReason.USER);
                } else {
                    Utils.playAndWarnIfMetered((RadioDroidApp) context.getApplicationContext(), stationToPlay, PlayerType.RADIODROID,
                            () -> Utils.play((RadioDroidApp) context.getApplicationContext(), stationToPlay));
                }
            });
        } else if (holder.getItemViewType() == PlayerType.EXTERNAL.getValue()) {
            holder.textViewDescription.setText(R.string.action_play_in_external);

            holder.btnPlay.setOnClickListener(v -> Utils.playAndWarnIfMetered((RadioDroidApp) context.getApplicationContext(), stationToPlay,
                    PlayerType.EXTERNAL, () -> PlayStationTask.playExternal(stationToPlay, context).execute()));

        } else if (holder.getItemViewType() == PlayerType.CAST.getValue()) {
            holder.textViewDescription.setText(R.string.media_route_menu_title);

            holder.btnPlay.setOnClickListener(view -> PlayStationTask.playCAST(stationToPlay, context).execute());
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
            holder.btnStop.setVisibility(View.VISIBLE);

            holder.btnStop.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDStopTask(null)));
        } else {
            holder.btnStop.setVisibility(View.GONE);
        }

        if (holder.mpdServerData.connected && holder.mpdServerData.status != MPDServerData.Status.Idle) {
            holder.btnDecreaseVolume.setVisibility(View.VISIBLE);
            holder.btnIncreaseVolume.setVisibility(View.VISIBLE);

            holder.btnDecreaseVolume.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDChangeVolumeTask(-10, null, mpdServerData)));

            holder.btnIncreaseVolume.setOnClickListener(view -> mpdClient.enqueueTask(mpdServerData, new MPDChangeVolumeTask(10, null, mpdServerData)));
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
                        PlayStationTask.playMPD(mpdClient, mpdServerData, stationToPlay, context).execute();
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
                    holder.btnPlay.setOnClickListener(view -> PlayStationTask.playMPD(mpdClient, mpdServerData, stationToPlay, context).execute());
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
