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

package eu.power_switch.gui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.entity.Library;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.MiniDrawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import eu.power_switch.R;
import eu.power_switch.database.handler.DatabaseHandler;
import eu.power_switch.exception.gateway.GatewayAlreadyExistsException;
import eu.power_switch.gui.IconicsHelper;
import eu.power_switch.gui.StatusMessageHandler;
import eu.power_switch.gui.adapter.HistoryItemRecyclerViewAdapter;
import eu.power_switch.gui.dialog.DonationDialog;
import eu.power_switch.gui.fragment.ApartmentFragment;
import eu.power_switch.gui.fragment.BackupFragment;
import eu.power_switch.gui.fragment.SleepAsAndroidFragment;
import eu.power_switch.gui.fragment.TimersFragment;
import eu.power_switch.gui.fragment.geofences.GeofencesTabFragment;
import eu.power_switch.gui.fragment.main.RoomSceneTabFragment;
import eu.power_switch.gui.fragment.settings.SettingsTabFragment;
import eu.power_switch.history.HistoryItem;
import eu.power_switch.network.NetworkHandler;
import eu.power_switch.obj.gateway.Gateway;
import eu.power_switch.settings.DeveloperPreferencesHandler;
import eu.power_switch.settings.SmartphonePreferencesHandler;
import eu.power_switch.shared.constants.LocalBroadcastConstants;
import eu.power_switch.shared.constants.PermissionConstants;
import eu.power_switch.shared.constants.SettingsConstants;
import eu.power_switch.shared.log.Log;
import eu.power_switch.special.HolidaySpecialHandler;
import eu.power_switch.wear.service.UtilityService;
import eu.power_switch.widget.provider.ReceiverWidgetProvider;
import eu.power_switch.widget.provider.RoomWidgetProvider;
import eu.power_switch.widget.provider.SceneWidgetProvider;

/**
 * Main entry Activity for the app
 */
public class MainActivity extends AppCompatActivity {

    public static final int IDENTIFIER_ROOMS_SCENES = 10;
    public static final int IDENTIFIER_APARTMENTS = 11;
    public static final int IDENTIFIER_GEOFENCES = 12;
    public static final int IDENTIFIER_SLEEP_AS_ANDROID = 13;
    public static final int IDENTIFIER_TIMERS = 14;
    public static final int IDENTIFIER_BACKUP_RESTORE = 15;
    public static final int IDENTIFIER_SETTINGS = 16;
    public static final int IDENTIFIER_ABOUT = 17;

    public static boolean appIsInForeground = false;
    private static Stack<Class> lastFragmentClasses = new Stack<>();
    private static Stack<String> lastFragmentTitles = new Stack<>();
    private static Stack<Integer> drawerPositionStack = new Stack<>();

    private static AppBarLayout appBarLayout;
    private static MainActivity activity;
    private LinkedList<HistoryItem> historyItems = new LinkedList<>();
    private RecyclerView recyclerViewHistory;
    private HistoryItemRecyclerViewAdapter historyItemArrayAdapter;
    private BroadcastReceiver broadcastReceiver;
    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private Drawer historyDrawer;
    private MiniDrawer miniDrawer;

    /**
     * Add class to Backstack
     *
     * @param newFragmentClass Classname
     * @param title            Header title
     */
    public static void addToBackstack(int menuItemIdentifier, Class newFragmentClass, String title) {
        lastFragmentClasses.push(newFragmentClass);
        lastFragmentTitles.push(title);
        drawerPositionStack.push(menuItemIdentifier);
    }

    /**
     * Indicates whether the App is running in Foreground or in Background
     *
     * @return true if the App is in Foreground
     */
    public static boolean isInForeground() {
        return appIsInForeground;
    }

    /**
     * Get this activity.
     * Used for always accessible fragmentManager in {@link StatusMessageHandler}
     *
     * @return this
     */
    public static AppCompatActivity getActivity() {
        return activity;
    }

