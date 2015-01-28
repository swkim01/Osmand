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

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLUtils;
import android.util.Log;

// This class manages a regular grid of LandTile objects.  In this sample,
// all the objects are the same mesh.  In a real game, they would probably be
// different to create a more interesting landscape.
// This class also abstracts the concept of tiles away from the rest of the
// code, so that the collision system (amongst others) can query the height of any
// given point in the world.
public class LandTileMap {
	public enum Direction { UP, DOWN, LEFT, RIGHT};
	public final static int FILTER_NEAREST_NEIGHBOR = 0;
	public final static int FILTER_BILINEAR = 1;
	public final static int FILTER_TRILINEAR = 2;
	
	private Context mContext;
	private static float mNorthEast = 0.785398f/*45dec, 0x3f490fdb*/;
	private static float mNorthWest = 2.356194f/*135dec, 0x4016cbe4*/;
	private static float mSouthWest = 3.926991f/*225dec, 0x407b53d1*/;
	private static float mSouthEast = 5.497787f/*315dec, 0x40afeddf*/;

	private LandTile[] mTiles;
	private Direction mPrevDirection = Direction.DOWN;
	private ITileSource map = null;

	private int mTilesAcross;
	private int mTilesDown;
	private boolean mUseTexture;
	private boolean mUseBlend;
	
	private static BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
	private int[] mTextureNameWorkspace;
	private int mTextureFilter = FILTER_NEAREST_NEIGHBOR;
	private int mMaxTextureSize;
	private float mHscale;
	private float mScale = 0.2f;
	private Object mLock = new Object();
	private float mTileSize;
	private float mHalfTileSize;
	private float mCenterPointX;
	private float mCenterPointZ;
	private float mDownCenterX, mDownCenterZ, mUpCenterX, mUpCenterZ, mRightCenterX, mRightCenterZ, mLeftCenterX, mLeftCenterZ;
	private int mSubdivisions;
	private Vector3 mCameraPosition = new Vector3();
	private float mWorldWidth;
	private float mWorldHeight;
	private float mLocalWidth;
	private float mLocalHeight;
	private boolean mUpdateFlag = true;
	private boolean mDrawFlag;
	private boolean mClearFlag;
	private int mResourceId = R.drawable.land;
	private int[] mTextureNames = new int[]{-1};
	private HeightSource mHeightSource;
	private MapGLSurfaceView view;
	protected ResourceManager resourceManager;
	private OsmandSettings settings;
	
	private MapUtils mapUtils;
	private TileHandler mHandler;
	
	public LandTileMap(Context context, MapGLSurfaceView view, HeightSource heightSource,
			boolean useTexture, boolean useBlend, int subdivisions, int textureFilter, float scale) {
		
		super();
				
		mContext = context;
		mHandler = new TileHandler(this);
		mHeightSource = heightSource;
		this.view = view;
		mapUtils = view.getMapUtils();
		settings = view.getSettings();
		resourceManager = view.getApplication().getResourceManager();
		
		mTileSize = 256;//tileSize;
		mHalfTileSize = mTileSize / 2.0f;
		mMaxTextureSize = (int)mTileSize;
		mTextureFilter = textureFilter;
		mHscale = scale;
		mScale = scale * mHalfTileSize;
		
		//heightSource.a(mHandler);
		
		mTextureNameWorkspace = new int[1];
		
		// Set our bitmaps to 16-bit, 565 format.
		sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		
		mUseTexture = useTexture;
		mUseBlend = useBlend;
		mSubdivisions = subdivisions;
		int tilesAcross = Math.min((int)(512.f/mTileSize) * 4, 8);
		//int tilesAcross = Math.min(2 * 4, 8);
		int tilesDown = tilesAcross;
		mTilesAcross = tilesAcross;
		mTilesDown = tilesDown;
		mTiles = new LandTile[tilesAcross*tilesDown];
		mDownCenterX = (float)(mTilesAcross / 2) * mTileSize + mHalfTileSize;
		mDownCenterZ = (float)(mTilesDown - 2) * mTileSize + mHalfTileSize;
		mUpCenterX = mDownCenterX;
		mUpCenterZ = mHalfTileSize;
		mLeftCenterX = mHalfTileSize;
		mLeftCenterZ = (float)(mTilesDown / 2) * mTileSize + mHalfTileSize;
		mRightCenterX = (float)(mTilesAcross - 2) * mTileSize + mHalfTileSize;
		mRightCenterZ = (float)(mTilesDown / 2) * mTileSize + mHalfTileSize;
		mCenterPointX = mDownCenterX;
		mCenterPointZ = mDownCenterZ;
	}
	
