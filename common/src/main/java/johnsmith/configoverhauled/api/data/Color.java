package johnsmith.configoverhauled.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Acts as a type-key for dynamic factory registration and constructor resolution.
 * This class is not instantiable. Color properties process data strictly as primitive integers.
 */
public final class Color {
    /**
     * The minimum integer boundary representing a valid RGB color (0x000000).
     */
    public static final int LOWER_BOUND = 0x000000;

    /**
     * The maximum integer boundary representing a valid RGB color (0xFFFFFF).
     */
    public static final int UPPER_BOUND = 0xFFFFFF;

    /**
     * Serializes primitive integer colors to hexadecimal strings and deserializes valid hexadecimal strings into integers.
     */
    public static final Codec<Integer> CODEC = Codec.STRING.comapFlatMap(
            hexString -> {
                int len = hexString.length();
                if (len == 0) {
                    return DataResult.error(() -> "Empty string is not a valid hex color");
                }
                int startIndex = hexString.charAt(0) == '#' ? 1 : 0;
                try {
                    int val = Integer.parseInt(hexString, startIndex, len, 16);
                    return DataResult.success(val);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a valid hex color: " + hexString);
                }
            },
            colorVal -> String.format("#%06X", colorVal)
    );
}