package edu.gatech.ppl.cycleatlanta;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
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
        
        if (loc != null) {
        	mySpot = new LatLng(loc.getLatitude(), loc.getLongitude());
        } else {
        	mySpot = new LatLng(39.952451,-75.163664); // city hall by default
        }
		
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
		
		try {
			AddRacksToMapLayerTask add_racks = new AddRacksToMapLayerTask();
			URL city_racks = new URL("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/0?f=json");
			URL adopted_racks = new URL("http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/1?f=json");
			add_racks.execute(city_racks, adopted_racks);
			
		} catch (MalformedURLException e) {
			Log.e("adding racks", e.getMessage());
		}
	}
	
	private class AddRacksToMapLayerTask extends AsyncTask<URL, Void, ArrayList<MarkerOptions>> {

		@Override
		protected ArrayList<MarkerOptions> doInBackground(URL... urls) {
			ArrayList<MarkerOptions> rack_markers;
			rack_markers = new ArrayList<MarkerOptions>();
			LatLng myLoc = ShowMapNearby.this.mySpot;
			
			String bufferStr = "http://gis.phila.gov/ArcGIS/rest/services/Geometry/GeometryServer/buffer";
			bufferStr += "?geometries={%22geometryType%22%3A%22esriGeometryPoint%22%2C%22geometries%22%3A[{%22x%22%3A";
			bufferStr += myLoc.longitude + "%2C%22y%22%3A" + myLoc.latitude;
			// buffer 1/2 mi
			bufferStr += "}]}%0D%0A&inSR=4326&outSR=4326&bufferSR=4326&distances=2640&unit=9003&unionResults=false&f=json";
			
			try {
				//URL bufferSvc = new URL("http://gis.phila.gov/ArcGIS/rest/services/Geometry/GeometryServer/buffer");
				URL bufferSvc = new URL(bufferStr);
				//HttpURLConnection conn = (HttpURLConnection) urls[0].openConnection();
				
				HttpURLConnection conn = (HttpURLConnection) bufferSvc.openConnection();
				
				InputStream in = new BufferedInputStream(conn.getInputStream());
				JSONObject buffer_json = new JSONObject(new Scanner(in).useDelimiter("\\A").next());
				
				conn.disconnect();
				in.close();
				
				JSONArray geom_json = buffer_json.getJSONArray("geometries");
				JSONObject rings_json = geom_json.getJSONObject(0);
				rings_json = rings_json.put("wkid", 4326); // TODO: needs closing bracket %7D
				
				String qryRacks = "http://gis.phila.gov/ArcGIS/rest/services/Streets/Bike_Racks/MapServer/1/query?text=&geometry=%7B%22spatialReference%22%3A";
				//qryRacks += URLEncoder.encode(rings_json.toString());
				qryRacks += rings_json.toString();
				qryRacks += "&geometryType=esriGeometryPolygon&inSR=4326&spatialRel=esriSpatialRelContains&relationParam=&objectIds=&where=&time=&returnCountOnly=false&returnIdsOnly=false&returnGeometry=true&maxAllowableOffset=&outSR=4326&outFields=%27LOCATION%27&f=json";
				Log.d("racks query URL", qryRacks);
				
				URL racksSvc = new URL(qryRacks);
				
				conn = (HttpURLConnection) racksSvc.openConnection();
				conn.setRequestMethod("POST");
				Log.d("got connection", "got connection to racks service");
				in = new BufferedInputStream(conn.getInputStream());
				Log.d("got stream", "got racks service stream");
				buffer_json = new JSONObject(new Scanner(in).useDelimiter("\\A").next());
				Log.d("query result", buffer_json.toString());
				
			} catch (MalformedURLException e1) {
				Log.e("adding racks MalformedURLException", e1.getMessage());
			} catch (IOException e) {
				Log.e("adding racks IOException", e.getMessage());
				e.printStackTrace();
			} catch (JSONException e) {
				Log.e("adding racks JSONException", e.getMessage());
			}
			
			
			// TODO:
			
			
			return rack_markers;
		}
		
		@Override
		protected void onPostExecute(ArrayList<MarkerOptions> racks) {
			// TODO:
		}
	}
}
