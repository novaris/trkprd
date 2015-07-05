package ru.novoscan.trkpd.resources;

import java.util.ArrayList;

public class SubRecord implements ModConstats {

	private int type;

	private int length;

	private boolean valid;

	private SubRecordTermIdentity subRecordTermIdentity;

	private ArrayList<Byte> subRecordInfo = new ArrayList<Byte>();

	private int srRecordRst;

	private StringBuffer dump = new StringBuffer();

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public SubRecord(ArrayList<Byte> subRecordsData, int srRecordRst) {
		this.srRecordRst = srRecordRst;
		this.subRecordInfo.clear();
		int i = 0;
		while (i < subRecordsData.size()) {
			this.setType(subRecordsData.get(i++));
			this.setLength(subRecordsData.get(i++)
					+ ((subRecordsData.get(i++) << 8) & 0xff00));
			for (int k = 0; k < this.getLength(); k++) {
				subRecordInfo.add(subRecordsData.get(i++));
			}
			this.parse();
		}
	}

	public SubRecord() {
		
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public void parse() {
		/* TODO парсинг всех сервисов */
		if (srRecordRst == EGTS_AUTH_SERVICE) {
			//
			if (getType() == EGTS_SR_TERM_IDENTITY) {
				setSubRecordTermIdentity(new SubRecordTermIdentity(
						subRecordInfo));
			}
		} else if (srRecordRst == EGTS_TELEDATA_SERVICE) {
			// Навигация
		}

	}

	public boolean isValid() {
		return valid;
	}

	public SubRecordTermIdentity getSubRecordTermIdentity() {
		return subRecordTermIdentity;
	}

	public void setSubRecordTermIdentity(
			SubRecordTermIdentity subRecordTermIdentity) {
		this.subRecordTermIdentity = subRecordTermIdentity;
	}

	public StringBuffer dump() {
		dump.setLength(0);
		dump.append("\nSR Type\t:").append(getType());
		return dump;

	}

}
