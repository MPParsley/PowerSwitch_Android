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

package eu.power_switch.database.table.action;

import android.database.sqlite.SQLiteDatabase;

/**
 * ReceiverAction table description
 */
public class ReceiverActionTable {

    public static final String TABLE_NAME = "receiver_actions";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ACTION_ID = "action_id";
    public static final String COLUMN_ROOM_ID = "room_id";
    public static final String COLUMN_RECEIVER_ID = "receiver_id";
    public static final String COLUMN_BUTTON_ID = "button_id";

    //@formatter:off
    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + "(" +
            COLUMN_ID + " integer primary key autoincrement," +
            COLUMN_ACTION_ID + " integer not null," +
            COLUMN_ROOM_ID + " integer not null," +
            COLUMN_RECEIVER_ID + " integer not null," +
            COLUMN_BUTTON_ID + " integer not null," +
            "FOREIGN KEY(" + COLUMN_ACTION_ID + ") REFERENCES " +
                ActionTable.TABLE_NAME + "(" + ActionTable.COLUMN_ID +
            ")" +
        ");";
    //@formatter:on

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                onCreate(db);
                break;

        }
    }
}