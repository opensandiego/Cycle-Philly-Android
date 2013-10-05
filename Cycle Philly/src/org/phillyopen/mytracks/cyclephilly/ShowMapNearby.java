package org.phillyopen.mytracks.cyclephilly;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.phillyopen.mytracks.cyclephilly.R;

public class ShowMapNearby extends FragmentActivity {
	
	private final static int MENU_SHOW_HIDE_RACKS = 0;
    private final static int MENU_SHOW_HIDE_ROUTES = 1;
    private final static int MENU_ABOUT = 2;
    
	private GoogleMap mMap;
	private TileOverlay racksOverlay;
	private TileOverlay routesOverlay;
	private MenuItem racksMenu;
	private MenuItem routesMenu;
	private LinearLayout layout;
	private LocationManager lm = null;
	private LatLng mySpot = null;
	TextView t1;
    TextView t2; 
    TextView t3;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nearbymapview);
		
		t1 = (TextView) findViewById(R.id.TextViewT1);
		t2 = (TextView) findViewById(R.id.TextViewT2);
		t3 = (TextView) findViewById(R.id.TextViewT3);
	
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
		Location loc = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
		
        t1.setText("Bicycle Racks and Parking");
        //t3.setText("loading...");
        
        if (loc != null) {
        	mySpot = new LatLng(loc.getLatitude(), loc.getLongitude());
        } else {
        	// try with coarse accuracy
        	criteria.setAccuracy(Criteria.ACCURACY_FINE);
    		loc = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
    		
    		if (loc == null) {
	        	mySpot = new LatLng(39.952451,-75.163664); // city hall by default
	        	t2.setText("Current location not found; enable GPS to search nearby.");
    		}
        }
		
        // test location in S Philly
		//mySpot = new LatLng(39.924877,-75.158871);
		/////////////////
		
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
			        
			        // Center & zoom the map after map layout completes
			        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mySpot, 15));
			    }
			});
		} else {
			mMap.clear();
		}

		// check if got map
		if (mMap == null) {
			Log.e("Couldn't get map fragment!", "No map fragment");
			return;
		}
		
		mMap.setInfoWindowAdapter(new BikeRackInfoWindow(getLayoutInflater()));
		
		// use mapbox map with base layer
		//TileOverlayOptions tileOpts = new TileOverlayOptions();
		//tileOpts.tileProvider(new MapTileProvider("banderkat.map-xdg8ubm7"));
		//mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		//mMap.addTileOverlay(tileOpts);
		
		TileOverlayOptions racksOpts = new TileOverlayOptions();
		racksOpts.tileProvider(new MapTileProvider("banderkat.philly_bikeracks"));
		TileOverlayOptions routesOpts = new TileOverlayOptions();
		routesOpts.tileProvider(new MapTileProvider("banderkat.philly_bikeroutes"));
		
		routesOverlay = mMap.addTileOverlay(routesOpts);
		racksOverlay = mMap.addTileOverlay(racksOpts);
	}
	
	// TODO: menu to toggle routes and racks overlays
	
	/* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        racksMenu = menu.add(0, MENU_SHOW_HIDE_RACKS, 0, "Hide Bike Parking").setIcon(android.R.drawable.ic_menu_view);
        routesMenu = menu.add(0, MENU_SHOW_HIDE_ROUTES, 0, "Hide Bike Routes").setIcon(android.R.drawable.ic_menu_view);
        menu.add(0, MENU_ABOUT, 0, "About this Map").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SHOW_HIDE_RACKS:
            if (racksOverlay.isVisible()) {
            	racksOverlay.setVisible(false);
            	racksMenu.setTitle("Show Bike Parking");
            } else {
            	racksOverlay.setVisible(true);
            	racksMenu.setTitle("Hide Bike Parking");
            }
            return true;
        case MENU_SHOW_HIDE_ROUTES:
        	if (routesOverlay.isVisible()) {
        		routesOverlay.setVisible(false);
        		routesMenu.setTitle("Show Bike Routes");
        	} else {
        		routesOverlay.setVisible(true);
        		routesMenu.setTitle("Hide Bike Routes");
        	}
            return true;
        case MENU_ABOUT:
        	// TODO:
        	
        	//t3.setText("you clicked me");
        	//PopupWindow pop = new PopupWindow(t3, 100, 200);
        	//View myView = this.findViewById(R.layout.nearbymapview);
        	//pop.showAtLocation(myView, Gravity.CENTER, 0, 0);
        	
        	return true;
        }
        return false;
    }
	
	private class BikeRackInfoWindow implements InfoWindowAdapter {
		LayoutInflater inflater=null;
		
		BikeRackInfoWindow(LayoutInflater inflater) {
			this.inflater=inflater;
		}

		@Override
		public View getInfoContents(Marker marker) {
			View popup=inflater.inflate(R.layout.popup, null);

		    TextView tv=(TextView)popup.findViewById(R.id.title);

		    tv.setText(marker.getTitle());
		    tv=(TextView)popup.findViewById(R.id.snippet);
		    tv.setText(marker.getSnippet());

		    return(popup);
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}
		
	}
}
