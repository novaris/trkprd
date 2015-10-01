package ru.novoscan.trkpd.resources;

import java.util.ArrayList;

public class SubRecordTermIdentity implements ModConstats {

	private ArrayList<Byte> data;

	private String terminalId;

	private String imei;

	private String imsi;

	private String langCode;

	private int networkId;

	private int bufferSize;

	private int homeDispatcherId;

	private String msisdn;

	private boolean preHomeDispatcherId;

	private boolean preImei;

	private boolean preImsi;

	private boolean preLangCode;

	private boolean preSsra;

	private boolean preNetworkId;

	private boolean preBufferSize;

	private boolean preMsisdn;

	private int size;

	private byte flags;

	private StringBuffer dump = new StringBuffer();

	public byte getFlags() {
		return flags;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

	public SubRecordTermIdentity(ArrayList<Byte> subRecordInfo) {
		clear();
		byte flags = subRecordInfo.get(4);
		for (int k = 0; k < this.getLenght(flags); k++) {
			this.add(subRecordInfo.get(k));
		}
		this.parse();
	}

	public void clear() {
		setPreHomeDispatcherIdentifier(false);
		setPreImei(false);
		setPreImsi(false);
		setPreLangCode(false);
		setPreSsra(false);
		setPreNetworkId(false);
		data = new ArrayList<Byte>();
		setSize(EGTS_SR_TERM_IDENTITY_MIN_LEN);
	}

	public String getTerminalId() {
		return terminalId;
	}

	public void setTerminalId(String terminalId) {
		this.terminalId = terminalId;
	}

	public void setFlags(Byte flags) {
		this.flags = flags;
		if ((flags & 0x01) == 1) {
			setPreHomeDispatcherIdentifier(true);
			setSize(getSize() + 2);
		}
		if (((flags >> 1) & 0x01) == 1) {
			setPreImei(true);
			setSize(getSize() + 15);
		}
		if (((flags >> 2) & 0x01) == 1) {
			setPreImsi(true);
			setSize(getSize() + 16);
		}
		if (((flags >> 3) & 0x01) == 1) {
			setPreLangCode(true);
			setSize(getSize() + 3);
		}
		if (((flags >> 4) & 0x01) == 1) {
			setPreSsra(true);
		}
		if (((flags >> 5) & 0x01) == 1) {
			setPreNetworkId(true);
			setSize(getSize() + 3);
		}
		if (((flags >> 6) & 0x01) == 1) {
			setPreBufferSize(true);
			setSize(getSize() + 2);
		}
		if (((flags >> 7) & 0x01) == 1) {
			setPreMsisdn(true);
			setSize(getSize() + 15);
		}

	}

	public boolean isPreHomeDispatcherIdentifier() {
		return preHomeDispatcherId;
	}

	public void setPreHomeDispatcherIdentifier(
			boolean preHomeDispatcherIdentifier) {
		this.preHomeDispatcherId = preHomeDispatcherIdentifier;
	}

	public boolean isPreImei() {
		return preImei;
	}

	public void setPreImei(boolean preImei) {
		this.preImei = preImei;
	}

	public boolean isPreImsi() {
		return preImsi;
	}

	public void setPreImsi(boolean preImsi) {
		this.preImsi = preImsi;
	}

	public boolean isPreLangCode() {
		return preLangCode;
	}

	public void setPreLangCode(boolean preLangCode) {
		this.preLangCode = preLangCode;
	}

	public boolean isPreSsra() {
		return preSsra;
	}

	public void setPreSsra(boolean preSsra) {
		this.preSsra = preSsra;
	}

	public boolean isPreNetworkId() {
		return preNetworkId;
	}

	public void setPreNetworkId(boolean preNetworkId) {
		this.preNetworkId = preNetworkId;
	}

	public boolean isPreBufferSize() {
		return preBufferSize;
	}

	public void setPreBufferSize(boolean preBufferSize) {
		this.preBufferSize = preBufferSize;
	}

	public boolean isPreMsisdn() {
		return preMsisdn;
	}

	public void setPreMsisdn(boolean preMsisdn) {
		this.preMsisdn = preMsisdn;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public StringBuffer dump() {
		dump.setLength(0);
		dump.append("Dump of Terminal Identity").append("\nTID\t\t: ")
				.append(getTerminalId()).append("\nFlags\t\t: ")
				.append(Integer.toBinaryString(getFlags()))
				.append("\nHDID\t\t: ").append(getHomeDispatcherId())
				.append("\nIMEI\t\t: ").append(getImei())
				.append("\nIMSI\t\t: ").append(getImsi())
				.append("\nLNGC\t\t: ").append(getLangCode())
				.append("\nNID\t\t: 0x")
				.append(Integer.toHexString(getNetworkId()))
				.append("\nBS\t\t: ").append(getBufferSize())
				.append("\nMSISDN\t\t: ").append(getMsisdn()).append("\n");
		return dump;
	}

	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public int getNetworkId() {
		return networkId;
	}

	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getHomeDispatcherId() {
		return homeDispatcherId;
	}

	public void setHomeDispatcherId(int homeDispatcherId) {
		this.homeDispatcherId = homeDispatcherId;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public int getLenght(byte flags) {
		setFlags(flags);
		return getSize();
	}

	public void add(Byte b) {
		data.add(b);
	}

	public void parse() {
		this.imei = "";
		this.imsi = "";
		this.langCode = "";
		this.msisdn = "";
		setTerminalId(String.valueOf((data.get(0) & 0xff)
				+ ((data.get(1) & 0xff) << 8) + ((data.get(2) & 0xff) << 16)
				+ ((data.get(3) & 0xff) << 24)));
		if (isPreHomeDispatcherIdentifier()) {
			setHomeDispatcherId((data.get(5) & 0xff) + ((data.get(6) & 0xff) << 8));
		}
		if (isPreImei()) {
			for (int k = 7; k < 22; k++) {
				this.imei = this.imei + data.get(k).toString();
			}
		}
		if (isPreImsi()) {
			for (int k = 22; k < 38; k++) {
				this.imsi = this.imsi + data.get(k).toString();
			}
		}
		if (isPreLangCode()) {
			for (int k = 38; k < 41; k++) {
				this.langCode = this.langCode + data.get(k).toString();
			}
		}
		if (isPreNetworkId()) {
			setNetworkId((data.get(41) & 0xff) + ((data.get(42) & 0xff) << 8)
					+ ((data.get(43) & 0xff) << 16));
		}
		if (isPreBufferSize()) {
			setBufferSize((data.get(44) & 0xff) + ((data.get(45)  & 0xff) << 8));
		}
		if (isPreMsisdn()) {
			for (int k = 46; k < 61; k++) {
				this.msisdn = this.msisdn + data.get(k).toString();
			}
		}

	}

}
