// Copyright (c) 2015, Nova Generacija Softvera d.o.o.
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 
// 3. Neither the name of the copyright holder nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.github.loki4j.common;

import java.util.Arrays;

public final class JsonWriter {

    private int position;
    private byte[] buffer = new byte[1024];

    /**
     * Helper for writing JSON object start: {
     */
    public static final byte OBJECT_START = '{';
    /**
     * Helper for writing JSON object end: }
     */
    public static final byte OBJECT_END = '}';
    /**
     * Helper for writing JSON array start: [
     */
    public static final byte ARRAY_START = '[';
    /**
     * Helper for writing JSON array end: ]
     */
    public static final byte ARRAY_END = ']';
    /**
     * Helper for writing comma separator: ,
     */
    public static final byte COMMA = ',';
    /**
     * Helper for writing semicolon: :
     */
    public static final byte SEMI = ':';
    /**
     * Helper for writing JSON quote: "
     */
    public static final byte QUOTE = '"';
    /**
     * Helper for writing JSON escape: \\
     */
    public static final byte ESCAPE = '\\';

    public void beginStreams(LogRecord firstRecord, String[] firstLabels) {
        writeByte(OBJECT_START);
        writeAsciiString("streams");
        writeByte(SEMI);
        writeByte(ARRAY_START);
        stream(firstRecord, firstLabels);
    }

    public void nextStream(LogRecord firstRecord, String[] labels) {
        writeByte(ARRAY_END);
        writeByte(OBJECT_END);
        writeByte(COMMA);
        stream(firstRecord, labels);
    }

    private void stream(LogRecord firstRecord, String[] labels) {
        writeByte(OBJECT_START);
        writeAsciiString("stream");
        writeByte(SEMI);
        labels(labels);
        writeByte(COMMA);
        writeAsciiString("values");
        writeByte(SEMI);
        writeByte(ARRAY_START);
        record(firstRecord);
    }

    private void labels(String[] labels) {
        writeByte(OBJECT_START);
        if (labels.length > 0) {
            for (int i = 0; i < labels.length; i+=2) {
                writeString(labels[i]);
                writeByte(SEMI);
                writeString(labels[i + 1]);
                if (i < labels.length - 2)
                    writeByte(COMMA);
            }
        }
        writeByte(OBJECT_END);
    }

    public void nextRecord(LogRecord record) {
        writeByte(COMMA);
        record(record);
    }

    private void record(LogRecord record) {
        writeByte(ARRAY_START);
        writeAsciiString(String.format("%s%06d", record.timestampMs, record.nanos));
        writeByte(COMMA);
        writeString(record.message);
        writeByte(ARRAY_END);
    }

    public void endStreams() {
        writeByte(ARRAY_END);
        writeByte(OBJECT_END);
        writeByte(ARRAY_END);
        writeByte(OBJECT_END);
    }


    final byte[] ensureCapacity(final int free) {
        if (position + free >= buffer.length) {
            enlargeOrFlush(position, free);
        }
        return buffer;
    }

    void advance(int size) {
        position += size;
    }
    
    private void enlargeOrFlush(final int size, final int padding) {
        buffer = Arrays.copyOf(buffer, buffer.length + buffer.length / 2 + padding);
    }

    /**
     * Optimized method for writing 'null' into the JSON.
     */
    public final void writeNull() {
        if ((position + 4)>= buffer.length) {
            enlargeOrFlush(position, 0);
        }
        final int s = position;
        final byte[] _result = buffer;
        _result[s] = 'n';
        _result[s + 1] = 'u';
        _result[s + 2] = 'l';
        _result[s + 3] = 'l';
        position += 4;
    }

    /**
     * Write a single byte into the JSON.
     *
     * @param value byte to write into the JSON
     */
    public final void writeByte(final byte value) {
        if (position == buffer.length) {
            enlargeOrFlush(position, 0);
        }
        buffer[position++] = value;
    }

