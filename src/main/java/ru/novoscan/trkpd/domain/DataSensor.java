package ru.novoscan.trkpd.domain;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

/**
 * DataSensor generated by hbm2java
 */
public class DataSensor {

	/**
	 * 
	 */

	private Long dasnId;

	private Long dasnSpsnId;

	private String dasnUid;

	private Date dasnDatetime;

	private Double dasnLatitude;

	private Double dasnLongitude;

	private Long dasnStatus = 1L;

	private Long dasnSatUsed;

	private Long dasnZoneAlarm;

	private Long dasnMacroId;

	private Long dasnMacroSrc;

	private Double dasnSog;

	private Double dasnCourse;

	private Double dasnHdop;

	private Double dasnHgeo;

	private Double dasnHmet;

	private Long dasnGpio;

	private Long dasnAdc;

	private Double dasnTemp;

	private Long dasnType = 1L;

	private String dasnXml;

	private HashMap<String, String> dasnValues = new HashMap<String, String>();

	private Timestamp dasnTimestamp;

	public DataSensor() {
	}

	public DataSensor(Long dasnId, Long dasnSpsnId, String dasnUid,
			Date dasnDatetime, Double dasnLatitude, Double dasnLongitude,
			Long dasnStatus, Long dasnType, String dasnXml) {
		this.dasnId = dasnId;
		this.dasnSpsnId = dasnSpsnId;
		this.dasnUid = dasnUid;
		this.dasnDatetime = dasnDatetime;
		this.dasnLatitude = dasnLatitude;
		this.dasnLongitude = dasnLongitude;
		this.dasnStatus = dasnStatus;
		this.dasnType = dasnType;
		this.dasnXml = dasnXml;
	}

	public DataSensor(Long dasnId, Long dasnSpsnId, String dasnUid,
			Date dasnDatetime, Double dasnLatitude, Double dasnLongitude,
			Long dasnStatus, Long dasnSatUsed, Long dasnZoneAlarm,
			Long dasnMacroId, Long dasnMacroSrc, Double dasnSog,
			Double dasnCourse, Double dasnHdop, Double dasnHgeo,
			Double dasnHmet, Long dasnGpio, Long dasnAdc, Double dasnTemp,
			Long dasnType, String dasnXml) {
		this.dasnId = dasnId;
		this.dasnSpsnId = dasnSpsnId;
		this.dasnUid = dasnUid;
		this.dasnDatetime = dasnDatetime;
		this.dasnLatitude = dasnLatitude;
		this.dasnLongitude = dasnLongitude;
		this.dasnStatus = dasnStatus;
		this.dasnSatUsed = dasnSatUsed;
		this.dasnZoneAlarm = dasnZoneAlarm;
		this.dasnMacroId = dasnMacroId;
		this.dasnMacroSrc = dasnMacroSrc;
		this.dasnSog = dasnSog;
		this.dasnCourse = dasnCourse;
		this.dasnHdop = dasnHdop;
		this.dasnHgeo = dasnHgeo;
		this.dasnHmet = dasnHmet;
		this.dasnGpio = dasnGpio;
		this.dasnAdc = dasnAdc;
		this.dasnTemp = dasnTemp;
		this.dasnType = dasnType;
		this.dasnXml = dasnXml;
	}

	public Long getDasnId() {
		return this.dasnId;
	}

	public void setDasnId(Long dasnId) {
		this.dasnId = dasnId;
	}

	public String getDasnUid() {
		return this.dasnUid;
	}

	public void setDasnUid(String dasnUid) {
		this.dasnUid = dasnUid;
	}

	public Date getDasnDatetime() {
		return this.dasnDatetime;
	}

	public void setDasnDatetime(Date dasnDatetime) {
		this.dasnDatetime = dasnDatetime;
		this.dasnTimestamp = new java.sql.Timestamp(dasnDatetime.getTime());
	}

	public Timestamp getDasnTimestamp() {
		return dasnTimestamp;
	}

	public Double getDasnLatitude() {
		return this.dasnLatitude;
	}

	public void setDasnLatitude(Double dasnLatitude) {
		this.dasnLatitude = dasnLatitude;
	}

	public Double getDasnLongitude() {
		return this.dasnLongitude;
	}

	public void setDasnLongitude(Double dasnLongitude) {
		this.dasnLongitude = dasnLongitude;
	}

	public Long getDasnStatus() {
		return this.dasnStatus;
	}

	public void setDasnStatus(Long dasnStatus) {
		this.dasnStatus = dasnStatus;
	}

	public Long getDasnSatUsed() {
		return this.dasnSatUsed;
	}

	public void setDasnSatUsed(Long dasnSatUsed) {
		this.dasnSatUsed = dasnSatUsed;
	}

	public Long getDasnZoneAlarm() {
		return this.dasnZoneAlarm;
	}

	public void setDasnZoneAlarm(Long dasnZoneAlarm) {
		this.dasnZoneAlarm = dasnZoneAlarm;
	}

