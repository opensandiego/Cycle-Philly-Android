package org.phillyopen.mytracks.cyclephilly;

import com.firebase.geofire.GeoLocation;

/**
 * Created by toby on 7/22/15.
 */
public class IndegoStation {
    public String kioskId;
    private String name;
    public GeoLocation location;
    public double distance;

    public IndegoStation() {}

    public IndegoStation(String kioskId, GeoLocation location,double distance){
        this.kioskId = kioskId;
        this.location = location;
        this.distance = distance;
    }

    public String getKioskId(){
        return kioskId;
    }

    public double getDistance(){
        return distance;
    }
}
