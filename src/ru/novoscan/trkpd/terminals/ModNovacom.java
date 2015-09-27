package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModNovacom {

	static Logger logger = Logger.getLogger(ModNovacom.class);

	private int readbytes = 0;

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	// private static String tailSymbol = "$";

	private HashMap<String, String> map = new HashMap<String, String>();

	// private static final Pattern Pattern_ACK_GTHBD =
	// Pattern.compile("\\+ACK:GTHBD,([0-9A-Z]{6}),(\\d{0,15}),[\\w]{0,10},(\\d{14}),([0-9A-Fa-f]{4})\\$");
	private static final Pattern Pattern_ACK = Pattern.compile("\\+ACK:GT.+,"
			+ ",([0-9A-Fa-f]{6})" + // Version
			",(\\d{0,15})" + // IMEI
			",.*" + ",(\\d{14}),([0-9A-Fa-f]{4})\\$");

	/*
	 * Example:
	 * +RESP:GTFRI,0A0100,135790246811220,,0,0,1,1,4.3,92,70.0,121.354335
	 * ,31.222073,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0$
	 * +RESP
	 * :GTFRI,0A0100,135790246811220,,0,0,2,1,4.3,92,70.0,121.354335,31.222073
	 * ,20090214013254
	 * ,0460,0000,18d8,6141,00,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090101000000,04 60,0000,18d8,6141,00,,20090214093254,11F0$
	 * +RESP:GTGEO,0
	 * A0100,135790246811220,,0,0,1,1,4.3,92,70.0,121.354335,31.222073
	 * ,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0$
	 */
	/*
	 * +RESP:GTDOG,0A0100,135790246811220,,0,0,1,1,4.3,92,70.0,121.354335,31.222073
	 * ,20090214013254,0460,0000,18d8,6141,00,2000.0,20090214093254,11F0$
	 */
	// +BUFF:GTNMR,0A0100,011874002002587,GL100M,0,1,1,2,0.1,0,97.2,82.832753,54.996892,20130327085134,0250,0001,983C,017E,00,0.7,89,20130327085134,0F1B$
	private static final Pattern Pattern_RESP_GTNMR = Pattern
			.compile("\\+(RESP|BUFF):GTNMR" + ",([0-9A-Fa-f]{6})" + // Version 2
					",(\\d{0,15})" + // IMEI 3
					",[\\w]{0,10}" + ",(\\d*,\\d*,\\d*)" + //
					",(\\d{0,2})" + // GPS количество спутников 5
					",([0-9\\.]{0,12})" + // Скорость 6
					",([0-9]{0,3})" + // Направление в градусах 7
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",00" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",.*" + // Резерв2 0
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	// "+RESP:GTFRI,0A0100,011874002002587,GL100M,0,0,1,2,0.0,0,200.6,82.968932,55.036390,20130318172403,0250,0001,9831,0C09,00,40.0,9,20130318172417,02B0$";
	private static final Pattern Pattern_RESP_GTFRI = Pattern
			.compile("\\+(RESP|BUFF):GTFRI" + ",([0-9A-Fa-f]{6})" + // Version 2
					",(\\d{0,15})" + // IMEI 3
					",[\\w]{0,10}" + ",(\\d*,\\d*,\\d*)" + //
					",(\\d{0,2})" + // GPS количество спутников 5
					",([0-9\\.]{0,12})" + // Скорость 6
					",([0-9]{0,3})" + // Направление в градусах 7
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",00" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",.*" + // Резерв2 0
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	private static final Pattern Pattern_RESP_GTDOG = Pattern
			.compile("\\+(RESP|BUFF):GTDOG" + ",([0-9A-Fa-f]{6})" + // Version 2
					",(\\d{0,15})" + // IMEI 3
					",[\\w]{0,10}" + ",\\d+" + // ИД отчёта
					",\\d+" + // Тип отчёта
					",([0-9\\+]*)" + // Номер 4
					",(\\d{0,2})" + // GPS количество спутников 5
					",([0-9\\.]{0,12})" + // Скорость 6
					",([0-9]{0,3})" + // Направление в градусах 7
					",([\\-0-9\\.]{0,8})" + // Высота alt 8
					",([\\-0-9\\.]{0,11})" + // long 9
					",([\\-0-9\\.]{0,10})" + // lat 10
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS 11
					",([0-9A-Fa-f]+)" + // MCC 12
					",([0-9A-Fa-f]+)" + // MNC 13
					",([0-9A-Fa-f]+)" + // LAC 14
					",([0-9A-Fa-f]+)" + // Cell ID 15
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр 16
					",(\\d+)" + // Уровень заряда % 17
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + "$");

	/*
	 * +RESP:GTLBC,0A0100,135790246811220,,+8613800000000,1,4.3,92,70.0,121.354335
	 * ,31.222073,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTLBC = Pattern
			.compile("\\+(RESP|BUFF):GTLBC" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9\\+]*)" + // Номер телефона
					",(\\d{0,2})" + // GPS количество спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",00" + // Резерв 00
					",([0-9\\.]*)" + // Одометр
					"," + // Резерв2 0
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTGCR,0A0100,135790246811220,,3,50,180,2,0.4,296,-5.4,121.391055,
	 * 31.164473,20100714104934,0460,0000,1878,0873,00,,20100714104934,000C$
	 */
	private static final Pattern Pattern_RESP_GTGCR = Pattern
			.compile("\\+(RESP|BUFF):GTGCR" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",[0-9]+" + // GEO Mode
					",[0-9]+" + // GEO Radius
					",([0-9]+)" + // GEO Check Interval
					",([0-9]+)" + // GPS количество спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",00" + // Резерв 00
					",([0-9\\.]*)" + // Одометр
					".*" + // Резерв2 0
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTINF,0A0100,135790246811220,,41,898600810906F8048812,16,0,0,0,,4.10
	 * ,0,0,0,0,20100214013254,,,,+0800,0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTINF = Pattern
			.compile("\\+(RESP|BUFF):GTINF" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9]+)" + // State
					",([0-9A-Fa-f]+)" + // ICCID
					",([0-9]+)" + // CSQ RSSI
					",([0-9]+)" + // CSQ BER
					",([0-9]+)" + // External power supply
					",([0-9\\.]*)" + // Mileage
					",\\d*" + // Reserved
					",([0-9\\.]*)" + // Batary Voltage
					",(0|1)" + // Charging
					",(0|1)" + // LED on
					",(0|1)" + // GPS on need
					",(0|1)" + // GPS antenna state
					",(\\d{14})" + // UTC
					",([0-9]*)" + // battery percentage
					".*" + // 4 Reserv
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTGPS,0A0100,135790246811220,,0,1F,1F,0,0,20100214013254,20100214093254
	 * ,11F0$
	 */
	private static final Pattern Pattern_RESP_GTGPS = Pattern
			.compile("\\+(RESP|BUFF):GTGPS" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9]+)" + // GPS on need
					",([0-9A-Fa-f]+)" + // GPS fix delay
					",.*" + // Reserver
					",([0-9A-Fa-f]+)" + // FRI report mask
					",([0-9]+)" + // GPS antenna state
					",(\\d{14})" + // Last GPS Fix Time
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTALL,0A0100,135790246811220,,BSI,cmnet,,,,,,,SRI,4,,1,116.226.44.17
	 * ,9001,116.226.44.16
	 * ,9002,+8613812341234,0,1,,,,,CFG,gl100m,GL100M,,,0,5,001F
	 * ,0,,0FFF,,1,1,300,1
	 * ,,,,,,NMD,1,3,2,3,300,300,,,,,,,,TMZ,-0330,0,FRI,1,1,,
	 * ,0000,2359,60,60,,,1F,1000,1000,0,5,50,
	 * 5,,,GEO,0,3,101.412248,21.187891,1000
	 * ,600,,,,,,,,,1,0,,,500,0,,,,,,,,,2,0,,,500,0,,,,,,,,,3,0,,,500,0,,
	 * ,,,,,,,
	 * 4,0,,,500,0,,,,,,,,,SPD,2,0,80,30,60,,,,,,,,,,,,,,,,FKS,1,,1,,,,,WLT
	 * ,0,,,,,,,,,,,,,,,,,,,GLM ,1,,,,,,,,,,,,PIN,1,,0,,,,,
	 * DOG,0,,30,0200,,1,,,,,,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTALL = Pattern
			.compile("\\+(RESP|BUFF):GTALL" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + // Device Name
					",BSI" + // BSI
					",(.*)" + // APN
					",(.*)" + // APN User Name
					",(.*)" + // APN Passwd
					",,,," + ",SRI" + // SRI
					",(.+){1}" + // Report mode
					"," + ",\\d*" + // Buffer enable
					",([0-9\\.]+)" + // Main server IP/domain name
					",([0-9]+)" + // Main port
					",([0-9\\.]+)" + // Backup server IP/domain name
					",([0-9]+)" + // Backup port
					",([0-9\\+]+)" + // SMS gateway
					",([0-9]+)" + // Heartbeat interval
					",([0-9]+)" + // SACK enable
					",.*" + ",CFG" + ",([0-9a-zA-Z]*)" + // New password
					",.{0,10}" + // Device Name
					",(\\d*)" + // ODO enable
					",([0-9\\.]*)" + // ODO mileage
					",(\\d+)" + // GPS on need
					",([0-9A-Fa-f]+)" + // GPS fix delay
					",([0-9A-Fa-f]+)" + // Report items mask
					",(\\d+)" + // Gsm report
					",.*" + ",NMD" + ",([0-9A-Fa-f])" + // mode
					",([0-9A-Fa-f]+)" + // Non-movement duration
					",([0-9A-Fa-f]+)" + // Movement duration
					",([0-9A-Fa-f]+)" + // Movement threshold
					",([0-9A-Fa-f]+)" + // rest fix interval
					",([0-9A-Fa-f]+)" + // rest send interval
					".*" + ",TMZ" + ",([\\-\\+0-9]+)" + // Time Zone
					",([0-9]+)" + // Daylight Saving
					",FRI" + ",(0|1)" + // Mode
					",(0|1)" + // Discard no fix
					",.*" + // Не обрабатываем поля
					",GEO" + ",.*" + // Не обрабатываем поля
					",SPD" + ",.*" + // Не обрабатываем поля
					",FKS" + ",.*" + // Не обрабатываем поля
					",WLT" + ",.*" + // Не обрабатываем поля
					",GLM" + ",.*" + // Не обрабатываем поля
					",PIN" + ",.*" + // Не обрабатываем поля
					",DOG" + ",.*" + // Не обрабатываем поля
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTCID,0A0100,135790246811220,,898600810906F8048812,20100214093254,11F
	 * 0$
	 */
	private static final Pattern Pattern_RESP_GTCID = Pattern
			.compile("\\+(RESP|BUFF):GTCID" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9a-fA-F]+)" + // ICCID
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/* +RESP:GTCSQ,0A0100,135790246811220,,16,0,20100214093254,11F0$ */
	private static final Pattern Pattern_RESP_GTCSQ = Pattern
			.compile("\\+(RESP|BUFF):GTCSQ" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9a-fA-F]+)" + // CSQ RSSI
					",([0-9a-fA-F]+)" + // CSQ BER
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/* +RESP:GTVER,0A0100,135790246811220,,GL100M,0100,0101,20100214093254,11F0$ */
	private static final Pattern Pattern_RESP_GTVER = Pattern
			.compile("\\+(RESP|BUFF):GTVER" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",([0-9a-fA-F]+)" + // CSQ RSSI
					",([0-9a-fA-F]+)" + // CSQ BER
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/* +RESP:GTBAT,0A0100,135790246811220,,0,,,4.10,0,1,20100214093254,11F0$ */
	private static final Pattern Pattern_RESP_GTBAT = Pattern
			.compile("\\+(RESP|BUFF):GTBAT" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(0|1)" + // External power supply
					",(.*)" + // Reserved
					",(.*)" + // Battery percentage
					",([0-9\\.]+)" + // Battery voltage
					",(0|1)" + // Charging
					",(0|1)" + // LED on
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTTMZ,0A0100,135790246811220,,-0330,0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTTMZ = Pattern
			.compile("\\+(RESP|BUFF):GTTMZ" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",((\\-|\\+)[0-9]{4})" + // Time zone
																// offset
					",(0|1)" + // Daylight saving
					",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * События
	 */
	/*
	 * +RESP:GTPNA,0A0100,135790246811220,,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTPNA = Pattern
			.compile("\\+(RESP|BUFF):GTPNA" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTPFA,0A0100,135790246811220,,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTPFA = Pattern
			.compile("\\+(RESP|BUFF):GTPFA" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTEPN,0A0100,135790246811220,,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTEPN = Pattern
			.compile("\\+(RESP|BUFF):GTEPN" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",([\\w]{0,10})" + ",(\\d{0,2})" + // GPS количество
														// спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTBPL,0A0100,135790246811220,,3.53,0,4.3,92,70.0,121.354335,31.222073
	 * ,20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTBPL = Pattern
			.compile("\\+(RESP|BUFF):GTBPL" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",([\\w]{0,10})" + ",(\\d{0,2})" + // GPS количество
														// спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTEPF,0A0100,135790246811220,,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTEPF = Pattern
			.compile("\\+(RESP|BUFF):GTEPF" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",([\\w]{0,10})" + ",(\\d{0,2})" + // GPS количество
														// спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTBTC,0A0100,135790246811220,,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTBTC = Pattern
			.compile("\\+(RESP|BUFF):GTBTC" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",([\\w]{0,10})" + ",(\\d{0,2})" + // GPS количество
														// спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTSTC,0A0100,135790246811220,,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTSTC = Pattern
			.compile("\\+(RESP|BUFF):GTSTC" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",([\\w]{0,10})" + ",(\\d{0,2})" + // GPS количество
														// спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTSTT,0A0100,135790246811220,,41,0,4.3,92,70.0,121.354335,31.222073,
	 * 20090214013254,0460,0000,18d8,6141,00,0.0,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTSTT = Pattern
			.compile("\\++(RESP|BUFF):GTSTT" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(\\d{0,2})" + // Состояние (21|22|41|42)
					",(\\d{0,2})" + // GPS количество спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTPDP,0A0100,135790246811220,,20100214093254,11F0$
	 */
	private static final Pattern Pattern_RESP_GTPDP = Pattern
			.compile("\\++(RESP|BUFF):GTPDP" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(\\d{14})" + // Send Time
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTSWG,0A0100,135790246811220,,1,0,2.1,0,27.1,121.390717,31.164424,
	 * 20100901073917,0460,0000,1878,0873,00,0.0,20100901154653,0015$
	 */
	private static final Pattern Pattern_RESP_GTSWG = Pattern
			.compile("\\+(RESP|BUFF):GTSWG" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",[\\w]{0,10}" + ",(0|1)" + // GEO Active
					",(\\d{0,2})" + // GPS количество спутников
					",([0-9\\.]{0,12})" + // Скорость
					",([0-9]{0,3})" + // Направление в градусах
					",([\\-0-9\\.]{0,8})" + // Высота alt
					",([\\-0-9\\.]{0,11})" + // long
					",([\\-0-9\\.]{0,10})" + // lat
					",(\\d{14})" + // UTC YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // MCC
					",([0-9A-Fa-f]+)" + // MNC
					",([0-9A-Fa-f]+)" + // LAC
					",([0-9A-Fa-f]+)" + // Cell ID
					",\\d+" + // Резерв 00
					",([0-9\\.]+)" + // Одометр
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	/*
	 * +RESP:GTGSM,0A0100,011874000103767,FRI,0460,0000,1878,0871,20,,0460,0000,1878
	 * ,
	 * 0152,16,,,,,,,,,,,,,,,,,,,,,,,,,,0460,0000,1878,0873,57,00,20100712071540
	 * ,0008$
	 */
	private static final Pattern Pattern_RESP_GTGSM = Pattern
			.compile("\\+(RESP|BUFF):GTGSM" + ",([0-9A-Fa-f]{6})" + // Version
					",(\\d{0,15})" + // IMEI
					",(SOS|RTL|LBC|FRI)" + ",.*" + // тело сообщения не
													// обрабатываем.
					",(\\d{14})" + // Дата отправки отчёта YYYYMMDDHHMMSS
					",([0-9A-Fa-f]+)" + // Количество
					"\\$");

	private static final Pattern PatternReports = Pattern
			.compile("(\\+(RESP|BUFF).*\\$)");

	/*
	 * +RESP:GTLBC ,135790246811220 ,02132523415 ,1 ,4.3 ,92 ,70.0 ,1 ,61.354335
	 * ,30.222073 ,20090101000000 ,0460 ,0000 ,18d8 ,6141 ,00 ,11F0
	 */
	private String packet;

	private int navStatus;

	private float navSog;

	private String navLatitude;

	private String navLongitude;

	private int navCource;

	private String IMEI;

	private boolean isNavigate;

	private String navDateTime;

	private int navSatUsed;

	private int navHdop;

	private String navXML;

	public ModNovacom(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon) throws ParseException, IOException {

		String data = "";
		int cread;
		// Получение от устройства CIO;
		readbytes = 0;
		data = "";
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			logger.debug("Read[" + readbytes + "] " + (char) cread);
			data = data + (char) cread;
			if (PatternReports.matcher(data).matches()) {
				// Разберём пакет
				packet = data;
				logger.debug("Парсим : " + packet);
				parsePacket();
				/*
				 * String vehicleId; int dasnUid; String dasnDateTime; float
				 * dasnLatitude; float dasnLongitude; int dasnStatus; int
				 * dasnSatUsed; int dasnZoneAlarm; int dasnMacroId; int
				 * dasnMacroSrc; float dasnSog; float dasnCource; float
				 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio; int
				 * dasnAdc; float dasnTemp; int8 i_spmt_id;
				 */
				if (isNavigate) {
					map.put("vehicleId", IMEI);
					map.put("dasnUid", IMEI);
					map.put("dasnDateTime", navDateTime);
					map.put("dasnLatitude", navLatitude);
					map.put("dasnLongitude", navLongitude);
					map.put("dasnStatus", String.valueOf(navStatus));
					map.put("dasnSatUsed", String.valueOf(navSatUsed));
					map.put("dasnZoneAlarm", null);
					map.put("dasnMacroId", null);
					map.put("dasnMacroSrc", null);
					map.put("dasnSog", String.valueOf(navSog));
					map.put("dasnCource", String.valueOf(navCource));
					map.put("dasnHdop", String.valueOf(navHdop));
					map.put("dasnHgeo", null);
					map.put("dasnHmet", null);
					map.put("dasnGpio", null);
					map.put("dasnAdc", null);
					map.put("dasnTemp", null);
					map.put("i_spmt_id", Integer.toString(conf.getModType()));
					map.put("dasnXML", navXML);
					// запись в БД

					pgcon.setDataSensor(map, sdf.parse(navDateTime));
					try {
						pgcon.addDataSensor();
						logger.debug("Write Database OK");
					} catch (SQLException e) {
						logger.warn("Error Writing Database : "
								+ e.getMessage());
					}
					map.clear();
				} else {
					logger.debug("Данные не содержат навигационные данные.");
				}
				packet = "";
				data = "";
			}
		}

		readbytes = 0;
		logger.debug("Close reader console");

	}

	private void parsePacket() {

		logger.debug("Обработка данных : " + packet);
		Matcher m;
		IMEI = null;
		navHdop = -1;
		navLongitude = null;
		navLatitude = null;
		navDateTime = null;
		navSatUsed = -1;
		navSog = -1;
		navXML = null;
		isNavigate = false;
		if (Pattern_ACK.matcher(packet).matches()) {
			m = Pattern_ACK.matcher(packet);
			m.matches();
			logger.error("Не обрабатываемый запрос ACK UID : "
					+ getUID(m.group(3)));
		} else if (Pattern_RESP_GTDOG.matcher(packet).matches()) {
			m = Pattern_RESP_GTDOG.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTDOG UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml>" + "<pw>" + m.group(17) + "%</pw>" + "<mcc>"
					+ m.group(12) + "</mcc>" + "<mnc>" + m.group(13) + "</mnc>"
					+ "<lac>" + m.group(14) + "</lac>" + "<cell>" + m.group(15)
					+ "</cell>" + "<odo>" + m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTFRI.matcher(packet).matches()) {
			m = Pattern_RESP_GTFRI.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTFRI UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTNMR.matcher(packet).matches()) {
			m = Pattern_RESP_GTNMR.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTNMR UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTLBC.matcher(packet).matches()) {
			m = Pattern_RESP_GTLBC.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTLBC UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTGCR.matcher(packet).matches()) {
			m = Pattern_RESP_GTGCR.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTGCR UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = 1;
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = Integer.valueOf(m.group(5));
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTINF.matcher(packet).matches()) {
			m = Pattern_RESP_GTINF.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTINF UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTGPS.matcher(packet).matches()) {
			m = Pattern_RESP_GTGPS.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTGPS UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTALL.matcher(packet).matches()) {
			m = Pattern_RESP_GTALL.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTALL UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTCID.matcher(packet).matches()) {
			m = Pattern_RESP_GTCID.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTCID UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTCSQ.matcher(packet).matches()) {
			m = Pattern_RESP_GTCSQ.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTCSQ UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTVER.matcher(packet).matches()) {
			m = Pattern_RESP_GTVER.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTVER UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTBAT.matcher(packet).matches()) {
			m = Pattern_RESP_GTBAT.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTBAT UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTTMZ.matcher(packet).matches()) {
			m = Pattern_RESP_GTTMZ.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTTMZ UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTPNA.matcher(packet).matches()) {
			m = Pattern_RESP_GTPNA.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTPNA UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTPFA.matcher(packet).matches()) {
			m = Pattern_RESP_GTPFA.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTPFA UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTEPN.matcher(packet).matches()) {
			m = Pattern_RESP_GTEPN.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTEPN UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTBPL.matcher(packet).matches()) {
			m = Pattern_RESP_GTBPL.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTBPL UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTEPF.matcher(packet).matches()) {
			m = Pattern_RESP_GTEPF.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTEPF UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTBTC.matcher(packet).matches()) {
			m = Pattern_RESP_GTBTC.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTBTC UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTSTC.matcher(packet).matches()) {
			m = Pattern_RESP_GTSTC.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTSTC UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTSTT.matcher(packet).matches()) {
			m = Pattern_RESP_GTSTT.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTSTT UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTPDP.matcher(packet).matches()) {
			m = Pattern_RESP_GTPDP.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTPDP UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (Pattern_RESP_GTSWG.matcher(packet).matches()) {
			m = Pattern_RESP_GTSWG.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTSWG UID : " + getUID(m.group(3)));
			IMEI = getUID(m.group(3));
			navHdop = Integer.valueOf(m.group(5));
			navLongitude = m.group(9);
			navLatitude = m.group(10);
			navDateTime = m.group(11);
			navSatUsed = getSatUsed(navHdop);
			navSog = Float.valueOf(m.group(6)).floatValue();
			navCource = Integer.valueOf(m.group(7));
			navXML = "<xml><mcc>" + m.group(12) + "</mcc>" + "<mnc>"
					+ m.group(13) + "</mnc>" + "<lac>" + m.group(14) + "</lac>"
					+ "<cell>" + m.group(15) + "</cell>" + "<odo>"
					+ m.group(16) + "</odo>" + "</xml>";
			isNavigate = true;
		} else if (Pattern_RESP_GTGSM.matcher(packet).matches()) {
			m = Pattern_RESP_GTGSM.matcher(packet);
			m.matches();
			logger.debug("Тип пакета GTGSM UID : " + getUID(m.group(3)));
			isNavigate = false;
		} else if (PatternReports.matcher(packet).matches()) {
			m = PatternReports.matcher(packet);
			m.matches();
			logger.error("Неизвестный тип пакета : " + packet);
			isNavigate = false;
		} else {
			logger.error("Неверные данные : " + packet);
			isNavigate = false;
		}

	}

	private String getUID(String imsi) {
		long uid = Long.valueOf(imsi);
		return String.valueOf(uid);
	}

	private int getSatUsed(int navHdopInfo) {
		int satUsed = 0;
		if (navHdopInfo == 0) {
			satUsed = 0;
		} else if (navHdopInfo == 1) {
			satUsed = 12; // максимальная точность
		} else if (navHdopInfo < 7) {
			satUsed = 6;
		} else if (navHdopInfo < 9) {
			satUsed = 6;
		} else if (navHdopInfo < 20) {
			satUsed = 4;
		} else {
			satUsed = 3;
		}
		if (satUsed == 0) {
			navStatus = 0;
		} else {
			navStatus = 1;
		}
		return satUsed;
	}

	/*
	 * private void sendACK() { // If server acknowledgement is enabled by
	 * AT+GTQSS or AT+GTSRI command String ack = ""+"";
	 * 
	 * }
	 */
	public float getReadBytes() {
		return readbytes;
	}

}