	/*
	public void setupSkybox(Bitmap heightmap) {
		if (mSkybox == null) {
			mSkybox = new LandTile(mWorldWidth, 1024, mWorldHeight, 1, 16, 1000000.0f);
			mMeshLibrary.addMesh(mSkybox.generateLods(heightmap, null));
			mSkybox.setPosition(0.0f, 0.0f, 0.0f);
		}
	}
	*/
	
	public int getMaximumShownMapZoom(){
		return 21;
	}
	
	public int getMinimumShownMapZoom(){
		return 1;
	}
	
	public void setMap(ITileSource map) {
		this.map = map;
		if (map != null)
			this.mapUtils = map.getMapUtils();
		else
			this.mapUtils = TileSourceManager.mapUtilsList[0];
		
		//this.mTileSize = getSourceTileSize();
		this.mTileSize = getBaseTileSize();
		this.mHalfTileSize = mTileSize / 2.0f;
		this.mMaxTextureSize = (int)mTileSize;
		this.mScale = mHscale * mHalfTileSize;
		int tilesAcross = Math.min((int)(512.f/mTileSize) * 4, 8);
		//int tilesAcross = Math.min(2 * 4, 8);
		int tilesDown = tilesAcross;
		mTilesAcross = tilesAcross;
		mTilesDown = tilesDown;
		mTiles = new LandTile[tilesAcross*tilesDown];
		this.mDownCenterX = (float)(mTilesAcross / 2) * mTileSize + mHalfTileSize;
		this.mDownCenterZ = (float)(mTilesDown - 2) * mTileSize + mHalfTileSize;
		this.mUpCenterX = mDownCenterX;
		this.mUpCenterZ = mHalfTileSize;
		this.mLeftCenterX = mHalfTileSize;
		this.mLeftCenterZ = (float)(mTilesDown / 2) * mTileSize + mHalfTileSize;
		this.mRightCenterX = (float)(mTilesAcross - 2) * mTileSize + mHalfTileSize;
		this.mRightCenterZ = (float)(mTilesDown / 2) * mTileSize + mHalfTileSize;
		this.mCenterPointX = mDownCenterX;
		this.mCenterPointZ = mDownCenterZ;
	}
	
	public int getSourceTileSize() {
		return map == null ? 256 : map.getTileSize();
	}
	
	public int getBaseTileSize() {
		return view.getSourceTileSize();
	}
	
	public ITileSource getMap() {
		return this.map;
	}

	public float getHeight(float longitude, float latitude) {
		return (float)mHeightSource.getHeight(longitude, latitude) * mScale / (1 << (20 - view.getZoom()));
	}
	
	public void draw(GL10 gl, Vector3 cameraPosition) {
		if (mDrawFlag == false) return;

		Grid.beginDrawing(gl, mUseTexture, mUseBlend);
		synchronized (mLock) {
			try {
			   	final int count = mTiles.length;
				for (int i=0; i < count; i++) {
					LandTile tile = mTiles[i];
					if (tile.mClearFlag == false) {
						Bitmap bitmap = tile.getBitmap();
						if (bitmap != null) {
							int[] LODTextures = loadBitmap(gl, bitmap);
							tile.setLODTextures(LODTextures, true);
						} else {
							tile.setLODTextures(mTextureNames, false);
						}
					}
					tile.draw(gl, cameraPosition);
				}
			} catch (Exception e) {}
		}
		Grid.endDrawing(gl, mUseBlend);
	}	
	
