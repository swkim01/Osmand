package net.osmand.plus.opengl;

import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

public class MapGLSurfaceView extends GLSurfaceView {
	private static final char ACTION_MASK = 255;
	private static final float TWO_PI = (float)Math.PI * 2;
	private static final float PI = (float)Math.PI;
	protected final static int LOWEST_ZOOM_TO_ROTATE = 10;
	
	private SimpleGLRenderer mSimpleRenderer;
	private final OsmandApplication application;
	
	/**MapTree
	 * zoom level - could be float to show zoomed tiles
	 */
	private float zoom = 14;
	private double longitude = 0d;
	private double latitude = 0d;
	private float scale;
	private float rotateSin = 0;
	private float rotateCos = 1;
	
	private MapUtils mapUtils;
	private MapUtils defaultMapUtils = TileSourceManager.mapUtilsList[0];
	private LandTileMap mTileMap;
	private LandTileMap mOverlayMap;
	private OverlayView mOverlayView;
	
	private float mLastScreenX;
	private float mLastScreenY;
	private int mPressed;
	private float mDistance;
	private float mAngle;
	private float mCameraDirection;
	private PointF mPoint = new PointF();
	
	private Handler handler = new Handler();
	
	private Vector3 mCameraPosition = new Vector3(100.0f, 0.0f, 0.0f);
	private Vector3 mTargetPosition = new Vector3(300.0f, 0.0f, 0.0f);
	//private Vector3 mCameraPosition = new Vector3(100.0f, 128.0f, 400.0f);
	//private Vector3 mTargetPosition = new Vector3(350.0f, 128.0f, 650.0f);
	private Vector3 mWorkVector = new Vector3();
	private float mCameraXZAngle;
	private float mCameraZYAngle;
	private float mCameraLookAtDistance = (float)Math.sqrt(mTargetPosition.distance2(mCameraPosition));
	private boolean mCameraDirty = true;
	private static float CAMERA_ORBIT_SPEED = 0.3f;
	private static float CAMERA_MOVE_SPEED = 8.0f;
	private static float mViewerHeight = 400.0f;
	
	private DisplayMetrics dm;
	
	/**
	 * @param context
	 */
	public MapGLSurfaceView(Context context) {
		super(context);
		
		application = (OsmandApplication) context.getApplicationContext();
		
		mCameraXZAngle = PI / 2;
		mCameraZYAngle = 3.9f;
		WindowManager mgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
	}
	
	public void setZoom(float zoom) {
		if (mTileMap != null && zoom <= mTileMap.getMaximumShownMapZoom() && zoom >= mTileMap.getMinimumShownMapZoom()) {
			this.zoom = zoom;
			updateCamera();
			requestRender();
		}
	}
	
