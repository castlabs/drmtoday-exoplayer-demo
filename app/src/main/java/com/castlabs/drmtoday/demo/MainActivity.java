/**
 * Copyright 2018 Castlabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.castlabs.drmtoday.demo;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.castlabs.drmtoday.DrmtodayCallback;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ExoPlayer player;
    private FrameworkMediaDrm mediaDrm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String userAgent = Util.getUserAgent(this, "DRMtoday Demo");


            // The UUID that decides which DRM system will be used
            UUID drmSystemUuid = C.WIDEVINE_UUID;
            // UUID drmSystemUuid = C.PLAYREADY_UUID;

            // We need an HttpDataSource for the DRM callback
            HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(userAgent);

            // Create an instance of the Media DRM
            mediaDrm = FrameworkMediaDrm.newInstance(drmSystemUuid);

            // Create a callback for DRMtoday. The provided data source will be used to
            // send the DRM requests
            DrmtodayCallback drmtodayCallback = new DrmtodayCallback(
                    httpDataSourceFactory // The data source
            );

            // Create an exo player instance and make sure we pass the
            // drmtoday callback to the drm session manager
            player = new ExoPlayer.Builder(this)
                    .setRenderersFactory(new DefaultRenderersFactory(this))
                    .setTrackSelector(new DefaultTrackSelector())
                    .build();

            DrmSessionManager drmSessionManager =
                    new DefaultDrmSessionManager.Builder()
                            .setUuidAndExoMediaDrmProvider(drmSystemUuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
                            .build(drmtodayCallback);


            // Load content. NOTE: We also configure the content related DRM parameters below
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent);
            MediaSource videoSource = new DashMediaSource.Factory(dataSourceFactory)
                    .setDrmSessionManagerProvider(unusedMediaItem -> drmSessionManager)
                    .createMediaSource(MediaItem.fromUri(Uri.parse("https://demo.cf.castlabs.com/media/prestodrm/Manifest.mpd")));

            // Since the DRM configuration is content specific, we need to
            // configure the DRMtoday callback accordingly
            drmtodayCallback.configure(
                    DrmtodayCallback.DRMTODAY_STAGING, // The Environment
                    "six",       // The merchant
                    "purchase",    // The User ID
                    "default",   // The Session ID
                    null,       // The (optional) auth token
                    "prestoplay", // The asset ID
                    null // The variant ID
            );

            PlayerView playerView = findViewById(R.id.player_view);
            playerView.setPlayer(player);
            player.prepare(videoSource);
            player.setPlayWhenReady(true);

        } catch (Exception e) {
            Log.e(TAG, "Error while creating player!", e);
            Toast.makeText(this, "Error while creating player", Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if(player != null) {
            player.release();
            player = null;
        }

        if(mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }
}
