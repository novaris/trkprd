/*
 * Copyright 2016 Novaris Ltd.
 *
 * This file is part of Novaris Track System (NTS).
 * NTS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * NCC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NTS. If not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * SNMP for Spring
 *
 * http://spring-snmp.sourceforge.net/index.html
 *
 * SNMP for Spring is licensed under the Apache License, Version 2.0 
 *
 * Original codes: https://github.com/jpragma/spring-snmp
 *
 */

package ru.novoscan.trkpd.snmp.spring;

import com.jpragma.snmp.asn.AsnInteger;
import com.jpragma.snmp.asn.ber.BerTlv;
import com.jpragma.snmp.asn.ber.BerTlvIdentifier;

/**
 * Updated the original Spring for Snmp class
 * to avoid the effect of 'negative numbers'
 * for the range 128-255, 32768-65535, 8388608-16777215
 * i.e. the highest bit in the major non-zero bite is '1'
 * 
 * @author Vladimir Koldakov
 */

public class TrackAsnInteger extends AsnInteger {
    public TrackAsnInteger() {
        super();
    }

    public TrackAsnInteger(long value) {
        super(value);
    }

    public TrackAsnInteger(BerTlv tlv) {
        super(tlv);
    }

    @Override
    public BerTlv toBerTlv(int tagNumber) {
        BerTlvIdentifier tag = new BerTlvIdentifier();
        tag.setTagValue(tagNumber);
        BerTlv tlv = new BerTlv();
        tlv.setTag(tag);
        byte tlvValueBuf[] = value.toByteArray();
//        tlvValueBuf = BitManipulationHelper.removeLeadingZeroBytes(tlvValueBuf);
        tlv.setLength(tlvValueBuf.length);
        tlv.setValue(tlvValueBuf);
        return tlv;
    }
}

