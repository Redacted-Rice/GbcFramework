package redactedrice.gbcframework.utils;


import java.util.List;
import java.util.zip.CRC32;

public final class ByteUtils {
    private ByteUtils() {}

    public static final int MAX_BYTE_VALUE = 0xff;
    public static final int MIN_BYTE_VALUE = 0x00;
    public static final int MAX_HEX_CHAR_VALUE = 0xf;
    public static final int MIN_HEX_CHAR_VALUE = 0x0;

    public static final int BYTE_UPPER_HEX_CHAR_MASK = 0xf0;
    public static final int BYTE_LOWER_HEX_CHAR_MASK = 0x0f;

    public static void printBytes(byte[] bytes, int index, int bytesPerNumber, int numberToPrint) {
        String formatString = "0x%" + bytesPerNumber * 2 + "X";
        for (int i = 0; i < numberToPrint; i++) {
            System.out.println(String.format(formatString,
                    readLittleEndian(bytes, index + i * bytesPerNumber, bytesPerNumber)));
        }
    }

    public static short unsignedByteAsShort(byte value) {
        short asShort = value;
        if (value < 0) {
            asShort += 1 << 8;
        }
        return asShort;
    }

    public static byte readUpperHexChar(byte value) {
        return (byte) ((value & BYTE_UPPER_HEX_CHAR_MASK) >> 4);
    }

    public static byte readLowerHexChar(byte value) {
        return (byte) (value & BYTE_LOWER_HEX_CHAR_MASK);
    }

    public static byte packHexCharsToByte(byte upper, byte lower) {
        if (upper > MAX_HEX_CHAR_VALUE || upper < MIN_HEX_CHAR_VALUE) {
            throw new IllegalArgumentException(
                    "Upper bit (" + upper + " must be a hex char value unshifted (i.e. between "
                            + MIN_HEX_CHAR_VALUE + " and " + MAX_HEX_CHAR_VALUE + ")");
        }
        if (lower > MAX_HEX_CHAR_VALUE || lower < MIN_HEX_CHAR_VALUE) {
            throw new IllegalArgumentException("Lower bit (" + lower + " must be a hex char value");
        }
        return (byte) (upper << 4 & 0xff | lower);
    }

    // Sorts so the negatives are treated as positive - i.e. -128 is treated as 255
    public static int unsignedCompareBytes(byte b1, byte b2) {
        return unsignedCompare(b1, b2, 1);
    }

    public static int unsignedCompare(int i1, int i2, int numBytes) {
        // Treats the negatives as positives. Useful for sorting
        long l1 = i1;
        if (l1 < 0) {
            l1 += 1 << (numBytes * 8);
        }

        long l2 = i2;
        if (l2 < 0) {
            l2 += 1 << (numBytes * 8);
        }

        if (l1 < l2) {
            return -1;
        } else if (l1 > l2) {
            return 1;
        }
        return 0;
    }

