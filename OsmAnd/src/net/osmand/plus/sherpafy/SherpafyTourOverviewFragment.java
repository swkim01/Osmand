package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.LinearLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public class SherpafyTourOverviewFragment extends SherlockListFragment {
	private static final int ACTION_DOWNLOAD = 5;
	OsmandApplication app;
	private View view;
	private SherpafyCustomization customization;
	private TourInformation item;

	public SherpafyTourOverviewFragment() {
	}
	
	public void setTour(TourInformation item) {
		this.item = item;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getSherlockActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();
		TourAdapter tourAdapter = new TourAdapter(customization.getTourInformations());
		setListAdapter(tourAdapter);
		setHasOptionsMenu(true);
	}
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(getActivity(), getListAdapter().getItem(position).toString(), Toast.LENGTH_LONG).show();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
//		MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//createMenuItem(menu, ACTION_SETTINGS_ID, R.string.settings, R.drawable.ic_action_settings_light,
//		R.drawable.ic_action_settings_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
//				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
//		com.actionbarsherlock.view.MenuItem menuItem = menu.add(0, ACTION_DOWNLOAD, 0, R.string.sherpafy_download_tours).setShowAsActionFlags(
//				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
////		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
////		boolean light = true; //app.getSettings().isLightActionBar();
//		//menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdirections_light : R.drawable.ic_action_gdirections_dark);
//		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//			@Override
//			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
//				return true;
//			}
//		});
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setBackgroundColor(0x00eeeeee);
	}
	
	
	
	class TourAdapter extends ArrayAdapter<TourInformation> {

		public TourAdapter(List<TourInformation> list) {
			super(getActivity(), R.layout.sherpafy_list_tour_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.sherpafy_list_tour_item, parent, false);
			}
			TourInformation ti = getItem(position);
			TextView description = (TextView) row.findViewById(R.id.TourDescription);
			TextView name = (TextView) row.findViewById(R.id.TourName);
			description.setText(ti.getShortDescription());
			name.setText(ti.getName());
			ImageView iv = (ImageView) row.findViewById(R.id.TourImage);
			if(ti.getImageBitmap() != null) {
				iv.setImageBitmap(ti.getImageBitmap());
			}
			return row;
		}
	}
	

	private ImageGetter getImageGetter(final View v) {
		return new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String s) {
				Bitmap file = customization.getSelectedTour().getImageBitmapFromPath(s);
				v.setTag(file);
				Drawable bmp = new BitmapDrawable(getResources(), file);
				// if image is thicker than screen - it may cause some problems, so we need to scale it
				int imagewidth = bmp.getIntrinsicWidth();
				// TODO
//				if (displaySize.x - 1 > imagewidth) {
//					bmp.setBounds(0, 0, bmp.getIntrinsicWidth(), bmp.getIntrinsicHeight());
//				} else {
//					double scale = (double) (displaySize.x - 1) / imagewidth;
//					bmp.setBounds(0, 0, (int) (scale * bmp.getIntrinsicWidth()),
//							(int) (scale * bmp.getIntrinsicHeight()));
//				}
				return bmp;
			}

		};
	}
	


	private void addOnClickListener(final TextView tv) {
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getTag() instanceof Bitmap) {
					final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(getActivity());
					dlg.setPositiveButton(R.string.default_buttons_ok, null);
					ScrollView sv = new ScrollView(getActivity());
					ImageView img = new ImageView(getActivity());
					img.setImageBitmap((Bitmap) tv.getTag());
					sv.addView(img);
					dlg.setView(sv);
					dlg.show();
				}
			}
		});
	}

	private void prepareBitmap(Bitmap imageBitmap) {
		ImageView img = null;
		if (imageBitmap != null) {
			img.setImageBitmap(imageBitmap);
			img.setAdjustViewBounds(true);
			img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			img.setCropToPadding(true);
			img.setVisibility(View.VISIBLE);
		} else {
			img.setVisibility(View.GONE);
		}
	}

	private void goToMap() {
		if (customization.getSelectedStage() != null) {
			GPXFile gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null && sgpx.size() > 0) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					WptPt p = gpx.findPointToShow();
					getMyApplication().getSettings().setMapLocationToShow(p.lat, p.lon, 16, null);
					getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		Intent newIntent = new Intent(getActivity(), customization.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		this.startActivityForResult(newIntent, 0);
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
}