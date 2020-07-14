package net.osmand.plus.rfobject;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.graphics.Color;

import net.osmand.Location;

public class RfObject {
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private ConcurrentLinkedQueue<Location> locations = new ConcurrentLinkedQueue<Location>();
    private Location lastLocation;
    private int color;
    private boolean visible = true;

    public RfObject() {
    }
    public RfObject(double latitude, double longitude, String name) {
	this.latitude = latitude;
        this.longitude = longitude;
        this.lastLocation = new Location("rf");
        this.lastLocation.setTime(System.currentTimeMillis());
        this.lastLocation.setLatitude(latitude);
        this.lastLocation.setLongitude(longitude);
        this.name = name;
    }

    public int getColor() {
        //return this.color;
	if (isMe())
		return 0xffffa500; /* orange */
	else
		return Color.CYAN;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getLatitude() {
        //return this.latitude;
        return this.lastLocation.getLatitude();
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        //return this.longitude;
        return this.lastLocation.getLongitude();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public ConcurrentLinkedQueue<Location> getLocations(long threshold) {
        while(!locations.isEmpty() && locations.peek().getTime() < threshold) {
            locations.poll();
        }
        return locations;
    }

    public void setLocation(double latitude, double longitude) {
        if (this.lastLocation != null)
            locations.add(this.lastLocation);
        this.lastLocation = new Location("rf");
        this.lastLocation.setTime(System.currentTimeMillis());
        this.lastLocation.setLatitude(latitude);
        this.lastLocation.setLongitude(longitude);
    }

    public Location getLastLocation() {
        return this.lastLocation;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMe() {
        if (getName().startsWith("Soft")) { // SoftRF
            return true;
        } else {
            return false;
        }
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
