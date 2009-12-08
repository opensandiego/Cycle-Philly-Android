package org.sfcta.cycletracks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class RecordingService extends Service implements LocationListener {
	RecordingActivity recordActivity;
	LocationManager lm = null;
	Location lastLocation;

	double latestUpdate;

	DbAdapter mDb;
	TripData trip;

	public static int STATE_IDLE = 0;
	public static int STATE_RECORDING = 1;
	public static int STATE_PAUSED = 2;  //TODO: may not need this one.
	public static int STATE_FULL = 3;

	int state = STATE_IDLE;
	private final MyServiceBinder myServiceBinder = new MyServiceBinder();

	// ---SERVICE methods - required! -----------------
	@Override
	public IBinder onBind(Intent arg0) {
		return myServiceBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public class MyServiceBinder extends Binder implements IRecordService {
		public int getState() {
			return state;
		}
		public void startRecording(TripData trip) {
			RecordingService.this.startRecording(trip);
		}
		public void cancelRecording() {
			RecordingService.this.cancelRecording();
		}
		public void finishRecording() {
			RecordingService.this.finishRecording();
		}
		public void reset() {
			RecordingService.this.state = STATE_IDLE;
		}
	}

	// ---end SERVICE methods -------------------------

	// Start "business logic":

	public void startRecording(TripData trip) {
		this.state = STATE_RECORDING;
		this.trip = trip;

		setNotification();

		// Start listening for GPS updates!
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public void finishRecording() {
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);

		clearNotifications();
		this.state = STATE_FULL;
	}

	public void cancelRecording() {
		if (trip != null) {
			trip.dropTrip();
		}

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);

		clearNotifications();
		this.state = STATE_IDLE;
	}

	public void registerUpdates(RecordingActivity r) {
		this.recordActivity = r;
	}

	public TripData getCurrentTrip() {
		return trip;
	}

	// LocationListener implementation:
	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			// Only save one beep per second.
			double currentTime = System.currentTimeMillis();
			if (currentTime - latestUpdate > 999) {
				trip.addPointNow(loc, currentTime);
				latestUpdate = currentTime;
				updateTripStats(loc);

				// Update the status page every time, if we can.
				if (recordActivity != null) {
					recordActivity.updateStatus();
				}
			}
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}
	// END LocationListener implementation:

	private void updateTripStats(Location newLocation) {
	    final float spdConvert = 2.2369f;
	    if (lastLocation != null) {
	        Float segmentDistance = lastLocation.distanceTo(newLocation);
	        trip.distanceTraveled = trip.distanceTraveled.floatValue() + segmentDistance.floatValue();
	        trip.curSpeed = newLocation.getSpeed() * spdConvert;
	        trip.maxSpeed = Math.max(trip.maxSpeed, trip.curSpeed);
            trip.numpoints++;
	    }
	    lastLocation = newLocation;
	}

	private void setNotification() {
		// Create the notification icon - maybe this goes somewhere else?
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon25;
		CharSequence tickerText = "Recording...";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		CharSequence contentTitle = "CycleTracks - Recording";
		CharSequence contentText = "Tap to finish your trip";
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		final int RECORDING_ID = 1;
		mNotificationManager.notify(RECORDING_ID, notification);
	}

	private void clearNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
	}
}