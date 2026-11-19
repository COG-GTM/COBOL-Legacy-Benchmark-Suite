package com.clbs.portfolio.batch;

import java.math.BigDecimal;

/** COMP-3 (packed decimal) codec. */
public final class PackedDecimal {

    private PackedDecimal() {
    }

    public static BigDecimal decode(byte[] data, int offset, int length, int scale) {
        StringBuilder digits = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            int hi = (data[i] >> 4) & 0x0F;
            int lo = data[i] & 0x0F;
            digits.append(digit(hi));
            if (i < offset + length - 1) {
                digits.append(digit(lo));
            } else {
                BigDecimal value = new BigDecimal(new java.math.BigInteger(digits.toString()), scale);
                return lo == 0x0D || lo == 0x0B ? value.negate() : value;
            }
        }
        throw new IllegalArgumentException("Empty packed decimal");
    }

    public static byte[] encode(BigDecimal value, int length, int scale) {
        String digits = value.abs().setScale(scale, java.math.RoundingMode.DOWN).unscaledValue().toString();
        int totalDigits = length * 2 - 1;
        if (digits.length() > totalDigits) {
            digits = digits.substring(digits.length() - totalDigits);
        }
        String nibbles = "0".repeat(totalDigits - digits.length()) + digits;
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            int hi = nibbles.charAt(i * 2) - '0';
            int lo = i == length - 1 ? (value.signum() < 0 ? 0x0D : 0x0C) : nibbles.charAt(i * 2 + 1) - '0';
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static char digit(int nibble) {
        if (nibble > 9) {
            throw new IllegalArgumentException("Invalid packed decimal digit: " + nibble);
        }
        return (char) ('0' + nibble);
    }
}
