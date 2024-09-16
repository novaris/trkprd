package ru.novoscan.trkpd.exception;

public class BadModuleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final int modType;

	public int getModType() {
		return modType;
	}

	public BadModuleException(String message, int modType) {
		super(message);
		this.modType = modType;
	}

}
