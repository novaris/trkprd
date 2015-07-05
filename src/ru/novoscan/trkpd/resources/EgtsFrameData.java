package ru.novoscan.trkpd.resources;

import java.util.ArrayList;


public class EgtsFrameData implements ModConstats {

	private int fdRpid;

	private int fdProcResult;

	private int fdSignatureLength;

	private ArrayList<Byte> fdSignature = new ArrayList<Byte>();

	private boolean valid = false;

	private ArrayList<ServiceDataRecord> serviceDataRecord = new ArrayList<ServiceDataRecord>();

	public ArrayList<ServiceDataRecord> getServiceDataRecord() {
		return serviceDataRecord;
	}

	public void setServiceDataRecord(
			ArrayList<ServiceDataRecord> serviceDataRecord) {
		this.serviceDataRecord = serviceDataRecord;
	}

	public void addServiceDataRecord(ServiceDataRecord serviceDataRecord) {
		this.serviceDataRecord.add(serviceDataRecord);
	}

	public int sizeServiceDataRecord(ServiceDataRecord serviceDataRecord) {
		return this.serviceDataRecord.size();
	}

	private int errorCode;

	private String errorMessage;

	public EgtsFrameData(byte[] ptFrameData, int ptFrameDataLength,
			int ptPacketType) {
		int k = 0;
		setValid(false);
		if (ptPacketType == EGTS_PT_RESPONSE) {
			setFdRpid(ptFrameData[k++] + ((ptFrameData[k++] << 8) & 0xff00));
			setFdProcResult(ptFrameData[k++]);
			setValid(true);
		} else if (ptPacketType == EGTS_PT_APPDATA) {
			setValid(true);
		} else if (ptPacketType == EGTS_PT_SIGNED_APPDATA) {
			setFdSignatureLength(ptFrameData[k++]);
			for (int i = 0; i < getFdSignatureLength(); i++) {
				fdSignature.add(ptFrameData[k++]);
			}
			setValid(true);
		} else {
			setErrorCode(EGTS_PC_SRVC_NFOUND);
			setErrorMessage("Тип сервиса не найден : " + ptPacketType);
			setValid(false);
		}
		if (isValid()) {
			ServiceDataRecord rd = new ServiceDataRecord(ptFrameData, k,
					(ptFrameDataLength - k));
			serviceDataRecord.add(rd);
		}
	}

	public EgtsFrameData() {
		
	}

	public void clear() {
		setValid(false);
	}

	public int getFdRpid() {
		return fdRpid;
	}

	public void setFdRpid(int fdRpid) {
		this.fdRpid = fdRpid;
	}

	public int getFdProcResult() {
		return fdProcResult;
	}

	public void setFdProcResult(int fdProcResult) {
		this.fdProcResult = fdProcResult;
	}

	public ArrayList<ServiceDataRecord> getFdSdr() {
		return serviceDataRecord;
	}

	public int getFdSdrSize() {
		return serviceDataRecord.size();
	}

	public ServiceDataRecord getFdSdr(int i) {
		return serviceDataRecord.get(i);
	}

	public void setFdSdr(ArrayList<ServiceDataRecord> fdSdr) {
		this.serviceDataRecord = fdSdr;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public int getFdSignatureLength() {
		return fdSignatureLength;
	}

	public void setFdSignatureLength(int fdSignatureLength) {
		this.fdSignatureLength = fdSignatureLength;
	}

	public String getFdSignatureData() {
		if (fdSignature.isEmpty()) {
			return "";
		}
		return String.valueOf(fdSignature);
	}

	public StringBuffer dump() {
		StringBuffer dumpData = new StringBuffer();
		return dumpData.append("\nResponse Packet ID\tRPID : ")
				.append(Integer.toString(getFdRpid()))
				.append("\nProcessing Result\tPR : ").append("0x")
				.append(Integer.toHexString(getFdProcResult()))
				.append("\nSignature Length\tSIGL : ")
				.append(Integer.toString(getFdSignatureLength()))
				.append("\nSignature Data\t\tSIGD : ").append("0x")
				.append(getFdSignatureData());
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getErrorCode() {
		return this.errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

}
