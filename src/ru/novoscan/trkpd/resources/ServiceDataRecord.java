package ru.novoscan.trkpd.resources;

import java.util.ArrayList;

public class ServiceDataRecord {
	private int srRecordLength;

	public int getSrRecordLength() {
		return srRecordLength;
	}

	private int srRecordNumber;

	public int getSrRecordNumber() {
		return srRecordNumber;
	}

	private byte srRecordFlags = 0x00;

	private int srRecordOid = 0;

	private int srRecordEvid = 0;

	private int srRecordTm = 0;

	private boolean preRecordOid = false;

	private boolean preRecordEvid = false;

	private boolean preRecordTm = false;

	private int srRecordRst;

	private int srRecordSst;

	private int srRecordRsod;

	private int srRecordSsod;

	private int srRecordPriority;

	private int srRecordGroup;

	private StringBuffer dump = new StringBuffer();

	private ArrayList<ServiceSubRecordData> serviceSubRecordData = new ArrayList<ServiceSubRecordData>();

	public byte getSrRecordFlags() {
		return srRecordFlags;
	}

	public ServiceDataRecord(byte[] ptFrameData, int fdHeadLength,
			int fdDataLength) {
		int k = fdHeadLength;
		serviceSubRecordData.clear();
		while (k < fdDataLength) {

			this.setSrRecordLength(ptFrameData[k++]
					+ ((ptFrameData[k++] << 8) & 0xff00));
			this.setSrRecordNumber(ptFrameData[k++]
					+ ((ptFrameData[k++] << 8) & 0xff00));
			this.setSrRecordFlags(ptFrameData[k++]);
			if (this.isPreRecordOid()) {
				this.setSrRecordOid(ptFrameData[k++] + (ptFrameData[k++] << 8)
						+ (ptFrameData[k++] << 16) + (ptFrameData[k++] << 24));
			}
			if (this.isPreRecordEvid()) {
				this.setSrRecordEvid(ptFrameData[k++] + (ptFrameData[k++] << 8)
						+ (ptFrameData[k++] << 16) + (ptFrameData[k++] << 24));
			}
			if (this.isPreRecordTm()) {
				this.setSrRecordTm(ptFrameData[k++] + (ptFrameData[k++] << 8)
						+ (ptFrameData[k++] << 16) + (ptFrameData[k++] << 24));
			}
			this.setSrRecordSst(ptFrameData[k++]);
			this.setSrRecordRst(ptFrameData[k++]);
			ServiceSubRecordData subRecord = new ServiceSubRecordData(
					ptFrameData, k, this.getSrRecordLength(),
					this.getSrRecordRst());
			if (subRecord.isValid()) {
				this.addSubRecordData(subRecord);
			}

			k = k + this.getSrRecordLength();

		}

	}

	public ServiceDataRecord() {
	}

	private void addSubRecordData(ServiceSubRecordData subRecord) {
		serviceSubRecordData.add(subRecord);
	}

	public void setSrRecordLength(int srRecordLength) {
		this.srRecordLength = srRecordLength;

	}

	public void setSrRecordNumber(int srRecordNumber) {
		this.srRecordNumber = srRecordNumber;

	}

	public void setSrRecordFlags(byte srRecordFlags) {
		this.srRecordFlags = srRecordFlags;
		if ((srRecordFlags & 0x01) == 1) {
			setPreRecordOid(true);
		}
		if (((srRecordFlags >> 1) & 0x01) == 1) {
			setPreRecordEvid(true);
		}
		if (((srRecordFlags >> 2) & 0x01) == 1) {
			setPreRecordTm(true);
		}
		setSrRecordPriority((srRecordFlags >> 3) & 0x02);
		setSrRecordGroup((srRecordFlags >> 5) & 0x01);
		setSrRecordRsod((srRecordFlags >> 6) & 0x01);
		setSrRecordSsod((srRecordFlags >> 7) & 0x01);

	}

