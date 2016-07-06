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

import java.util.Comparator;

/**
 * Updated the original Spring for Snmp class
 * to avoid the compilation warning
 * 
 * @author Evgeny Kurensky
 */

public class TrackOidComparator implements Comparator<int[]> {

    public TrackOidComparator() {
    }

    @Override
    public final int compare(int[] o1, int[] o2) {
        int intArray1[] = (int[]) (int[]) o1;
        int intArray2[] = (int[]) (int[]) o2;
        int shorterIntArray[];
        int longerIntArray[];
        if (intArray1.length > intArray2.length) {
            shorterIntArray = intArray2;
            longerIntArray = intArray1;
        } else {
            shorterIntArray = intArray1;
            longerIntArray = intArray2;
        }
        for (int i = 0; i < shorterIntArray.length; i++)
            if (shorterIntArray[i] != longerIntArray[i])
                if (shorterIntArray[i] > longerIntArray[i])
                    return shorterIntArray != intArray1 ? -1 : 1;
                else
                    return longerIntArray != intArray1 ? -1 : 1;

        if (shorterIntArray.length == longerIntArray.length)
            return 0;
        else
            return longerIntArray != intArray1 ? -1 : 1;
    }
}

