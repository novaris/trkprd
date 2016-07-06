package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModAzimut extends Terminal {
	private static final Logger logger = Logger.getLogger(ModAzimut.class);

	private int fullreadbytes = 0;

	private int readbytes;

	private byte[] packetData;

	private String navPacketType;

	private int navMsgLen;

	private int navBalance;

	private int navCRC;

	public ModAzimut(DatagramPacket dataPacket, DatagramSocket clientSocket,
			ModConfig conf, TrackPgUtils pgcon) {
		this.setDasnType(conf.getModType());
		logger.debug("Чтение потока..");
		conf.getMaxSize();
		// packetData = new byte[maxPacketSize];
		fullreadbytes = 0;
		readbytes = 0;
		parseHeader();
		for (int i = 0; i < navMsgLen; i++) {
			readByte();
		}
		//
		parsePacket();

	}

	public ModAzimut(DataInputStream inp, DataOutputStream out,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon) {
		conf.getMaxSize();
	}

	private void parsePacket() {
		int readData = 0;
		while (readData < navMsgLen) {
			// 9 байт заголовок
			parseBlokHeader();
			readData = readData + 9;
		}

	}

	private void parseBlokHeader() {
		// TODO Auto-generated method stub

	}

	private void parseHeader() {
		navPacketType = "";
		for (int i = 0; i < 2; i++) {
			navPacketType = navPacketType + (char) readByte();
		}
		if (navPacketType.equalsIgnoreCase("AZ5")) {
			dasnUid = "";
			for (int i = 0; i < 2; i++) {
				dasnUid = dasnUid + readValue(3);
			}
			logger.debug("DEV_ID : " + dasnUid);
			navMsgLen = readByte() & 0x09;
			logger.debug("MSG_LEN : " + navMsgLen);
			navBalance = readValue(3);
			logger.debug("BALANCE : " + navBalance);
			navCRC = readByte();
			int navCheckCRC = ModUtils.getCrc8Egts(packetData, 0, 12);
			logger.debug("CRC Header : " + navCRC + " CRC Check : "
					+ navCheckCRC);
			if (navCheckCRC != navCRC) {
				throw new RuntimeException("Incorrect packet CRC.");
			}
			;
		} else {
			throw new RuntimeException("Incorrect packet type : "
					+ navPacketType);
		}

	}

	public int getReadBytes() {
		return this.fullreadbytes;
	}

	private int readByte() {
		byte bread = packetData[readbytes];
		int packet = bread & 0xff;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet));
		readbytes++;
		fullreadbytes++;
		return packet;
	}

	private int readValue(int byteLength) {
		int data = 0;
		int len = byteLength - 1;
		for (int i = 0; i < byteLength; i++) {
			data = data + (readByte() << ((len - i) * 8));
		}
		return data;
	}

}
