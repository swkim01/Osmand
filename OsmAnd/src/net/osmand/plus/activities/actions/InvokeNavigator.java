package net.osmand.plus.activities.actions;

import java.util.ArrayList;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

public class InvokeNavigator extends OsmAndAction {
	
	public InvokeNavigator(MapActivity mapActivity) {
		super(mapActivity);
	}
	
	@Override
	public int getDialogID() {
		return OsmAndDialogs.DIALOG_INVOKE_NAVIGATOR;
	}
	
	@Override
	public void run() {
    	super.showDialog();
	}
	
	final int defaultFlag = PackageManager.MATCH_DEFAULT_ONLY;
	Intent[] explicitIntents;
	String[] navigators = new String[] { "Copilot", "iGO", "Navigon", "Sygic",
			"Google Navigation", "Ginius", "Ndrive", "Wisepilot" };
	// see an example here
	// http://stackoverflow.com/questions/2662531/launching-google-maps-directions-via-an-intent-on-android
	private Intent[] getExplicitIntents() {
		if (explicitIntents == null) {
			PackageManager currentPM = mapActivity.getPackageManager();
			explicitIntents = new Intent[] {
				currentPM.getLaunchIntentForPackage("com.alk.copilot.eumarket.premiumeupan"), // Copilot
				currentPM.getLaunchIntentForPackage("com.navngo.igo.javaclient"), // iGO
				new Intent("android.intent.action.navigon.START_PUBLIC"), // navigon with public intent
				// currentPM.getLaunchIntentForPackage("com.navigon.navigator"),
				// //navigon without public intent
				currentPM.getLaunchIntentForPackage("com.sygic.aura"), // Sygic Aura
				currentPM.getLaunchIntentForPackage("com.google.android.apps.maps"), // google navigation
				// com.google.android.maps.driveabout.app.NavigationActivity
				currentPM.getLaunchIntentForPackage("hr.mireo.dp"), // ginius driver dont panic
				currentPM.getLaunchIntentForPackage("com.ndrive.android"), // ndrive
				currentPM.getLaunchIntentForPackage("org.microemu.android.se.appello.lp.Lightpilot") // wisepilot
			};
		}

		return explicitIntents;
	}

	ArrayList<String> items = new ArrayList<String>();
	ArrayList<Intent> itemIntents = new ArrayList<Intent>();

	public Dialog createDialog(Activity activity, final Bundle args) {
		mapActivity = (MapActivity) activity;
		AlertDialog.Builder builder = new Builder(mapActivity);
		builder.setTitle(R.string.navigator_choose_title);
		PackageManager currentPM = mapActivity.getPackageManager();
		items.clear();
		itemIntents.clear();
		for (int i = 0; i < getExplicitIntents().length; i++) {
			Intent navigationAppIntent = explicitIntents[i];
			/*
			 * if (navigationAppIntent != null) { items.add(navigators[i]);
			 * itemIntents.add(navigationAppIntent); }
			 */
			try {
				for (ResolveInfo explicitActivityInfo : currentPM
						.queryIntentActivities(navigationAppIntent, defaultFlag)) {
					items.add(navigators[i]);
					itemIntents.add(navigationAppIntent);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		builder.setItems(items.toArray(new String[] {}),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final double latitude = args.getDouble(MapActivityActions.KEY_LATITUDE);
						final double longitude = args.getDouble(MapActivityActions.KEY_LONGITUDE);
						if (items.get(which).contains("Google")) {
 							// NOTE: google navigation can't be launched without destination
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + latitude + "," + longitude));
							mapActivity.startActivity(intent);
						} else if (items.get(which).contains("Sygic")) {
							//Intent intent = new Intent(
							//		Intent.ACTION_VIEW,
							//		Uri.parse("http://com.sygic.aura/type=drive?lat=" + latitude + "?lon=" + longitude));
							ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
							Intent intent = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("com.sygic.aura://coordinate|"
											+ longitude + "|" + latitude + "|"
											+ (mode != ApplicationMode.PEDESTRIAN?"drive":"walk")));
							mapActivity.startActivity(intent);
						} else {
							Intent intent = itemIntents.get(which);
							intent.putExtra("latitude", (float) latitude);
							intent.putExtra("longitude", (float) longitude);
							mapActivity.startActivity(intent);
						}
					}
			}
		);
    	return builder.create();
    }

}
