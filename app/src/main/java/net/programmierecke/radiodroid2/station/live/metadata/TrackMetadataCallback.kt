package net.programmierecke.radiodroid2.station.live.metadata

interface TrackMetadataCallback {
    enum class FailureType {
        RECOVERABLE, UNRECOVERABLE
    }

    fun onFailure(failureType: FailureType)
    fun onSuccess(trackMetadata: TrackMetadata)
}
