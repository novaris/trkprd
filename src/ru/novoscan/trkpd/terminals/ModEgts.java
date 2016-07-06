package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.resources.EgtsFrameData;
import ru.novoscan.trkpd.resources.EgtsResponse;
import ru.novoscan.trkpd.resources.EgtsTransport;
import ru.novoscan.trkpd.resources.ServiceDataRecord;
import ru.novoscan.trkpd.resources.ServiceSubRecordData;
import ru.novoscan.trkpd.resources.SubRecord;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModEgts extends Terminal {

	private static Logger logger = Logger.getLogger(ModEgts.class);

	private float fullreadbytes;

	private final static int READ_OK = 0x55;

	private final TrackPgUtils pgcon;

	private final DataOutputStream oDs;

	private StringBuffer message = new StringBuffer();

	private int egtsPacketId;

	public int getEgtsPacketId() {
		return egtsPacketId;
	}

	public void setEgtsPacketId(int egtsPacketId) {
		this.egtsPacketId = egtsPacketId;
	}

	private int writebytes;

	private int responsePacketId;

	private int responseRecordNumber;

	private EgtsFrameData ptServices;

	private ServiceDataRecord fdSdr;

	private ServiceSubRecordData serviceSubRecordData;

	private SubRecord subRecord;

	private SubRecord subRecordResponse;

	private EgtsResponse egtsResponse;

	public int getResponseRecorNumber() {
		return responseRecordNumber;
	}

	public void setResponseRecorNumber(int responseRecordNumber) {
		this.responseRecordNumber = responseRecordNumber;
	}

	public int getResponsePacketId() {
		return responsePacketId;
	}

	public void setResponsePacketId(int responsePacketId) {
		this.responsePacketId = responsePacketId;
	}

	public ModEgts(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws IOException {
		this.setDasnType(conf.getModType());
		this.pgcon = pgcon;
		this.oDs = oDs;
		logger.debug("Read streems..");
		writebytes = 0;
		fullreadbytes = 0;
		// setResponsePacketId(EGTS_TRANSPORT_LAYER_MIN_TP_ADDRESS);
		while (true) {
			EgtsTransport egtsTransport = new EgtsTransport(iDs);
			if (!egtsTransport.isValid()) {
				raiseEgts(egtsTransport.getErrorCode(),
						egtsTransport.getErrorMessage());
			}
			message = egtsTransport.dump();
			debug();
			ptServices = egtsTransport.getPtServicesFrameData();
			message = ptServices.dump();
			debug();
			if (ptServices.isValid()) {
				message.append("Count of Service Record Frames : ").append(
						ptServices.getFdSdrSize());
				debug();
				for (int m = 0; m < ptServices.getFdSdrSize(); m++) {
					fdSdr = ptServices.getFdSdr(m);
					message = fdSdr.dump();
					debug();
					for (int l = 0; l < fdSdr.size(); l++) {
						serviceSubRecordData = fdSdr.getServiceSubRecordData(l);
						serviceSubRecordData.dump();
						subRecord = serviceSubRecordData.getSubRecord();
						message = subRecord.dump();
						debug();
						message = subRecord.getSubRecordTermIdentity().dump();
						debug();
						dasnUid = subRecord.getSubRecordTermIdentity()
								.getTerminalId();
						// запись
						if (subRecordResponse == null) {
							subRecordResponse = new SubRecord();
						}
					}
				}
				if (egtsResponse == null) {
					egtsResponse = new EgtsResponse();
				}
				;
				egtsResponse.setFramedRpid(egtsTransport.getPtPacketId());
				egtsResponse.setFramedProcResult(EGTS_PC_OK);
				egtsResponse.setDataRecordSst(fdSdr.getSrRecordSst());
				egtsResponse.setDataRecordRst(fdSdr.getSrRecordRst());
				// oDs.write(egtsResponse.getData());
			} else {
				raiseEgts(ptServices.getErrorCode(),
						ptServices.getErrorMessage());
			}
		}

	}

	private void debug() {
		logger.debug(message);
		message.setLength(0);
	}

	private void raiseEgts(int code, String info) {
		/*
		 * @ToDo: возврат кодов ошибок
		 */
		message.setLength(0);
		message.append("Код ошибки : ").append(code).append(" Информация : ")
				.append(info);
		logger.error(message);
		return;
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	@SuppressWarnings("unused")
	private void writeByte(int data) throws IOException {
		message.append("packet[").append(writebytes).append("] : ")
				.append(Integer.toHexString(data & 0xff));
		debug();
		oDs.writeByte(data & 0xff);
		writebytes++;
	}

	@SuppressWarnings("unused")
	private void writeData() throws IOException {
		// Сохраним в БД данные
		dataSensor.setDasnUid(dasnUid);
		;
		dataSensor.setDasnDatetime(dasnDatetime);
		dataSensor.setDasnLatitude(dasnLatitude);
		;
		dataSensor.setDasnLongitude(dasnLongitude);
		;
		dataSensor.setDasnSatUsed(dasnSatUsed);
		dataSensor.setDasnSog(dasnSog);
		dataSensor.setDasnCourse(dasnCourse);
		dataSensor.setDasnHgeo(dasnHgeo);
		dataSensor.setDasnHmet(dasnHmet);
		dataSensor.setDasnAdc(dasnAdc);
		dataSensor.setDasnGpio(dasnGpio);
		dataSensor.setDasnTemp(dasnTemp);

		//
		dataSensor.setDasnMacroId(dasnMacroId);
		dataSensor.setDasnMacroSrc(dasnMacroSrc);

		if (dasnStatus.equals(DATA_STATUS.OK)) {
			pgcon.setDataSensorValues(dataSensor);
			// Ответ блоку
			try {
				pgcon.addDataSensor();
				logger.debug("Writing Database : " + dasnUid);
			} catch (SQLException e) {
				logger.warn("Error Writing Database : " + e.getMessage());
			}
			oDs.write(READ_OK);
		} else {
			logger.debug("Данные не валидные : " + dasnUid);			
		}
		this.clear();
	}

}