    /**
     * Get Main App View
     *
     * @return View
     */
    public static View getMainAppView() {
        return appBarLayout;
    }

    /**
     * Used to notify Room Fragment (this) that Rooms have changed
     *
     * @param context any suitable context
     */
    public static void sendHistoryChangedBroadcast(Context context) {
        Log.d(MainActivity.class, "sendHistoryChangedBroadcast");
        Intent intent = new Intent(LocalBroadcastConstants.INTENT_HISTORY_CHANGED);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void startFragmentTransaction(int menuItemIdentifier, String menuItemTitle, Fragment fragment) {
        try {
            if (fragment != null && (lastFragmentClasses.isEmpty()) || !lastFragmentClasses.peek()
                    .equals(fragment.getClass())) {
                lastFragmentClasses.push(fragment.getClass());
                lastFragmentTitles.push(String.valueOf(menuItemTitle));
                drawerPositionStack.push(menuItemIdentifier);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim
                                .slide_in_right, R.anim.slide_out_left, android.R.anim
                                .slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContentFrameLayout, fragment)
                        .addToBackStack(fragment.getTag()).commit();

                setTitle(menuItemTitle);
            }

        } catch (Exception e) {
            Log.e(e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set Theme before anything else in onCreate();
        applyTheme();
        // apply forced locale (if set in developer options)
        applyLocale();

        activity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initNavigationDrawer();
        initHistoryDrawer();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateHistory();
            }
        };

        // Load first Fragment
        try {
            Fragment fragment;
            if (SmartphonePreferencesHandler.getCurrentApartmentId() == SettingsConstants.INVALID_APARTMENT_ID) {
                fragment = ApartmentFragment.class.newInstance();
            } else {
                fragment = RoomSceneTabFragment.newInstance(SmartphonePreferencesHandler.getStartupDefaultTab());
            }
            lastFragmentClasses.push(fragment.getClass());
            lastFragmentTitles.push(String.valueOf(getTitle()));
            drawerPositionStack.push(IDENTIFIER_ROOMS_SCENES);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainContentFrameLayout, fragment)
                    .commit();
        } catch (Exception e) {
            Log.e(e);
        }

        if (SmartphonePreferencesHandler.getAutoDiscover() && NetworkHandler.isWifiAvailable()) {
            new AsyncTask<Context, Void, Void>() {

                @Override
                protected Void doInBackground(Context... contexts) {
                    Context context = contexts[0];
                    NetworkHandler.init(context);
                    List<Gateway> foundGateways = NetworkHandler.searchGateways();

                    if (foundGateways != null) {
                        try {
                            if (foundGateways.isEmpty() && DatabaseHandler.getAllGateways().isEmpty()) {
                                StatusMessageHandler.showInfoMessage(context, R.string.no_gateway_found, Snackbar
                                        .LENGTH_LONG);
                            } else {
                                for (Gateway gateway : foundGateways) {
                                    if (gateway == null) {
                                        continue;
                                    }
                                    try {
                                        DatabaseHandler.addGateway(gateway);
                                        StatusMessageHandler.showInfoMessage(context, R.string.gateway_found, Snackbar.LENGTH_LONG);
                                    } catch (GatewayAlreadyExistsException e) {
                                        try {
                                            DatabaseHandler.enableGateway(e.getIdOfExistingGateway());
                                            StatusMessageHandler.showInfoMessage(context, R.string.gateway_found, Snackbar.LENGTH_LONG);
                                        } catch (Exception e1) {
                                            Log.e(e1);
                                            StatusMessageHandler.showInfoMessage(context, R.string.error_enabling_gateway, Snackbar.LENGTH_LONG);
                                        }
                                    } catch (Exception e) {
                                        StatusMessageHandler.showErrorMessage(context, e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            StatusMessageHandler.showErrorMessage(context, e);
                        }
                    }
                    return null;
                }
            }.execute(this);
        }

        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                Context context = contexts[0];

                // update wear data
                UtilityService.forceWearDataUpdate(context);
                UtilityService.forceWearSettingsUpdate(context);

                // update receiver widgets
                ReceiverWidgetProvider.forceWidgetUpdate(context);
                // update room widgets
                RoomWidgetProvider.forceWidgetUpdate(context);
                // update scene widgets
                SceneWidgetProvider.forceWidgetUpdate(context);
                return null;
            }
        }.execute(this);
    }

    private void applyLocale() {
        if (DeveloperPreferencesHandler.getForceLanguage()) {
            Resources res = getResources();
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = DeveloperPreferencesHandler.getLocale();
            res.updateConfiguration(conf, dm);
        }
    }

    private void initNavigationDrawer() {
        // Set the menu icon instead of the launcher icon.
        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(IconicsHelper.getMenuIcon(this));
        ab.setDisplayHomeAsUpEnabled(true);

        int tintColor;
        if (SettingsConstants.THEME_DARK_BLUE == SmartphonePreferencesHandler.getTheme()) {
            tintColor = ContextCompat.getColor(this, R.color.textColorSecondary);
        } else {
            tintColor = ContextCompat.getColor(this, R.color.textColorSecondaryInverse);
        }

        // if you want to update the items at a later time it is recommended to keep it in a variable
        final PrimaryDrawerItem itemHome = new PrimaryDrawerItem().withName(R.string.menu_home)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_arrow_back)
                        .color(ContextCompat.getColor(this, R.color.color_light_blue_a700))
                        .sizeDp(24))
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        while (drawerPositionStack.size() > 1) {
                            drawerPositionStack.pop();
                        }
                        while (lastFragmentClasses.size() > 1) {
                            lastFragmentClasses.pop();
                        }
                        while (lastFragmentTitles.size() > 1) {
                            lastFragmentTitles.pop();
                        }