	public final Grid makeGrid(HeightSource heightSource, int subdivisions, float baseX, float baseZ,
			float width, float height, float scale) {
		
		final float subdivisionRange = subdivisions - 1;
		final float vertexSizeX = width / subdivisionRange;
		final float vertexSizeZ = height / subdivisionRange;
		if (heightSource == null) return null;
		Grid grid = new Grid(subdivisions, subdivisions);
		final float heightMapScaleX = width / subdivisionRange;
		final float heightMapScaleZ = height / subdivisionRange;
		final float[] vertexColor = { 1.0f, 1.0f, 1.0f, 1.0f};
		final float[] longitude = new float[subdivisions * subdivisions];
		final float[] latitude = new float[subdivisions * subdivisions];
		int index=0;
		int nzoom = view.getZoom();
		float nscale = scale / (1 << (20 - nzoom));
		float positionX, positionZ;
		for (int i=0 ; i < subdivisions; i++) {
			for (int j=0 ; j < subdivisions; j++) {
				index = i * subdivisions + j;
				positionX = ((float)i * heightMapScaleX + baseX) / mTileSize;
				if (mapUtils.getVIndexOrder() > 0)
					positionZ = ((float)j * heightMapScaleZ + baseZ) / mTileSize;
				else
					positionZ = ( - (float)j * heightMapScaleZ + baseZ + mTileSize) / mTileSize;
				longitude[index] = (float) mapUtils.getLongitudeFromTile(nzoom, positionX, positionZ);
				latitude[index] = (float) mapUtils.getLatitudeFromTile(nzoom, positionX, positionZ);
			}
		}
		
		int[] heights = heightSource.getHeight(longitude, latitude);
		for (int i=0 ; i < subdivisions; i++) {
			final float u = (float)i / subdivisionRange;
			for (int j=0 ; j < subdivisions; j++) {
				final float v = (float)j / subdivisionRange;
				
				float vertexSizeHeight = (heights != null) ? (float)heights[i * subdivisions + j]*nscale : 0;
				grid.set(i, j, i * vertexSizeX, vertexSizeHeight, j * vertexSizeZ, u, v, vertexColor);
			}
		}
		
		return grid;
	}
	
	static Object getLock(LandTileMap tileMap) {
		return tileMap.mLock;
	}
	static void setDrawFlag(LandTileMap tileMap, boolean flag) {
		tileMap.mDrawFlag = flag;
	}
	
	static void setUpdateFlag(LandTileMap tileMap, boolean flag) {
		tileMap.mUpdateFlag = flag;
	}
	
	static boolean getClearFlag(LandTileMap tileMap) {
		return tileMap.mClearFlag;
	}
	
	static LandTile[] getTiles(LandTileMap tileMap) {
		return tileMap.mTiles;
	}
	
	static float getWorldWidth(LandTileMap tileMap) {
		return tileMap.mWorldWidth;
	}
	
	static float getWorldHeight(LandTileMap tileMap) {
		return tileMap.mWorldHeight;
	}
	
	static void setWorldWidth(LandTileMap tileMap, float width) {
		tileMap.mWorldWidth = width;
	}
	
	static void setWorldHeight(LandTileMap tileMap, float height) {
		tileMap.mWorldHeight = height;
	}
	
	static float getLocaldWidth(LandTileMap tileMap) {
		return tileMap.mLocalWidth;
	}
	
	static float getLocalHeight(LandTileMap tileMap) {
		return tileMap.mLocalHeight;
	}
	
	static void setLocalWidth(LandTileMap tileMap, float width) {
		tileMap.mLocalWidth = width;
	}
	
