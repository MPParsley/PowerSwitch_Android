/*
 *  PowerSwitch by Max Rosin & Markus Ressel
 *  Copyright (C) 2015  Markus Ressel
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.power_switch.gui.adapter;

import android.support.wear.widget.WearableRecyclerView;
import android.view.View;

import butterknife.ButterKnife;

/**
 * Base class for a RecyclerView.ViewHolder backed by ButterKnife
 * <p>
 * Created by Markus on 30.06.2017.
 */

public abstract class ButterKnifeWearableViewHolder extends WearableRecyclerView.ViewHolder {

    public ButterKnifeWearableViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

}