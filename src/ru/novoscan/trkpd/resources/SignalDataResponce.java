package ru.novoscan.trkpd.resources;

public class SignalDataResponce {
	/*
	 * Структура ответа терминалов Навтелеком Сигнал
	 * cmd - строковая команда
	 * index - индекс
	 */
	private String cmd;

	private char index;

	public SignalDataResponce() {
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public char getIndex() {
		return index;
	}

	public void setIndex(char index) {
		this.index = index;
	}
}
