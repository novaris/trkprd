package ru.novoscan.trkpd;

public class TrackVersion {
	public static final String svnRevision="2549";

    public static final String buildTimestamp="20161122-2216";

    public static final String projectVersion="1.5.0";
    
    public static final String osInfo="Linux 4.4.0-34-generic amd64";

	public static String getSvnVersion() {
		return svnRevision;
	}

	public static String getBuildDate() {
		return buildTimestamp;
	}

	public static String getVersion() {
		return projectVersion;
	}
	
	public static String getBuildOs() {
        return osInfo;
    }

}
