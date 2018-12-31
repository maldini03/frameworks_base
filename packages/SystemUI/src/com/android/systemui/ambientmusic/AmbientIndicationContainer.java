package com.android.systemui.ambientmusic;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.os.Handler;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.R;
import com.android.systemui.ambientmusic.AmbientIndicationInflateListener;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.StatusBar;

import liquid.support.lottie.LottieAnimationView;

import java.util.concurrent.TimeUnit;

public class AmbientIndicationContainer extends AutoReinflateContainer {
    private View mAmbientIndication;
    private LottieAnimationView mIcon;
    private CharSequence mIndication;
    private StatusBar mStatusBar;
    private TextView mText;
    private TextView mTrackLenght;
    private Context mContext;
    private MediaMetadata mMediaMetaData;
    private String mMediaText;
    private boolean mForcedMediaDoze;
    private Handler mHandler;
    private boolean mInfoAvailable;
    private String mInfoToSet;
    private String mLengthInfo;
    private boolean mDozing;
    private String mLastInfo;

    private boolean mNpInfoAvailable;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
    }

    public void hideIndication() {
        setIndication(null, null, false);
    }

    public void initializeView(StatusBar statusBar, Handler handler) {
        mStatusBar = statusBar;
        addInflateListener(new AmbientIndicationInflateListener(this));
        mHandler = handler;
    }

    public void updateAmbientIndicationView(View view) {
        mAmbientIndication = findViewById(R.id.ambient_indication);
        mText = (TextView)findViewById(R.id.ambient_indication_text);
        mTrackLenght = (TextView)findViewById(R.id.ambient_indication_track_lenght);
        mIcon = (LottieAnimationView)findViewById(R.id.ambient_indication_icon);
        setIndication(mMediaMetaData, mMediaText, false);
    }

    public void setDozing(boolean dozing) {
        if (dozing == mDozing) return;

        mDozing = dozing;
        setTickerMarquee(dozing, false);
        if (dozing && (mInfoAvailable || mNpInfoAvailable)) {
            mText.setText(mInfoToSet);
            mLastInfo = mInfoToSet;
            mTrackLenght.setText(mLengthInfo);
            mAmbientIndication.setVisibility(View.VISIBLE);
            updatePosition();
        } else {
            setCleanLayout(-1);
            mAmbientIndication.setVisibility(View.INVISIBLE);
            mText.setText(null);
            mTrackLenght.setText(null);
        }
    }

    private void setTickerMarquee(boolean enable, boolean extendPulseOnNewTrack) {
        if (enable) {
            setTickerMarquee(false, false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mText.setEllipsize(TruncateAt.MARQUEE);
                    mText.setMarqueeRepeatLimit(2);
                    mText.setSelected(true);
                    if (extendPulseOnNewTrack && mStatusBar.isPulsing()) {
                        mStatusBar.getDozeScrimController().extendPulseForMusicTicker();
                    }
                }
            }, 1600);
        } else {
            mText.setEllipsize(null);
            mText.setSelected(false);
        }
    }

    public void setOnPulseEvent(int reason, boolean pulsing) {
        setCleanLayout(reason);
        setTickerMarquee(pulsing,
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION);
    }

    public void setCleanLayout(int reason) {
        mForcedMediaDoze =
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION;
        updatePosition();
    }

    public void updatePosition() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.getLayoutParams();
        lp.gravity = mForcedMediaDoze ? Gravity.CENTER : Gravity.BOTTOM;
        this.setLayoutParams(lp);
    }

    public void setNowPlayingIndication(String trackInfo) {
        // don't trigger this if we are already showing local/remote session track info
        setIndication(null, trackInfo, true);
    }

    public void setIndication(MediaMetadata mediaMetaData, String notificationText, boolean nowPlaying) {
        // never override local music ticker
        if (nowPlaying && mInfoAvailable) return;
        CharSequence charSequence = null;
        mLengthInfo = null;
        mInfoToSet = null;
        if (mediaMetaData != null) {
            CharSequence artist = mediaMetaData.getText(MediaMetadata.METADATA_KEY_ARTIST);
            CharSequence album = mediaMetaData.getText(MediaMetadata.METADATA_KEY_ALBUM);
            CharSequence title = mediaMetaData.getText(MediaMetadata.METADATA_KEY_TITLE);
            long duration = mediaMetaData.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (artist != null && album != null && title != null) {
                /* considering we are in Ambient mode here, it's not worth it to show
                    too many infos, so let's skip album name to keep a smaller text */
                charSequence = artist.toString() /*+ " - " + album.toString()*/ + " - " + title.toString();
                if (duration != 0) {
                    mLengthInfo = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(duration),
                            TimeUnit.MILLISECONDS.toSeconds(duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))).toString();
                }
            }
        }
        if (mDozing) {
            // if we are already showing an Ambient Notification with track info,
            // stop the current scrolling and start it delayed again for the next song
            setTickerMarquee(true, true);
        }

        if (!TextUtils.isEmpty(charSequence)) {
            mInfoToSet = charSequence.toString();
        } else if (!TextUtils.isEmpty(notificationText)) {
            mInfoToSet = notificationText;
            mLengthInfo = null;
        }

        if (nowPlaying) {
            mNpInfoAvailable = mInfoToSet != null;
        } else {
            mInfoAvailable = mInfoToSet != null;
        }

        if (mInfoAvailable || mNpInfoAvailable) {
            mMediaMetaData = mediaMetaData;
            mMediaText = notificationText;
            boolean isAnotherTrack = (mInfoAvailable || mNpInfoAvailable)
                    && (TextUtils.isEmpty(mLastInfo) || (!TextUtils.isEmpty(mLastInfo) && !mLastInfo.equals(mInfoToSet)));
            if (!DozeParameters.getInstance(mContext).getAlwaysOn() && mStatusBar != null && isAnotherTrack) {
                mStatusBar.triggerAmbientForMedia();
            }
            if (mDozing) {
                mLastInfo = mInfoToSet;
            }
        }
        mText.setText(mInfoToSet);
        mTrackLenght.setText(mLengthInfo);
        mAmbientIndication.setVisibility(mDozing && (mInfoAvailable || mNpInfoAvailable) ? View.VISIBLE : View.INVISIBLE);
        mIcon.setAnimation(R.raw.ambient_music_note);
        mIcon.playAnimation();
    }

    public View getIndication() {
        return mAmbientIndication;
    }
}
