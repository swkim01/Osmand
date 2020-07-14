package net.osmand.plus.rfobject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car.
 * 
 * @author Alena Fedasenka
 */
public class RfObjectPlugin extends OsmandPlugin {

	public static final String ID = "osmand.rfobject";
	public static final String RF_PLUGIN_COMPONENT = "net.osmand.rfPlugin"; //$NON-NLS-1$
	public final static String RF_FREQ_PLAN = "rf_freq_plan"; //$NON-NLS-1$
	public final static String RF_UPDATE_PERIOD = "rf_update_period"; //$NON-NLS-1$
	public final static String RF_TYPE = "rf_type"; //$NON-NLS-1$
	private OsmandApplication app;
	private MapActivity activity;
	protected DeviceScanActivity scanActivity;

	private RfObjectLayer rfLayer;
	private TextInfoWidget rfControl;
	private final CommonPreference<Integer> rfFreqPlan;
	private final CommonPreference<Integer> rfUpdatePeriod;
	private CommonPreference<Integer> rfType;

	private Map<String, RfObject> objects = new LinkedHashMap<String, RfObject>();
	private boolean mConnected = false;
	private boolean mBound = false;
	private String mDeviceName;
	private String mDeviceAddress;
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic characteristicTX;
	private BluetoothGattCharacteristic characteristicRX;
	private NmeaParser nmeaParser;

	public RfObjectPlugin(OsmandApplication app) {
		this.app = app;
		OsmandSettings set = app.getSettings();
		ApplicationMode. regWidget("rf", (ApplicationMode[]) null);
		rfFreqPlan = set.registerIntPreference(RF_FREQ_PLAN, 9).makeGlobal();
		rfUpdatePeriod = set.registerIntPreference(RF_UPDATE_PERIOD, 10).makeGlobal();
		rfType = set.registerIntPreference(RF_TYPE, 0).makeGlobal();
	}
	

	public Collection<RfObject> getRfObjects() {
		return objects.values();
	}

	public int getRfFreqPlan() {
		return rfFreqPlan.get();
	}

	public int getRfUpdatePeriod() {
		return rfUpdatePeriod.get();
	}
	
	public int getRfType() {
		return rfType.get();
	}
	
	public boolean clearRfObjects() {
		objects.clear();
		return true;
	}

	public DeviceScanActivity getScanActivity() {
		return scanActivity;
	}

	public void setScanActivity(DeviceScanActivity scanActivity) {
		this.scanActivity = scanActivity;
	}
	
