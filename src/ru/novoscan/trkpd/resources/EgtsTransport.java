/**-
 * 
 */
package ru.novoscan.trkpd.resources;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.utils.ModUtils;

/**
 * @author kurensky
 * 
 */
public class EgtsTransport implements ModConstats {

	private static Logger logger = Logger.getLogger(EgtsTransport.class);

	private static StringBuffer message = new StringBuffer();

	private int ptVersion = EGTS_PROTOCOL_VERSION;

	private int ptSecurityKeyId = EGTS_PT_NOTSKID;

	private int ptPrefix = EGTS_PT_PREF;

	private int ptRoute = EGTS_PT_NOTROUTE;

	private int ptEncryptionAlgorithm = EGTS_PT_NOTCRYPT;

	private int ptCompressed = EGTS_PT_NOTCOMPRES;

	private int ptPriority = EGTS_PT_PRIORITY_LOW;

	private int ptHeaderLength = EGTS_TRANSPORT_LAYER_MAX_HEADER_LEN;

	private int ptHeaderEncoding;

	private int ptFrameDataLength;

	private int ptPacketId = 0;

	private int ptPacketType;

	private int ptPeerAddress;

	private int ptRecipientAddress;

	private int ptTimeToLive;

	private int ptHeaderCheckSum;

	private EgtsFrameData ptServicesFrameData;

	private int ptFrameDataCheckSum = EGTS_UNDEFINE;

	private int ptFrameDataCheckSumReal = EGTS_UNDEFINE;

	// private int[] egtsData;
	private DataInputStream iDs;

	private int byteCount;

	private boolean valid;

	private int errorCode;

	private byte[] serviceFrameData = new byte[EGTS_MAX_PACKET_LENGTH];

	private String errorMessage;

	private byte[] ptHeader = new byte[EGTS_TRANSPORT_LAYER_MAX_HEADER_LEN];

	private int ptHeaderCheckSumModReal;

	public EgtsTransport(DataInputStream iDs) throws IOException {
		this.iDs = iDs;
		parsePacket();
	}

	public EgtsTransport() {
	}

	private void parsePacket() throws IOException {
		setByteCount(0);
		setPtVersion(readByte());
		setPtSecurityKeyId(readByte());
		/* 3 байт */
		int b = readByte();
		setPtPrefix(b >> 6);
		setPtRoute((b >> 5) & 0x01);
		setPtEncryptionAlgorithm((b >> 3) & 0x02);
		setPtCompressed((b >> 2) & 0x01);
		setPtPriority(b & 0x02);

		setPtHeaderLength(readByte() - EGTS_TP_HEADER_CRC_LEN);
		setPtHeaderEncoding(readByte());
		setPtFrameDataLength(readByte() + ((readByte() << 8) & 0xff00));
		setPtPacketId(readByte() + ((readByte() << 8) & 0xff00));
		setPtPacketType(readByte());

		if (getPtHeaderLength() == EGTS_TRANSPORT_LAYER_MAX_HEADER_LEN) {
			setPtPeerAddress(readByte() + ((readByte() << 8) & 0xff00));
			setPtRecipientAddress(readByte() + ((readByte() << 8) & 0xff00));
			setPtTimeToLive(readByte());
		}
		setPtHeaderCheckSum(readByte());
		if (getPtFrameDataLength() > 0) {
			for (int i = 0; i < getPtFrameDataLength(); i++) {
				serviceFrameData[i] = (byte) readByte();
			}
			setPtFrameData(new EgtsFrameData(serviceFrameData,
					getPtFrameDataLength(), getPtPacketType()));
			setPtFrameDataCheckSum(readByte() + ((readByte() << 8) & 0xff00));
			setPtFrameDataCheckSumReal(ModUtils.getCrc16(serviceFrameData, 0,
					getPtFrameDataLength()));
		}
		setPtHeaderCheckSumModReal(ModUtils.getCrc8Egts(ptHeader, 0,
				getPtHeaderLength()));
		// выполняем проверки
		checkEgtsTransport();
	}

