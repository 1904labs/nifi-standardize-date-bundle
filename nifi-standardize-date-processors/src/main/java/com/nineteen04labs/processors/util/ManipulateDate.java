/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nineteen04labs.processors.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ManipulateDate {

    public static String standardize(String dateTime, String format, String timezone) {
        DateTimeFormatter dtFormat = null;
        try {
            dtFormat = DateTimeFormatter.ofPattern(format);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
        }

        LocalDateTime localDT = null;
        try {
            localDT = LocalDateTime.parse(dateTime, dtFormat);
        } catch (DateTimeParseException e) {
            localDT = LocalDate.parse(dateTime, dtFormat).atStartOfDay();
        }
        
        ZoneId localZone = null;
        try {
            localZone = ZoneId.of(ZoneId.SHORT_IDS.get(timezone));
        } catch (NullPointerException e) {
            localZone = ZoneId.of(timezone);
        }

        ZonedDateTime localZonedDT = ZonedDateTime.of(localDT, localZone);
        ZonedDateTime standardizedDT = localZonedDT.withZoneSameInstant(ZoneOffset.UTC);

        return standardizedDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

}
