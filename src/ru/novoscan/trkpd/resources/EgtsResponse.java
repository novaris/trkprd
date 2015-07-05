package ru.novoscan.trkpd.resources;


public class EgtsResponse implements ModConstats {

	private final static EgtsTransport egtsTransport = new EgtsTransport();
		
    private final static EgtsFrameData frameData = new EgtsFrameData();
    
    private final static ServiceDataRecord dataRecord = new ServiceDataRecord();
    
    private static int pid = -1;

    public EgtsResponse() {
    	egtsTransport.setPtVersion(EGTS_PROTOCOL_VERSION);
    	egtsTransport.setPtSecurityKeyId(EGTS_PT_NOTSKID);
    	egtsTransport.setPtPriority(EGTS_PT_PRIORITY_NORMAL);
    	egtsTransport.setPtEncryptionAlgorithm(EGTS_PT_NOTCRYPT);
    	egtsTransport.setPtPacketType(EGTS_PT_RESPONSE);
    	egtsTransport.setPtHeaderEncoding(EGTS_PT_NOTENCODING);
    	egtsTransport.setPtPrefix(EGTS_PT_PREF);
    	egtsTransport.setPtPacketId(getPacketId());
    	
    	//
    	dataRecord.setPreRecordEvid(false);
    	dataRecord.setPreRecordOid(false);
    	dataRecord.setPreRecordTm(false);
    	dataRecord.setSrRecordPriority(EGTS_PT_PRIORITY_NORMAL);
    }
	public byte[] getData() {
		// TODO Auto-generated method stub
		return null;
	}
	public void setPacketInfo(int ptPacketId) {
		egtsTransport.setPtPacketId(ptPacketId);
	}
	public void setFramedRpid(int fdRpid) {
		frameData.setFdRpid(fdRpid);
	}
	public void setFramedProcResult(int fdProcResult) {
		frameData.setFdProcResult(fdProcResult);
	}
	public void setDataRecordRst(int srRecordRst) {
		dataRecord.setSrRecordRst(srRecordRst);
	}
	public void setDataRecordSst(int srRecordSst) {
		dataRecord.setSrRecordSst(srRecordSst);
	}
	public void setDataRecordPriority(int srRecordPriority) {
		dataRecord.setSrRecordPriority(srRecordPriority);
	}
	private int getPacketId() {
		if (pid < 0) {
			pid = 0;
		} else if (pid < EGTS_MAX_COUNT) {
			pid = pid + 1;
		} else {
			pid = 0;
		}
		return pid;
	}
}