	static void setLocalHeight(LandTileMap tileMap, float height) {
		tileMap.mLocalHeight = height;
	}
	
	static float getTileSize(LandTileMap tileMap) {
		return tileMap.mTileSize;
	}
	
	static float getHalfTileSize(LandTileMap tileMap) {
		return tileMap.mHalfTileSize;
	}
	
	public Bitmap[] getTileBitmap(int[][] tilePoints) {
		if(map == null){
			return null;
		}
		ResourceManager mgr = resourceManager;
		int nzoom = view.getZoom();
		Bitmap[] bmp = new Bitmap[tilePoints[0].length];

		boolean useInternet = settings.USE_INTERNET_TO_DOWNLOAD_TILES.get()
					&& settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();
		int maxLevel = Math.min(settings.MAX_LEVEL_TO_DOWNLOAD_TILE.get(), map.getMaximumZoomSupported());
		
		for (int i=0; i < tilePoints[0].length; i++) {
			String ordImgTile = mgr.calculateTileId(map, tilePoints[0][i], tilePoints[1][i], nzoom);
			// asking tile image async
			boolean imgExist = mgr.tileExistOnFileSystem(ordImgTile, map, tilePoints[0][i], tilePoints[1][i], nzoom);
			boolean originalBeLoaded = useInternet && nzoom <= maxLevel;
			if (imgExist || originalBeLoaded) {
				bmp[i] = mgr.getTileImageForMapAsync(ordImgTile, map, tilePoints[0][i], tilePoints[1][i], nzoom, useInternet);
			}
		}
		return bmp;
	}
	
	public void calculateTileRectangle(Rect pixRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect) {
		float x1 = view.calcDiffTileX(pixRect.left - cx, pixRect.top - cy);
		float x2 = view.calcDiffTileX(pixRect.left - cx, pixRect.bottom - cy);
		float x3 = view.calcDiffTileX(pixRect.right - cx, pixRect.top - cy);
		float x4 = view.calcDiffTileX(pixRect.right - cx, pixRect.bottom - cy);
		float y1 = view.calcDiffTileY(pixRect.left - cx, pixRect.top - cy);
		float y2 = view.calcDiffTileY(pixRect.left - cx, pixRect.bottom - cy);
		float y3 = view.calcDiffTileY(pixRect.right - cx, pixRect.top - cy);
		float y4 = view.calcDiffTileY(pixRect.right - cx, pixRect.bottom - cy);
		float l = Math.min(Math.min(x1, x2), Math.min(x3, x4)) + ctilex;
		float r = Math.max(Math.max(x1, x2), Math.max(x3, x4)) + ctilex;
		float t, b;
		if (mapUtils.getVIndexOrder() > 0) {
			t = Math.min(Math.min(y1, y2), Math.min(y3, y4)) + ctiley;
			b = Math.max(Math.max(y1, y2), Math.max(y3, y4)) + ctiley;
		}
		else {
			b = - Math.min(Math.min(y1, y2), Math.min(y3, y4)) + ctiley;
			t = - Math.max(Math.max(y1, y2), Math.max(y3, y4)) + ctiley;
		}
		//float t = Math.min(Math.min(y1, y2), Math.min(y3, y4)) + ctiley;
		//float b = Math.max(Math.max(y1, y2), Math.max(y3, y4)) + ctiley;
		tileRect.set(l, t, r, b);
	}
	
