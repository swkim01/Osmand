package net.osmand.plus.rfobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.Location;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Class represents a layer which depicts the rf objects
 * @author Aquilegia
 * @see RfPositionPlugin
 *
 */
public class RfObjectLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	/**
	 * magic number so far
	 */
	private static int POINT_OUTER_COLOR = 0x88555555;
	private static int PAINT_TEXT_ICON_COLOR = Color.BLACK;
	private static final int radius = 18;
	private static final int startZoom = 10;
	private DisplayMetrics dm;
	private final MapActivity map;
	private OsmandMapTileView view;
	private Paint paintPath;
	private Paint pointInnerCircle;
	private Paint pointOuter;
	private Paint bitmapPaint;
	private Paint paintTextIcon;
	private Path pth;
	private Bitmap rfMeIcon;
	private Bitmap rfOtherIcon;
	private RfObjectPlugin plugin;

	public RfObjectLayer(MapActivity map, RfObjectPlugin plugin) {
		this.map = map;
		this.plugin = plugin;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Style.FILL);
		pointInnerCircle.setAntiAlias(true);
		
		paintPath = new Paint();
		paintPath.setStyle(Style.STROKE);
		paintPath.setStrokeWidth(14);
		paintPath.setAntiAlias(true);
		paintPath.setStrokeCap(Cap.ROUND);
		paintPath.setStrokeJoin(Join.ROUND);

		pth = new Path();
		
		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
		paintTextIcon.setAntiAlias(true);
		
		pointOuter = new Paint();
		pointOuter.setColor(POINT_OUTER_COLOR);
		pointOuter.setAntiAlias(true);
		pointOuter.setStyle(Style.FILL_AND_STROKE);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		rfMeIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_rf_me);
		rfOtherIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_rf_other);
	}

	public Collection<RfObject> getRfObjects() {
		return plugin.getRfObjects();
	}

	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final double zoom = tb.getZoom();
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 11){
			r = 10;
		} else if(zoom <= 14){
			r = 12;
		} else {
			r = 14;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		final int r = getRadiusPoi(tb);
		if (tb.getZoom() >= startZoom) {
			long treshold = System.currentTimeMillis() - 60000;
		        for (RfObject o : getRfObjects()) {
				Location l = o.getLastLocation();
				ConcurrentLinkedQueue<Location> locations = o.getLocations(treshold);
				if (!locations.isEmpty() && l != null) {
					int x = (int) tb.getPixXFromLonNoRot(l.getLongitude(), l.getLatitude());
					int y = (int) tb.getPixYFromLatNoRot(l.getLongitude(), l.getLatitude());
					pth.rewind();
					Iterator<Location> it = locations.iterator();
					boolean f = true;
					while (it.hasNext()) {
						Location lo = it.next();
						int xt = (int) tb.getPixXFromLonNoRot(lo.getLongitude(), lo.getLatitude());
						int yt = (int) tb.getPixYFromLatNoRot(lo.getLongitude(), lo.getLatitude());
						if (f) {
							f = false;
							pth.moveTo(xt, yt);
						} else {
							pth.lineTo(xt, yt);
						}
					}
					pth.lineTo(x, y);
					paintPath.setColor(o.getColor());
					canvas.drawPath(pth, paintPath);
				}
			}

			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		        for (RfObject o : getRfObjects()) {
				int x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				int y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				Bitmap b;
				int c;
				if (o.isMe()) {
					b = rfMeIcon;
					c = 0xffffa500; /* orange */
				} else {
					b = rfOtherIcon;
					c = Color.CYAN;
				}
				//canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, bitmapPaint);
				pointInnerCircle.setColor(c);
				pointOuter.setColor(POINT_OUTER_COLOR);
				paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
				canvas.drawCircle(x, y, r + (float)Math.ceil(tb.getDensity()), pointOuter);
				canvas.drawCircle(x, y, r - (float)Math.ceil(tb.getDensity()), pointInnerCircle);
				paintTextIcon.setTextSize(r * 3 / 2);
				canvas.drawText(o.getName().substring(0, 1).toUpperCase(), x, y + r / 2, paintTextIcon);
			}
		}
	}
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof RfObject) {
			return ((RfObject)o).getName();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_MARKER,
				view.getContext().getString(R.string.osmand_rf_object_name), "");
	}
	
	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List <RfObject> o = new ArrayList<RfObject>();
		getRfObjectsFromPoint(point, tileBox, o);
		if(o.size() > 0){
			StringBuilder res = new StringBuilder();
			for (RfObject r : o) {
				res.append(getObjectDescription(r)).append('\n');
			}
			AccessibleToast.makeText(view.getContext(), res.toString().trim(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
	}
	
	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tb, List<Object> objects) {
	//public void collectObjectsFromPoint(PointF point, RotatedTileBox tb, List<? super RfObject> objects) {
		getRfObjectsFromPoint(point, tb, objects);
	}

	/**
	 * @param point
	 * @param RfObject
	 *            is in this case not necessarily has to be a list, but it's also used in method
	 *            <link>collectObjectsFromPoint(PointF point, List<Object> o)</link>
	 */
	public void getRfObjectsFromPoint(PointF point, RotatedTileBox tb, List<? super RfObject> objects) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(tb);
		int radius = compare * 3 / 2;
		for (RfObject o : getRfObjects()) {
			int x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
			int y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
			// the width of an image is 40 px, the height is 60 px -> radius = 20,
			// the position of a rf point relatively to the icon is at the center of the bottom line of the image
			int rad = (int) (radius * tb.getDensity());
			if (Math.abs(x - ex) <= rad && (ey - y) <= rad && (y - ey) <= 2.5 * rad) {
				compare = radius;
				objects.add(o);
			}
		}
	}
	
	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof RfObject) {
			return new LatLon(((RfObject)o).getLatitude(), ((RfObject)o).getLongitude());
		}
		return null;
	}

}
