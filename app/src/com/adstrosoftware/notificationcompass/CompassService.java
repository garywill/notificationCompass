/* Copyright 2013 Adam Stroud                                                                     */
/*                                                                                                */
/*   Licensed under the Apache License, Version 2.0 (the "License");                              */
/*   you may not use this file except in compliance with the License.                             */
/*   You may obtain a copy of the License at                                                      */
/*                                                                                                */
/*        http://www.apache.org/licenses/LICENSE-2.0                                              */
/*                                                                                                */
/*    Unless required by applicable law or agreed to in writing, software                         */
/*    distributed under the License is distributed on an "AS IS" BASIS,                           */
/*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                    */
/*    See the License for the specific language governing permissions and                         */
/*    limitations under the License.                                                              */

package com.adstrosoftware.notificationcompass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class CompassService extends Service implements SensorEventListener
{
	private static final int NOTIFICATION_ID = 1337;
	private static final String TAG = "CompassService";
	private static final int NORTH = 0;
	private static final int NORTH_EAST = 45;
	private static final int NORTH_WEST = -45;
	private static final int EAST = 90;
	private static final int WEST = -90;
	private static final int SOUTH_EAST = 135;
	private static final int SOUTH_WEST = -135;
	private static final float ANGLE = 22.5f;
	private static final long DELAY_IN_NS = 500000000; // .5 seconds
	private static final String ACTION_STOP_SERVICE = "com.adstrosoftware.notificationcompass.action.STOP_SERVICE";
	
	private final IBinder binder = new LocalBinder();
	
	private SensorManager sensorManager;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder notificationBuilder;
	private boolean actionAdded = false;
	private long previousEventTimeStamp = Long.MIN_VALUE;
	private StopServiceReceiver stopServiceReceiver;
	
	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate()
	{
		super.onCreate();
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		sensorManager.registerListener(this,
				                      sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				                      (int)(DELAY_IN_NS / 1000));
		
		notificationBuilder = new NotificationCompat.Builder(this);
		startForeground(NOTIFICATION_ID, buildNotification(R.string.north, R.drawable.ic_stat_north, 0));
		
		stopServiceReceiver = new StopServiceReceiver();
		registerReceiver(stopServiceReceiver, new IntentFilter(ACTION_STOP_SERVICE));
		
		return START_STICKY;
	}

	/* (non-Javadoc) 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// no-op
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
		{
			if (previousEventTimeStamp == Long.MIN_VALUE
					|| (event.timestamp - previousEventTimeStamp) > DELAY_IN_NS)
			{
				previousEventTimeStamp = event.timestamp;
				
    			float[] orientation = new float[3];
    			float[] rotationMatrix = new float[16];
    			float[] remappedRotationMatrix = new float[16];
    			
    			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
    			
    			if (event.values[0] <= -45)
    			{
    				SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);
    			}
    			else
    			{
    				remappedRotationMatrix = rotationMatrix;
    			}
    			
    			SensorManager.getOrientation(remappedRotationMatrix, orientation);
    			
    			Notification notification;
    			
    			double azimuth = Math.toDegrees(orientation[0]);
    			
    			if (BuildConfig.DEBUG)
    			{
    				Log.d(TAG, "Azimuth = " + azimuth);
    			}
    			
    			if (azimuth <= (NORTH + ANGLE) && azimuth >= (NORTH - ANGLE))
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "NORTH");
    				notification = buildNotification(R.string.north, R.drawable.ic_stat_north, azimuth);
    			}
    			else if (azimuth <= (NORTH_EAST + ANGLE) && azimuth > 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "NORTH_EAST");
    				notification = buildNotification(R.string.north_east, R.drawable.ic_stat_north_east, azimuth);
    			}
    			else if (azimuth >= (NORTH_WEST - ANGLE) && azimuth < 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "NORTH_WEST");
    				notification = buildNotification(R.string.north_west, R.drawable.ic_stat_north_west, azimuth);
    			}
    			else if (azimuth <= (EAST + ANGLE) && azimuth > 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "EAST");
    				notification = buildNotification(R.string.east, R.drawable.ic_stat_east, azimuth);
    			}
    			else if (azimuth >= (WEST - ANGLE) && azimuth < 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "WEST");
    				notification = buildNotification(R.string.west, R.drawable.ic_stat_west, azimuth);
    			}
    			else if (azimuth <= (SOUTH_EAST + ANGLE) && azimuth > 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "SOUTH_EAST");
    				notification = buildNotification(R.string.south_east, R.drawable.ic_stat_south_east, azimuth);
    			}
    			else if (azimuth >= (SOUTH_WEST - ANGLE) && azimuth < 0)
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "SOUTH_WEST");
    				notification = buildNotification(R.string.south_west, R.drawable.ic_stat_south_west, azimuth);
    			}
    			else
    			{
    				if (BuildConfig.DEBUG) Log.d(TAG, "SOUTH");
    				notification = buildNotification(R.string.south, R.drawable.ic_stat_south, azimuth);
    			}
    			
    			notificationManager.notify(NOTIFICATION_ID, notification);
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		sensorManager.unregisterListener(this);
		stopForeground(true);
		unregisterReceiver(stopServiceReceiver);
	}
	
	/**
	 * TODO
	 * 
	 * @return
	 */
	private Notification buildNotification(int textResId, int iconResId, double azimuth)
	{
		Notification notification;
		
		PendingIntent contentIntent = PendingIntent.getActivity(this,
				                                                0,
				                                                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
				                                                PendingIntent.FLAG_UPDATE_CURRENT);
		
		PendingIntent stopServiceIntent = PendingIntent.getBroadcast(this,
				                                                     0,
				                                                     new Intent(ACTION_STOP_SERVICE),
				                                                     PendingIntent.FLAG_UPDATE_CURRENT);
		
		if (!actionAdded)
		{
			notification =  notificationBuilder 
					.setContentTitle(getString(textResId))
					.setContentText(String.valueOf(azimuth))
					.setSmallIcon(iconResId)
					.setOngoing(true)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(contentIntent)
					.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopServiceIntent)
					.build();
			
			actionAdded = true;
		}
		else
		{
			notification =  notificationBuilder 
					.setContentTitle(getString(textResId))
					.setContentText(String.valueOf(azimuth))
					.setSmallIcon(iconResId)
					.setOngoing(true)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(contentIntent)
					.build();
		}
		
		return notification;
	}
	
	/**
	 * TODO
	 * 
	 * @author Adam Stroud &#60;<a href="mailto:adstro@adstrosoftware.com">adstro@adstrosoftware.com</a>&#62;
	 */
	public class LocalBinder extends Binder
	{
		/**
		 * TODO
		 * 
		 * @return
		 */
		public CompassService getService()
		{
			return CompassService.this;
		}
	}
	
	/**
	 * TODO
	 * 
	 * @author Adam Stroud &#60;<a href="mailto:adstro@adstrosoftware.com">adstro@adstrosoftware.com</a>&#62;
	 */
	private class StopServiceReceiver extends BroadcastReceiver
	{
		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent)
		{
			CompassService.this.stopSelf();
		}
	}
}