	private void buildTile(float width, float height, Direction direction) {
		
		long time = System.currentTimeMillis();
		
		int totalSize = mTilesAcross * mTilesDown;
		LandTile[] tiles = new LandTile[mTilesAcross * mTilesDown];
		
		int baseZ=0;
		int baseX=0;
		//Log.d("buildTile", "direction="+direction.name()+", prevDirection="+mPrevDirection.name());
		if (direction != mPrevDirection) {
			switch (direction) {
			case UP:
				mCenterPointX = mUpCenterX;
				mCenterPointZ = mUpCenterZ;
				switch (mPrevDirection) {
				case DOWN:
					baseZ = mTilesDown - 2;
					break;
				case LEFT:
					baseX = - mTilesAcross / 2;
					baseZ = mTilesDown / 2;
					break;
				case RIGHT:
					baseX = mTilesAcross - 2 - mTilesAcross / 2;
					baseZ = mTilesDown / 2;
					break;
				}
				break;
			case DOWN:
				mCenterPointX = mDownCenterX;
				mCenterPointZ = mDownCenterZ;
				switch (mPrevDirection) {
				case UP:
					baseZ = -mTilesDown + 2;
					break;
				case LEFT:
					baseX = - mTilesAcross / 2;
					baseZ = -mTilesDown + 2 + mTilesDown / 2;
					break;
				case RIGHT:
					baseX = mTilesAcross - 2 - mTilesAcross / 2;
					baseZ = -mTilesDown + 2 + mTilesDown / 2;
					break;
				}
				break;
			case LEFT:
				mCenterPointX = mLeftCenterX;
				mCenterPointZ = mLeftCenterZ;
				switch (mPrevDirection) {
				case UP:
					baseX = mTilesAcross / 2;
					baseZ = - mTilesDown / 2;
					break;
				case DOWN:
					baseX = mTilesAcross / 2;
					baseZ = mTilesDown - 2 - mTilesDown / 2;
					break;
				case RIGHT:
					baseX = mTilesAcross - 2;
					break;
				}
				break;
			case RIGHT:
				mCenterPointX = mRightCenterX;
				mCenterPointZ = mRightCenterZ;
				switch (mPrevDirection) {
				case UP:
					baseX = -mTilesAcross + 2 + mTilesAcross / 2;
					baseZ = - mTilesDown / 2;
					break;
				case DOWN:
					baseX = -mTilesAcross + 2 + mTilesAcross / 2;
					baseZ = mTilesDown - 2 - mTilesDown / 2;
					break;
				case LEFT:
					baseX = -mTilesAcross + 2;
					break;
				}
				break;
			}
			mPrevDirection = direction;
		}
		
		double longitude = MapUtils.getLongitudeFromTile(view.getZoom(), width/mTileSize);
		double latitude = MapUtils.getLatitudeFromTile(view.getZoom(), height/mTileSize);
		float worldWidth = (int)(width/mTileSize)*mTileSize+mHalfTileSize;
		float worldHeight = (int)(height/mTileSize)*mTileSize+mHalfTileSize;
		float localTileWidth = (float)mapUtils.getTileNumberX(view.getZoom(), longitude, latitude);
		float localTileHeight = (float)mapUtils.getTileNumberY(view.getZoom(), longitude, latitude);
		float localWidth = (int)localTileWidth*mTileSize+mHalfTileSize;
		float localHeight = (int)localTileHeight*mTileSize+mHalfTileSize;
		float gx = width+mHalfTileSize - worldWidth;
		float gz = height+mHalfTileSize - worldHeight;
		float lx = localTileWidth*mTileSize+mHalfTileSize - localWidth;
		float lz = localTileHeight*mTileSize+mHalfTileSize - localHeight;
		
		int offsetX = (int)((mLocalWidth - localWidth) / mTileSize) - baseX;
		int offsetZ;
		if (mapUtils.getVIndexOrder() > 0)
			offsetZ = (int)((mLocalHeight - localHeight) / mTileSize) - baseZ;
		else
			offsetZ = (int)((mLocalHeight - localHeight) / mTileSize) + baseZ;
			
		for (int i=0; i < mTilesAcross; i++) {
			int indexX = offsetX + i;
			
			if (indexX >= 0 && indexX < mTilesAcross) {
				for (int j=0; j < mTilesDown; j++) {
					int indexZ;
					if (mapUtils.getVIndexOrder() > 0)
						indexZ = offsetZ + j;
					else
						indexZ = - offsetZ + j;

					int index = mTilesDown * i + j;
					if (mTiles[index] != null) {
						if (mTiles[index].mClearFlag == true && indexZ >= 0 && indexZ < mTilesDown) {
							tiles[mTilesDown * indexX + indexZ] = mTiles[index];
							totalSize = totalSize - 1;
						}
						else {
							mTiles[index].deleteBitmap();
						}
					}
				}
			}
		}

		int[][] tilePoints = new int[2][totalSize];
		int[] tileIndex = new int[totalSize];
		int indexH = 0;
		int subdivisions = mSubdivisions;
		int divide = view.getZoom() - 15;
		if (divide > 0)
			subdivisions = ((subdivisions - 1) >> divide) + 1;
		for (int i=0; i < mTilesAcross; i++) {
			for (int j=0; j < mTilesDown; j++) {
				if (tiles[mTilesDown * i + j] == null) {
					Grid[] grid = new Grid[1];
					float heightX = (float)i * mTileSize + localWidth - mCenterPointX;
					float heightZ;
					if (mapUtils.getVIndexOrder() > 0) {
						heightZ = (float)j * mTileSize + localHeight - mCenterPointZ;
					} else {
						heightZ = - (float)j * mTileSize + localHeight + mCenterPointZ;
					}
					
					grid[0] = makeGrid(mHeightSource, subdivisions, heightX, heightZ, mTileSize, mTileSize, mScale);
					
					if (mClearFlag == true)
						return;
					
					tilePoints[0][indexH] = (int)(heightX / mTileSize);
					tilePoints[1][indexH] = (int)(heightZ / mTileSize);
					LandTile tile = new LandTile(subdivisions, tilePoints[0][indexH], tilePoints[1][indexH], mTileSize);
					tile.setLods(grid);
					
					tiles[mTilesDown*i + j] = tile;
					tileIndex[indexH] = mTilesDown * i + j;
					indexH = indexH + 1;
				}
				float tileX = (float)i * mTileSize + worldWidth - mCenterPointX + gx - lx;
				float tileZ;
					
				if (mapUtils.getVIndexOrder() > 0) {
					tileZ = (float)j * mTileSize + worldHeight - mCenterPointZ + gz - lz;
				} else {
					tileZ = (float)j * mTileSize + worldHeight - mCenterPointZ + gz + lz - 2 * mTileSize;
				}
				tiles[mTilesDown*i + j].setPosition(tileX, 0, tileZ);
			}
		}

		Bitmap[] bitmaps = getTileBitmap(tilePoints);
	
		for (int i=0; i < indexH; i++) {
			tiles[tileIndex[i]].setBitmap(bitmaps[i]);
		}

		synchronized (mLock) {
			try {
				mTiles = tiles;
				mWorldWidth = worldWidth;
				mWorldHeight = worldHeight;
				mLocalWidth = localWidth;
				mLocalHeight = localHeight;
			} catch(Exception e) {}
		}
		Log.d("LandTileMap:", System.currentTimeMillis()-time+"");
	}
	
