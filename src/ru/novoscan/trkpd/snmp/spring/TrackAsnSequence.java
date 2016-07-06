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

import java.util.ArrayList;
import java.util.List;

import com.jpragma.snmp.asn.AsnSequence;
import com.jpragma.snmp.asn.AsnOID;
import com.jpragma.snmp.asn.AsnObjectValueException;
import com.jpragma.snmp.types.VarBind;

/**
 * Updated the original Spring for Snmp class to exclude
 * one of the two extra bindings while sendind
 * a datagram package by providing the method
 * to get the List value without binding.
 * 
 * The second binding is excluded in the NccSpringSnmpAgent
 * while composing a datagram package.
 * 
 * Also provide the static method to compose the list
 * of the binding (int) values.
 * 
 * @author Vladimir Koldakov
 */

public class TrackAsnSequence extends AsnSequence {

    public TrackAsnSequence() {
        super();
    }

    public TrackAsnSequence(List<VarBind> value) throws AsnObjectValueException {
        super(value);
    }

    public static List<VarBind> toVarBindList(String string_oid, int from, List<Long> value){
        List<VarBind> list = new ArrayList<VarBind>();

        for(int i=0;i<value.size();i++) {
            AsnOID oid = null;
            StringBuilder sb = new StringBuilder();
            sb.append(string_oid);
            sb.append(".");
            sb.append(from+i);
            try {
                oid = new AsnOID(sb.toString());
            } catch (AsnObjectValueException ex) {}
            TrackAsnInteger val = new TrackAsnInteger(value.get(i));
            list.add(new VarBind(oid,val));
        }
        return list;
    }

    public List<?> getList() {
        return value;
    }

}
