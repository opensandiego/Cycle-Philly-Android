package edu.gatech.ppl.cycleatlanta;

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

import edu.gatech.ppl.cycleatlanta.R;

public class ShowMapNearby extends FragmentActivity {
	private GoogleMap mMap;
	private LinearLayout layout;
	private LocationManager lm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nearbymapview);
		
		// check if already instantiated
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			layout = (LinearLayout)findViewById(R.id.LinearLayout01);
			ViewTreeObserver vto = layout.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
			    public void onGlobalLayout() {
			        layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			        // Center & zoom the map after map layout completes
			        //mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));
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
		
		mMap.setMyLocationEnabled(true);
		
	}
}