	private void checkEgtsTransport() {
		// TODO Auto-generated method stub
		if (getPtVersion() != EGTS_PROTOCOL_VERSION) {
			/* Поддерживаем только 1 версию. */
			setErrorCode(EGTS_PC_UNS_PROTOCOL, "Не поддерживаемый протокол : "
					+ getPtVersion());
		} else if (getPtSecurityKeyId() != EGTS_PT_NOTSKID) {
			setErrorCode(EGTS_PC_DECRYPT_ERROR,
					"Не поддерживается шифрование : " + getPtSecurityKeyId());
		} else if (getPtPrefix() != EGTS_PT_PREF) {
			setErrorCode(EGTS_PC_UNS_PROTOCOL, "Префикс некорректен : "
					+ getPtPrefix());
		} else if (getPtRoute() != EGTS_PT_NOTROUTE) {
			setErrorCode(EGTS_PC_INC_DATAFORM,
					"Маршрутизация не поддерживается : " + getPtRoute());
		} else if (getPtEncryptionAlgorithm() != EGTS_PT_NOTCRYPT) {
			setErrorCode(EGTS_PC_DECRYPT_ERROR,
					"Шифрование не поддерживается : "
							+ getPtEncryptionAlgorithm());
		} else if (getPtCompressed() != EGTS_PT_NOTCOMPRES) {
			setErrorCode(EGTS_PC_INC_DATAFORM, "Сжатие не поддерживается : "
					+ getPtCompressed());
		} else if ((getPtHeaderLength() >= EGTS_TRANSPORT_LAYER_MIN_HEADER_LEN)
				&& (getPtHeaderLength() <= EGTS_TRANSPORT_LAYER_MAX_HEADER_LEN)) {
			setErrorCode(EGTS_PC_INC_HEADERFORM, "Неверный размер заголовка : "
					+ getPtHeaderLength());
		} else if ((getPtPacketType() != EGTS_PT_APPDATA)
				&& (getPtPacketType() != EGTS_PT_SIGNED_APPDATA)) {
			setErrorCode(EGTS_PC_UNS_TYPE, "Неверный тип пакета : "
					+ getPtPacketType());
		} else if (getPtHeaderCheckSum() != getPtHeaderCheckSumModReal()) {
			setErrorCode(EGTS_PC_HEADERCRC_ERROR,
					"Неверная контрольная сумма заголовка : "
							+ getPtHeaderCheckSumModReal());
		} else {
			setValid(true);
			message.append("Транспортный пакет валидный.");
			debug();
		}
	}

	private void setValid(boolean b) {
		this.valid = b;
	}

	public void clean() {
		ptVersion = EGTS_PROTOCOL_VERSION;
		ptSecurityKeyId = EGTS_PT_NOTSKID;
		ptPrefix = EGTS_PT_PREF;
		ptRoute = EGTS_PT_NOTROUTE;
		ptEncryptionAlgorithm = EGTS_PT_NOTCRYPT;
		ptCompressed = EGTS_PT_NOTCOMPRES;
		ptPriority = EGTS_PT_PRIORITY_LOW;
		ptHeaderLength = EGTS_UNDEFINE;
		ptHeaderEncoding = EGTS_PT_NOTENCODING;
		ptFrameDataLength = EGTS_UNDEFINE;
		ptPacketId = EGTS_UNDEFINE;
		ptPacketType = EGTS_UNDEFINE;
		ptPeerAddress = EGTS_UNDEFINE;
		ptRecipientAddress = EGTS_UNDEFINE;
		ptTimeToLive = EGTS_UNDEFINE;
		ptHeaderCheckSum = EGTS_UNDEFINE;
		ptServicesFrameData.clear();
		setPtFrameDataCheckSum(EGTS_UNDEFINE);
		setByteCount(0);
		valid = false;
	}

	public int getPtVersion() {
		return ptVersion;
	}

	public void setPtVersion(int ptVersion) {
		this.ptVersion = ptVersion;
	}

