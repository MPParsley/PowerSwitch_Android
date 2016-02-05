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

package eu.power_switch.gui.fragment.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import eu.power_switch.R;
import eu.power_switch.database.handler.DatabaseHandler;
import eu.power_switch.developer.PlayStoreModeDataModel;
import eu.power_switch.exception.gateway.GatewayAlreadyExistsException;
import eu.power_switch.gui.IconicsHelper;
import eu.power_switch.gui.StatusMessageHandler;
import eu.power_switch.gui.adapter.GatewayRecyclerViewAdapter;
import eu.power_switch.gui.animation.AnimationHandler;
import eu.power_switch.gui.dialog.ConfigureGatewayDialog;
import eu.power_switch.gui.fragment.RecyclerViewFragment;
import eu.power_switch.network.NetworkHandler;
import eu.power_switch.obj.gateway.Gateway;
import eu.power_switch.settings.DeveloperPreferencesHandler;
import eu.power_switch.settings.SmartphonePreferencesHandler;
import eu.power_switch.shared.constants.LocalBroadcastConstants;
import eu.power_switch.shared.constants.SettingsConstants;
import eu.power_switch.shared.log.Log;

/**
 * Fragment containing all settings related to Gateways
 * <p/>
 * Created by Markus on 30.08.2015.
 */
public class GatewaySettingsFragment extends RecyclerViewFragment {

    private View rootView;

    private BroadcastReceiver broadcastReceiver;

    private GatewayRecyclerViewAdapter gatewayRecyclerViewAdapter;
    private RecyclerView recyclerViewGateways;
    private ArrayList<Gateway> gateways = new ArrayList<>();
    private FloatingActionButton searchGatewayFAB;
    private FloatingActionButton addGatewayFAB;

    public static void sendGatewaysChangedBroadcast(Context context) {
        Log.d(GatewaySettingsFragment.class, "sendGatewaysChangedBroadcast");
        Intent intent = new Intent(LocalBroadcastConstants.INTENT_GATEWAY_CHANGED);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_gateway_settings, container, false);
        setHasOptionsMenu(true);

