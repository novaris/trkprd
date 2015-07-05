package ru.novoscan.trkpd.resources;

import java.util.ArrayList;

public class ServiceSubRecordData implements ModConstats {
	private ArrayList<Byte> subRecordsData = new ArrayList<Byte>();

	private int srRecordRst;

	private int subRecordLength;

	private StringBuffer dump = new StringBuffer();

	private SubRecord subRecord;

	public ServiceSubRecordData(byte[] ptFrameData, int recordSeek,
			int subRecordLength, int srRecordRst) {
		this.setSrRecordRst(srRecordRst);
		this.setSubRecordLength(subRecordLength);
		int k = recordSeek;
		for (int m = 0; m < subRecordLength; m++) {
			this.add(ptFrameData[k++]);
		}
		subRecord = new SubRecord(subRecordsData, srRecordRst);
	}

	public ArrayList<Byte> getSubRecordsData() {
		return subRecordsData;
	}

	public void setSubRecordsData(ArrayList<Byte> subRecordsData) {
		this.subRecordsData = subRecordsData;
	}

	private void add(byte b) {
		subRecordsData.add(b);
	}

	public void clear() {
		subRecordsData.clear();
	}

	public int getSrRecordRst() {
		return srRecordRst;
	}

	public void setSrRecordRst(int srRecordRst) {
		this.srRecordRst = srRecordRst;
	}

	public int getSubRecordLength() {
		return subRecordLength;
	}

	public void setSubRecordLength(int subRecordLength) {
		this.subRecordLength = subRecordLength;
	}

	public StringBuffer dump() {
		dump.setLength(0);
		dump.append("\nSub Service Data Record Dump").append("\nSRL\t\t: ")
				.append(this.getSubRecordLength());
		return dump;

	}

	public SubRecord getSubRecord() {
		return subRecord;
	}

	public void setSubRecord(SubRecord subRecord) {
		this.subRecord = subRecord;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}


}