	static void buildTile(LandTileMap tileMap, float width, float height, Direction direction) {
		tileMap.buildTile(width, height, direction);
	}
	
	public void clearTileMap() {
		mClearFlag = true;
		synchronized (mLock) {
			try {
				for (int i=0; i < mTiles.length; i++) {
					LandTile tile = mTiles[i];
					if (tile != null)
						tile.deleteBitmap();
				}
			} catch (Exception e) {	}
		}
	}
	
	public void update(float x, float y, float z, float XZAngle) {
		Direction direction;
		
		if (getMap() == null || mUpdateFlag == false || x <= 0 || z <= 0) return;
		mCameraPosition.set(x, y, z);
		//Log.d("update", "XZAngle="+XZAngle*180/3.1415927);
		if (XZAngle >= mNorthEast && XZAngle < mNorthWest) {
			direction = Direction.DOWN;
		}
		else if (XZAngle >= mNorthWest && XZAngle < mSouthWest){
			direction = Direction.LEFT;
		}
		else if (XZAngle >= mSouthWest && XZAngle < mSouthEast) {
			direction = Direction.UP;
		}
		else {
			direction = Direction.RIGHT;
		}
		
		if (mPrevDirection == direction || x > mWorldWidth + mHalfTileSize ||
				x < mWorldWidth - mHalfTileSize ||
				z < mWorldHeight - mHalfTileSize ||
				z > mWorldHeight + mHalfTileSize) {
			update(x, y, z, direction);
		}
	}
	
