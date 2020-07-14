/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.osmand.plus.opengl;

import java.lang.ref.WeakReference;

import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.SQLiteTileSource;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ViewGroup.LayoutParams;

// The main entry point for the actual landscape rendering test.
// This class pulls options from the preferences set in the MainMenu
// activity and builds the landscape accordingly.
public class HeightMapActivity extends Activity {
	// App settings
	private OsmandSettings settings;
	
	private MapGLSurfaceView mGLSurfaceView;
	private SimpleGLRenderer mSimpleRenderer;
	private HeightSource mHeightSource;
	private OverlayView mOverlayView;
	private float mZoom;
	private LandTileMap mTileMap;
	private LandTileMap mOverlayMap = null;
	
	private double mLongitude, mLatitude;
	private float mScale;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean texture = prefs.getBoolean("texture", true);
		final int maxTextureSize = Integer.parseInt(prefs.getString("maxTextureSize", "512"));
		final String textureFilter = prefs.getString("textureFiltering", "nearest");
		final int complexity = Integer.parseInt(prefs.getString("complexity", "17"));
		final float scale = Float.parseFloat(prefs.getString("height_scale", "0.08f"));//"0.0625f"));
		
		/*
		final File dir = new File(ResourceManager.HEIGHTS_PATH);
		if(!dir.exists()){
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				dir.mkdirs();
			}
		}
		*/
		
		settings = getMyApplication().getSettings();		
		
		mGLSurfaceView = new MapGLSurfaceView(this);
		//mGLSurfaceView.setEGLConfigChooser(true);
		setContentView(mGLSurfaceView);
		mGLSurfaceView.requestFocus();
		mGLSurfaceView.setFocusableInTouchMode(true);
		
		mHeightSource = new HeightSource(this);
		
		mTileMap = new LandTileMap(this, mGLSurfaceView, mHeightSource, texture, false,
					complexity, (textureFilter.equals("nearest")?0:1), scale);
		mOverlayMap = new LandTileMap(this, mGLSurfaceView, mHeightSource, texture, true,
					complexity, (textureFilter.equals("nearest")?0:1), scale);
		
		mSimpleRenderer = new SimpleGLRenderer(this, mTileMap, mOverlayMap);
		mGLSurfaceView.setRenderer(mSimpleRenderer);
		mGLSurfaceView.set3DRenderer(mSimpleRenderer);
		//mGLSurfaceView.setRenderMode(mGLSurfaceView.RENDERMODE_WHEN_DIRTY);
		//mGLSurfaceView.setMainLayer(mTileMap);
		
   	 	/*
		if(!settings.isLastKnownMapLocation()){
			// show first time when application ran
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = null;
			for (String provider : service.getAllProviders()) {
				try {
					Location loc = service.getLastKnownLocation(provider);
					if (location == null) {
						location = loc;
					} else if (loc != null && location.getTime() < loc.getTime()) {
						location = loc;
					}
				} catch (IllegalArgumentException e) {
					Log.d(LogUtil.TAG, "Location provider not available"); //$NON-NLS-1$
				}
			}
			if(location != null){
				mGLSurfaceView.setLatLon(location.getLatitude(), location.getLongitude());
				mGLSurfaceView.setZoom(14);
			}
		}
		*/
		
