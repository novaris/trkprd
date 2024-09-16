package ru.novoscan.trkpd.terminals;

import org.apache.log4j.Logger;
import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;
import ru.novoscan.trkpd.utils.ModUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 * Created by moiseev on 06.02.2017.
 */
public class ModiRZonline extends Terminal {
    static Logger logger = Logger.getLogger(ModScoutOpen.class);
    private static int packetSize;
    private static byte[] usefulData;
    private static byte[] messageBuffer;
    private ModUtils utl = new ModUtils();
    private final int NAV_REALTIME_GPS = 22;
    private final int DEVICE_STATUS = 1;
    private final int HW_EVENT = 2;
    private final int UNIVERSAL_INPUT_DATA = 91;
    private final int DATA_RS485_SENSORS = 92;
    private final int CAN_LOG_DATA = 104;
    private final int DRIVE_STYLE = 106;
    private final int DIAG_INFO = 205;

    public ModiRZonline(InputStream iDs, DataOutputStream oDs,
                        InputStreamReader console, ModConfig conf, TrackPgUtils pgcon) throws IOException {
        int messageStart = iDs.read();
        byte[] headerBuffer = new byte[3]; //буфер для считывания заголовка
        byte[] crc = new byte[2];
        if ((char) messageStart != '#') {
            logger.error("Unexpected start of the message: " + (char) messageStart); //нужно ли как-то прерывать выполнение кода???
        } else {
            iDs.read(headerBuffer);//читаем заголовок - 3 байта - в буфер
            parseHeader(headerBuffer);// обрабатываем заголовок
            messageBuffer = new byte[packetSize];
            iDs.read(messageBuffer);// читаем сообщение в буфер
            getUsefulData(messageBuffer); //вызов обработки сообщения
            switch ((int) usefulData[0]) { // имеет ли смысл кастование в инт?
                case NAV_REALTIME_GPS:
                    parseNavRealtimeGPS(usefulData);
                    break;
                case DEVICE_STATUS:
                    parseDeviceStatus(usefulData);
                    break;
                case HW_EVENT:
                    parseHwEvent(usefulData);
                    break;
                case UNIVERSAL_INPUT_DATA:
                    parseUniversalInputData(usefulData);
                    break;
                case DATA_RS485_SENSORS:
                    parseRS485Sensors(usefulData);
                    break;
                case CAN_LOG_DATA:
                    parseCanLogData(usefulData);
                    break;
                case DRIVE_STYLE:
                    parseDriveStyle(usefulData);
                    break;
                case DIAG_INFO:
                    parseDiagInfo(usefulData);
                    break;
                default:
                    logger.error("Unknown packet type!");
                    break;
            }
            iDs.read(crc);
            checkCRC(crc);
        }
    }

    private void checkCRC(byte[] crc) {
        int crcReceived = ByteBuffer.wrap(crc).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int crcCalculated = ModUtils.getCrc16(messageBuffer, 0, messageBuffer.length);
        if (crcReceived == crcCalculated) logger.debug("CRC check: OK");
        else logger.error("CRC ERROR.");
    }