	public Direction checkDirection(float x, float y, float z, float XZAngle) {
		Direction direction = Direction.DOWN;
		
		if (mUpdateFlag == false || x <= 0 || z <= 0) return direction;
		mCameraPosition.set(x, y, z);
		//Log.d("update", "XZAngle="+XZAngle*180/3.1415927);
		if (XZAngle >= mNorthEast && XZAngle < mNorthWest) {
			direction = Direction.DOWN;
		}
		else if (XZAngle >= mNorthWest && XZAngle < mSouthWest){
			direction = Direction.LEFT;
		}
		else if (XZAngle >= mSouthWest && XZAngle < mSouthEast) {
			direction = Direction.UP;
		}
		else {
			direction = Direction.RIGHT;
		}
		return direction;
	}
	
	public boolean checkUpdate(float x, float y, float z, Direction direction) {
		if (mPrevDirection == direction || x > mWorldWidth + mHalfTileSize ||
				x < mWorldWidth - mHalfTileSize ||
				z < mWorldHeight - mHalfTileSize ||
				z > mWorldHeight + mHalfTileSize) {
			return true;
		}
		return false;
	}
	
	public void update(float x, float y, float z, Direction direction) {
		//setUpdateFlag(this, false);
		//buildTile(this, x, z, direction);
		//setUpdateFlag(this, true);
		UpdateMapThread updateThread = new UpdateMapThread(this, x, z, direction);
		updateThread.start();
	}
	
	public void loadBitmap(Context context, GL10 gl) {
		mTextureNames = loadBitmap(context, gl, mResourceId);
	}

	protected int[] loadBitmap(Context context, GL10 gl, int resourceId) {
		InputStream is = context.getResources().openRawResource(resourceId);
		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(is, null, sBitmapOptions);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
		
		int[] textureNames = loadBitmap(gl, bitmap);
		bitmap.recycle();
		return textureNames;
	}
	
	protected int[] loadBitmap(GL10 gl, Bitmap bitmap) {
		if (mMaxTextureSize > 0 && Math.max(bitmap.getWidth(), bitmap.getHeight()) != mMaxTextureSize) {
			bitmap = Bitmap.createScaledBitmap( bitmap, mMaxTextureSize, mMaxTextureSize, false );
		}
		
		int[] textureNames = new int[1];
		textureNames[0] = loadBitmapIntoOpenGL(gl, bitmap);
		return textureNames;
	}
	
	protected int loadBitmapIntoOpenGL(GL10 gl, Bitmap bitmap) {
		int textureName = -1;
		
		if (gl != null) {
			gl.glGenTextures(1, mTextureNameWorkspace, 0);
			
			textureName = mTextureNameWorkspace[0];			
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName);
			if (mTextureFilter == FILTER_NEAREST_NEIGHBOR) {
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			}
			else {
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			}
			if (mTextureFilter == FILTER_NEAREST_NEIGHBOR) {
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
			}
			else {
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			}
			
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
			
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			
			int error = gl.glGetError();
			if (error != GL10.GL_NO_ERROR) {
				Log.e("SimpleGLRenderer", "Texture Load GLError: " + error);
			}
		}
		return textureName;
	}
}