	public int getPtSecurityKeyId() {
		return ptSecurityKeyId;
	}

	public void setPtSecurityKeyId(int ptSecurityKeyId) {
		this.ptSecurityKeyId = ptSecurityKeyId;
	}

	public int getPtPrefix() {
		return ptPrefix;
	}

	public void setPtPrefix(int ptPrefix) {
		this.ptPrefix = ptPrefix;
	}

	public int getPtRoute() {
		return ptRoute;
	}

	public void setPtRoute(int ptRoute) {
		this.ptRoute = ptRoute;
	}

	public int getPtEncryptionAlgorithm() {
		return ptEncryptionAlgorithm;
	}

	public void setPtEncryptionAlgorithm(int ptEncryptionAlgorithm) {
		this.ptEncryptionAlgorithm = ptEncryptionAlgorithm;
	}

	public int getPtCompressed() {
		return ptCompressed;
	}

	public void setPtCompressed(int ptCompressed) {
		this.ptCompressed = ptCompressed;
	}

	public int getPtPriority() {
		return ptPriority;
	}

	public void setPtPriority(int ptPriority) {
		this.ptPriority = ptPriority;
	}

	public int getPtHeaderLength() {
		return ptHeaderLength;
	}

	public int getPtHeaderLengthCrc() {
		return ptHeaderLength + EGTS_TP_HEADER_CRC_LEN;
	}

	public void setPtHeaderLength(int ptHeaderLength) {
		this.ptHeaderLength = ptHeaderLength;
	}

	public int getPtHeaderEncoding() {
		return ptHeaderEncoding;
	}

	public void setPtHeaderEncoding(int ptHeaderEncoding) {
		this.ptHeaderEncoding = ptHeaderEncoding;
	}

	public int getPtFrameDataLength() {
		return ptFrameDataLength;
	}

	public void setPtFrameDataLength(int ptFrameDataLength) {
		this.ptFrameDataLength = ptFrameDataLength;
	}

	public int getPtPacketId() {
		return ptPacketId;
	}

	public void setPtPacketId(int ptPacketId) {
		this.ptPacketId = ptPacketId;
	}

	public int getPtPacketType() {
		return ptPacketType;
	}

	public void setPtPacketType(int ptPacketType) {
		this.ptPacketType = ptPacketType;
	}

	public int getPtPeerAddress() {
		return ptPeerAddress;
	}

	public void setPtPeerAddress(int ptPeerAddress) {
		this.ptPeerAddress = ptPeerAddress;
	}

	public int getPtRecipientAddress() {
		return ptRecipientAddress;
	}

	public void setPtRecipientAddress(int ptRecipientAddress) {
		this.ptRecipientAddress = ptRecipientAddress;
	}

	public int getPtTimeToLive() {
		return ptTimeToLive;
	}

	public void setPtTimeToLive(int ptTimeToLive) {
		this.ptTimeToLive = ptTimeToLive;
	}

	public int getPtHeaderCheckSum() {
		return ptHeaderCheckSum;
	}

	public void setPtHeaderCheckSum(int ptHeaderCheckSum) {
		this.ptHeaderCheckSum = ptHeaderCheckSum;
	}

	public EgtsFrameData getPtServicesFrameData() {
		return ptServicesFrameData;
	}

	public void setPtFrameData(EgtsFrameData ptServicesFrameData) {
		this.ptServicesFrameData = ptServicesFrameData;
	}

	public void getNextPacketId() {
		if (ptPacketId > EGTS_TRANSPORT_LAYER_MAX_TP_ADDRESS) {
			ptPacketId = 0;
		} else {
			ptPacketId++;
		}
	}

	private int readByte() throws IOException {
		byte bread = iDs.readByte();
		if (getByteCount() < getPtHeaderLength()) {
			ptHeader[getByteCount()] = bread;
		}
		setByteCount(getByteCount() + 1);
		logger.debug("byte [" + getByteCount() + "] : "
				+ Integer.toHexString(bread & 0xff));
		return bread & 0xff;
	}