		ITileSource overlaySource = settings.getTileSourceByName(settings.MAP_OVERLAY.get(), settings.MAP_OVERLAY == null);
		//Log.e("overlaySourcea", "overlaySource="+overlaySource.getName());
		mOverlayMap.setMap(overlaySource);
		mGLSurfaceView.setOverlayLayer(mOverlayMap);
		
		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == null);
		ITileSource oldMap = mTileMap.getMap();
		if(oldMap instanceof SQLiteTileSource){
			((SQLiteTileSource)oldMap).closeDB();
		}
		mTileMap.setMap(newSource);
		mGLSurfaceView.setMainLayer(mTileMap);
		
   	 	Intent intent = getIntent();
   	 	mZoom = intent.getIntExtra("zoom", 14);
   	 	mGLSurfaceView.setZoom(mZoom);
   	 	mLatitude = intent.getFloatExtra("latitude", 0.f);
   	 	mLongitude = intent.getFloatExtra("longitude", 0.f);
   	 	mGLSurfaceView.setLatLon(mLatitude, mLongitude);
   	 	mScale = scale;
   	 	mGLSurfaceView.setHeightScale(mScale);
   	 	
		mOverlayView = new OverlayView(this, mGLSurfaceView, mHeightSource);
		mGLSurfaceView.setOverlay(mOverlayView);
		addContentView(mOverlayView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		mOverlayView.getHolder().setFormat(PixelFormat.TRANSLUCENT);  
		mOverlayView.setZOrderOnTop(true); 
		
		WeakReference<HeightSource> weak = new WeakReference(mHeightSource);
		
		TileMapThread thread = new TileMapThread(mTileMap, mOverlayMap, weak);
		thread.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mGLSurfaceView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		ITileSource overlaySource = settings.getTileSourceByName(settings.MAP_OVERLAY.get(), settings.MAP_OVERLAY == null);
		mOverlayMap.setMap(overlaySource);
		mGLSurfaceView.setOverlayLayer(mOverlayMap);
		
		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == null);
		ITileSource oldMap = mTileMap.getMap();
		if(oldMap instanceof SQLiteTileSource){
			((SQLiteTileSource)oldMap).closeDB();
		}
		mTileMap.setMap(newSource);
		mGLSurfaceView.setMainLayer(mTileMap);
		/*
		if (settings != null && settings.isLastKnownMapLocation()) {
			LatLon l = settings.getLastKnownMapLocation();
			mGLSurfaceView.setLatLon(l.getLatitude(), l.getLongitude());
			mGLSurfaceView.setZoom(settings.getLastKnownMapZoom());
		}
		*/
		mGLSurfaceView.onResume();
	}
	
	OsmandApplication getMyApplication() {
		return ((OsmandApplication) getApplication());
	}
	
	/*
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
        if (mTask != null) {
        	mTask.rotate(event.getRawX(), -event.getRawY());
        }
		return true;
	}
	*/

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mTask != null) {
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
					float cosD = FloatMath.cos(mCameraDirection);
					float sinD = FloatMath.sin(mCameraDirection);
					float xMove = xDelta * cosD - yDelta * sinD;
					float yMove = xDelta * sinD + yDelta * cosD;
					mTask.move(-xMove, -yMove);
				}
				else if (mPressed == 2) {
					float distance = getDistance(event);
					if (distance > 10.f) {
						float angle = getAngle(event);
						float diffY = mPoint.y;
						average(mPoint, event);
						diffY = diffY - mPoint.y;
						float diffAngle = - (angle - mAngle) * 0.5f;
						mTask.rotateZY(diffY * 0.005f);
						Log.d("getAngle", "angle="+angle+", diffANgle="+diffAngle);
						mTask.rotateXZ(diffAngle);
						mTask.move(distance - mDistance);
						mCameraDirection += diffAngle;
						if (mCameraDirection > TWO_PI) {
							mCameraDirection -= TWO_PI;
						}
						else if (mCameraDirection < 0) {
							mCameraDirection += TWO_PI;
						}
						mDistance = distance;
						mAngle = angle;
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
		return FloatMath.sqrt(diffX * diffX + diffY * diffY);
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
	*/
	
	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mTask != null) {
			final float speed = 1.0f;
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_DOWN: 
		    	mTask.rotate(0.0f, -speed);
	
				return true; 
			case KeyEvent.KEYCODE_DPAD_LEFT: 
		    	mTask.rotate(-speed, 0.0f);
	
				return true; 
			case KeyEvent.KEYCODE_DPAD_RIGHT: 
		    	mTask.rotate(speed, 0.0f);
	
				return true; 
			case KeyEvent.KEYCODE_DPAD_UP: 
		    	mTask.rotate(0.0f, speed);
	
				return true; 
			} 
        }
		return super.onKeyDown(keyCode, event);
	}
	*/

}