	public int getSrRecordSsod() {
		return srRecordSsod;
	}

	public void setSrRecordSsod(int srRecordSsod) {
		this.srRecordSsod = srRecordSsod;
	}

	public boolean isPreRecordOid() {
		return preRecordOid;
	}

	public void setPreRecordOid(boolean preRecordOid) {
		this.preRecordOid = preRecordOid;
	}

	public boolean isPreRecordEvid() {
		return preRecordEvid;
	}

	public void setPreRecordEvid(boolean preRecordEvid) {
		this.preRecordEvid = preRecordEvid;
	}

	public boolean isPreRecordTm() {
		return preRecordTm;
	}

	public void setPreRecordTm(boolean preRecordTm) {
		this.preRecordTm = preRecordTm;
	}

	public int getSrRecordPriority() {
		return srRecordPriority;
	}

	public void setSrRecordPriority(int srRecordPriority) {
		this.srRecordPriority = srRecordPriority;
	}

	public int getSrRecordGroup() {
		return srRecordGroup;
	}

	public void setSrRecordGroup(int srRecordGroup) {
		this.srRecordGroup = srRecordGroup;
	}

	public int getSrRecordOid() {
		return srRecordOid;
	}

	public void setSrRecordOid(int srRecordOid) {
		this.srRecordOid = srRecordOid;
	}

	public int getSrRecordEvid() {
		return srRecordEvid;
	}

	public void setSrRecordEvid(int srRecordEvid) {
		this.srRecordEvid = srRecordEvid;
	}

	public int getSrRecordTm() {
		return srRecordTm;
	}

	public void setSrRecordTm(int srRecordTm) {
		this.srRecordTm = srRecordTm;
	}

	public int getSrRecordRst() {
		return srRecordRst;
	}

	public void setSrRecordRst(int srRecordRst) {
		this.srRecordRst = srRecordRst;
	}

	public int getSrRecordSst() {
		return srRecordSst;
	}

	public void setSrRecordSst(int srRecordSst) {
		this.srRecordSst = srRecordSst;
	}

	public StringBuffer dump() {
		dump.setLength(0);
		dump.append("\nService Data Record Dump")
				.append("\nRecord Length\t\t\tRL : ")
				.append(getSrRecordLength())
				.append("\nRecord Number\t\t\tRN : ")
				.append(getSrRecordNumber()).append("\nRecord Flags\t\t\tRF: ")
				.append(Integer.toBinaryString(getSrRecordFlags()))
				.append("\nObject Identifier\t\tOID : ")
				.append(getSrRecordOid())
				.append("\nEvent Identifier\t\tEVID : ")
				.append(getSrRecordEvid()).append("\nTime\t\t\t\tTM : ")
				.append(getSrRecordTm())
				.append("\nSource Service Type\t\tSST : ")
				.append(getSrRecordSst())
				.append("\nRecipient Service Type\t\tRST : ")
				.append(getSrRecordRst())
				.append("\nSource Service On Device\tSSOD : ")
				.append(getSrRecordSsod())
				.append("\nRecipient Service On Device\tRSOD : ")
				.append(getSrRecordRsod())
				.append("\nRecord Processing Priority\tPRI : ")
				.append(getSrRecordPriority()).append("\nGROUP\t\t\t\tGRP : ")
				.append(getSrRecordGroup());
		return dump;
	}

	public int getSrRecordRsod() {
		return srRecordRsod;
	}

	public void setSrRecordRsod(int srRecordRsod) {
		this.srRecordRsod = srRecordRsod;
	}

	public ServiceSubRecordData getServiceSubRecordData(int i) {
		return serviceSubRecordData.get(i);
	}

	public int size() {
		return serviceSubRecordData.size();
	}

	public void setServiceSubRecordData(
			ArrayList<ServiceSubRecordData> serviceSubRecordData) {
		this.serviceSubRecordData = serviceSubRecordData;
	}

}
