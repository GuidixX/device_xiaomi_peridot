/*
 * Copyright (C) 2025 kenway214
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
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;

public class SoFodTouchService extends Service {
    private static final int TOUCH_FOD_ENABLE     = 10;
    private static final int TOUCH_AOD_ENABLE     = 11;
    private static final int TOUCH_FODICON_ENABLE = 16;
    private ITouchFeature mTouchFeature;

    @Override
    public void onCreate() {
        super.onCreate();
        initTouchFeature();
        enableSoFodModes();
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

    private void enableSoFodModes() {
        if (mTouchFeature == null) return;
        try {
            mTouchFeature.setTouchMode(0, TOUCH_FOD_ENABLE, 1);
            mTouchFeature.setTouchMode(0, TOUCH_AOD_ENABLE, 1);
            mTouchFeature.setTouchMode(0, TOUCH_FODICON_ENABLE, 1);
        } catch (Exception e) {
            // Silent catch
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
