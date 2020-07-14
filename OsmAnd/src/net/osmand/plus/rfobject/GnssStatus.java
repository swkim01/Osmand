/*
 * Copyright 2014 Hiroshi Miura <miurahr@linux.com>
 * 
 * This file is part of BluetoothGPS4Droid.
 *
 * BluetoothGPS4Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BluetoothGPS4Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with BluetoothGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
 */

package net.osmand.plus.rfobject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;

import android.os.Bundle;
import android.os.SystemClock;
import android.location.Location;
import android.location.LocationManager;

public class GnssStatus extends RfStatus {

  private float HDOP;
  private float VDOP;
  private int nbsat;
  private int numSatellites;
  // for NMEA 0183 version 3.00 active the Mode indicator field is added
  // Mode indicator, (A=autonomous, D=differential, E=Estimated, N=not valid, S=Simulator )
  private String mode;


  /******************************************************************************
   * 
   * GnssSatellite list in view
   * satellites PRN list in fix
   *
   */
  private HashMap<Integer, GnssSatellite> gnssSatellitesList = new HashMap<Integer, GnssSatellite>();

  // satallite list
  // accessor
  public void addSatellite(GnssSatellite sat){
    gnssSatellitesList.put(sat.getRpn(), sat);
  }
  // clear list at all
  public void clearSatellitesList(){
    gnssSatellitesList.clear();
  }
  // clear list except for ones in activeList
  public void clearSatellitesList(ArrayList<Integer> activeList){
    ArrayList<Integer> currentRpnList = new ArrayList<Integer>();
    for (Integer rpn: gnssSatellitesList.keySet()){
      currentRpnList.add(rpn);
    }
    for (Integer i : currentRpnList){
      if (!activeList.contains(i)) {
        removeSatellite(i);
      }
    }
  }

  // remove sat data, return cleared satellite
  public GnssSatellite removeSatellite(int rpn){
    return gnssSatellitesList.remove(rpn);
  }
  // get sat data with index
  public GnssSatellite getSatellite(int rpn){
    return gnssSatellitesList.get(rpn);
  }
  // get iterator
  public Iterator<Entry<Integer, GnssSatellite>> getSatellitesIter(){
    return gnssSatellitesList.entrySet().iterator();
  }
  // get list of satellites
  public ArrayList<GnssSatellite> getSatellitesList(){
    return new ArrayList<GnssSatellite>(gnssSatellitesList.values());
  }


  /***************************************************************************
   *
   * tracked satellites PRN list
   *
   */
  private ArrayList<Integer> satellitesPrnListInFix = new ArrayList<Integer>();

  public void setTrackedSatellites(ArrayList<Integer> rpnList){
      satellitesPrnListInFix.clear();
      for (Integer rpn : rpnList){
        satellitesPrnListInFix.add(rpn);
      }
  }

  public void addTrackedSatellites(int rpn){
      satellitesPrnListInFix.add(rpn);
  }

  public ArrayList<Integer> getTrackedSatellites() {
      return satellitesPrnListInFix;
  }

  public void clearTrackedSatellites(){
      this.satellitesPrnListInFix.clear();
  }

  /***************************************************************************
   *
   * clear all
   */
  public void clear(){
    super.clear();
    clearSatellitesList();
    HDOP = 0f;
    VDOP = 0f;
    nbsat = 0;
    numSatellites = 0;
    mode = "N";
  }

  /**********************************************************************
   * 
   * @return Location fix
   */
  public Location getFixLocation(){
    Location fix = new Location(LocationManager.GPS_PROVIDER);

    if (android.os.Build.VERSION.SDK_INT >= 17)
        fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

    fix.setLatitude(this.getLatitude());
    fix.setLongitude(this.getLongitude());
    fix.setAccuracy(this.HDOP*this.getPrecision());
    fix.setTime(this.getFixTimestamp());
    fix.setAltitude(this.getAltitude());
    Bundle extras = new Bundle();
    extras.putInt("satellites", this.nbsat);
    fix.setExtras(extras);
    fix.setBearing(this.getBearing());
    fix.setSpeed(this.getSpeed());

    return fix;
  }

  /***********************************************************************
   *
   * accessors
   */

  public double getHDOP(){
    return this.HDOP;
  }
  public void setHDOP(float dop){
    this.HDOP = dop;
  }
  public double getVDOP(){
    return this.VDOP;
  }
  public void setVDOP(float dop){
    this.VDOP = dop;
  }
  // others
  public int getNbSat(){
    return this.nbsat;
  }
  public void setNbSat(int sat){
    this.nbsat = sat;
  }
  public String getMode(){
    return this.mode;
  }
  public void setMode(String mode){
    this.mode = mode;
  }
  public int getNumSatellites(){
    return this.numSatellites;
  }
  public void setNumSatellites(int num){
    this.numSatellites = num;
  }
  public void addNumSatellites(int num){
    this.numSatellites = this.numSatellites + num;
  }
}
// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
