package sk.henrichg.phoneprofileshelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.stericson.RootShell.RootShell;
import com.stericson.RootTools.RootTools;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SetProfilePreferenceService extends IntentService
{
    Context context;

    int GPSChange = 0;
    int airplaneModeChange = 0;
    int NFCChange = 0;
    int WifiChange = 0;
    int bluetoothChange = 0;
    int mobileDataChange = 0;
    int WifiAPChange = 0;
    int networkTypeChange = 0;

    public static final String PROCEDURE = "procedure";

    public static final String PROCEDURE_RADIO_CHANGE = "radioChange";

    public static final String GPS_CHANGE = "GPSChange";
    public static final String AIRPLANE_MODE_CHANGE = "airplaneModeChange";
    public static final String NFC_CHANGE = "NFCChange";
    public static final String WIFI_CHANGE = "WiFiChange";
    public static final String BLUETOOTH_CHANGE = "bluetoothChange";
    public static final String MOBILE_DATA_CHANGE = "mobileDataChange";
    public static final String WIFI_AP_CHANGE = "WiFiAPChange";
    public static final String NETWORK_TYPE_CHANGE = "networkTypeChange";

    private static final String PREF_PROFILE_DEVICE_GPS = "prf_pref_deviceGPS";
    private static final String PREF_PROFILE_DEVICE_AIRPLANE_MODE = "prf_pref_deviceAirplaneMode";
    private static final String PREF_PROFILE_DEVICE_NFC = "prf_pref_deviceNFC";
    private static final String PREF_PROFILE_DEVICE_WIFI = "prf_pref_deviceWiFi";
    private static final String PREF_PROFILE_DEVICE_BLUETOOTH = "prf_pref_deviceBluetooth";
    private static final String PREF_PROFILE_DEVICE_MOBILE_DATA = "prf_pref_deviceMobileData";
    private static final String PREF_PROFILE_DEVICE_WIFI_AP = "prf_pref_deviceWiFiAP";
    private static final String PREF_PROFILE_DEVICE_NETWORK_TYPE = "prf_pref_deviceNetworkType";

    // for synchronization between wifi/bluetooth scanner, local radio changes and PPHelper radio changes
    static final String RADIO_CHANGE_PREFS_NAME = "sk.henrichg.phoneprofiles.radio_change";
    static final String PREF_RADIO_CHANGE_STATE = "sk.henrichg.phoneprofiles.radioChangeState";

    static final String PHONEPROFILES_PACKAGENAME = "sk.henrichg.phoneprofiles";
    static final String PHONEPROFILESPLUS_PACKAGENAME = "sk.henrichg.phoneprofilesplus";

    public SetProfilePreferenceService() {
        super("SetProfilePreferenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        context = getBaseContext();

        SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent", "-- start --------------------------------");

        String	procedure = intent.getStringExtra (PROCEDURE);
        SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","procedure="+procedure);

        if (procedure.equals(PROCEDURE_RADIO_CHANGE))
        {
            GPSChange = intent.getIntExtra(GPS_CHANGE, 0);
            airplaneModeChange = intent.getIntExtra(AIRPLANE_MODE_CHANGE, 0);
            NFCChange = intent.getIntExtra(NFC_CHANGE, 0);
            WifiChange = intent.getIntExtra(WIFI_CHANGE, 0);
            bluetoothChange = intent.getIntExtra(BLUETOOTH_CHANGE, 0);
            mobileDataChange = intent.getIntExtra(MOBILE_DATA_CHANGE, 0);
            WifiAPChange = intent.getIntExtra(WIFI_AP_CHANGE, 0);
            networkTypeChange = intent.getIntExtra(NETWORK_TYPE_CHANGE, 0);

            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","GPSChange="+GPSChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","airplaneModeChange="+airplaneModeChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","NFCChange="+NFCChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","WifiChange="+WifiChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","bluetoothChange="+bluetoothChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","mobileDataChange="+mobileDataChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","WifiAPChange="+WifiAPChange);
            SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent","networkTypeChange="+networkTypeChange);

            executeForRadios();
        }


        PPHeplerBroadcastReceiver.completeWakefulIntent(intent);

        SystemRoutines.logE("SetProfilePreferenceService.onHandleIntent", "-- end --------------------------------");

    }
    
    private void executeForRadios()
    {
        // synchronization, wait for end of radio state change
        waitForRadioStateChange();

        boolean _isAirplaneMode = false;
        boolean _setAirplaneMode = false;
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_AIRPLANE_MODE))
        {
            _isAirplaneMode = isAirplaneMode();
            switch (airplaneModeChange) {
                case 1:
                    if (!_isAirplaneMode)
                    {
                        _isAirplaneMode = true;
                        _setAirplaneMode = true;
                    }
                    break;
                case 2:
                    if (_isAirplaneMode)
                    {
                        _isAirplaneMode = false;
                        _setAirplaneMode = true;
                    }
                    break;
                case 3:
                    _isAirplaneMode = !_isAirplaneMode;
                    _setAirplaneMode = true;
                    break;
            }
        }

        if (_setAirplaneMode && _isAirplaneMode)
            // switch ON airplane mode, set it before executeForRadios
            setAirplaneMode(_isAirplaneMode);

        doExecuteForRadios();

        if (_setAirplaneMode && !(_isAirplaneMode))
            // switch OFF airplane mode, set if after executeForRadios
            setAirplaneMode(_isAirplaneMode);

    }

    @SuppressWarnings("deprecation")
    private void doExecuteForRadios()
    {

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            System.out.println(e);
        }

        // nahodenie network type
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_NETWORK_TYPE))
        {
            if (networkTypeChange >= 100) {
                setPreferredNetworkType(networkTypeChange - 100);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }

        // nahodenie mobilnych dat
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_MOBILE_DATA))
        {
            SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","mobile data");
            boolean _isMobileData = isMobileData();
            boolean _setMobileData = false;
            switch (mobileDataChange) {
                case 1:
                    if (!_isMobileData)
                    {
                        _isMobileData = true;
                        _setMobileData = true;
                    }
                    break;
                case 2:
                    if (_isMobileData)
                    {
                        _isMobileData = false;
                        _setMobileData = true;
                    }
                    break;
                case 3:
                    _isMobileData = !_isMobileData;
                    _setMobileData = true;
                    break;
            }
            if (_setMobileData)
            {
                setMobileData(_isMobileData);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }

        // nahodenie WiFi AP
        boolean canChangeWifi = true;
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_WIFI_AP))
        {
            WifiApManager wifiApManager = null;
            try {
                wifiApManager = new WifiApManager(context);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            if (wifiApManager != null) {
                boolean setWifiAPState = false;
                boolean isWifiAPEnabled = wifiApManager.isWifiAPEnabled();
                switch (WifiAPChange) {
                    case 1:
                        if (!isWifiAPEnabled) {
                            isWifiAPEnabled = true;
                            setWifiAPState = true;
                            canChangeWifi = false;
                        }
                        break;
                    case 2:
                        if (isWifiAPEnabled) {
                            isWifiAPEnabled = false;
                            setWifiAPState = true;
                            canChangeWifi = true;
                        }
                        break;
                    case 3:
                        isWifiAPEnabled = !isWifiAPEnabled;
                        setWifiAPState = true;
                        canChangeWifi = !isWifiAPEnabled;
                        break;
                }
                if (setWifiAPState) {
                    wifiApManager.setWifiApState(isWifiAPEnabled);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            }
        }

        if (canChangeWifi) {
            // nahodenie WiFi
            if (isPreferenceAllowed(PREF_PROFILE_DEVICE_WIFI)) {
                SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios", "wifi");
                if (!WifiApManager.isWifiAPEnabled(context)) { // only when wifi AP is not enabled, change wifi
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    int wifiState = wifiManager.getWifiState();
                    boolean isWifiEnabled = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_ENABLING));
                    boolean setWifiState = false;
                    switch (WifiChange) {
                        case 1:
                            if (!isWifiEnabled) {
                                isWifiEnabled = true;
                                setWifiState = true;
                            }
                            break;
                        case 2:
                            if (isWifiEnabled) {
                                isWifiEnabled = false;
                                setWifiState = true;
                            }
                            break;
                        case 3:
                            isWifiEnabled = !isWifiEnabled;
                            setWifiState = true;
                            break;
                    }
                    if (setWifiState) {
                        try {
                            wifiManager.setWifiEnabled(isWifiEnabled);
                        } catch (Exception e) {
                            // barla pre security exception INTERACT_ACROSS_USERS - chyba ROM
                            wifiManager.setWifiEnabled(isWifiEnabled);
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                }
            }
        }

        // nahodenie bluetooth
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_BLUETOOTH))
        {
            SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","bluetooth");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            boolean isBluetoothEnabled = bluetoothAdapter.isEnabled();
            boolean setBluetoothState = false;
            switch (bluetoothChange) {
                case 1 :
                    if (!isBluetoothEnabled)
                    {
                        isBluetoothEnabled = true;
                        setBluetoothState = true;
                    }
                    break;
                case 2 :
                    if (isBluetoothEnabled)
                    {
                        isBluetoothEnabled = false;
                        setBluetoothState = true;
                    }
                    break;
                case 3 :
                    isBluetoothEnabled = ! isBluetoothEnabled;
                    setBluetoothState = true;
                    break;
            }
            if (setBluetoothState)
            {
                if (isBluetoothEnabled)
                    bluetoothAdapter.enable();
                else
                    bluetoothAdapter.disable();
            }
        }

        // nahodenie GPS
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_GPS))
        {
            SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","gps="+GPSChange);

            //String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            //SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","gps  provider="+provider);

            boolean isEnabled;
            if (android.os.Build.VERSION.SDK_INT < 21)
                isEnabled = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER);
            else {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
            SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","isEnabled="+isEnabled);


            //Log.d("ActivateProfileHelper.execute", provider);

            switch (GPSChange) {
                case 1 :
                    setGPS(true);
                    break;
                case 2 :
                    setGPS(false);
                    break;
                case 3 :
                    if (!isEnabled)
                    {
                        setGPS(true);
                    }
                    else
                    if (isEnabled)
                    {
                        setGPS(false);
                    }
                    break;
            }
        }

        // nahodenie NFC
        if (isPreferenceAllowed(PREF_PROFILE_DEVICE_NFC))
        {
            SystemRoutines.logE("SetProfilePreferenceService.doExecuteForRadios","nfc");
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);

            if(nfcAdapter != null)
            {
                switch (NFCChange) {
                    case 1 :
                        setNFC(true);
                        break;
                    case 2 :
                        setNFC(false);
                        break;
                    case 3 :
                        if (!nfcAdapter.isEnabled())
                        {
                            setNFC(true);
                        }
                        else
                        if (nfcAdapter.isEnabled())
                        {
                            setNFC(false);
                        }
                        break;
                }
            }
        }
    }

    
    private boolean isPreferenceAllowed(String preferenceKey)
    {
        //long nanoTimeStart = startMeasuringRunTime();

        boolean featurePresented = false;

        if (preferenceKey.equals(PREF_PROFILE_DEVICE_AIRPLANE_MODE))
        {
            featurePresented = true;
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_WIFI))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI))
                // device ma Wifi
                featurePresented = true;
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_BLUETOOTH))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
                // device ma bluetooth
                featurePresented = true;
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_MOBILE_DATA))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            {
                // device ma mobilne data
                if (canSetMobileData())
                    featurePresented = true;
            }
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_GPS))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
            {
                // device ma gps
                featurePresented = true;
            }
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_NFC))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC))
            {
                // device ma nfc
                featurePresented = true;
            }
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_WIFI_AP))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI))
                // device ma Wifi
                featurePresented = true;
        }
        else
        if (preferenceKey.equals(PREF_PROFILE_DEVICE_NETWORK_TYPE))
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            {
                // device ma mobilne data
                if (canSetPreferredNetworkType())
                    featurePresented = true;
            }

        }
        else
            featurePresented = true;

        //getMeasuredRunTime(nanoTimeStart, "GlobalData.hardwareCheck for "+preferenceKey);

        return featurePresented;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private boolean isAirplaneMode()
    {
        if (android.os.Build.VERSION.SDK_INT >= 17)
            return Settings.Global.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) != 0;
        else
            return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneMode(boolean mode)
    {
        if (android.os.Build.VERSION.SDK_INT >= 17)
            setAirplaneMode_SDK17(mode);
        else
            setAirplaneMode_SDK8(mode);
    }

    @SuppressLint("NewApi")
    private void setAirplaneMode_SDK17(boolean mode)
    {
        boolean isRooted = RootTools.isRootAvailable();
        if (!isRooted)
            isRooted = RootTools.isAccessGiven();
        if (isRooted) {

            String command;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, mode ? 1 : 0);
            else
                Settings.System.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, mode ? 1 : 0);
            /*if (android.os.Build.VERSION.SDK_INT < 18)
            {
                // Not working in Android 4.3+ (SecurityException :-/ )
                Intent intentBr = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intentBr.putExtra("state", mode);
                context.sendBroadcast(intentBr);
            }
            else
            {*/
                //SystemRoutines.getSUVersion();
                // This shows grant root privileges dialog :-/
                if (mode)
                    command = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true";
                else
                    command = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false";
                //if (SystemRoutines.isSELinuxEnforcing())
                //{
                //	command = SystemRoutines.getSELinuxEnforceCommand(command, Shell.ShellContext.SYSTEM_APP);
                //}
                Command commandCapture = new Command(0, false, command);
                try {
                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(commandCapture);
                    commandWait(commandCapture);
                    //RootTools.closeAllShells();
                } catch (Exception e) {
                    Log.e("SetProfilePreferenceService.setAirplaneMode_SDK17", "Error on run su");
                }
            /*}*/
        }
    }

    @SuppressWarnings("deprecation")
    private void setAirplaneMode_SDK8(boolean mode)
    {
        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", mode);
        context.sendBroadcast(intent);
    }

    private boolean canSetMobileData()
    {
        if (android.os.Build.VERSION.SDK_INT >= 22)
        {
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("getDataEnabled");
                getDataEnabledMethod.setAccessible(true);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            try {
                final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                final Method getMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
                getMobileDataEnabledMethod.setAccessible(true);
                return true;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private boolean isMobileData()
    {
        if (android.os.Build.VERSION.SDK_INT < 21)
        {
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            try {
                final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                final Method getMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
                getMobileDataEnabledMethod.setAccessible(true);
                return (Boolean)getMobileDataEnabledMethod.invoke(connectivityManager);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
        if (android.os.Build.VERSION.SDK_INT < 22)
        {
            Method getDataEnabledMethod;
            Class<?> telephonyManagerClass;
            Object ITelephonyStub;
            Class<?> ITelephonyClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                getDataEnabledMethod = ITelephonyClass.getDeclaredMethod("getDataEnabled");

                getDataEnabledMethod.setAccessible(true);

                return (Boolean)getDataEnabledMethod.invoke(ITelephonyStub);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            Method getDataEnabledMethod;
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                getDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("getDataEnabled");
                getDataEnabledMethod.setAccessible(true);

                return (Boolean)getDataEnabledMethod.invoke(telephonyManager);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    private void setMobileData(boolean enable)
    {
        if (android.os.Build.VERSION.SDK_INT < 21)
        {
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            boolean OK = false;
            try {
                final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                final Field iConnectivityManagerField = connectivityManagerClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(connectivityManager);
                final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);

                setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);

                OK = true;

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (!OK)
            {
                try {
                    Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);

                    setMobileDataEnabledMethod.setAccessible(true);
                    setMobileDataEnabledMethod.invoke(connectivityManager, enable);

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        else
        if (android.os.Build.VERSION.SDK_INT < 22)
        {
            Method dataConnSwitchmethod;
            Class<?> telephonyManagerClass;
            Object ITelephonyStub;
            Class<?> ITelephonyClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                /*
                if (isEnabled) {
                    dataConnSwitchmethod = ITelephonyClass
                            .getDeclaredMethod("disableDataConnectivity");
                } else {
                    dataConnSwitchmethod = ITelephonyClass
                            .getDeclaredMethod("enableDataConnectivity");
                }
                */
                dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("setDataEnabled", Boolean.TYPE);

                dataConnSwitchmethod.setAccessible(true);
                dataConnSwitchmethod.invoke(ITelephonyStub, enable);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        else
        {
            Method setDataEnabledMethod;
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            try {
                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                setDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("setDataEnabled", Boolean.TYPE);
                setDataEnabledMethod.setAccessible(true);

                setDataEnabledMethod.invoke(telephonyManager, enable);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }

    @SuppressWarnings("deprecation")
    private void setGPS(boolean enable)
    {
        if (enable)
        {
            String newSet;
            if (android.os.Build.VERSION.SDK_INT < 23) {
                String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                if (provider.equals(""))
                    newSet = LocationManager.GPS_PROVIDER;
                else
                    newSet = String.format("%s,%s", provider, LocationManager.GPS_PROVIDER);
            }
            else
                newSet = "+gps";
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, newSet);
        }
        else
        {
            String newSet = "";
            if (android.os.Build.VERSION.SDK_INT < 23) {
                String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                String[] list = provider.split(",");
                int j = 0;
                for (int i = 0; i < list.length; i++) {
                    if (!list[i].equals(LocationManager.GPS_PROVIDER)) {
                        if (j > 0)
                            newSet += ",";
                        newSet += list[i];
                        j++;
                    }
                }
            }
            else
                newSet = "-gps";
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, newSet);
        }
    }

    private void setNFC(boolean enable)
    {
        Class<?> NfcClass;
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if(nfcAdapter != null)
        {
            if(enable && !nfcAdapter.isEnabled())
            {
                try {
                    Method enableNfc;
                    NfcClass = Class.forName(nfcAdapter.getClass().getName());
                    enableNfc   = NfcClass.getDeclaredMethod("enable");
                    enableNfc.setAccessible(true);
                    enableNfc.invoke(nfcAdapter);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            if(!enable && nfcAdapter.isEnabled())
            {
                try {
                    Method disableNfc;
                    NfcClass = Class.forName(nfcAdapter.getClass().getName());
                    disableNfc   = NfcClass.getDeclaredMethod("disable");
                    disableNfc.setAccessible(true);
                    disableNfc.invoke(nfcAdapter);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean canSetPreferredNetworkType()
    {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final int phoneType = telephonyManager.getPhoneType();
        if ((phoneType == TelephonyManager.PHONE_TYPE_GSM) || (phoneType == TelephonyManager.PHONE_TYPE_CDMA)) {
            if (serviceBinaryExists() && telephonyServiceExists(context, PREF_PROFILE_DEVICE_NETWORK_TYPE))
                return true;
        }
        return false;
    }

    private static String getTransactionCode(Context context, String fieldName) throws Exception {
        try {
            final TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final Class<?> mTelephonyClass = Class.forName(mTelephonyManager.getClass().getName());
            final Method mTelephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
            mTelephonyMethod.setAccessible(true);
            final Object mTelephonyStub = mTelephonyMethod.invoke(mTelephonyManager);
            final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
            final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
            final Field field = mClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return String.valueOf(field.getInt(null));
        } catch (Exception e) {
            // The "TRANSACTION_setDataEnabled" field is not available,
            // or named differently in the current API level, so we throw
            // an exception and inform users that the method is not available.
            throw e;
        }
    }

    private void setPreferredNetworkType(int networkType) {
        boolean isRooted = RootTools.isRootAvailable();
        if (!isRooted)
            isRooted = RootTools.isAccessGiven();
        if (isRooted) {
            try {
                // Get the value of the "TRANSACTION_setPreferredNetworkType" field.
                String transactionCode = getTransactionCode(context, "TRANSACTION_setPreferredNetworkType");
                if (Build.VERSION.SDK_INT >= 23) {
                    SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    // Loop through the subscription list i.e. SIM list.
                    for (int i = 0; i < mSubscriptionManager.getActiveSubscriptionInfoCountMax(); i++) {
                        if (transactionCode != null && transactionCode.length() > 0) {
                            // Get the active subscription ID for a given SIM card.
                            int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(i).getSubscriptionId();
                            String command1 = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + networkType;
                            Command command = new Command(0, false, command1);
                            try {
                                RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                commandWait(command);
                                //RootTools.closeAllShells();
                            } catch (Exception e) {
                                Log.e("SetProfilePreferenceService.setPreferredNetworkType", "Error on run su");
                            }
                        }
                    }
                } else {
                    if (transactionCode != null && transactionCode.length() > 0) {
                        String command1 = "service call phone " + transactionCode + " i32 " + networkType;
                        Command command = new Command(0, false, command1);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            commandWait(command);
                            //RootTools.closeAllShells();
                        } catch (Exception e) {
                            Log.e("SetProfilePreferenceService.setPreferredNetworkType", "Error on run su");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean serviceBinaryExists()
    {
        boolean isRooted = RootTools.isRootAvailable();
        if (!isRooted)
            isRooted = RootTools.isAccessGiven();
        if (isRooted) {
            List<String> servicePaths = RootTools.findBinary("service");
            return servicePaths.size() > 0;
        }
        else
            return false;
    }

    static boolean telephonyServiceExists(Context context, String preference) {
        try {
            if (preference.equals(PREF_PROFILE_DEVICE_MOBILE_DATA)) {
                String s = getTransactionCode(context, "TRANSACTION_setDataEnabled");
            }
            else
            if (preference.equals(PREF_PROFILE_DEVICE_NETWORK_TYPE)) {
                String s = getTransactionCode(context, "TRANSACTION_setPreferredNetworkType");
            }
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private void commandWait(Command cmd) throws Exception {
        int waitTill = 50;
        int waitTillMultiplier = 2;
        int waitTillLimit = 3200; //7 tries, 6350 msec

        while (!cmd.isFinished() && waitTill<=waitTillLimit) {
            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(waitTill);
                        waitTill *= waitTillMultiplier;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!cmd.isFinished()){
            Log.e("SetProfilePreferenceService", "Could not finish root command in " + (waitTill/waitTillMultiplier));
        }
    }

    private static void waitForRadioStateChange()
    {
        for (int i = 0; i < 5 * 60; i++) // 60 seconds for wifi scan (Android 5.0 bug, normally required 5 seconds :-/)
        {
            if (!ReceiversService.radioStateChange)
                break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
        ReceiversService.radioStateChange = false;
    }
    
}
