/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2010 (C) Sindre Mehus
 */
package github.madmarty.madsonic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.util.Logger;
import github.madmarty.madsonic.util.Util;

/**
 * Request media button focus when connected to Bluetooth A2DP.
 *
 * @author Sindre Mehus
 */
public class BluetoothIntentReceiver extends BroadcastReceiver {

    private static final Logger LOG = new Logger(BluetoothIntentReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra("android.bluetooth.a2dp.extra.SINK_STATE", -1);
        LOG.info("android.bluetooth.a2dp.extra.SINK_STATE, state = " + state);
        boolean connected = state == 2;  // android.bluetooth.BluetoothA2dp.STATE_CONNECTED
        if (connected) {
            LOG.info("Connected to Bluetooth A2DP, requesting media button focus.");
            Util.registerMediaButtonEventReceiver(context);
        }

        boolean disconnected = state == 0; // android.bluetooth.BluetoothA2dp.STATE_DISCONNECTED
        if (disconnected) {
            LOG.info("Disconnected from Bluetooth A2DP, requesting pause.");
            context.sendBroadcast(new Intent(DownloadServiceImpl.CMD_PAUSE));
        }
    }
}