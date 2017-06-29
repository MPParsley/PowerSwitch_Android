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

package eu.power_switch.wizard.gui;

import android.os.Bundle;

import eu.power_switch.R;
import eu.power_switch.gui.IconicsHelper;

/**
 * "Time based actions" page, showing the possibilities of timers and alarm clock actions.
 * <p>
 * Created by Markus on 04.11.2016.
 */
public class TimerAlarmClockPage extends BasicPage {

    public static TimerAlarmClockPage newInstance() {
        Bundle              args     = new Bundle();
        TimerAlarmClockPage fragment = new TimerAlarmClockPage();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onSetUiValues() {
        super.onSetUiValues();
        setIcon(IconicsHelper.getAlarmClockIcon(getActivity()));
        setTitle(R.string.wizard_timer_alarm_clock_title);
        setDescription(R.string.wizard_timer_alarm_clock_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.md_red_700);
    }

}