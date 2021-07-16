package ru.novoscan.trkpd.domain;

import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.terminals.ModMarker;



public class Terminal implements ModConstats  {
	
	protected static final Logger logger = LogManager.getLogger(Terminal.class);
	
	protected final DataSensor dataSensor = new DataSensor();
	
	protected String dasnPacketType;
	
	protected String dasnUid;

	protected DATA_STATUS dasnStatus;

	protected Double dasnLatitude;

	protected Double dasnLongitude;

	protected Double dasnHeight;

	protected Double dasnSog;

	protected Double dasnCourse;
	
	protected Double dasnHgeo;
	
	protected Double dasnHmet;

	protected Long dasnSatUsed;

	protected Double dasnTemp;
	
	protected Long dasnAdc;
	
	protected Date dasnDatetime = new Date();
	
	protected Double dasnHdop;

	protected Long dasnGpio;	
	
	protected Long dasnMacroId;

	protected Long dasnMacroSrc;
	
	protected HashMap<String, String> dasnValues = new HashMap<>();
	
	protected void setDasnType(int dasnType) {
		dataSensor.setDasnType((long) dasnType);
	}
	
	protected void clear() {
		dasnPacketType = null;
		dasnUid = null;
		dasnStatus = null;
		dasnLatitude = null;
		dasnLongitude = null;
		dasnHeight = null;
		dasnSog = null;
		dasnCourse = null;
		dasnHdop = null;
		dasnHgeo = null;
		dasnHmet = null;
		dasnSatUsed = null;
		dasnTemp = null;
		dasnAdc = null;
		dasnHdop = null;
		dasnGpio = null;
		dasnMacroId = null;
		dasnMacroSrc = null;
		dasnValues.clear();
	}

}