	public StringBuffer dump() {
		/*
		 * Protocol Version PRV: 01h Security Key ID SKID: 00h Prefix PRF: 00h
		 * Route RTE: 0 Encryption Algorithm ENA: 00h Compressed CMP: 0 Priority
		 * PR: 0 Header Length HL: 11 Header Encoding HE: 00h Frame Data Length
		 * FDL: 46 Packet Identifier PID: 1 Packet Type PT: 0001h Peer Address
		 * PRA: - Recipient Address RCA: - Time To Live TTL: - Header Check Sum
		 * HCS: 2Dh
		 */
		StringBuffer dumpData = new StringBuffer();
		return dumpData.append("\nProtocol Version\tPRV : ").append("0x")
				.append(Integer.toHexString(getPtVersion()))
				.append("\nPrefix\t\t\tPRF : ").append("0x")
				.append(Integer.toHexString(getPtPrefix()))
				.append("\nRoute\t\t\tRTE : ")
				.append(Integer.toString(getPtRoute()))
				.append("\nEncryption Algorithm\tENA : ").append("0x")
				.append(Integer.toHexString(getPtEncryptionAlgorithm()))
				.append("\nCompressed\t\tCMP : ")
				.append(Integer.toString(getPtEncryptionAlgorithm()))
				.append("\nPriority\t\tPR : ").append("0x")
				.append(Integer.toHexString(getPtPriority()))
				.append("\nHeader Length\t\tHL : ")
				.append(Integer.toString(getPtHeaderLength()))
				.append("\nHeader Encoding\t\tHE : ").append("0x")
				.append(Integer.toHexString(getPtHeaderEncoding()))
				.append("\nFrame Data Length\tFDL : ")
				.append(Integer.toString(getPtFrameDataLength()))
				.append("\nPacket Identifier\tPID : ")
				.append(Integer.toString(getPtPacketId()))
				.append("\nPacket Type\t\tPT : ").append("0x")
				.append(Integer.toHexString(getPtPacketType()))
				.append("\nPeer Address\t\tPRA : ").append("0x")
				.append(Integer.toHexString(getPtPeerAddress()))
				.append("\nRecipient Address\tRCA : ").append("0x")
				.append(Integer.toHexString(getPtRecipientAddress()))
				.append("\nTime To Live\t\tTTL : ").append("0x")
				.append(Integer.toHexString(getPtTimeToLive()))
				.append("\nHeader Check Sum\tHCS : ").append("0x")
				.append(Integer.toHexString(getPtHeaderCheckSum()));
	}

	public boolean isValid() {
		return this.valid;
	}

	public int getErrorCode() {
		return this.errorCode;
	}

	private void setErrorCode(int errorCode, String errorMessage) {
		message.append("Ошибка [").append(errorCode).append("] : ")
				.append(errorMessage);
		debug();
		this.errorCode = errorCode;
		this.setErrorMessage(errorMessage);
		this.valid = false;
	}

	public int getPtFrameDataCheckSum() {
		return ptFrameDataCheckSum;
	}

	public void setPtFrameDataCheckSum(int ptFrameDataCheckSum) {
		this.ptFrameDataCheckSum = ptFrameDataCheckSum;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getByteCount() {
		return byteCount;
	}

	private void setByteCount(int byteCount) {
		this.byteCount = byteCount;
	}

	public int getPtHeaderCheckSumModReal() {
		return ptHeaderCheckSumModReal;
	}

	public void setPtHeaderCheckSumModReal(int ptHeaderCheckSumModReal) {
		this.ptHeaderCheckSumModReal = ptHeaderCheckSumModReal;
	}

	public int getPtFrameDataCheckSumReal() {
		return ptFrameDataCheckSumReal;
	}

	public void setPtFrameDataCheckSumReal(int ptFrameDataCheckSumReal) {
		this.ptFrameDataCheckSumReal = ptFrameDataCheckSumReal;
	}

	private void debug() {
		logger.debug(message);
		message.setLength(0);
	}
}
