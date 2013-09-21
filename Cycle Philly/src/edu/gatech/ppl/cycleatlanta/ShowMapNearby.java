package edu.gatech.ppl.cycleatlanta;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import edu.gatech.ppl.cycleatlanta.R;

public class ShowMapNearby extends FragmentActivity implements LocationListener {
	private GoogleMap mMap;
	private LinearLayout layout;
	private LocationManager lm = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nearbymapview);
		
		// Start listening for GPS updates!
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		// check if already instantiated
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			mMap.setMyLocationEnabled(true);
			layout = (LinearLayout)findViewById(R.id.LinearLayout01);
			ViewTreeObserver vto = layout.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
			    public void onGlobalLayout() {
			        layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			        
			        Criteria criteria = new Criteria();
			        criteria.setAccuracy(Criteria.ACCURACY_FINE);
			        Location loc = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
			        
			        if (loc != null) {
			        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				        		new LatLng(loc.getLatitude(), loc.getLongitude()), 17));
			        } else {
			        	// Center & zoom the map after map layout completes to City Hall
				        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				        		new LatLng(39.952451,-75.163664), 10));
			        }
			        
			    }
			});
		} else {
			mMap.clear();
		}

		// check if got map
		if (mMap == null) {
			// TODO: anything?
			Log.d("Couldn't get map fragment!", "No map fragment");
			return;
		}
		
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("location update", "new location " + location.getLatitude() + location.getLongitude());
		
		mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), 
		        		location.getLongitude())));
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