	public void setLatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		setCamera();
		updateCamera();
		requestRender();
	}
	
	public void setHeightScale(float scale) {
		this.scale = scale;
	}
	
	public void setOverlay(OverlayView overlayView) {
		mOverlayView = overlayView;
	}

	public MapUtils getMapUtils() {
		return mapUtils;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	
	public int getZoom() {
		return (int) zoom;
	}
	
	public float getHeightScale() {
		return scale;
	}
	
	public LandTileMap getMainLayer() {
		return mTileMap;
	}
	
	public LandTileMap getOverlayLayer() {
		return mOverlayMap;
	}
	
	public void set3DRenderer(Renderer renderer) {
		this.mSimpleRenderer = (SimpleGLRenderer) renderer;
	}
	
	public void setMainLayer(LandTileMap mainLayer) {
		this.mTileMap = mainLayer;
		if(mainLayer instanceof LandTileMap && mainLayer.getMap() != null)
			this.mapUtils = mainLayer.getMap().getMapUtils();
		else
			this.mapUtils = TileSourceManager.mapUtilsList[0];
		if (mainLayer.getMaximumShownMapZoom() < this.zoom) {
			zoom = mainLayer.getMaximumShownMapZoom();
		}
		if (mainLayer.getMinimumShownMapZoom() > this.zoom) {
			zoom = mainLayer.getMinimumShownMapZoom();
		}
		updateCamera();
		requestRender();
	}
	
	public void setOverlayLayer(LandTileMap overlayLayer) {
		this.mOverlayMap = overlayLayer;
	}
	
	public OsmandApplication getApplication() {
		return application;
	}
	
	/**
	 * Returns real tile size in pixels for float zoom .  
	 */
	public float getTileSize() {
		float res = getSourceTileSize();
		if (zoom != (int) zoom) {
			res *= (float) Math.pow(2, zoom - (int) zoom);
		}

		// that trigger allows to scale tiles for certain devices
		// for example for device with density > 1 draw tiles the same size as with density = 1
		// It makes text bigger but blurry, the settings could be introduced for that
		/*
		if (dm != null && dm.density > 1f && !getSettings().USE_HIGH_RES_MAPS.get() ) {
			res *= dm.density;
		}
		*/
		return res;
	}

	public int getSourceTileSize() {
		if(mTileMap instanceof LandTileMap){
			return mTileMap.getSourceTileSize();
		}
		return 256;
	}
	
	protected OsmandSettings settings = null;
	
	public OsmandSettings getSettings(){
		if(settings == null){
			settings = getApplication().getSettings();
		}
		return settings;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (true/*mTask != null*/) {
			switch(event.getAction() & ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mPressed = 1;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				mDistance = getDistance(event);
				mAngle = getAngle(event);
				if (mDistance > 10.f) {
					average(mPoint, event);
					mAngle = getAngle(event);
					mPressed = 2;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mPressed == 1) {
					float xDelta = event.getX() - mLastScreenX;
					float yDelta = event.getY() - mLastScreenY;
					float cosD = (float)Math.cos(mCameraDirection);
					float sinD = (float)Math.sin(mCameraDirection);
					float xMove = xDelta * cosD - yDelta * sinD;
					float yMove = xDelta * sinD + yDelta * cosD;
					move(-xMove, -yMove);
					if (mCameraDirty) {
						// snap the camera to the floor
						//float height = mTileMap.getHeight(mCameraPosition.x, mCameraPosition.z);
						//mCameraPosition.y = height + mViewerHeight;
						//mCameraPosition.y = mViewerHeight;
						updateCamera();
						requestRender();
					}
				}
				else if (mPressed == 2) {
					float distance = getDistance(event);
					if (distance > 10.f) {
						float angle = getAngle(event);
						float diffY = mPoint.y;
						average(mPoint, event);
						diffY = diffY - mPoint.y;
						float diffAngle = - (angle - mAngle) * 0.5f;
						//Log.e("getAngle", "angle="+angle+", diffANgle="+diffAngle+",CameraXZAngle="+mCameraXZAngle);
						if (diffAngle >= 3.f) {
							diffAngle -= PI;
						} else if (diffAngle <= -3.f) {
							diffAngle += PI;
						}
						rotateZY(diffY * 0.005f);
						rotateXZ(diffAngle);
						move(distance - mDistance);
						mCameraDirection += diffAngle;
						if (mCameraDirection > TWO_PI) {
							mCameraDirection -= TWO_PI;
						}
						else if (mCameraDirection < 0) {
							mCameraDirection += TWO_PI;
						}
						mDistance = distance;
						mAngle = angle;
						if (mCameraDirty) {
							// snap the camera to the floor
							//float height = mTileMap.getHeight(mCameraPosition.x, mCameraPosition.z);
							//mCameraPosition.y = height + mViewerHeight;
							//mCameraPosition.y = mViewerHeight;
							updateCamera();
							requestRender();
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				mPressed = 0;
				break;
			}
			mLastScreenX = event.getX();
			mLastScreenY = event.getY();
			try {
				Thread.sleep(16);
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
		return true;
	}
	
	private float getDistance(MotionEvent event) {
		float diffX = event.getX(0) - event.getX(1);
		float diffY = event.getY(0) - event.getY(1);
		return (float)Math.sqrt(diffX * diffX + diffY * diffY);
	}
	
	private float getAngle(MotionEvent event) {
		float diffX = event.getX(0) - event.getX(1);
		float diffY = event.getY(0) - event.getY(1);
		return (float)Math.atan2((float)diffY, (float)diffX);
	}
	
	private void average(PointF position, MotionEvent event) {
		float sumX = event.getX(0) + event.getX(1);
		float sumY = event.getY(0) + event.getY(1);
		position.set(sumX / 2, sumY / 2);
	}
	
	public boolean mapIsRefreshing() {
		return handler.hasMessages(1);
	}

	// this method could be called in non UI thread
	public void refreshMap() {
		refreshMap(false);
	}
	
	// this method could be called in non UI thread
	public void refreshMap(final boolean force) {
		if (!handler.hasMessages(1) || force) {
			handler.removeMessages(1);
			Message msg = Message.obtain(handler, new Runnable() {
				@Override
				public void run() {
					handler.removeMessages(1);
					mTileMap.update(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, mCameraXZAngle);
					if (mOverlayMap != null)
						mOverlayMap.update(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, mCameraXZAngle);
					//refreshMapInternal(force);
				}
			});
			msg.what = 1;
			handler.sendMessageDelayed(msg, 20);
		}
	}
	
	
	synchronized private void updateCamera() {
		float cosZY = (float)Math.cos(mCameraZYAngle);
		float sinZY = (float)Math.sin(mCameraZYAngle);
		mWorkVector.set((float)Math.cos(mCameraXZAngle)*cosZY, sinZY, (float)Math.sin(mCameraXZAngle)*cosZY);
		mCameraLookAtDistance = Math.abs(mViewerHeight/sinZY);
		mWorkVector.multiply(mCameraLookAtDistance);
		mWorkVector.add(mCameraPosition);
    	
		mTargetPosition.set(mWorkVector);
		mSimpleRenderer.setCameraPosition(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z);
		mSimpleRenderer.setCameraLookAtPosition(mTargetPosition.x, mTargetPosition.y, mTargetPosition.z);
		//Log.e("updateCamera", "cameraPositon.x="+mCameraPosition.x+", cameraPosition.z="+mCameraPosition.z);
		//Log.e("updateCamera", "cameraPositon.y="+mCameraPosition.y);
		//refreshMap(false);
		/*
		Direction direction = mTileMap.checkDirection(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, mCameraXZAngle);
		if (mTileMap.checkUpdate(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, direction)) {
			UpdateMapThread updateThread = new UpdateMapThread(mTileMap, mOverlayMap, mCameraPosition.x, mCameraPosition.z, direction);
			updateThread.start();
		}
		*/
		
		mTileMap.update(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, mCameraXZAngle);
		if (mOverlayMap != null)
			mOverlayMap.update(mCameraPosition.x, mCameraPosition.y, mCameraPosition.z, mCameraXZAngle);
		
		if (mOverlayView != null)
			mOverlayView.setPositionAngle(mCameraPosition, mCameraXZAngle, mCameraZYAngle);
		mCameraDirty = false;
	}
	
	synchronized public void rotate(float x, float y) {
		if (x != 0.0f) {
			mCameraXZAngle += x * CAMERA_ORBIT_SPEED;
			mCameraDirty = true;
		}
		
		if (y != 0.0f) {
			mCameraZYAngle += y * CAMERA_ORBIT_SPEED;
			mCameraDirty = true;
		}
	}
	
	synchronized public void rotateZY(float amount) {
		if (amount != 0.0f) {
			mCameraZYAngle += amount;
			mCameraDirty = true;
			if (mCameraZYAngle > 4.71f)
				mCameraZYAngle = 4.71f;
			else if (mCameraZYAngle < 3.17f)
				mCameraZYAngle = 3.17f;
		}
	}
	
	synchronized public void rotateXZ(float amount) {
		if (amount != 0.0f) {
			mCameraXZAngle += amount;
			if (mCameraXZAngle > TWO_PI)
				mCameraXZAngle -= TWO_PI;
			else if (mCameraXZAngle < 0.0f)
				mCameraXZAngle += TWO_PI;
			float cosZYAngle = (float)Math.cos(mCameraZYAngle);
			mWorkVector.set(-(float)Math.cos(mCameraXZAngle)*cosZYAngle, -(float)Math.sin(mCameraZYAngle), -(float)Math.sin(mCameraXZAngle)*cosZYAngle);
			mWorkVector.multiply(mCameraLookAtDistance);
			mWorkVector.add(mTargetPosition);
			mCameraPosition.set(mWorkVector);
			mCameraDirty = true;
		}
	}
	
	synchronized public void move(float amount) {
		mWorkVector.set(mTargetPosition);
		mWorkVector.subtract(mCameraPosition);
		mWorkVector.normalize();
		mWorkVector.multiply(amount/* * CAMERA_MOVE_SPEED*/);
		mCameraPosition.add(mWorkVector);
		mTargetPosition.add(mWorkVector);
		mCameraDirty = true;
	}
	
	synchronized public void move(float x, float z) {
		if (x != 0.0f || z != 0.0f) {
			mWorkVector.zero();
			mWorkVector.add(mViewerHeight*x/500, 0, mViewerHeight*z/500);
			mCameraPosition.add(mWorkVector);
			mTargetPosition.add(mWorkVector);
			mCameraDirty = true;
		}
	}
	
	synchronized private void setCamera() {
		mCameraPosition.x = (float)defaultMapUtils.getTileNumberX(zoom, longitude, latitude)*getTileSize();
		mCameraPosition.z = (float)defaultMapUtils.getTileNumberY(zoom, longitude, latitude)*getTileSize();
		//Log.e("setCamera", "tileSize="+getTileSize()+", longitude="+longitude+", latitude="+latitude+", pos.x="+mCameraPosition.x+", posz="+mCameraPosition.z);
		LandTileMap.setWorldWidth(mTileMap, mCameraPosition.x);
		LandTileMap.setWorldHeight(mTileMap, mCameraPosition.z);
		LandTileMap.setLocalWidth(mTileMap, (float)mapUtils.getTileNumberX(zoom, longitude, latitude)*getTileSize());
		LandTileMap.setLocalHeight(mTileMap, (float)mapUtils.getTileNumberY(zoom, longitude, latitude)*getTileSize());
		mViewerHeight = Math.max(getSourceTileSize(), mTileMap.getHeight((float)longitude, (float)latitude)+64);
		//mViewerHeight = 8.0f * (1 << (20 - (int)zoom));
		mCameraPosition.y = 0.f;
		float cosZY = (float)Math.cos(mCameraZYAngle);
		float sinZY = (float)Math.sin(mCameraZYAngle);
		mWorkVector.set((float)Math.cos(mCameraXZAngle)*cosZY, sinZY, (float)Math.sin(mCameraXZAngle)*cosZY);
		mCameraLookAtDistance = Math.abs(mViewerHeight/sinZY);
		mWorkVector.multiply(mCameraLookAtDistance);
		mTargetPosition.set(mCameraPosition);
		mCameraPosition.subtract(mWorkVector);
	}
	
	public boolean isMapRotateEnabled(){
		return zoom > LOWEST_ZOOM_TO_ROTATE;
	}
	
	public float calcDiffTileY(float dx, float dy) {
		if(isMapRotateEnabled()){
			return (-rotateSin * dx + rotateCos * dy) / getTileSize();
		} else {
			return dy / getTileSize();
		}
	}

	public float calcDiffTileX(float dx, float dy) {
		if(isMapRotateEnabled()){
			return (rotateCos * dx + rotateSin * dy) / getTileSize();
		} else {
			return dx / getTileSize();
		}
	}
}
