package edu.gatech.ppl.cycleatlanta;

import java.util.ArrayList;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.FeatureSet;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.ags.query.*;
import com.esri.core.geometry.GeometryEngine;
import org.json.JSONObject;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import com.google.android.gms.maps.model.MarkerOptions;
import edu.gatech.ppl.cycleatlanta.R;

public class ShowMapNearby extends FragmentActivity {
	private GoogleMap mMap;
	private LinearLayout layout;
	private LocationManager lm = null;
	private LatLng mySpot = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nearbymapview);
	
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
		Location loc = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
        
        //if (loc != null) {
       // 	mySpot = new LatLng(loc.getLatitude(), loc.getLongitude());
       // } else {
        	mySpot = new LatLng(39.952451,-75.163664); // city hall by default
        //}
		
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
			        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mySpot, 17));
			    }
			});
		} else {
			mMap.clear();
		}

		// check if got map
		if (mMap == null) {
			// TODO: anything?
			Log.e("Couldn't get map fragment!", "No map fragment");
			return;
		}
		
		AddRacksToMapLayerTask add_racks = new AddRacksToMapLayerTask();
		add_racks.execute();
		
	}
	
	private class AddRacksToMapLayerTask extends AsyncTask<Void, Void, ArrayList<MarkerOptions>> {

		@Override
		protected ArrayList<MarkerOptions> doInBackground(Void... foo) {
			ArrayList<MarkerOptions> rack_markers;
			rack_markers = new ArrayList<MarkerOptions>();
			LatLng myLoc = ShowMapNearby.this.mySpot;
			SpatialReference mercator = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR);
			SpatialReference sr = SpatialReference.create(4326);
			
			Query qry = new Query();
			qry.setInSpatialReference(sr);
			qry.setOutSpatialReference(sr);
			qry.setReturnGeometry(true);
			qry.setSpatialRelationship(SpatialRelationship.CONTAINS);
			qry.setReturnIdsOnly(false);
			
			String[] flds = {"LOCATION", "NUM_RACKS", "RACK_TYPE", "SIDEWALK"};
			qry.setOutFields(flds);
			
			Geometry buffer = null;
			Unit lu = Unit.create(9003);
			
			// 1/2 mi
			buffer = GeometryEngine.buffer(new Point(myLoc.longitude, myLoc.latitude), mercator, 2540, lu);
			
			Log.d("buffer area",  "buffer area is " + Double.toString(buffer.calculateArea2D()));
			
			qry.setGeometry(buffer);
			qry.setMaxFeatures(25); // show max 50 nearby racks (if 25 from each service)
			QueryTask getCityRacks = new QueryTask("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/0");
			QueryTask getAdoptedRacks = new QueryTask("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/1");
			
			try {
				FeatureSet gotCityRacks = getCityRacks.execute(qry);
				FeatureSet gotAdoptedRacks = getAdoptedRacks.execute(qry);
				
				JSONObject city = new JSONObject(FeatureSet.toJson(gotCityRacks));
				JSONObject adopted = new JSONObject(FeatureSet.toJson(gotAdoptedRacks));
				
				Log.d("gotCityRacks", city.toString());
				Log.d("gotAdoptedRacks", adopted.toString());
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("Async get FeatureSets", e.getMessage());
				e.printStackTrace();
			}
			
			return rack_markers;
		}
		
		@Override
		protected void onPostExecute(ArrayList<MarkerOptions> racks) {
			// TODO:
		}
	}
}
