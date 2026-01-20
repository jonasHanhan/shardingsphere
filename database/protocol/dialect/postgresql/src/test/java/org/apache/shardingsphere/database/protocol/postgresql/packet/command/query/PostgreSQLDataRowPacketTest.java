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

import org.apache.shardingsphere.database.protocol.binary.BinaryCell;
import org.apache.shardingsphere.database.protocol.postgresql.packet.command.query.extended.PostgreSQLColumnType;
import org.apache.shardingsphere.database.protocol.postgresql.packet.identifier.PostgreSQLMessagePacketType;
import org.apache.shardingsphere.database.protocol.postgresql.payload.PostgreSQLPacketPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.postgresql.jdbc.TimestampUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostgreSQLDataRowPacketTest {
    
    @Mock
    private PostgreSQLPacketPayload payload;
    
    @Mock
    private SQLXML sqlxml;
    
    @BeforeEach
    void setup() {
        when(payload.getCharset()).thenReturn(StandardCharsets.UTF_8);
    }
    
    @Test
    void assertWriteWithNull() {
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.VARCHAR, null, null)));
        actual.write(payload);
        verify(payload).writeInt4(0xFFFFFFFF);
    }
    
    @Test
    void assertWriteWithBytes() {
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.BINARY, "bytea", new byte[]{'a'})));
        actual.write(payload);
        byte[] expectedBytes = buildExpectedByteaText(new byte[]{'a'});
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithSQLXML() throws SQLException {
        when(sqlxml.getString()).thenReturn("value");
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.SQLXML, "xml", sqlxml)));
        actual.write(payload);
        byte[] valueBytes = "value".getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(valueBytes.length);
        verify(payload).writeBytes(valueBytes);
    }
    
    @Test
    void assertWriteWithString() {
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.VARCHAR, "text", "value")));
        actual.write(payload);
        byte[] valueBytes = "value".getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(valueBytes.length);
        verify(payload).writeBytes(valueBytes);
    }
    
    @Test
    void assertWriteWithSQLXML4Error() throws SQLException {
        when(sqlxml.getString()).thenThrow(new SQLException("mock"));
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.SQLXML, "xml", sqlxml)));
        assertThrows(RuntimeException.class, () -> actual.write(payload));
        verify(payload, never()).writeStringEOF(any());
    }
    
    @Test
    void assertWriteWithTimestampWithoutFractionalSeconds() {
        Timestamp input = Timestamp.valueOf("1973-06-03 10:30:01");
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.TIMESTAMP, "timestamp", input)));
        actual.write(payload);
        byte[] expectedBytes = "1973-06-03 10:30:01".getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithTimestampWithFractionalSeconds() {
        Timestamp input = Timestamp.valueOf("1973-06-03 10:30:01.123");
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.TIMESTAMP, "timestamp", input)));
        actual.write(payload);
        byte[] expectedBytes = "1973-06-03 10:30:01.123".getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithTimestampWithTimeZone() {
        OffsetDateTime input = OffsetDateTime.of(1973, 6, 3, 10, 30, 1, 123000000, ZoneOffset.ofHours(8));
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", input)));
        actual.write(payload);
        byte[] expectedBytes = new TimestampUtils(false, null).toString(input).getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithDate() {
        LocalDate input = LocalDate.of(1973, 6, 3);
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.DATE, "date", input)));
        actual.write(payload);
        byte[] expectedBytes = new TimestampUtils(false, null).toString(input).getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithTime() {
        LocalTime input = LocalTime.of(10, 30, 1);
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.TIME, "time", input)));
        actual.write(payload);
        byte[] expectedBytes = new TimestampUtils(false, null).toString(input).getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteWithTimeWithTimeZone() {
        OffsetTime input = OffsetTime.of(10, 30, 1, 0, ZoneOffset.ofHours(8));
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new PostgreSQLTextCell(Types.TIME_WITH_TIMEZONE, "timetz", input)));
        actual.write(payload);
        byte[] expectedBytes = new TimestampUtils(false, null).toString(input).getBytes(StandardCharsets.UTF_8);
        verify(payload).writeInt4(expectedBytes.length);
        verify(payload).writeBytes(expectedBytes);
    }
    
    @Test
    void assertWriteBinaryNull() {
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new BinaryCell(PostgreSQLColumnType.INT4, null)));
        actual.write(payload);
        verify(payload).writeInt2(1);
        verify(payload).writeInt4(0xFFFFFFFF);
    }
    
    @Test
    void assertWriteBinaryInt4() {
        int value = 12345678;
        PostgreSQLDataRowPacket actual = new PostgreSQLDataRowPacket(Collections.singleton(new BinaryCell(PostgreSQLColumnType.INT4, value)));
        actual.write(payload);
        verify(payload).writeInt2(1);
        verify(payload).writeInt4(4);
        verify(payload).writeInt4(value);
    }
    
    @Test
    void assertGetIdentifier() {
        assertThat(new PostgreSQLDataRowPacket(Collections.emptyList()).getIdentifier(), is(PostgreSQLMessagePacketType.DATA_ROW));
    }
    
    private byte[] buildExpectedByteaText(final byte[] value) {
        byte[] result = new byte[value.length * 2 + 2];
        result[0] = '\\';
        result[1] = 'x';
        byte[] hexDigits = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < value.length; i++) {
            int unsignedByte = value[i] & 0xFF;
            result[2 + i * 2] = hexDigits[unsignedByte >>> 4];
            result[3 + i * 2] = hexDigits[unsignedByte & 0x0F];
        }
        return result;
    }
}