    private void parseDiagInfo(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        int hop;
        int start = 48;
        int end, value, id, offset;
        int counter = 1;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            int tableElementSize = 8;
            hop += tableElementSize;
            end = start + tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            tableElementSize = 16;
            hop += tableElementSize;
            end = start + tableElementSize;
            offset = givemeBits(usefulData, start, end);
            start = end;
            logger.debug("Смещение, мс: " + offset);
            if (id > 0 & id < 200) {
                // Числовая переменная №0-199. Размер 4 байта. Формат записи - число с
                // плавающей точкой стандарт IEEE 754
                tableElementSize = 32;
                end = start + tableElementSize;
                value = givemeBits(usefulData, start, end);
                logger.debug("Значение " + counter + "-го сообытия: " + byteArrayToFloat(intToByteArray(value)));
                counter++;
                start = end;
            } else if (id > 199 & id < 255) {
                // Строковая переменная. Размер строки указывается в первом байте поля «значение»
                tableElementSize = 8;
                end = start + tableElementSize;
                int stringSize = givemeBits(usefulData, start, end);
                start = end;
                StringBuilder sb = new StringBuilder();
                for (int sym = end; sym < stringSize; sym += 8) {
                    end = start + 8;
                    sb.append((char) givemeBits(usefulData, start, end));
                    start = end;
                }
                logger.debug("Значение " + counter + "-го сообытия: " + sb);
                tableElementSize += stringSize;
            } else {
                logger.error("Unknown ID.");
                tableElementSize = 0;
            }
            hop += tableElementSize;
        }
    }

    private void parseDriveStyle(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        int hop;
        int start = 48;
        int end, value, id;
        byte[] vals;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            int tableElementSize = 8;
            hop += tableElementSize;
            end = start + tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            switch (id) {
                case 1:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    vals = intToByteArray(value);
                    logger.debug("Превышение ускорения, значение, скорость движения: " + (int)vals[0] + " ,"
                            + (int)vals[1]);
                    break;
                case 2:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    vals = intToByteArray(value);
                    logger.debug("Превышение торможения, значение, скорость движения: " + (int)vals[0] + " ,"
                            + (int)vals[1]);
                    break;
                case 3:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    vals = intToByteArray(value);
                    logger.debug("Превышение при входе в поворот, значение, скорость движения: " + (int)vals[0] + " ,"
                            + (int)vals[1]);
                    break;
                case 4:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    vals = intToByteArray(value);
                    logger.debug("Тряска в баллах, значение, скорость движения: " + (int)vals[0] + " ,"
                            + (int)vals[1]);
                    break;
                default:
                    tableElementSize = 0;
                    logger.error("Unknown ID.");
                    break;
            }
            hop += tableElementSize;
        }
    }

    private void parseCanLogData(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        int hop;
        int start = 48;
        int end, value, id;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            int tableElementSize = 8;
            hop += tableElementSize;
            end = start + tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            switch (id) {
                case 1:
                    // Security state flags
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    char[] flags = ModUtils.bytesToHex(intToByteArray(value));
                    logger.debug("First security flag: " + flags[0]);
                    logger.debug("Second security flag: " + flags[1]);
                    logger.debug("Third security flag: " + flags[2]);
                    break;
                case 2:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Полное время работы двигателя, ч (float)
                    float engineWorkTime = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Full time engine work, hours: " + engineWorkTime);
                    break;
                case 3:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Полный пробег транспортного средства, км (float)
                    float mileage = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Full mileage, km: " + mileage);
                    break;
                case 4:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Полный расход топлива, л (float)
                    float fuelConsumption = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Full fuel consumption, litres: " + fuelConsumption);
                    break;
                case 5:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Уровень топлива в баке, 0.1 % либо л /бит
                    logger.debug("Уровень топлива в баке, 0.1 % либо л /бит: " + value);
                    break;
                case 6:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Скорость оборотов двигателя, rpm
                    logger.debug("Скорость оборотов двигателя, rpm: " + value);
                    break;
                case 7:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Температура двигателя, °C сдвиг -40 град
                    logger.debug("Температура двигателя, °C сдвиг -40 град: " + value);
                    break;
                case 8:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Скорость тр средства, км/ч
                    logger.debug("Скорость тр средства, км/ч: " + value);
                    break;
                case 9:
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на ось 1, 0.1 кг/бит: " + value);
                    break;
                case 10:
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на ось 2, 0.1 кг/бит: " + value);
                    break;
                case 11:
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на ось 3, 0.1 кг/бит: " + value);
                    break;
                case 12:
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на ось 4, 0.1 кг/бит: " + value);
                    break;
                case 13:
                    tableElementSize = 24;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на ось 5, 0.1 кг/бит: " + value);
                    break;
                case 14:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    char[] crashFlags = ModUtils.bytesToHex(intToByteArray(value));
                    for (int a = 0; a < crashFlags.length; a++) {
                        int crashFlag = a + 1;
                        logger.debug("Контроллер аварии, " + crashFlag + "-й флаг: " + crashFlags[a]);
                    }
                    break;
                case 15:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Уровень жидкости AdBLUE, 0.1 % либо л /бит: " + value);
                    break;
                case 16:
                    tableElementSize = 48;
                    end = start + tableElementSize;
                    long state = bitsToLong(usefulData, start, end);
                    char[] agriFlags = ModUtils.bytesToHex(longToByteArray(state));
                    for (int a = 0; a < agriFlags.length; a++) {
                        int agriFlag = a + 1;
                        logger.debug("Состояние сельхозтехники, " + agriFlag + "-й флаг: " + agriFlags[a]);
                    }
                    break;
                case 17:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Время жатки, ч (float)
                    float reaperTime = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Время жатки, ч: " + reaperTime);
                    break;
                case 18:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Убранная площадь, Га (float)
                    float space = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Убранная площадь, Га: " + space);
                    break;
                case 19:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Производительность, 0.01 Га/час: " + value);
                    break;
                case 20:
                    tableElementSize = 32;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    // Количество собранного урожая, т (float)
                    float harvest = byteArrayToFloat(intToByteArray(value));
                    logger.debug("Количество собранного урожая, т: " + harvest);
                    break;
                case 21:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Влажность зерна 0.1 %/бит: " + value);
                    break;
                case 22:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Обороты молотильного барабана, rpm: " + value);
                    break;
                case 23:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Зазор подбарабанья на выходе, мм: " + value);
                    break;
                case 24:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Положение педали газа, %: " + value);
                    break;
                case 25:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Нагрузка на двигатель, %: " + value);
                    break;
                default:
                    tableElementSize = 0;
                    logger.error("Unknown ID.");
                    break;
            }
            hop += tableElementSize;
        }
    }

    private void parseRS485Sensors(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        int hop;
        int start = 48;
        int end, value, id;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            int tableElementSize = 8;
            hop += tableElementSize;
            end = start + tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            switch (id) {
                case 1:
                    // Датчик уровня LLS
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Sensor number: " + value);
                    start = end;
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Temperature, C: " + value);
                    start = end;
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    logger.debug("Level value: " + value);
                    start = end;
                    tableElementSize = 32;
                    break;
                default:
                    tableElementSize = 0;
                    logger.error("Unknown ID.");
                    break;
            }
            hop += tableElementSize;
        }
    }

    private void parseUniversalInputData(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        int hop;
        int start = 48;
        int end, value, id;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            int tableElementSize = 8;
            hop += tableElementSize;
            end = start + tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            switch (id) {
                case 1:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Универсальный вход №1 (int)
                    logger.debug("Universal input №1: " + value);
                    start = end;
                    break;
                case 2:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Универсальный вход №2 (int)
                    logger.debug("Universal input №2: " + value);
                    start = end;
                    break;
                case 3:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Универсальный вход №3 (int)
                    logger.debug("Universal input №3: " + value);
                    start = end;
                    break;
                case 4:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Универсальный вход №4 (int)
                    logger.debug("Universal input №4: " + value);
                    start = end;
                    break;
                case 7:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Напряжение АКБ, мВ (int)
                    logger.debug("Battery voltage, mV: " + value);
                    start = end;
                    break;
                case 8:
                    tableElementSize = 16;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Напряжение внешнего питания, мВ (int)
                    logger.debug("External power supply voltage, mV: " + value);
                    start = end;
                    break;
                case 10:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-1
                    logger.debug("State of DI-1: " + value);
                    start = end;
                    break;
                case 11:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-2
                    logger.debug("State of DI-2: " + value);
                    start = end;
                    break;
                case 12:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-3
                    logger.debug("State of DI-3: " + value);
                    start = end;
                    break;
                case 13:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-4
                    logger.debug("State of DI-4: " + value);
                    start = end;
                    break;
                case 14:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-5
                    logger.debug("State of DI-5: " + value);
                    start = end;
                    break;
                case 15:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-6
                    logger.debug("State of DI-6: " + value);
                    start = end;
                    break;
                case 16:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-7
                    logger.debug("State of DI-7: " + value);
                    start = end;
                    break;
                case 17:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-8
                    logger.debug("State of DI-8: " + value);
                    start = end;
                    break;
                case 18:
                    tableElementSize = 0;
                    value = ((id >> 7) & 1);
                    //Состояние DI-9
                    logger.debug("State of DI-9: " + value);
                    start = end;
                    break;
                case 19:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
//                    Уровень сигнала GSM:
//                    0: -113 dBm или меньше
//                    1: -111 dBM
//                    2...30: -109...-53 dBm
//                    31: -51 dBm или выше
//                    99: неизвестно или невозможно определить
                    if (value == 99) logger.debug("GSM signal, dBm: Unknown");
                    else {
                        int signal = -113 + value * 2;
                        logger.debug("GSM signal, dBm: " + signal);
                    }
                    start = end;
                    break;
                case 20:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Температура в устройстве (signed byte)
                    logger.debug("Device temperature: " + value);
                    start = end;
                    break;
                case 22:
                    tableElementSize = 8;
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Температура GSM-модуля (signed byte)
                    logger.debug("GSM module temperature: " + value);
                    start = end;
                    break;
                default:
                    tableElementSize = 0;
                    logger.error("Unknown ID.");
                    break;
            }
            hop += tableElementSize;
        }
    }

    private void parseHwEvent(byte[] usefulData) {
        int packetLength = givemeBits(usefulData, 8, 16);
        long timeStamp = givemeBits(usefulData, 16, 48);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        double latitude = givemeBits(usefulData, 48, 77);
        logger.debug("Latitude: " + ((latitude > 90000000) ? ((latitude - 90000000) / 10000 + " N") : (latitude / 10000 + " S")));
        double longtitude = givemeBits(usefulData, 77, 106);
        logger.debug("Longtitude: " + ((longtitude > 180000000) ? ((longtitude - 180000000) / 10000 + " E") : (longtitude / 10000 + " W")));
        int statusFlags = givemeBits(usefulData, 106, 112);
        //нулевой бит если 0 то координаты валидны
        logger.debug("Coordinates validity: " + (((statusFlags & 1) == 0) ? ("valid") : ("not valid")));
        //первый бит если 0 то нет движения
        logger.debug("Movement: " + ((((statusFlags >> 1) & 1) == 0) ? ("no") : ("yes")));
        int tableElementSize = 8;
        int start = 112;
        int hop;
        int end, value, id;
        for (int i = start; i <= packetLength; i = i + hop) {
            hop = 0;
            end = start + tableElementSize;
            hop += tableElementSize;
            id = givemeBits(usefulData, start, end);
            start = end;
            switch (id) {
                case 1:
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //тревожная кнопка нажата если 1 и восстановлена в исходное состояние если 0
                    logger.debug("Alarm button: " + ((value == 1) ? "Pressed" : "restored"));
                    start = end;
                    break;
                case 2:
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Изменение состояния “вкл. Зажигания” (0 – выкл., 1 – вкл.)
                    logger.debug("Ignition state: " + ((value == 0) ? "off" : "on"));
                    start = end;
                    break;
                case 3:
                    end = start + tableElementSize;
                    value = givemeBits(usefulData, start, end);
                    //Изменение состояния “Подключен разъем OBDII” (0– отключен, 1 – подключен)
                    logger.debug("OBDII device connection: " + ((value == 0) ? "connected" : "not connected"));
                    start = end;
                    break;
                default:
                    logger.error("Unknown ID.");
                    break;
            }
            hop += tableElementSize;
        }
    }

    private void parseDeviceStatus(byte[] usefulData) {
        long timeStamp = givemeBits(usefulData, 8, 40);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        float baseFirmware = givemeBits(usefulData, 40, 72);
        logger.debug("Base firmware version: " + baseFirmware);
        float curFirmware = givemeBits(usefulData, 72, 104);
        logger.debug("Current firmware version: " + curFirmware);
        int firmwareUpdateFlag = givemeBits(usefulData, 104, 112);
        if (firmwareUpdateFlag == 0) logger.debug("It is Factory firmware.");
        if (firmwareUpdateFlag == 1) logger.debug("Firmware was updated due to server command.");
        if (firmwareUpdateFlag == 2) logger.debug("Firmware was updated due to device command.");
        int tableElements = givemeBits(usefulData, 120, 128);
        int tableElementSize;
        int start = 128;
        int end, value, id;
        if (tableElements > 0) {
            for (int i = 1; i <= tableElements; i++) {
                tableElementSize = 8;
                end = start + tableElementSize;
                id = givemeBits(usefulData, start, end);
                start = end;
                switch (id) {
                    case 1:
                        tableElementSize = 8;
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        if (value == 0) logger.debug("Device operation mode: normal");
                        if (value == 1) logger.debug("Device operation mode: low power consumption");
                        if (value == 2) logger.debug("Device operation mode: sleep mode");
                        if (value == 3) logger.debug("Device operation mode: hibernate");
                        start = end;
                        break;
                    case 2:
                        tableElementSize = 8;
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("Case button state: " + ((value == 0) ? "open" : "closed"));
                        start = end;
                        break;
                    case 3:
                        tableElementSize = 8;
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("User cell button state: " + ((value == 0) ? "open" : "closed"));
                        start = end;
                        break;
                    case 4:
                        //Первый байт содержит количество байтов в номере SCID
                        tableElementSize = givemeBits(usefulData, start, start + 8);
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("SIM1 SCID: " + value);
                        start = end;
                        break;
                    case 5:
                        //Первый байт содержит количество байтов в номере SCID
                        tableElementSize = givemeBits(usefulData, start, start + 8);
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("SIM2 SCID: " + value);
                        start = end;
                        break;
                    case 6:
                        //Первый байт содержит количество байтов в версии прошивки
                        tableElementSize = givemeBits(usefulData, start, start + 8);
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("Base firmware size: " + value + " bytes");
                        start = end;
                        break;
                    case 7:
                        //Первый байт содержит количество байтов в версии прошивки
                        tableElementSize = givemeBits(usefulData, start, start + 8);
                        end = start + tableElementSize;
                        value = givemeBits(usefulData, start, end);
                        logger.debug("Current firmware size: " + value + " bytes");
                        start = end;
                        break;
                    default:
                        logger.error("Unknown ID.");
                        break;
                }
            }
        }
    }

    private void parseNavRealtimeGPS(byte[] usefulData) {
        long timeStamp = givemeBits(usefulData, 8, 40);
        double latitude = givemeBits(usefulData, 40, 69);
        double longtitude = givemeBits(usefulData, 69, 98);
        int course = givemeBits(usefulData, 98, 104);
        int speed = givemeBits(usefulData, 104, 112); // скорость в узлах км/ч = узлы * 1,852
        int sat = givemeBits(usefulData, 112, 117);
        int hdop = givemeBits(usefulData, 117, 120);
        int flag = givemeBits(usefulData, 120, 128);
        Date date = new Date(timeStamp * 1000); //дата должна быть лонг и в милисекундах
        logger.debug("Date: " + date);
        logger.debug("Latitude: " + ((latitude > 90000000) ? ((latitude - 90000000) / 10000 + " N") : (latitude / 10000 + " S")));
        logger.debug("Longtitude: " + ((longtitude > 180000000) ? ((longtitude - 180000000) / 10000 + " E") : (longtitude / 10000 + " W")));
        logger.debug("Path: " + course);
        logger.debug("Speed: " + utl.getSpeed(String.valueOf(speed)) + " km/h");
        logger.debug("Number of satellites: " + sat);
        logger.debug("HDOP: " + hdop);
        //logger.debug("Flag: " + Integer.toBinaryString(flag)); // или нужно расписать значение каждого бита?
        logger.debug("Working on SIM: SIM" + (((flag & 1) == 0) ? ("1") : ("2"))); //нулевой бит если 0 то на СИМ1
        logger.debug("GPS antenna: " + ((((flag >> 1) & 1) == 0) ? ("Ext") : ("Int"))); //первый бит если 0 то на внешней антенне GPS
        logger.debug("GSM antenna: " + ((((flag >> 2) & 1) == 0) ? ("Ext") : ("Int"))); //второй бит если 0 то на внешней антенне GSM
        logger.debug("Accelerometer: " + ((((flag >> 3) & 1) == 0) ? ("No movement") : ("Movement"))); //третий бит если 0 то нет движения по акселерометру
        int energySave = (flag >> 4) & 1;
        energySave = (energySave << 1) + ((flag >> 5) & 1);
        if (energySave == 0) logger.debug("Energy saving: normal");
        if (energySave == 1) logger.debug("Energy saving: low power consumption");
        if (energySave == 2) logger.debug("Energy saving: sleep mode");
        if (energySave == 3) logger.debug("Energy saving: hibernate");
        logger.debug("Coordinate validity: " + ((((flag >> 6) & 1) == 0) ? ("Valid") : ("Not valid"))); //шестой бит если 0 то координаты валидны
    }

    private void getUsefulData(byte[] message) {
        int imeiP1 = 0;
        int imeiP2 = 0;
        int messageNumber;
        usefulData = new byte[message.length - 10]; // полезные данные. Размер = размер всего сообщения - уже обработанные байты
        int usefulDataBytes = 0;
        for (int i = 0; i < 3; i++) {
            imeiP1 = (imeiP1 << 8) + message[i];
        }
        for (int i = 3; i < 7; i++) {
            imeiP2 = (imeiP2 << 8) + message[i];
        }
        logger.debug("IMEI: " + String.valueOf(imeiP1).concat(String.valueOf(imeiP2)));
        logger.debug("Message type: " + (int) message[7]);
        messageNumber = (message[8] << 8) + message[9];
        logger.debug("Index number of message: " + messageNumber);
        for (int i = 10; i < message.length; i++) {
            usefulData[usefulDataBytes] = message[i]; // формирую массив с полезными данными
            usefulDataBytes++;
        }
    }

    private void parseHeader(byte[] header) {
        logger.debug("Protocol version: " + (int) header[0]); //записываем версию протокола. Может нужен каст в инт?
        packetSize = (header[1] << 8) + header[2]; //второй и третий байты - размер сообщения
        logger.debug("Packet size: " + packetSize);
    }

    private int givemeBits(byte[] buf, int start_bit, int end_bit) {
        int wholeByteNumber_start, wholeByteNumber_end;
        int ostatok_start, ostatok_end;
        int res = 0;
        ostatok_start = start_bit % 8;
        wholeByteNumber_start = start_bit / 8;
        if (ostatok_start == 0) {
            res = buf[wholeByteNumber_start];
        } else {
            for (int a = 7 - ostatok_start; a >= 0; a--) {
                int bit = (buf[wholeByteNumber_start] >> a) & 1;
                if (res == 0) {
                    res = bit;
                } else {
                    res = (res << 1) + bit;
                }
            }
        }
        ostatok_end = end_bit % 8;
        wholeByteNumber_end = end_bit / 8;
        for (int i = wholeByteNumber_start + 1; i < wholeByteNumber_end; i++) {
            res = (res << 8) + buf[i];
        }
        if (ostatok_end == 0) {
            return res;
        } else {
            res = (res << ostatok_end) + (buf[wholeByteNumber_end] >> (8 - ostatok_end));
            return res;
        }
    }

    private long bitsToLong(byte[] buf, int start_bit, int end_bit) {
        int wholeByteNumber_start, wholeByteNumber_end;
        int ostatok_start, ostatok_end;
        long res = 0;
        ostatok_start = start_bit % 8;
        wholeByteNumber_start = start_bit / 8;
        if (ostatok_start == 0) {
            res = buf[wholeByteNumber_start];
        } else {
            for (int a = 7 - ostatok_start; a >= 0; a--) {
                int bit = (buf[wholeByteNumber_start] >> a) & 1;
                if (res == 0) {
                    res = bit;
                } else {
                    res = (res << 1) + bit;
                }
            }
        }
        ostatok_end = end_bit % 8;
        wholeByteNumber_end = end_bit / 8;
        for (int i = wholeByteNumber_start + 1; i < wholeByteNumber_end; i++) {
            res = (res << 8) + buf[i];
        }
        if (ostatok_end == 0) {
            return res;
        } else {
            res = (res << ostatok_end) + (buf[wholeByteNumber_end] >> (8 - ostatok_end));
            return res;
        }
    }

    private byte[] intToByteArray(int value) {
        // создаем массив байт размером 4 (allocate(4)), помещаем в него Int value, конвертируем в массив
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private byte[] longToByteArray(long value) {
        // создаем массив байт размером 4 (allocate(4)), помещаем в него Int value, конвертируем в массив
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    private float byteArrayToFloat(byte[] bytes) {
        // берем массив байт, режем (wrap) его по байтам, порядок байт LITTLE_ENDIAN (по умолчанию BIG_ENDIAN)
        // достаем из него float
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }
}
