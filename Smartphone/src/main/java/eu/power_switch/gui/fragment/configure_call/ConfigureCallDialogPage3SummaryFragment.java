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

package eu.power_switch.gui.fragment.configure_call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import eu.power_switch.R;
import eu.power_switch.action.Action;
import eu.power_switch.database.handler.DatabaseHandler;
import eu.power_switch.gui.StatusMessageHandler;
import eu.power_switch.gui.dialog.ConfigurationDialogFragment;
import eu.power_switch.gui.dialog.ConfigurationDialogTabbedSummaryFragment;
import eu.power_switch.gui.dialog.ConfigureCallEventDialog;
import eu.power_switch.phone.call.CallEvent;
import eu.power_switch.shared.constants.LocalBroadcastConstants;
import eu.power_switch.shared.constants.PhoneConstants;

/**
 * Created by Markus on 05.04.2016.
 */
public class ConfigureCallDialogPage3SummaryFragment extends ConfigurationDialogFragment implements ConfigurationDialogTabbedSummaryFragment {

    private long callEventId = -1;

    private View rootView;
    private BroadcastReceiver broadcastReceiver;
    private TextView textViewContacts;
    private TextView textViewActions;

    private ArrayList<String> currentPhoneNumbers = new ArrayList<>();
    private ArrayList<Action> currentActions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.dialog_fragment_configure_call_event_page_3_summary, container, false);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocalBroadcastConstants.INTENT_CALL_EVENT_PHONE_NUMBERS_CHANGED.equals(intent.getAction())) {
                    currentPhoneNumbers = intent.getStringArrayListExtra(ConfigureCallDialogPage1ContactsFragment.KEY_PHONE_NUMBERS);
                } else if (LocalBroadcastConstants.INTENT_CALL_EVENT_ACTIONS_CHANGED.equals(intent.getAction())) {
                    currentActions = (ArrayList<Action>) intent.getSerializableExtra(ConfigureCallDialogPage2ActionsFragment.KEY_ACTIONS);
                }

                updateUi();

                notifyConfigurationChanged();
            }
        };

        textViewContacts = (TextView) rootView.findViewById(R.id.textView_contacts);
        textViewActions = (TextView) rootView.findViewById(R.id.textView_actions);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ConfigureCallEventDialog.CALL_EVENT_ID_KEY)) {
            callEventId = args.getLong(ConfigureCallEventDialog.CALL_EVENT_ID_KEY);
            initializeCallData(callEventId);
        }

        return rootView;
    }

    private void initializeCallData(long callEventId) {
        try {
            CallEvent callEvent = DatabaseHandler.getCallEvent(callEventId);

            currentPhoneNumbers.addAll(callEvent.getPhoneNumbers(PhoneConstants.CallType.INCOMING));
            currentActions.addAll(callEvent.getActions(PhoneConstants.CallType.INCOMING));

        } catch (Exception e) {
            StatusMessageHandler.showErrorMessage(getActivity(), e);
        }
    }

    private void updateUi() {


    }

    @Override
    public boolean checkSetupValidity() throws Exception {

        if (currentPhoneNumbers == null || currentPhoneNumbers.isEmpty()) {
            return false;
        }

        if (currentActions == null || currentActions.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void saveCurrentConfigurationToDatabase() throws Exception {
        // TODO: Save CallEvent to Database
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalBroadcastConstants.INTENT_CALL_EVENT_PHONE_NUMBERS_CHANGED);
        intentFilter.addAction(LocalBroadcastConstants.INTENT_CALL_EVENT_ACTIONS_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onStop();
    }
}