	@Override
	public boolean init(OsmandApplication app, Activity activity) {
		return true;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.rf_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.rf_plugin_name);
	}

	public String getDeviceName() {
		return mDeviceName;
	}

	public void setDeviceName(String deviceName) {
		this.mDeviceName = deviceName;
	}

	public String getDeviceAddress() {
		return mDeviceAddress;
	}

	public void setDeviceAddress(String deviceAddress) {
		this.mDeviceAddress = deviceAddress;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		this.activity = activity;
		// remove old if existing after turn
		if(rfLayer != null) {
			activity.getMapView().removeLayer(rfLayer);
		}
		rfLayer = new RfObjectLayer(activity, this);
		activity.getMapView().addLayer(rfLayer, 5.5f);
		registerWidget(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (rfLayer == null) {
			registerLayers(activity);
		}
		if (rfControl == null) {
			registerWidget(activity);
		}
	}

	private void registerWidget(final MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			rfControl = new TextInfoWidget(activity) {
				@Override
				public boolean updateInfo(DrawSettings drawSettings) {
					int image;
					if (mConnected)
						image = R.drawable.widget_rf_connected;
					else
						image = R.drawable.widget_rf_inactive;
					setImageDrawable(image);
					setText(app.getString(R.string.osmand_rf_object_name), "");
					return true;
				}
			};
			rfControl.updateInfo(null);
			rfControl.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(activity, DeviceScanActivity.class);
					activity.startActivity(intent);
				}
			});
			mapInfoLayer.registerSideWidget(rfControl,
					R.drawable.ic_action_rf_dark,  R.string.map_widget_rf, "rf", false, 23);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
			final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {
		OnContextMenuClick showListener = new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int resId,
					int pos, boolean isChecked) {
				if (resId == R.string.context_menu_item_show_rf_object) {
					showRfDialog(mapActivity, latitude, longitude);
				}
				return true;
			}
		};
		adapter.item(R.string.context_menu_item_show_rf_object)
		.iconColor( R.drawable.ic_action_rf_dark).listen(showListener).position(0).reg();
		
	}

	/**
	 * Method dialog for adding of a rf location.
	 * It allows user to choose a type of rf (time-limited or time-unlimited).
	 */
	private void showRfDialog(final MapActivity mapActivity, final double latitude, final double longitude) {
		final View showRf = mapActivity.getLayoutInflater().inflate(R.layout.rf_show_info, null);
		final Dialog rfinfo = new Dialog(mapActivity);
		rfinfo.setContentView(showRf);
		rfinfo.setCancelable(true);
		rfinfo.setTitle(mapActivity.getString(R.string.osmand_rf_show_info));		
		rfinfo.show();
	}
	
	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, ContextMenuAdapter helper) {
		helper.item(R.string.rf_plugin_name).iconColor(R.drawable.ic_action_rf_dark).position(7)
				.listen(new OnContextMenuClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						Intent intent = new Intent(mapActivity, DeviceScanActivity.class);
						mapActivity.startActivity(intent);
						return true;
					}
				}).reg();

	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.rf_object;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_rf_dark;
	}

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				//Log.e(LOG_TAG, "Unable to initialize Bluetooth");
				//mapActivity.finish();
				return;
			}
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	public void bindService() {
		Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
		activity.bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);
		mBound = true;
	}

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.
	//  This can be a result of read or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				/*
				 * Use the result of
				 * http://edu-observatory.org/gps/gps_accuracy.html
				 *
				 *  Table 2   Standard error model - L1 C/A (no SA)
				 *								 One-sigma error, m
				 *
				 *  Error source			Bias	Random  Total   DGPS
				 *  ------------------------------------------------------------
				 *  Ephemeris data		  2.1	 0.0	 2.1 0.0
				 *  Satellite clock		 2.0	 0.7	 2.1	 0.0
				 *  Ionosphere			  4.0	 0.5	 4.0	 0.4
				 *  Troposphere			 0.5	 0.5	 0.7	 0.2
				 *  Multipath			   1.0	 1.0	 1.4	 1.4
				 *  Receiver measurement	0.5	 0.2	 0.5	 0.5
				 *  ------------------------------------------------------------
				 *  User equivalent range 
				 *	error (UERE), rms*	5.1	 1.4	 5.3	 1.6
				 *  Filtered UERE, rms	  5.1	 0.4	 5.1	 1.5
				 *  ------------------------------------------------------------
				 *
				 *  Vertical one-sigma errors--VDOP= 2.5		   12.8	 3.9
				 *  Horizontal one-sigma errors--HDOP= 2.0		 10.2	 3.1
				 *
				 * -------------------------------------------------------------
				 *  I adopt 5.1
				 */
				nmeaParser = new NmeaParser(RfObjectPlugin.this, 5.1f);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
                List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
				if (gattServices == null) return;
				//String uuid = null;
				//String unknownServiceString = getResources().getString(R.string.unknown_service);
				// Loops through available GATT Services.
				for (BluetoothGattService gattService : gattServices) {
					//uuid = gattService.getUuid().toString();
					// If the service exists for HM 10 Serial, say so.
					//if(SerialGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
					//}

					// get characteristic when UUID matches RX/TX UUID
					characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
					characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
				}

				if(mConnected) {
					//characteristicTX.setValue(tx);
				//mBluetoothLeService.writeCharacteristic(characteristicTX);
					mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
				}
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				processData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
			}
		}
	};

	String inString = "";

	// Process incoming data from rf objects
	// BLE allows sending maximum 20 bytes,
	// then packets need to be connected.
	private void processData(String data) {
		if (data != null) {
			if (data.contains("$")) {
				inString += data.substring(0, data.indexOf("$"));
				if (inString.length() != 0) {
					String msg = new String(inString);
					parseData(msg);
				}
				inString = data.substring(data.indexOf("$"));
				if (inString.contains("\n")) {
					String msg = inString.substring(0,inString.indexOf("\n")+1);
					parseData(msg);
					inString = inString.substring(inString.indexOf("\n")+1);
				}
			}
			else if (data.contains("\n")) {
				inString += data.substring(0, data.indexOf("\n")+1);
				String msg = new String(inString);
				parseData(msg);
				inString = data.substring(data.indexOf("\n")+1);
			}
			else {
				inString += data;
			}
		}
	}

	// Parse incoming nmea0183 gps/gnss data from rf objects
	private void parseData(String data) {
		RfObject object = null;

		if (data != null) {
			try {
				object = nmeaParser.parseNmeaSentence(data);
			} catch (SecurityException e) {
				//Log.d(TAG, "error while parsing NMEA sentence: " + data, e);
				object = null;
			}
		}
	}

	public RfObject updateRfObjects(RfStatus s) {
		RfObject p = null;
		boolean rfObjectExist = objects.containsKey(s.getAddress());
		if (rfObjectExist) {
			p = objects.get(s.getAddress());
		} else {
			p = new RfObject();
			p.setName(s.getAddress());
			objects.put(s.getAddress(), p);
		}
		//p.setLatitude(s.getLatitude());
		//p.setLongitude(s.getLongitude());
		p.setLocation(s.getLatitude(), s.getLongitude());
		return p;
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if(mBluetoothLeService != null && mDeviceAddress != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
		}
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		activity.unregisterReceiver(mGattUpdateReceiver);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	@Override
	public void mapActivityDestroy(MapActivity activity) {
		if (mBound) {
			activity.unbindService(mServiceConnection);
			mBound = false;
		}
		mBluetoothLeService = null;
	}

}
