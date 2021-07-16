package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModScout  extends Terminal {
	/*
	 * <?xml version="1.0" encoding="utf-8"?> <xs:schema
	 * targetNamespace="https://novoscan.ru/utils/trkprd/mod/scout-mt500"
	 * xmlns:tn="https://novoscan.ru/utils/trkprd/mod/scout-mt500"
	 * xmlns:xs="http://www.w3.org/2001/XMLSchema"
	 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation=
	 * "http://www.w3.org/2001/XMLSchema http://www.w3.org/2001/XMLSchema.xsd">
	 * 
	 * <xs:annotation> <xs:documentation xml:lang="ru"> Описание протокола Scout
	 * MT-510 Series. Протокол определяет взаимодействие СКАУТ сервера и
	 * терминалов. При передаче многобайтовых чисел по TCP/IP соединению первым
	 * передается старший байт. Элементы следуют в порядке их описания
	 * </xs:documentation> </xs:annotation>
	 * 
	 * <xs:complexType name="packet"> <xs:annotation> <xs:documentation
	 * xml:lang="ru"> Общий формат пакетов. Определяет заголовок
	 * </xs:documentation> </xs:annotation> <xs:sequence> <xs:element
	 * name="opCode" type="xs:unsignedByte"> <xs:annotation> <xs:documentation
	 * xml:lang="ru"> Код операции. Используется для определения типа пакета и
	 * его размера </xs:documentation> </xs:annotation> </xs:element>
	 * <xs:element name="serialId" type="xs:unsignedShort"/> <xs:any/>
	 * </xs:sequence> </xs:complexType>
	 * 
	 * <xs:complexType name="angularCoordinate"> <xs:annotation>
	 * <xs:documentation xml:lang="ru"> Формат представления географической
	 * угловой координаты. Итоговое значение координаты складывается из значений
	 * трех байт по формуле total = degree + minute/60 + hundredth/6000
	 * </xs:documentation> </xs:annotation> <xs:sequence> <xs:element
	 * name="degree" type="xs:byte"/> <xs:element name="minute" type="xs:byte"/>
	 * <xs:element name="hundredth" type="xs:byte"/> <xs:any/> </xs:sequence>
	 * </xs:complexType>
	 * 
	 * <xs:complexType name="recorderRequest"> <xs:annotation> <xs:documentation
	 * xml:lang="ru"> Пакет передается от терминала к серверу и сообщает текущее
	 * состояние терминала. Код операции 1, размер пакета 26 байт
	 * </xs:documentation> </xs:annotation> <xs:complexContent> <xs:restriction
	 * base="tn:packet"> <xs:sequence> <xs:element name="opCode">
	 * <xs:simpleType> <xs:restriction base="xs:unsignedByte"> <xs:enumeration
	 * value="1"/> </xs:restriction> </xs:simpleType> </xs:element> <xs:element
	 * name="serialId" type="xs:unsignedShort"/> <xs:element name="stat1"
	 * type="xs:unsignedByte"/> <xs:element name="stat0"
	 * type="xs:unsignedByte"/> <xs:element name="unknown0"
	 * type="xs:unsignedShort"/> <xs:element name="ticks" type="xs:unsignedInt">
	 * <xs:annotation> <xs:documentation xml:lang="ru"> Некоторая величина,
	 * монотонно возрастающая с течением времени. Экспериментально установлено,
	 * что за секунду увеличивается примерно на 50-80 для разных блоков
	 * </xs:documentation> </xs:annotation> </xs:element> <xs:element
	 * name="latitude" type="tn:angularCoordinate"/> <xs:element
	 * name="longitude" type="tn:angularCoordinate"/> <xs:element name="dio"
	 * type="xs:unsignedByte"/> <xs:element name="unknown1"
	 * type="xs:unsignedByte"/> <xs:element name="adc1" type="xs:unsignedByte"/>
	 * <xs:element name="adc0" type="xs:unsignedByte"/> <xs:element
	 * name="unknown2" type="xs:unsignedByte"/> <xs:element name="speed"
	 * type="xs:unsignedByte"/> <xs:element name="course"
	 * type="xs:unsignedShort"/> <xs:element name="unknown3"
	 * type="xs:unsignedByte"/> </xs:sequence> </xs:restriction>
	 * </xs:complexContent> </xs:complexType>
	 * 
	 * <xs:complexType name="recorderResponse"> <xs:annotation>
	 * <xs:documentation xml:lang="ru"> Пакет передается от сервера к терминалу
	 * в ответ на recorderRequest. Код операции 242, размер пакета 6 байт
	 * </xs:documentation> </xs:annotation> <xs:complexContent> <xs:restriction
	 * base="tn:packet"> <xs:sequence> <xs:element name="opCode">
	 * <xs:simpleType> <xs:restriction base="xs:unsignedByte"> <xs:enumeration
	 * value="242"/> </xs:restriction> </xs:simpleType> </xs:element>
	 * <xs:element name="serialId" type="xs:unsignedShort"/> <xs:element
	 * name="ticks"> <xs:annotation> <xs:documentation xml:lang="ru"> Некоторая
	 * величина, монотонно возрастающая с течением времени и сбрасываемая в ноль
	 * при переходе через максимум трехбайтового целого. Экспериментально
	 * установлено, что за секунду увеличивается примерно на 940-980, но в
	 * среднем не достигает максимума 1000, необходимого для признания данной
	 * величины миллисекундами </xs:documentation> </xs:annotation>
	 * <xs:simpleType> <xs:restriction base="xs:hexBinary"> <xs:length
	 * value="3"/> </xs:restriction> </xs:simpleType> </xs:element>
	 * </xs:sequence> </xs:restriction> </xs:complexContent> </xs:complexType>
	 * </xs:schema>
	 */
	private float fullreadbytes = 0;

	private float packets = 0;

	private int readbytes = 0;

	private int scoutPacketType = 0;

	// private int scoutPacketSize;
	// private TRKUtils utl = new TRKUtils();

	private int[] packet;

	private int[] scoutReply;

	public ModScout(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws IOException {
		this.setDasnType(conf.getModType());
		int cread;
		String slog = "";
		packet = new int[conf.getMaxSize()];

		while ((cread = unbconsole.read()) != -1) {
			logger.debug("Read " + Integer.toHexString(cread));
			packet[readbytes] = cread;
			fullreadbytes = fullreadbytes + 1;
			readbytes = readbytes + 1;
			slog = slog + Integer.toHexString(cread);
			if (readbytes == 1) {
				scoutPacketType = cread;
				if (scoutPacketType == 0x01) {
					// scoutPacketSize = 26;
					scoutReply = new int[6];
					scoutReply[0] = 0xf2;
				} else if (scoutPacketType == 0x40) {
					// scoutPacketSize = 49;
					scoutReply = new int[5];
					scoutReply[0] = 0xf2;
				} else {
					logger.error("Unknown protocol type "
							+ Integer.toHexString(scoutPacketType));
					return;
				}
				logger.debug("Protocol type is "
						+ Integer.toHexString(scoutPacketType));
			} else if (readbytes == scoutPacketType) {
				slog = "";
				scoutReply[1] = packet[1];
				scoutReply[2] = packet[2];
				if (scoutPacketType == 0x01) {
					scoutReply[3] = 0x00;
					scoutReply[4] = 0x00;
					scoutReply[5] = 0x00;
				} else if (scoutPacketType == 0x40) {
					scoutReply[3] = 0x00;
					scoutReply[4] = 0x00;
				}
				packets = packets + 1;
				parsePacket();
				readbytes = 0; // сбросим счётчик данных внутри пакета
				logger.debug("Paket number " + packets + " data : " + slog);
				this.clear();
			}

		}
		logger.debug("Close reader console");
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private void parsePacket() {
		// разбор пакета
		scoutPacketType = (packet[1]);
		logger.debug("Packet Type : " + Integer.toString(scoutPacketType));
		int scoutDeviceID = (packet[3] << 8) + packet[2];
		logger.debug("Block ID : " + Integer.toString(scoutDeviceID));
	}
}
