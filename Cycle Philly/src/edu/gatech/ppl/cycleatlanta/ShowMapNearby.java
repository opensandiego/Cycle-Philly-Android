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

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.gatech.ppl.cycleatlanta.R;

public class ShowMapNearby extends FragmentActivity {
	private GoogleMap mMap;
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
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		Location loc = lm.getLastKnownLocation(lm.getBestProvider(criteria, false));
		
        t1.setText("Bicycle Parking");
        t3.setText("loading...");
        
        if (loc != null) {
        	mySpot = new LatLng(loc.getLatitude(), loc.getLongitude());
        } else {
        	mySpot = new LatLng(39.952451,-75.163664); // city hall by default
        	t2.setText("Current location not found; enable GPS to search nearby.");
        	// show message to tell user to enable location svcs and try again
        }
		
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
		AddRacksToMapLayerTask add_racks = new AddRacksToMapLayerTask();
		add_racks.execute(mySpot);
		
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
	
	private class AddRacksToMapLayerTask extends AsyncTask<LatLng, Void, ArrayList<MarkerOptions>> {

		@Override
		protected ArrayList<MarkerOptions> doInBackground(LatLng... centers) {
			ArrayList<MarkerOptions> rack_markers;
			rack_markers = new ArrayList<MarkerOptions>();
			LatLng myLoc = centers[0];
			
			/*
			 try {
				BufferedReader reader = new BufferedReader(
					        new InputStreamReader(getAssets().open("foo")));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			*/
			
			//SpatialReference mercator = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR);
			SpatialReference sr = SpatialReference.create(4326);
			SpatialReference serverSpatialRef = SpatialReference.create(2272);
			
			Query qry = new Query();
			qry.setInSpatialReference(serverSpatialRef);
			qry.setOutSpatialReference(sr);
			qry.setReturnGeometry(true);
			qry.setSpatialRelationship(SpatialRelationship.CONTAINS);
			qry.setReturnIdsOnly(false);
			
			String[] flds = {"LOCATION", "NUM_RACKS", "RACK_TYPE", "SIDEWALK"};
			qry.setOutFields(flds);
			
			Geometry buffer = null;
			Unit lu = Unit.create(9003);
			
			Point reprojPt = GeometryEngine.project(myLoc.longitude, myLoc.latitude, serverSpatialRef);
			
			// 1/2 mi - 2540
			buffer = GeometryEngine.buffer(reprojPt, serverSpatialRef, 2540, lu);
			
			qry.setGeometry(buffer);
			qry.setMaxFeatures(50); // show max 100 nearby racks (if 50 from each service)
			QueryTask getCityRacks = new QueryTask("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/0");
			QueryTask getAdoptedRacks = new QueryTask("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/1");
			
			try {
				FeatureSet gotCityRacks = getCityRacks.execute(qry);
				FeatureSet gotAdoptedRacks = getAdoptedRacks.execute(qry);
				
				JSONObject cityObj = new JSONObject(FeatureSet.toJson(gotCityRacks));
				JSONObject adoptedObj = new JSONObject(FeatureSet.toJson(gotAdoptedRacks));
				
				//Log.d("gotCityRacks", cityObj.toString());
				//Log.d("gotAdoptedRacks", adoptedObj.toString());
				
				JSONArray city = null;
				JSONArray adopted = null;
				
				// if no results, get no value for features
				if (cityObj.has("features")) {
					city = cityObj.getJSONArray("features");
				}
				
				if (adoptedObj.has("features")) {
					adopted = adoptedObj.getJSONArray("features");
				}
				
				JSONObject row;
				JSONObject geom;
				JSONObject attr;
				String snippetStr;
				BitmapDescriptor cityIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
				BitmapDescriptor adoptedIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
				
				if (city != null && adopted != null) {
					rack_markers = new ArrayList<MarkerOptions>(city.length() + adopted.length());
				} else if (city != null) {
					rack_markers = new ArrayList<MarkerOptions>(city.length());
				} else if (adopted != null) {
					rack_markers = new ArrayList<MarkerOptions>(adopted.length());
				} else {
					Log.d("get bike racks", "no results found");
				}
				
				if (city != null) {
					for (int i = city.length(); i--> 0; ) {
						row = city.getJSONObject(i);
						geom = row.getJSONObject("geometry");
						attr = row.getJSONObject("attributes");
						
						if (attr.getString("SIDEWALK").replace(" ",  "").length() > 0) {
							snippetStr = "Sidewalk: " + attr.getString("SIDEWALK") + '\n';
						} else {
							snippetStr = "";
						}
						
						snippetStr += "Type: " + 
								attr.getString("RACK_TYPE") + "\nNumber of racks: " + 
								attr.getInt("NUM_RACKS"); 
						
						rack_markers.add(new MarkerOptions()
							.position(new LatLng(geom.getDouble("y"), geom.getDouble("x")))
							.title(attr.getString("LOCATION"))
							.snippet(snippetStr)
							.icon(cityIcon));
					}
				}
				
				if (adopted != null) {
					for (int i = adopted.length(); i--> 0; ) {
						row = adopted.getJSONObject(i);
						geom = row.getJSONObject("geometry");
						attr = row.getJSONObject("attributes");
						
						if (attr.getString("SIDEWALK").replace(" ",  "").length() > 0) {
							snippetStr = "Sidewalk: " + attr.getString("SIDEWALK") + '\n';
						} else {
							snippetStr = "";
						}
						
						snippetStr += "Type: " + 
								attr.getString("RACK_TYPE") + "\nNumber of racks: " + 
								attr.getInt("NUM_RACKS"); 
						
						rack_markers.add(new MarkerOptions()
							.position(new LatLng(geom.getDouble("y"), geom.getDouble("x")))
							.title(attr.getString("LOCATION"))
							.snippet(snippetStr)
							.icon(adoptedIcon));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return rack_markers;
		}
		
		@Override
		protected void onPostExecute(ArrayList<MarkerOptions> racks) {
			if (racks != null) {
				for (int i = racks.size(); i--> 0; ) {
					mMap.addMarker(racks.get(i));
				}
				
				t2.setText("found " + racks.size() + " bike racks within 1/2 mile");
			} else {
				t2.setText("No bike racks found within 1/2 mile.");
			}
			
			t3.setText("");
		}
	}
}