                        setTitle(R.string.powerswitch_app_name);
                        navigationDrawer.setSelection(IDENTIFIER_ROOMS_SCENES);

                        navigationDrawer.closeDrawer();
                        return true;
                    }
                });
        final PrimaryDrawerItem itemRoomsScenes = new PrimaryDrawerItem().withName(R.string.menu_rooms_scenes)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_lamp)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_ROOMS_SCENES)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_ROOMS_SCENES, getString(R.string.menu_rooms_scenes),
                                    RoomSceneTabFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final PrimaryDrawerItem itemApartments = new PrimaryDrawerItem().withName(R.string.menu_apartments)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_home)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_APARTMENTS)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_APARTMENTS, getString(R.string.menu_apartments),
                                    ApartmentFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final PrimaryDrawerItem itemGeofences = new PrimaryDrawerItem().withName(R.string.menu_geofences)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_gps_dot)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_GEOFENCES)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_GEOFENCES, getString(R.string.menu_geofences),
                                    GeofencesTabFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final PrimaryDrawerItem itemSleepAsAndroid = new PrimaryDrawerItem().withName(R.string.menu_sleep_as_android)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_alarm)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_SLEEP_AS_ANDROID)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_SLEEP_AS_ANDROID, getString(R.string.menu_sleep_as_android),
                                    SleepAsAndroidFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        // if you want to update the items at a later time it is recommended to keep it in a variable
        final PrimaryDrawerItem itemTimer = new PrimaryDrawerItem().withName(R.string.timers)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_time)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_TIMERS)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_TIMERS, getString(R.string.timers),
                                    TimersFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final PrimaryDrawerItem itemBackupRestore = new PrimaryDrawerItem().withName(R.string.menu_backup_restore)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_time_restore)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_BACKUP_RESTORE)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_BACKUP_RESTORE, getString(R.string.menu_backup_restore),
                                    BackupFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final PrimaryDrawerItem itemSettings = new PrimaryDrawerItem().withName(R.string.menu_settings)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_settings)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_SETTINGS)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        try {
                            startFragmentTransaction(IDENTIFIER_SETTINGS, getString(R.string.menu_settings),
                                    SettingsTabFragment.class.newInstance());
                            navigationDrawer.closeDrawer();
                            return true;
                        } catch (Exception e) {
                            Log.e(e);
                            return false;
                        }
                    }
                });
        final SecondaryDrawerItem itemHelp = new SecondaryDrawerItem().withName(R.string.menu_help)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_help)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://power-switch.eu/faq/"));
                        startActivity(browserIntent);

                        navigationDrawer.closeDrawer();
                        return true;
                    }
                });
        final SecondaryDrawerItem itemDonate = new SecondaryDrawerItem().withName(R.string.donate)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_money)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        DonationDialog donationDialog = new DonationDialog();
                        donationDialog.show(getSupportFragmentManager(), null);

                        navigationDrawer.closeDrawer();
                        return true;
                    }
                });
        final SecondaryDrawerItem itemAbout = new SecondaryDrawerItem().withName(R.string.menu_about)
                .withIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_info)
                        .color(tintColor)
                        .sizeDp(24))
                .withSelectable(true)
                .withIdentifier(IDENTIFIER_ABOUT)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Fragment fragment = new LibsBuilder()
                                //get the fragment
                                .withAboutIconShown(true)
                                .withAboutVersionShown(true)
                                .withLicenseShown(true)
                                .withVersionShown(true)
                                .withAutoDetect(true)
                                .withAboutDescription(getString(R.string.app_description_html))
                                .withAboutSpecial1(getString(R.string.changelog))
                                .withAboutSpecial1Description(getString(R.string.changelog))
                                .withAboutSpecial2(getString(R.string.github))
                                .withAboutSpecial2Description(getString(R.string.github))
                                .withAboutSpecial3(getString(R.string.license))
                                .withAboutSpecial3Description(getString(R.string.gpl_v3_description_html))
                                .withListener(new LibsConfiguration.LibsListener() {
                                    @Override
                                    public void onIconClicked(View v) {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://power-switch.eu/"));
                                        startActivity(browserIntent);
                                    }

                                    @Override
                                    public boolean onLibraryAuthorClicked(View v, Library library) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLibraryContentClicked(View v, Library library) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLibraryBottomClicked(View v, Library library) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onExtraClicked(View v, Libs.SpecialButton specialButton) {
                                        if (specialButton == Libs.SpecialButton.SPECIAL1) {
                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://power-switch.eu/download/"));
                                            startActivity(browserIntent);
                                            return true;
                                        } else if (specialButton == Libs.SpecialButton.SPECIAL2) {
                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Power-Switch/PowerSwitch_Android"));
                                            startActivity(browserIntent);
                                            return true;
                                        } else if (specialButton == Libs.SpecialButton.SPECIAL3) {
                                            return false;
                                        }

                                        return false;
                                    }

                                    @Override
                                    public boolean onIconLongClicked(View v) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLibraryAuthorLongClicked(View v, Library library) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLibraryContentLongClicked(View v, Library library) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLibraryBottomLongClicked(View v, Library library) {
                                        return false;
                                    }
                                })
                                .supportFragment();
                        fragment.setHasOptionsMenu(true);
                        startFragmentTransaction(IDENTIFIER_ABOUT, getString(R.string.menu_about), fragment);

                        navigationDrawer.closeDrawer();
                        return true;
                    }
                });

        AccountHeader accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .withCompactStyle(false)
                .build();

        navigationDrawer = new DrawerBuilder(this)
                .withToolbar(toolbar)
                .withTranslucentStatusBar(true)
                .withAccountHeader(accountHeader)
                .withHeaderPadding(true)
                .addDrawerItems(
                        itemHome,
                        new DividerDrawerItem(),
                        itemApartments,
                        itemRoomsScenes,
                        new DividerDrawerItem(),
                        itemGeofences,
                        itemSleepAsAndroid,
                        itemTimer,
                        new DividerDrawerItem(),
                        itemBackupRestore,
                        itemSettings,
                        new DividerDrawerItem(),
                        itemHelp,
                        itemDonate,
                        itemAbout)
                .build();

        navigationDrawer.setSelection(IDENTIFIER_ROOMS_SCENES);
    }

    private void initHistoryDrawer() {
        View historyView = LayoutInflater.from(this).inflate(R.layout.drawer_history, null, false);

        recyclerViewHistory = (RecyclerView) historyView.findViewById(R.id.recyclerview_history);
        historyItemArrayAdapter = new HistoryItemRecyclerViewAdapter(this, historyItems);
        recyclerViewHistory.setAdapter(historyItemArrayAdapter);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        recyclerViewHistory.setLayoutManager(layoutManager);

        historyDrawer = new DrawerBuilder()
                .withActivity(this)
                .withCustomView(historyView)
                .withHeaderPadding(true)
                .withDrawerGravity(Gravity.END)
                .append(navigationDrawer);
    }

    private void applyTheme() {
        switch (SmartphonePreferencesHandler.getTheme()) {
            case SettingsConstants.THEME_DARK_BLUE:
                getApplicationContext().setTheme(R.style.PowerSwitchTheme_Dark_Blue);
                setTheme(R.style.PowerSwitchTheme_Dark_Blue);
                break;
            case SettingsConstants.THEME_DARK_RED:
                getApplicationContext().setTheme(R.style.PowerSwitchTheme_Dark_Red);
                setTheme(R.style.PowerSwitchTheme_Dark_Red);
                break;
            case SettingsConstants.THEME_LIGHT_BLUE:
                getApplicationContext().setTheme(R.style.PowerSwitchTheme_Light_Blue);
                setTheme(R.style.PowerSwitchTheme_Light_Blue);
                break;
            case SettingsConstants.THEME_LIGHT_RED:
                getApplicationContext().setTheme(R.style.PowerSwitchTheme_Light_Red);
                setTheme(R.style.PowerSwitchTheme_Light_Red);
                break;
            default:
                getApplicationContext().setTheme(R.style.PowerSwitchTheme_Dark_Blue);
                setTheme(R.style.PowerSwitchTheme_Dark_Blue);
                break;
        }
    }

    private void updateHistory() {
        try {
            historyItems.clear();
            historyItems.addAll(DatabaseHandler.getHistory());
        } catch (Exception e) {
            StatusMessageHandler.showErrorMessage(this, e);
        }

        historyItemArrayAdapter.notifyDataSetChanged();
        recyclerViewHistory.scrollToPosition(historyItems.size() - 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionConstants.REQUEST_CODE_STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    BackupFragment.sendBackupsChangedBroadcast(this);
                } else {
                    // Permission Denied
                    StatusMessageHandler.showInfoMessage(this, R.string.permission_denied, Snackbar.LENGTH_LONG);
                }
                break;
            case PermissionConstants.REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    // TODO: update geofences?
                } else {
                    // Permission Denied
                    StatusMessageHandler.showInfoMessage(this, R.string.permission_denied, Snackbar.LENGTH_LONG);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        lastFragmentClasses.pop();
        lastFragmentTitles.pop();
        drawerPositionStack.pop();
        if (!lastFragmentTitles.isEmpty()) {
            setTitle(lastFragmentTitles.peek());
            navigationDrawer.setSelection(drawerPositionStack.peek(), false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalBroadcastConstants.INTENT_HISTORY_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appIsInForeground = true;

        StatusMessageHandler.showErrorMessage(this, new Exception("HALLO"));

        updateHistory();
        HolidaySpecialHandler.showHolidaySpecial(this);
    }

    @Override
    protected void onPause() {
        appIsInForeground = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(this, "onActivityResult(" + requestCode + "," + resultCode + ","
                + data);

        if (DonationDialog.iapHelper == null) {
            return;
        }

        // Pass on the activity result to the helper for handling
        if (!DonationDialog.iapHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(this, "onActivityResult handled by IABUtil.");
        }
    }
}
