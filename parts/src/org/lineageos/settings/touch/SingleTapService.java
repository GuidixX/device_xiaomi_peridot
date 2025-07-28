/*
 * Copyright (C) 2025 GuidixX
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

package org.lineageos.settings.touch;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;
import android.os.ServiceManager;

public class SingleTapService extends Service {
    private static final int SINGLE_TAP_TO_WAKE_MODE = 21;
    private static final int TOUCH_AOD_ENABLE_MODE = 11;
    private ITouchFeature mTouchFeature;

    @Override
    public void onCreate() {
        super.onCreate();
        initTouchFeature();
        registerObserver();
    }

    private void initTouchFeature() {
        try {
            IBinder binder = Binder.allowBlocking(
                ServiceManager.waitForDeclaredService(
                  ITouchFeature.DESCRIPTOR + "/default"
                )
            );
            mTouchFeature = ITouchFeature.Stub.asInterface(binder);
        } catch (Exception e) {
            // Silent catch
        }
    }

    private void registerObserver() {
        ContentResolver cr = getContentResolver();
        cr.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DOZE_TAP_SCREEN_GESTURE),
            true,
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    updateSingleTapMode();
                }
            }
        );

        cr.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DOZE_SINGLE_TAP_GESTURE_AMBIENT),
            true,
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    updateAmbientDisplayMode();
                }
            }
        );
        
        updateSingleTapMode();
        updateAmbientDisplayMode();
    }

    private void updateSingleTapMode() {
        try {
            boolean enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.DOZE_TAP_SCREEN_GESTURE,
                0
            ) == 1;
            if (mTouchFeature != null) {
                mTouchFeature.setTouchMode(0, SINGLE_TAP_TO_WAKE_MODE, enabled ? 1 : 0);
            }
        } catch (Exception e) {
            // Silent catch
        }
    }

    private void updateAmbientDisplayMode() {
        try {
            boolean enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.DOZE_SINGLE_TAP_GESTURE_AMBIENT,
                0
            ) == 1;
            if (mTouchFeature != null) {
                mTouchFeature.setTouchMode(0, TOUCH_AOD_ENABLE_MODE, enabled ? 1 : 0);
            }
        } catch (Exception e) {
            // Silent catch
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
