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

package eu.power_switch.gui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import eu.power_switch.R;
import eu.power_switch.network.DataApiHandler;
import eu.power_switch.obj.Scene;
import eu.power_switch.shared.haptic_feedback.VibrationHandler;
import eu.power_switch.shared.settings.WearablePreferencesHandler;

/**
 * Created by Markus on 15.08.2015.
 */
public class SceneRecyclerViewAdapter extends RecyclerView.Adapter<SceneRecyclerViewAdapter.ViewHolder> {

    // Store a member variable for the users
    private ArrayList<Scene> scenes;
    private Context context;
    private DataApiHandler dataApiHandler;
    private RecyclerView parentRecyclerView;

    // Pass in the context and users array into the constructor
    public SceneRecyclerViewAdapter(Context context, RecyclerView parentRecyclerView, ArrayList<Scene> scenes,
                                    DataApiHandler dataApiHandler) {
        this.scenes = scenes;
        this.context = context;
        this.parentRecyclerView = parentRecyclerView;
        this.dataApiHandler = dataApiHandler;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public SceneRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the custom layout
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_scene__round, parent, false);
        // Return a new holder instance
        return new SceneRecyclerViewAdapter.ViewHolder(itemView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(final SceneRecyclerViewAdapter.ViewHolder holder, int position) {
        // Get the data model based on position
        final Scene scene = scenes.get(position);

        // Set item views based on the data model
        holder.sceneName.setText(scene.getName());
        holder.buttonActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vibration Feedback
                if (WearablePreferencesHandler.getVibrateOnButtonPress()) {
                    VibrationHandler.vibrate(context, WearablePreferencesHandler.getVibrationDuration());
                }

                String actionString = DataApiHandler.buildSceneActionString(scene);
                dataApiHandler.sendSceneActionTrigger(actionString);
            }
        });

        if (holder.getAdapterPosition() == getItemCount() - 1) {
            holder.footer.setVisibility(View.VISIBLE);
        } else {
            holder.footer.setVisibility(View.GONE);
        }
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return scenes.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView sceneName;
        public android.widget.Button buttonActivate;
        public LinearLayout footer;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            super(itemView);
            this.sceneName = (TextView) itemView.findViewById(R.id.textView_scene_name);
            this.buttonActivate = (android.widget.Button) itemView.findViewById(R.id.button_Activate);
            this.footer = (LinearLayout) itemView.findViewById(R.id.list_footer);
        }
    }
}
