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

public class RfStatus {

  private long timestamp;
  private long fixTimestamp;
  private long startTimestamp;
  private long firstFixTimestamp=0;
  private String acftType; // aircraft type
  private String addrType; // address type
  private String address; // address
  private float PDOP;
  private float precision;
  private double latitude;
  private double longitude;
  private double altitude;
  private double height;
  private float speed;
  private float angle;
  /* 1: nofix, 2: 2D fix, 3: 3D fix */
  private int fixMode; 
  /* 
       Fix quality: 0 = invalid
                    1 = GPS fix (SPS)
                    2 = DGPS fix
                    3 = PPS fix
                    4 = Real Time Kinematic
                    5 = double RTK
                    6 = estimated (dead reckoning) (2.3 feature)
                    7 = Manual input mode
                    8 = Simulation mode
   */
  private int quality;
  private float rssi;

  /*****************************************************************************
   *
   * time stamps handler
   *
   */
  public void clearTTFF(){
    this.firstFixTimestamp=0;
  }
  public long getTTFF(){
    if (this.firstFixTimestamp == 0 || this.startTimestamp == 0 ||
        this.firstFixTimestamp < this.startTimestamp) {
      // invalid status
      return 0;
    } else {
      return this.firstFixTimestamp - this.startTimestamp;
    }
  }
  public long getFixTimestamp(){
    return this.fixTimestamp;
  }
  public void setFixTimestamp(long timestamp){
    this.timestamp = timestamp;
    this.fixTimestamp = timestamp;
    if (this.firstFixTimestamp == 0){
      this.firstFixTimestamp = timestamp;
    }
  }
  public void setTimestamp(long timestamp){
    if (this.startTimestamp ==0 ){
      this.startTimestamp = timestamp;
    }
    this.timestamp = timestamp;
  }
  public long getTimestamp(){
    return this.timestamp;
  }

  /***************************************************************************
   *
   * clear all
   */
  public void clear(){
    clearTTFF();
    timestamp = 0;
    startTimestamp = 0;
    fixTimestamp = 0;
    precision = 10f;
    acftType = null;
    addrType = null;
    address = null;
    PDOP = 0f;
    latitude  = 0d;
    longitude = 0d;
    altitude  = 0d;
    height    = 0d;
    speed     = 0f;
    angle     = 0f;
    fixMode = 0;
    quality = 0;
    rssi     = 0f;
  }

  /***********************************************************************
   *
   * accessors
   */

  public String getAcftType(){
    return this.acftType;
  }
  public void setAcftType(String acftType){
    this.acftType = acftType;
  }
  public String getAddrType(){
    return this.addrType;
  }
  public void setAddrType(String addrType){
    this.addrType = addrType;
  }
  public String getAddress(){
    return this.address;
  }
  public void setAddress(String address){
    this.address = address;
  }

  // Lat/Lon/Alt
  public double getLatitude(){
    return this.latitude;
  }
  public void setLatitude(double lat){
    this.latitude = lat;
  }
  public double getLongitude(){
    return this.longitude;
  }
  public void setLongitude(double lon){
    this.longitude = lon;
  }
  public double getAltitude(){
    return this.altitude;
  }
  public void setAltitude(double alt){
    this.altitude = alt;
  }
  // DOP/precision
  public float getPrecision(){
    return this.precision;
  }
  public void setPrecision(float p){
    this.precision = p;
  }
  public double getPDOP(){
    return this.PDOP;
  }
  public void setPDOP(float dop){
    this.PDOP = dop;
  }
  // others
  public double getHeight(){
    return this.height;
  }
  public void setHeight(double height){
    this.height = height;
  }
  public float getSpeed(){
    return this.speed;
  }
  public void setSpeed(float speed){
    this.speed = speed;
  }
  public float getBearing(){
    return this.angle;
  }
  public void setBearing(float angle){
    this.angle = angle;
  }
  public int getQuality(){
    return this.quality;
  }
  public void setQuality(int q){
    this.quality = q;
  }
  public int getFixMode(){
    return this.fixMode;
  }
  public void setFixMode(int mode){
    this.fixMode = mode;
  }
  public float getRssi(){
    return this.rssi;
  }
  public void setRssi(float rssi){
    this.rssi = rssi;
  }

}
// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
