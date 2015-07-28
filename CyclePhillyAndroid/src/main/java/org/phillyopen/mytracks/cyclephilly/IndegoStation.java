package org.phillyopen.mytracks.cyclephilly;

import com.firebase.geofire.GeoLocation;

/**
 * Created by toby on 7/22/15.
 */
public class IndegoStation {
    public String kioskId;
    public String name;
    public Integer bikesAvailable;
    public Integer docksAvialable;
    public GeoLocation location;
    public double distance;

    public IndegoStation() {}

    public IndegoStation(String kioskId, GeoLocation location,double distance, String name, Integer bikesAvailable, Integer docksAvialable){
        this.kioskId = kioskId;
        this.location = location;
        this.bikesAvailable = bikesAvailable;
        this.docksAvialable = docksAvialable;
        this.distance = distance;
        this.name = name;

    }

    public String getKioskId(){
        return kioskId;
    }

    public String getName(){
        return name;
    }

    public Integer getBikesAvailable(){
        return bikesAvailable;
    }

    public Integer getDocksAvialable(){
        return docksAvialable;
    }

    public double getDistance(){
        return distance;
    }
}
