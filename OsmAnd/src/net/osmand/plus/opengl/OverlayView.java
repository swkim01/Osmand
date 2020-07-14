package net.osmand.plus.opengl;

import net.osmand.map.TileSourceManager;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

public class OverlayView extends SurfaceView implements Callback {
	
	private MapGLSurfaceView mapGLSurfaceView;
	private HeightSource mHeightSource;

	Paint paintGrayFill;
	Paint paintCenter;
	
	private DisplayMetrics dm;

	private MapUtils defaultMapUtils = TileSourceManager.mapUtilsList[0];
	private Vector3 mCameraPosition = new Vector3(100.0f, 0.0f, 0.0f);
	private float mCameraXZAngle;
	private float mCameraZYAngle;
	
	private int mX;
	private int mY;
	
	public OverlayView(Context context, MapGLSurfaceView glSurfaceView, HeightSource heightSource) {
		super(context);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		mapGLSurfaceView = glSurfaceView;
		mHeightSource = heightSource;
		
		paintGrayFill = new Paint();
		paintGrayFill.setColor(Color.GRAY);
		paintGrayFill.setStyle(Style.FILL);
		paintGrayFill.setAntiAlias(true);
		
		paintCenter = new Paint();
		paintCenter.setStyle(Style.STROKE);
		paintCenter.setColor(Color.rgb(120, 60, 60));
		paintCenter.setStrokeWidth(3);
		paintCenter.setAntiAlias(true);
		
		WindowManager mgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
    }

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		refreshPoi();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		refreshPoi();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
	
	Rect rect = new Rect();
	
	public void refreshPoi() {
		calculatePosition();
		//rect.set(mX-20, mY-20, mX+20, mY+20);

		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				try {
					canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
					if (mX > 0 && mX < getWidth() && mY > 0 && mY < getHeight()) {
						//canvas.drawRect(rect, paintGrayFill);
						canvas.drawCircle(mX, mY, 3 * dm.density, paintCenter);
						canvas.drawCircle(mX, mY, 7 * dm.density, paintCenter);
					}

				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	
	public void calculatePosition() {
		float zoom = mapGLSurfaceView.getZoom();
		float tileSize = mapGLSurfaceView.getTileSize();
		float longitude = (float)mapGLSurfaceView.getLongitude();
		float latitude = (float)mapGLSurfaceView.getLatitude();
		float posX = (float)defaultMapUtils.getTileNumberX(zoom, longitude, latitude)*tileSize;
		float posZ = (float)defaultMapUtils.getTileNumberY(zoom, longitude, latitude)*tileSize;
		float posY = mHeightSource.getHeight(longitude, latitude) * mapGLSurfaceView.getHeightScale() * tileSize / 2 / (1 << (20 - (int)zoom));
		float cosZY = (float)Math.cos(mCameraZYAngle);
		float sinZY = (float)Math.sin(mCameraZYAngle);
		float cosXZ = (float)Math.cos(mCameraXZAngle-(float)Math.PI/2);
		float sinXZ = (float)Math.sin(mCameraXZAngle-(float)Math.PI/2);
		
		float px = posX - mCameraPosition.x;
		float py = posY - mCameraPosition.y;
		float pz = posZ - mCameraPosition.z;
		
		//Log.e("calculatePosition", "cameraXZAngle="+mCameraXZAngle+", cameraZYAngle="+mCameraZYAngle);
		//Log.e("calculatePosition", "px="+px+", py="+py+", pz="+pz);

		float dx = cosXZ * px + sinXZ * pz;
		float dy = sinZY * sinXZ * px + cosZY * py - sinZY * cosXZ * pz;
		float dz = - cosZY * sinXZ * px + sinZY * py + cosZY * cosXZ * pz;
		//Log.e("calculatePosition", "dx="+dx+", dy="+dy+", dz="+dz);
		
		mX = (int)(getWidth() / 2 + dx * getHeight() * 0.8660254037844f / dz); // 0.8660254037844 = sqrt(3) / 2 by fov is 60 degree
		mY = (int)(getHeight() / 2 + dy * getHeight() * 0.8660254037844f / dz);
	}
	
	public void setPositionAngle(Vector3 cameraPosition, float cameraXZAngle, float cameraZYAngle) {
		mCameraPosition.set(cameraPosition);
		mCameraXZAngle = cameraXZAngle;
		mCameraZYAngle = cameraZYAngle;
		refreshPoi();
	}
}
