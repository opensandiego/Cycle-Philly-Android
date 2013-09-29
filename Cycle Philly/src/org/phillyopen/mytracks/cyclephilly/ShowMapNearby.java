package org.phillyopen.mytracks.cyclephilly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
import android.graphics.Color;
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
import com.google.android.gms.maps.model.PolylineOptions;

import org.phillyopen.mytracks.cyclephilly.R;

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
		AddRacksToMapLayerTask add_racks = new AddRacksToMapLayerTask();
		add_racks.execute(mySpot);
		
		AddRoutesToMapLayerTask add_routes = new AddRoutesToMapLayerTask();
		add_routes.execute(mySpot);
		
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
	
	private class AddRoutesToMapLayerTask extends AsyncTask<LatLng, Void, HashMap<String, PolylineOptions>> {

		@Override
		protected HashMap<String, PolylineOptions> doInBackground(LatLng... centers) {
			HashMap<String, PolylineOptions> route_options;
			route_options = new HashMap<String, PolylineOptions>();
			LatLng myLoc = centers[0];
			
			SpatialReference sr = SpatialReference.create(4326);
			SpatialReference serverSpatialRef = SpatialReference.create(3857);
			
			Query qry = new Query();
			qry.setInSpatialReference(serverSpatialRef);
			qry.setOutSpatialReference(sr);
			qry.setReturnGeometry(true);
			qry.setSpatialRelationship(SpatialRelationship.CONTAINS);
			qry.setReturnIdsOnly(false);
			
			String[] flds = {"STREETNAME", "ST_CODE", "ONEWAY", "TYPE", "CLASS", "SHAPE"};
			qry.setOutFields(flds);
			
			Geometry buffer = null;
			Unit lu = Unit.create(9003);
			
			Point reprojPt = GeometryEngine.project(myLoc.longitude, myLoc.latitude, serverSpatialRef);
			
			// 1 mi - 5080
			// 4 mi - 20320
			buffer = GeometryEngine.buffer(reprojPt, serverSpatialRef, 10160, lu);
			
			qry.setGeometry(buffer);
			qry.setMaxFeatures(20);
			QueryTask getNearbyRoutes = new QueryTask("http://gis.phila.gov/ArcGIS/rest/services/PhilaOIT-GIS_Transportation/MapServer/0");
			
			try {
				FeatureSet gotNearbyRoutes = getNearbyRoutes.execute(qry);
				
				JSONObject routesObj = new JSONObject(FeatureSet.toJson(gotNearbyRoutes));
				
				Log.d("gotNearbyRoutes", routesObj.toString());
				
				JSONArray routes = null;
				
				// if no results, get no value for features
				if (routesObj.has("features")) {
					routes = routesObj.getJSONArray("features");
				}
				
				JSONObject row = null;
				JSONObject geom = null;
				JSONObject attr = null;
				JSONArray paths = null;
				JSONArray path = null;
				PolylineOptions opt = null;
				JSONArray pt = null;
				String st_code = null;
				int use_color = 0;
				Integer path_class = null;
				
				// segments with matching ST_CODE belong to the same polyline
				
				if (routes != null) {
					for (int i = routes.length(); i--> 0; ) {
						row = routes.getJSONObject(i);
						geom = row.getJSONObject("geometry");
						attr = row.getJSONObject("attributes");
						st_code = attr.getString("ST_CODE");
						path_class = attr.getInt("CLASS");
						paths = geom.getJSONArray("paths");
						
						if (route_options.containsKey(st_code)) {
							// got polyline already
							opt = route_options.get(st_code);
						} else {
							switch (path_class) {
								case 1: 
									use_color = Color.GREEN;
									break;
								case 2:
									use_color = Color.CYAN;
									break;
								case 3:
									use_color = Color.BLUE;
									break;
								case 4:
									use_color = Color.YELLOW;
									break;
								case 5:
									use_color = Color.MAGENTA;
								case 9:
									use_color = Color.RED;
									break;
								default:
									use_color = Color.DKGRAY;
									break;
							}
							
							opt = new PolylineOptions()
								.color(use_color);
						}
						
						for (int j = paths.length(); j--> 0; ) {
							path = paths.getJSONArray(j);
							for (int k = path.length(); k--> 0; ) {
								pt = path.getJSONArray(k);
								opt.add(new LatLng(pt.getDouble(1), pt.getDouble(0)));
							}
						}
						
						route_options.put(st_code, opt);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return route_options;
		}
		
		@Override
		protected void onPostExecute(HashMap<String, PolylineOptions> routes) {
			if (routes != null) {
				Iterator<String> routesKeys = routes.keySet().iterator();
				while (routesKeys.hasNext()) {
					mMap.addPolyline(routes.get(routesKeys.next()));
				}
			}
		}
	}
}