	public Long getDasnMacroId() {
		return this.dasnMacroId;
	}

	public void setDasnMacroId(Long dasnMacroId) {
		this.dasnMacroId = dasnMacroId;
	}

	public Long getDasnMacroSrc() {
		return this.dasnMacroSrc;
	}

	public void setDasnMacroSrc(Long dasnMacroSrc) {
		this.dasnMacroSrc = dasnMacroSrc;
	}

	public Double getDasnSog() {
		return this.dasnSog;
	}

	public void setDasnSog(Double dasnSog) {
		this.dasnSog = dasnSog;
	}

	public Double getDasnCourse() {
		return this.dasnCourse;
	}

	public void setDasnCourse(Double dasnCourse) {
		this.dasnCourse = dasnCourse;
	}

	public Double getDasnHdop() {
		return this.dasnHdop;
	}

	public void setDasnHdop(Double dasnHdop) {
		this.dasnHdop = dasnHdop;
	}

	public Double getDasnHgeo() {
		return this.dasnHgeo;
	}

	public void setDasnHgeo(Double dasnHgeo) {
		this.dasnHgeo = dasnHgeo;
	}

	public Double getDasnHmet() {
		return this.dasnHmet;
	}

	public void setDasnHmet(Double dasnHmet) {
		this.dasnHmet = dasnHmet;
	}

	public Long getDasnGpio() {
		return this.dasnGpio;
	}

	public void setDasnGpio(Long dasnGpio) {
		this.dasnGpio = dasnGpio;
	}

	public Long getDasnAdc() {
		return this.dasnAdc;
	}

	public void setDasnAdc(Long dasnAdc) {
		this.dasnAdc = dasnAdc;
	}

	public Double getDasnTemp() {
		return this.dasnTemp;
	}

	public void setDasnTemp(Double dasnTemp) {
		this.dasnTemp = dasnTemp;
	}

	public Long getDasnType() {
		return this.dasnType;
	}

	public void setDasnType(Long dasnType) {
		this.dasnType = dasnType;
	}

	public String getDasnXml() {
		return this.dasnXml;
	}

	public void setDasnXml(String dasnXml) {
		this.dasnXml = dasnXml;
	}

	public void setDasnSpsnId(Long dasnSpsnId) {
		this.dasnSpsnId = dasnSpsnId;
	}

	public Long getDasnSpsnId() {
		return dasnSpsnId;
	}

	public HashMap<String, String> getDasnValues() {
		return dasnValues;
	}

	public void setDasnValues(HashMap<String, String> dasnValues) {
		this.dasnValues = dasnValues;
	}

	public boolean isValid() {
		if ((this.dasnUid != null) && (this.dasnLatitude != null)
				&& (this.dasnLongitude != null) && (this.dasnDatetime != null)
				&& (this.dasnType != null)) {
			return true;
		}
		return false;
	}

	public void clear() {
		this.dasnId = null;

		this.dasnSpsnId = null;

		this.dasnUid = null;

		this.dasnDatetime = null;

		this.dasnLatitude = null;

		this.dasnLongitude = null;

		this.dasnStatus = 1L;

		this.dasnSatUsed = null;

		this.dasnZoneAlarm = null;

		this.dasnMacroId = null;

		this.dasnMacroSrc = null;

		this.dasnSog = null;

		this.dasnCourse = null;

		this.dasnHdop = null;

		this.dasnHgeo = null;

		this.dasnHmet = null;

		this.dasnGpio = null;

		this.dasnAdc = null;

		this.dasnTemp = null;

		this.dasnType = 1L;

		this.dasnXml = null;

		this.dasnValues.clear();

	}

	@Override
	public String toString() {
		return  "dasnId=" + dasnId + 
				" dasnSpsnId=" + dasnSpsnId + 
				" dasnUid=" + dasnUid + 
				" dasnDatetime=" + dasnDatetime + 
				" dasnLatitude=" + dasnLatitude + 
				" dasnLongitude=" + dasnLongitude + 
				" dasnStatus=" + dasnStatus + 
				" dasnSatUsed=" + dasnSatUsed + 
				" dasnZoneAlarm=" + dasnZoneAlarm + 
				" dasnMacroId="	+ dasnMacroId + 
				" dasnMacroSrc=" + dasnMacroSrc	+ 
				" dasnSog=" + dasnSog +
				" dasnCourse=" + dasnCourse +
				" dasnHdop=" + dasnHdop +
				" dasnHgeo=" + dasnHgeo +
				" dasnHmet=" + dasnHmet +
				" dasnGpio=" + dasnGpio +
				" dasnAdc=" + dasnAdc +
				" dasnTemp=" + dasnTemp +
				" dasnType=" + dasnType +
				" dasnXml=" + dasnXml + 
				" dasnXml=" + dasnXml.toString() + 
				" dasnXml=" + dasnXml;
	}

}
