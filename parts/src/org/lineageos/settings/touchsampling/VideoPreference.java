/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.touchsampling;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import org.lineageos.settings.R;

public class VideoPreference extends Preference {

    private VideoView videoView;
    private boolean isVideoPrepared = false;

    public VideoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.htsr_media_layout);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        videoView = (VideoView) holder.findViewById(R.id.htsr_video);
        if (videoView != null) {
            Uri videoUri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.htsr_demo);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                isVideoPrepared = true;
                videoView.start();
            });

            videoView.setOnCompletionListener(mp -> videoView.start());
        }
    }

    public void restartVideo() {
        if (videoView != null && isVideoPrepared) {
            videoView.seekTo(0);
            videoView.start();
        }
    }
}