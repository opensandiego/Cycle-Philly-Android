package org.phillyopen.mytracks.cyclephilly;

import com.firebase.geofire.GeoLocation;

/**
 * Created by toby on 7/22/15.
 */
public class IndegoStation {
    private int kioskId;
    private String name;
    private GeoLocation location;
    public double distance;

    public IndegoStation() {}

    public IndegoStation(int kioskId, GeoLocation location,double distance){
        this.kioskId = kioskId;
        this.location = location;
        this.distance = distance;
    }

    public long getKioskId(){
        return kioskId;
    }

    public double getDistance(){
        return distance;
    }
}
