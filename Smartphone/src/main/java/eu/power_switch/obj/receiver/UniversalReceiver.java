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

package eu.power_switch.obj.receiver;

import android.content.Context;

import java.util.List;

import eu.power_switch.obj.UniversalButton;
import eu.power_switch.obj.button.Button;
import eu.power_switch.obj.gateway.Gateway;
import eu.power_switch.shared.exception.gateway.GatewayNotSupportedException;
import eu.power_switch.shared.exception.receiver.ActionNotSupportedException;
import eu.power_switch.shared.log.Log;

public class UniversalReceiver extends Receiver {

    private static final String MODEL = Receiver.getModelName(UniversalReceiver.class.getCanonicalName());

    public UniversalReceiver(Context context, Long id, String name, List<UniversalButton> buttons, Long
            roomId) {
        super(context, id, name, Brand.UNIVERSAL, MODEL, Type.UNIVERSAL, roomId);
        this.buttons.addAll(buttons);
    }

    @Override
    public String getSignal(Gateway gateway, String action) throws GatewayNotSupportedException, ActionNotSupportedException {
        try {
            for (Button button : buttons) {
                if (button.getName().equals(action)) {
                    return ((UniversalButton) button).getSignal();
                }
            }

            throw new ActionNotSupportedException(action);
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }
}