    public static boolean compareBytes(byte[] compareAgainst, int compareAgainstIdx,
            byte[] compareTo) {
        for (int i = 0; i < compareTo.length; i++) {
            if (compareAgainst[compareAgainstIdx + i] != compareTo[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] toLittleEndianBytes(int value, int numBytes) {
        byte[] asBytes = new byte[numBytes];
        writeLittleEndian(value, asBytes, 0, numBytes);
        return asBytes;
    }

    public static byte[] toLittleEndianBytes(long value, int numBytes) {
        byte[] asBytes = new byte[numBytes];
        writeLittleEndian(value, asBytes, 0, numBytes);
        return asBytes;
    }

    public static byte[] shortToLittleEndianBytes(short value) {
        return toLittleEndianBytes(value, 2);
    }

    public static byte[] shortListToLittleEndianBytes(List<Short> values) {
        byte[] bytes = new byte[values.size() * 2];
        for (int i = 0; i < values.size(); i++) {
            writeAsShort(values.get(i), bytes, i * 2);
        }
        return bytes;
    }

    public static byte[] sevenBitEncode(long value) {
        long valueCopy = value;

        // Determine how big of a byte array we need
        int byteSize = 1;
        valueCopy >>= 7;
        while (valueCopy != 0) {
            byteSize++;
            valueCopy--;
            valueCopy >>= 7;
        }

        // Create the array
        byte[] bytes = new byte[byteSize];

        // Get the last half byte (7 bits) of the value then shift
        // them off the value
        int index = 0;
        byte lastHalfByte = (byte) (value & 0x7f);
        value >>= 7;

        // While we still have bits left (and there is more than one
        // since the last set will be hanled special
        while (value != 0) {
            // Write the half byte as a full byte
            bytes[index++] = lastHalfByte;

            // subtract one from the value because they
            // try to save as much space as possible and
            // because we know its at least 1 since its
            // not the terminal byte
            value--;

            // Get the next half byte
            lastHalfByte = (byte) (value & 0x7f);
            value >>= 7;
        }

        // Write the terminal half byte by adding the signal that
        // it is the final half byte
        bytes[index] = (byte) (0x80 | lastHalfByte);
        return bytes;
    }

    public static byte[] sevenBitEncodeSigned(long value) {
        // If its negative, make it positive
        boolean negative = value < 0;
        if (negative) {
            value = Math.abs(value);
        }

        // Now shift it up one to make room for the sign bit and
        // set it if its negative
        value = value << 1;
        if (negative) {
            value++;
        }

        // Now write it like a normal value
        return sevenBitEncode(value);
    }

    public static void setBytes(byte[] bytes, int startIndex, int numberToSet, byte valueToSet) {
        for (int i = 0; i < numberToSet; i++) {
            bytes[startIndex + i] = valueToSet;
        }
    }

    public static void copyBytes(byte[] destination, int destinationStartIndex, byte[] toCopy) {
        copyBytes(destination, destinationStartIndex, toCopy, 0, toCopy.length);
    }

    public static void copyBytes(byte[] destination, int destinationStartIndex, byte[] toCopy,
            int toCopyStartIndex, int length) {
        if (destinationStartIndex + length > destination.length) {
            throw new IllegalArgumentException("The destination array (size " + destination.length
                    + ") is not large enough to copy the source array (size " + length
                    + ") to it starting at index " + destinationStartIndex);
        }

        for (int i = 0; i < length; i++) {
            destination[destinationStartIndex + i] = toCopy[toCopyStartIndex + i];
        }
    }

    public static long readLittleEndian(byte[] byteArray, int index, int numBytes) {
        if (numBytes > 8) {
            throw new IllegalArgumentException(
                    "readLittleEndian: Bytes must fit in a long (i.e. be less than 8)"
                            + " Was given " + numBytes);
        }

        long number = 0;
        for (int j = numBytes - 1; j >= 0; j--) {
            number = number << 8;
            // Its a pain because bytes are signed so we need to make sure when the
            // byte is promoted here that it only takes the last digits or else if its
            // > byte's max signed value, it will add FFs to promote it and keep the
            // same negative value whereas we only want the byte values
            number |= byteArray[index + j] & 0xff;
        }
        return number;
    }

    public static short readAsShort(byte[] byteArray, int index) {
        // little endian
        return (short) readLittleEndian(byteArray, index, 2);
    }

    public static void writeLittleEndian(int value, byte[] byteArray, int index, int numBytes) {
        if (numBytes > 4) {
            String errorText = "writeLittleEndian: Bytes must fit in a int (i.e. be less than 4) if an int "
                    + "is passed. Was given " + numBytes;
            if (numBytes <= 8) {
                errorText += ". Use the version that takes a long instead";
            }
            throw new IllegalArgumentException(errorText);
        }

        writeLittleEndian((long) value, byteArray, index, numBytes);
    }

    public static void writeLittleEndian(long value, byte[] byteArray, int index, int numBytes) {
        if (numBytes > 8) {
            throw new IllegalArgumentException(
                    "writeLittleEndian: Bytes must fit in a long (i.e. be less than 8)."
                            + " Was given " + numBytes);
        }

        for (int j = 0; j < numBytes; j++) {
            byteArray[index + j] = (byte) (value & 0xff);
            value = value >> 8;
        }
    }

    public static void writeAsShort(short value, byte[] byteArray, int index) {
        writeLittleEndian(value, byteArray, index, 2);
    }

    public static byte parseByte(String str) {
        int val = Integer.parseInt(str, 16); // 16 = hex
        if (val > MAX_BYTE_VALUE || val < MIN_BYTE_VALUE) {
            throw new NumberFormatException("Failed to parse unsigned hex byte from " + str);
        }
        return (byte) val;
    }

    public static long parseBytes(String str, int numBytes) {
        if (numBytes < 0 || numBytes > 7) {
            throw new IllegalArgumentException(
                    "To many bytes passed (" + numBytes + ") must be 0 <= x <= 7");
        }

        long val = Long.parseLong(str, 16); // 16 = hex
        if (val > (Math.pow(MAX_BYTE_VALUE + 1.0, numBytes) - 1) || val < MIN_BYTE_VALUE) {
            throw new NumberFormatException(
                    "Failed to parse " + numBytes + " unsigned hex bytes from " + str);
        }
        return val;
    }

    public static long computeCrc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
}
