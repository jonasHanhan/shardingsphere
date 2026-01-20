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

package org.apache.shardingsphere.database.protocol.postgresql.packet.command.query;

import org.postgresql.jdbc.TimestampUtils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

final class PostgreSQLTextValueFormatter {
    
    private static final ThreadLocal<TimestampUtils> TIMESTAMP_UTILS = ThreadLocal.withInitial(() -> new TimestampUtils(false, null));
    
    static String format(final PostgreSQLTextCell cell) {
        Object value = cell.getData();
        String columnTypeName = cell.getColumnTypeName().orElse(null);
        if (isTemporal(cell.getJdbcType(), columnTypeName)) {
            return formatTemporalValue(value);
        }
        return value.toString();
    }
    
    private static boolean isTemporal(final int jdbcType, final String columnTypeName) {
        if (null != columnTypeName) {
            String normalized = columnTypeName.toLowerCase();
            if ("timestamp".equals(normalized) || "timestamptz".equals(normalized) || "date".equals(normalized) || "time".equals(normalized) || "timetz".equals(normalized)) {
                return true;
            }
        }
        return Types.TIMESTAMP == jdbcType || Types.TIMESTAMP_WITH_TIMEZONE == jdbcType || Types.DATE == jdbcType || Types.TIME == jdbcType || Types.TIME_WITH_TIMEZONE == jdbcType;
    }
    
    private static String formatTemporalValue(final Object value) {
        TimestampUtils timestampUtils = TIMESTAMP_UTILS.get();
        if (value instanceof Timestamp) {
            return timestampUtils.toString(null, (Timestamp) value, false);
        }
        if (value instanceof Date) {
            return timestampUtils.toString(null, (Date) value, false);
        }
        if (value instanceof Time) {
            return timestampUtils.toString(null, (Time) value, false);
        }
        if (value instanceof LocalDateTime) {
            return timestampUtils.toString((LocalDateTime) value);
        }
        if (value instanceof LocalDate) {
            return timestampUtils.toString((LocalDate) value);
        }
        if (value instanceof LocalTime) {
            return timestampUtils.toString((LocalTime) value);
        }
        if (value instanceof OffsetDateTime) {
            return timestampUtils.toString((OffsetDateTime) value);
        }
        if (value instanceof OffsetTime) {
            return timestampUtils.toString((OffsetTime) value);
        }
        return value.toString();
    }
}