    /**
     * Write a quoted string into the JSON.
     * String will be appropriately escaped according to JSON escaping rules.
     *
     * @param value string to write
     */
    public final void writeString(final String value) {
        final int len = value.length();
        if (position + (len << 2) + (len << 1) + 2 >= buffer.length) {
            enlargeOrFlush(position, (len << 2) + (len << 1) + 2);
        }
        final byte[] _result = buffer;
        _result[position] = QUOTE;
        int cur = position + 1;
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (c > 31 && c != '"' && c != '\\' && c < 126) {
                _result[cur++] = (byte) c;
            } else {
                writeQuotedString(value, i, cur, len);
                return;
            }
        }
        _result[cur] = QUOTE;
        position = cur + 1;
    }

    private void writeQuotedString(final CharSequence str, int i, int cur, final int len) {
        final byte[] _result = this.buffer;
        for (; i < len; i++) {
            final char c = str.charAt(i);
            if (c == '"') {
                _result[cur++] = ESCAPE;
                _result[cur++] = QUOTE;
            } else if (c == '\\') {
                _result[cur++] = ESCAPE;
                _result[cur++] = ESCAPE;
            } else if (c < 32) {
                if (c == 8) {
                    _result[cur++] = ESCAPE;
                    _result[cur++] = 'b';
                } else if (c == 9) {
                    _result[cur++] = ESCAPE;
                    _result[cur++] = 't';
                } else if (c == 10) {
                    _result[cur++] = ESCAPE;
                    _result[cur++] = 'n';
                } else if (c == 12) {
                    _result[cur++] = ESCAPE;
                    _result[cur++] = 'f';
                } else if (c == 13) {
                    _result[cur++] = ESCAPE;
                    _result[cur++] = 'r';
                } else {
                    _result[cur] = ESCAPE;
                    _result[cur + 1] = 'u';
                    _result[cur + 2] = '0';
                    _result[cur + 3] = '0';
                    switch (c) {
                        case 0:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '0';
                            break;
                        case 1:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '1';
                            break;
                        case 2:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '2';
                            break;
                        case 3:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '3';
                            break;
                        case 4:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '4';
                            break;
                        case 5:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '5';
                            break;
                        case 6:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '6';
                            break;
                        case 7:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = '7';
                            break;
                        case 11:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = 'B';
                            break;
                        case 14:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = 'E';
                            break;
                        case 15:
                            _result[cur + 4] = '0';
                            _result[cur + 5] = 'F';
                            break;
                        case 16:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '0';
                            break;
                        case 17:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '1';
                            break;
                        case 18:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '2';
                            break;
                        case 19:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '3';
                            break;
                        case 20:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '4';
                            break;
                        case 21:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '5';
                            break;
                        case 22:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '6';
                            break;
                        case 23:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '7';
                            break;
                        case 24:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '8';
                            break;
                        case 25:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = '9';
                            break;
                        case 26:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'A';
                            break;
                        case 27:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'B';
                            break;
                        case 28:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'C';
                            break;
                        case 29:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'D';
                            break;
                        case 30:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'E';
                            break;
                        default:
                            _result[cur + 4] = '1';
                            _result[cur + 5] = 'F';
                            break;
                    }
                    cur += 6;
                }
            } else if (c < 0x007F) {
                _result[cur++] = (byte) c;
            } else {
                final int cp = Character.codePointAt(str, i);
                if (Character.isSupplementaryCodePoint(cp)) {
                    i++;
                }
                if (cp == 0x007F) {
                    _result[cur++] = (byte) cp;
                } else if (cp <= 0x7FF) {
                    _result[cur++] = (byte) (0xC0 | ((cp >> 6) & 0x1F));
                    _result[cur++] = (byte) (0x80 | (cp & 0x3F));
                } else if ((cp < 0xD800) || (cp > 0xDFFF && cp <= 0xFFFF)) {
                    _result[cur++] = (byte) (0xE0 | ((cp >> 12) & 0x0F));
                    _result[cur++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                    _result[cur++] = (byte) (0x80 | (cp & 0x3F));
                } else if (cp >= 0x10000 && cp <= 0x10FFFF) {
                    _result[cur++] = (byte) (0xF0 | ((cp >> 18) & 0x07));
                    _result[cur++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                    _result[cur++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                    _result[cur++] = (byte) (0x80 | (cp & 0x3F));
                } else {
                    throw new IllegalArgumentException("Unknown unicode codepoint in string! " + Integer.toHexString(cp));
                }
            }
        }
        _result[cur] = QUOTE;
        position = cur + 1;
    }

    /**
     * Write a quoted string consisting of only ascii characters.
     * String will not be escaped according to JSON escaping rules.
     *
     * @param value ascii string
     */
    @SuppressWarnings("deprecation")
    public final void writeAsciiString(final String value) {
        final int len = value.length() + 2;
        if (position + len >= buffer.length) {
            enlargeOrFlush(position, len);
        }
        final byte[] _result = buffer;
        _result[position] = QUOTE;
        value.getBytes(0, len - 2, _result, position + 1);
        _result[position + len - 1] = QUOTE;
        position += len;
    }

    /**
     * Write string consisting of only ascii characters.
     * String will not be escaped according to JSON escaping rules.
     *
     * @param value ascii string
     */
    @SuppressWarnings("deprecation")
    public final void writeAscii(final String value) {
        final int len = value.length();
        if (position + len >= buffer.length) {
            enlargeOrFlush(position, len);
        }
        value.getBytes(0, len, buffer, position);
        position += len;
    }

    /**
     * Copy bytes into JSON as is.
     * Provided buffer can't be null.
     *
     * @param buf byte buffer to copy
     */
    public final void writeRaw(final byte[] buf) {
        final int len = buf.length;
        if (position + len >= buffer.length) {
            enlargeOrFlush(position, len);
        }
        final int p = position;
        final byte[] _result = buffer;
        for (int i = 0; i < buf.length; i++) {
            _result[p + i] = buf[i];
        }
        position += len;
    }


    /**
     * Copy part of byte buffer into JSON as is.
     * Provided buffer can't be null.
     *
     * @param buf byte buffer to copy
     * @param offset in buffer to start from
     * @param len part of buffer to copy
     */
    public final void writeRaw(final byte[] buf, final int offset, final int len) {
        if (position + len >= buffer.length) {
            enlargeOrFlush(position, len);
        }
        System.arraycopy(buf, offset, buffer, position, len);
        position += len;
    }


    /**
     * Content of buffer can be copied to another array of appropriate size.
     * This method can't be used when targeting output stream.
     * Ideally it should be avoided if possible, since it will create an array copy.
     * It's better to use getByteBuffer and size instead.
     *
     * @return copy of the buffer up to the current position
     */
    public final byte[] toByteArray() {
        return Arrays.copyOf(buffer, position);
    }


    /**
     * Current position in the buffer. When stream is not used, this is also equivalent
     * to the size of the resulting JSON in bytes
     *
     * @return position in the populated buffer
     */
    public final int size() {
        return position;
    }


    /**
     * Resets the writer
     */
    public final void reset() {
        position = 0;
    }

}
