/*
 *     PowerSwitch by Max Rosin & Markus Ressel
 *     Copyright (C) 2015  Markus Ressel
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.power_switch.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import eu.power_switch.gui.activity.MainActivity;
import eu.power_switch.shared.constants.LocalBroadcastConstants;
import eu.power_switch.shared.log.Log;

/**
 * This is a helper Class to create and show status messages depending on the app state
 * <p/>
 * Created by Markus on 17.11.2015.
 */
public class StatusMessageHandler {

    private static Toast lastToast;

    /**
     * Shows a status message on screen, either as Toast if the app is running in the background or as a snackbar if
     * it is running in the foreground
     *
     * @param context       any suitable context
     * @param message       status message
     * @param actionMessage message of action button
     * @param runnable      code that should be executed when activating the action button
     * @param duration      duration
     */
    public static void showStatusMessage(Context context, String message, String actionMessage, SerializableRunnable
            runnable, int duration) {
        if (MainActivity.isInForeground()) {
            sendStatusSnackbarBroadcast(context, message, actionMessage, runnable, duration);
        } else {
            showStatusToast(context, message, duration);
        }
    }

    /**
     * Shows a status message on screen, either as Toast if the app is running in the background or as a snackbar if
     * it is running in the foreground
     *
     * @param context                 any suitable context
     * @param messageResourceId       status message resource id
     * @param actionMessageResourceId message resource id of action button
     * @param runnable                code that should be executed when activating the action button
     * @param duration                duration
     */
    public static void showStatusMessage(Context context, int messageResourceId, int actionMessageResourceId,
                                         SerializableRunnable runnable, int duration) {
        if (MainActivity.isInForeground()) {
            sendStatusSnackbarBroadcast(context, context.getString(messageResourceId), context.getString(actionMessageResourceId), runnable,
                    duration);
        } else {
            showStatusToast(context, context.getString(messageResourceId), duration);
        }
    }

    /**
     * Shows a status message on screen, either as Toast if the app is running in the background or as a snackbar if
     * it is running in the foreground
     *
     * @param context  any suitable context
     * @param message  status message
     * @param duration duration
     */
    public static void showStatusMessage(Context context, String message, int duration) {
        if (MainActivity.isInForeground()) {
            sendStatusSnackbarBroadcast(context, message, null, null, duration);
        } else {
            showStatusToast(context, message, duration);
        }
    }

    /**
     * Shows a status message on screen, either as Toast if the app is running in the background or as a snackbar if
     * it is running in the foreground
     *
     * @param context           any suitable context
     * @param messageResourceId status message resource id
     * @param duration          duration
     */
    public static void showStatusMessage(Context context, int messageResourceId, int duration) {
        if (MainActivity.isInForeground()) {
            sendStatusSnackbarBroadcast(context, context.getString(messageResourceId), null, null, duration);
        } else {
            showStatusToast(context, context.getString(messageResourceId), duration);
        }
    }

    /**
     * Show Snackbar on MainActivity context
     *
     * @param context       any suitable context
     * @param message       snackbar message
     * @param actionMessage optional action button message
     * @param runnable      optional action for action button
     * @param duration      duration of snackbar
     */
    private static void sendStatusSnackbarBroadcast(Context context, String message, String actionMessage,
                                                    SerializableRunnable runnable, int duration) {
        Log.d("Status Snackbar: " + message);
        Intent intent = new Intent(LocalBroadcastConstants.INTENT_STATUS_UPDATE_SNACKBAR);
        intent.putExtra("message", message);
        intent.putExtra("duration", duration);
        if (actionMessage != null && runnable != null) {
            intent.putExtra("actionMessage", actionMessage);
            intent.putExtra("runnable", runnable);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Show Toast above all other views
     *
     * @param context  any suitable context
     * @param message  toast message
     * @param duration duration of toast
     */
    private static void showStatusToast(final Context context, final String message, final int duration) {
        Log.d("Status Toast: " + message);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // cancel last toast
                if (lastToast != null) {
                    lastToast.cancel();
                }

                // create and show new toast
                Toast toast = Toast.makeText(context.getApplicationContext(), message, duration);
                toast.show();

                // save toast reference
                lastToast = toast;
            }
        });
    }
}