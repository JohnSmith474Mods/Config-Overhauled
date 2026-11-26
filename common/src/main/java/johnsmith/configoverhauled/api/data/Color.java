package johnsmith.configoverhauled.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Acts as a type-key for dynamic factory registration and constructor resolution.
 * This class is not instantiable. Color properties process data strictly as primitive integers.
 */
public final class Color {
    /**
     * The minimum integer boundary representing a valid RGB color (0x00000000).
     */
    public static final int RGB_LOWER_BOUND = 0x00000000;

    /**
     * The maximum integer boundary representing a valid RGB color (0x00FFFFFF).
     */
    public static final int RGB_UPPER_BOUND = 0x00FFFFFF;

    /**
     * The minimum integer boundary representing a valid RGB color (0x00000000).
     */
    public static final int ARGB_LOWER_BOUND = 0x00000000;

    /**
     * The maximum integer boundary representing a valid RGB color (0xFFFFFFFF).
     */
    public static final int ARGB_UPPER_BOUND = 0xFFFFFFFF;

    /**
     * Serializes primitive integer colors to hexadecimal strings and deserializes valid hexadecimal strings into integers.
     */
    public static final Codec<Integer> RGB_CODEC = Codec.STRING.comapFlatMap(
            hexString -> parseHexColor(hexString, 6, true),
            colorVal -> String.format("#%06X", colorVal & 0xFFFFFF)
    );

    /**
     * Serializes primitive integer colors to hexadecimal strings and deserializes valid hexadecimal strings into integers.
     */
    public static final Codec<Integer> ARGB_CODEC = Codec.STRING.comapFlatMap(
            hexString -> parseHexColor(hexString, 8, false),
            colorVal -> String.format("#%08X", colorVal)
    );

    private static DataResult<Integer> parseHexColor(String hexString, int maxDigits, boolean requireRgbRange) {
        int len = hexString.length();
        if (len == 0) {
            return DataResult.error(() -> "Empty string is not a valid hex color");
        }
        int startIndex = hexString.charAt(0) == '#' ? 1 : 0;
        if (startIndex >= len) {
            return DataResult.error(() -> "Not a valid hex color: " + hexString);
        }
        char firstChar = hexString.charAt(startIndex);
        if (firstChar == '+' || firstChar == '-') {
            return DataResult.error(() -> "Not a valid hex color: " + hexString);
        }
        if (len - startIndex > maxDigits) {
            return DataResult.error(() -> "Not a valid hex color: " + hexString);
        }
        try {
            int val = Integer.parseUnsignedInt(hexString, startIndex, len, 16);
            if (requireRgbRange && (val & 0xFF000000) != 0) {
                return DataResult.error(() -> "RGB hex color out of range: " + hexString);
            }
            return DataResult.success(val);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a valid hex color: " + hexString);
        }
    }
}