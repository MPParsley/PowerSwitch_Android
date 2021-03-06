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

package eu.power_switch.widget.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;
import java.util.LinkedList;

import eu.power_switch.R;
import eu.power_switch.database.handler.DatabaseHandler;
import eu.power_switch.obj.Apartment;
import eu.power_switch.obj.Room;
import eu.power_switch.obj.button.Button;
import eu.power_switch.obj.receiver.Receiver;
import eu.power_switch.settings.SmartphonePreferencesHandler;
import eu.power_switch.shared.log.Log;
import eu.power_switch.widget.ReceiverWidget;
import eu.power_switch.widget.WidgetIntentReceiver;

/**
 * This class is responsible for updating existing Receiver widgets
 */
public class ReceiverWidgetProvider extends AppWidgetProvider {

    /**
     * Forces an Update of all Receiver Widgets
     *
     * @param context any suitable context
     */
    public static void forceWidgetUpdate(Context context) {
        // update receiver widgets
        Intent intent = new Intent(context, ReceiverWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(context.getApplicationContext())
                .getAppWidgetIds(new ComponentName(context.getApplicationContext(),
                        ReceiverWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("Updating Receiver Widgets...");
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            RemoteViews remoteViews = new RemoteViews(context.getResources()
                    .getString(eu.power_switch.shared.R.string.PACKAGE_NAME), R.layout.widget_receiver);

            try {
                ReceiverWidget receiverWidget = DatabaseHandler.getReceiverWidget(appWidgetId);
                Room room = DatabaseHandler.getRoom(receiverWidget.getRoomId());
                if (room != null) {
                    Receiver receiver = DatabaseHandler.getReceiver(receiverWidget.getReceiverId());

                    if (receiver != null) {
                        Apartment apartment = DatabaseHandler.getApartment(room.getApartmentId());
                        // update UI
                        remoteViews.setTextViewText(R.id.textView_receiver_widget_name, apartment.getName() + ": " + room.getName() + ": " + receiver.getName());

                        LinkedList<Button> buttons = receiver.getButtons();

                        // remove all previous buttons
                        remoteViews.removeAllViews(R.id.linearlayout_receiver_widget);

                        // add buttons from database
                        int buttonOffset = 0;
                        for (Button button : buttons) {
                            // set button action
                            RemoteViews buttonView = new RemoteViews(context.getResources()
                                    .getString(eu.power_switch.shared.R.string.PACKAGE_NAME), R.layout.widget_receiver_button_layout);
                            SpannableString s = new SpannableString(button.getName());
                            s.setSpan(new StyleSpan(Typeface.BOLD), 0, button.getName().length(), 0);
                            buttonView.setTextViewText(R.id.button_widget_universal, s);
                            if (SmartphonePreferencesHandler.getHighlightLastActivatedButton() &&
                                    receiver.getLastActivatedButtonId().equals(button.getId())) {
                                buttonView.setTextColor(R.id.button_widget_universal,
                                        ContextCompat.getColor(context, R.color.color_light_blue_a700));
                            }

                            PendingIntent intent = WidgetIntentReceiver.buildReceiverWidgetActionPendingIntent(context, apartment, room,
                                    receiver, button, appWidgetId * 15 + buttonOffset);
                            buttonView.setOnClickPendingIntent(R.id.button_widget_universal, intent);

                            remoteViews.addView(R.id.linearlayout_receiver_widget, buttonView);
                            buttonOffset++;
                        }
                        remoteViews.setViewVisibility(R.id.linearlayout_receiver_widget, View.VISIBLE);
                    } else {
                        remoteViews.setTextViewText(R.id.textView_receiver_widget_name, context.getString(R.string.receiver_not_found));
                        remoteViews.removeAllViews(R.id.linearlayout_receiver_widget);
                        remoteViews.setViewVisibility(R.id.linearlayout_receiver_widget, View.GONE);
                    }
                } else {
                    remoteViews.setTextViewText(R.id.textView_receiver_widget_name, context.getString(R.string.room_not_found));
                    remoteViews.removeAllViews(R.id.linearlayout_receiver_widget);
                    remoteViews.setViewVisibility(R.id.linearlayout_receiver_widget, View.GONE);
                }
            } catch (Exception e) {
                Log.e(e);
                remoteViews.setTextViewText(R.id.textView_receiver_widget_name, context.getString(R.string.unknown_error));
                remoteViews.removeAllViews(R.id.linearlayout_receiver_widget);
                remoteViews.setViewVisibility(R.id.linearlayout_receiver_widget, View.GONE);
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d("Deleting Receiver Widgets: " + Arrays.toString(appWidgetIds));
        for (int appWidgetId : appWidgetIds) {
            try {
                DatabaseHandler.deleteReceiverWidget(appWidgetId);
            } catch (Exception e) {
                Log.e(e);
            }
        }
        super.onDeleted(context, appWidgetIds);
    }
}