        final RecyclerViewFragment recyclerViewFragment = this;
        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (view.getId()) {
                    case R.id.add_gateway_fab:
                        ConfigureGatewayDialog configureGatewayDialog = new ConfigureGatewayDialog();
                        configureGatewayDialog.setTargetFragment(recyclerViewFragment, 0);
                        configureGatewayDialog.show(getFragmentManager(), null);
                        break;
                    case R.id.search_gateway_fab:
                        startAutoDiscovery();
                        break;
                    default:
                        break;
                }
            }
        };

        searchGatewayFAB = (FloatingActionButton) rootView.findViewById(R.id.search_gateway_fab);
        searchGatewayFAB.setImageDrawable(IconicsHelper.getRefreshIcon(getActivity(), android.R.color.white));
        searchGatewayFAB.setOnClickListener(onClickListener);

        addGatewayFAB = (FloatingActionButton) rootView.findViewById(R.id.add_gateway_fab);
        addGatewayFAB.setImageDrawable(IconicsHelper.getAddIcon(getActivity(), android.R.color.white));
        addGatewayFAB.setOnClickListener(onClickListener);

        recyclerViewGateways = (RecyclerView) rootView.findViewById(R.id.recyclerview_list_of_gateways);
        gatewayRecyclerViewAdapter = new GatewayRecyclerViewAdapter(getActivity(), gateways);
        gatewayRecyclerViewAdapter.setOnItemLongClickListener(new GatewayRecyclerViewAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View itemView, int position) {
                Gateway gateway = gateways.get(position);

                ConfigureGatewayDialog configureGatewayDialog = ConfigureGatewayDialog.newInstance(gateway.getId());
                configureGatewayDialog.setTargetFragment(recyclerViewFragment, 0);
                configureGatewayDialog.show(getFragmentManager(), null);
            }
        });
        recyclerViewGateways.setAdapter(gatewayRecyclerViewAdapter);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.gateway_grid_span_count), StaggeredGridLayoutManager.VERTICAL);
        recyclerViewGateways.setLayoutManager(layoutManager);

        // BroadcastReceiver to get notifications from background service if room data has changed
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI();
            }
        };

        return rootView;
    }

    private void startAutoDiscovery() {
        if (!NetworkHandler.isWifiAvailable()) {
            StatusMessageHandler.showInfoMessage(this, R.string.missing_wifi_connection, Snackbar.LENGTH_LONG);
            return;
        }

        searchGatewayFAB.startAnimation(AnimationHandler.getRotationClockwiseAnimation(getContext()));
        final RecyclerViewFragment recyclerViewFragment = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkHandler.init(getContext());
                    final List<Gateway> foundGateways = NetworkHandler.searchGateways();

                    // stop animation
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchGatewayFAB.clearAnimation();
                        }
                    });

                    if (foundGateways == null || foundGateways.isEmpty()) {
                        StatusMessageHandler.showInfoMessage(recyclerViewFragment, R.string.no_gateway_found,
                                Snackbar.LENGTH_LONG);
                        return;
                    }

                    int unknownGatewaysCount = 0;
                    int existingGatewaysCount = 0;
                    int newGatewaysCount = 0;
                    for (Gateway newGateway : foundGateways) {
                        if (newGateway == null) {
                            unknownGatewaysCount++;
                            continue;
                        }

                        // save new Gateway if it doesn't exist already
                        try {
                            DatabaseHandler.addGateway(newGateway);
                            newGatewaysCount++;
                        } catch (GatewayAlreadyExistsException e) {
                            existingGatewaysCount++;
                            DatabaseHandler.enableGateway(e.getIdOfExistingGateway());
                        } catch (Exception e) {
                            StatusMessageHandler.showErrorMessage(recyclerViewFragment, e);
                        }
                    }

                    StatusMessageHandler.showInfoMessage(recyclerViewFragment,
                            getString(R.string.autodiscover_response_message, newGatewaysCount, existingGatewaysCount,
                                    unknownGatewaysCount), Snackbar.LENGTH_LONG);

                    sendGatewaysChangedBroadcast(recyclerViewFragment.getContext());
                } catch (Exception e) {
                    Log.e(e);
                }
            }
        }).start();
    }

    private void refreshGateways() {
        Log.d("GatewaySettingsFragment", "refreshGateways");

        gateways.clear();

        if (DeveloperPreferencesHandler.getPlayStoreMode()) {
            gateways.addAll(PlayStoreModeDataModel.getGateways());
        } else {
            try {
                gateways.addAll(DatabaseHandler.getAllGateways());
            } catch (Exception e) {
                StatusMessageHandler.showErrorMessage(getActivity(), e);
            }
        }
    }

    private void updateUI() {
        if (SmartphonePreferencesHandler.getHideAddFAB()) {
            searchGatewayFAB.setVisibility(View.GONE);
            addGatewayFAB.setVisibility(View.GONE);
        } else {
            searchGatewayFAB.setVisibility(View.VISIBLE);
            addGatewayFAB.setVisibility(View.VISIBLE);
        }

        refreshGateways();

        gatewayRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (super.onOptionsItemSelected(menuItem)) {
            return true;
        }

        switch (menuItem.getItemId()) {
            case R.id.create_gateway:
                ConfigureGatewayDialog configureGatewayDialog = new ConfigureGatewayDialog();
                configureGatewayDialog.setTargetFragment(this, 0);
                configureGatewayDialog.show(getFragmentManager(), null);
            case R.id.search_gateways:
                startAutoDiscovery();
            default:
                break;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.gateway_fragment_menu, menu);

        if (SettingsConstants.THEME_DARK_BLUE == SmartphonePreferencesHandler.getTheme()) {
            menu.findItem(R.id.create_gateway).setIcon(IconicsHelper.getAddIcon(getActivity(), android.R.color.white));
            menu.findItem(R.id.search_gateways)
                    .setIcon(IconicsHelper.getRefreshIcon(getActivity(), android.R.color.white));
        } else {
            menu.findItem(R.id.create_gateway).setIcon(IconicsHelper.getAddIcon(getActivity(), android.R.color.black));
            menu.findItem(R.id.search_gateways)
                    .setIcon(IconicsHelper.getRefreshIcon(getActivity(), android.R.color.black));
        }


        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalBroadcastConstants.INTENT_GATEWAY_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public RecyclerView getRecyclerView() {
        return recyclerViewGateways;
    }
}
