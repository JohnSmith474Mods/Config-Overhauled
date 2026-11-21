package johnsmith.configoverhauled.impl.client.gui.entry;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Renders clipped marquee-scrolling text when a label overflows its available horizontal bounds.
 * Mirrors vanilla's smooth ping-pong scrolling curve with a gentle pause at both ends.
 */
public final class MarqueeTextRenderer {
    private MarqueeTextRenderer() {}

    public static void extractScrollingText(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            Component text,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color,
            boolean centered,
            boolean dropShadow
    ) {
        int availableWidth = maxX - minX;
        if (availableWidth <= 0 || maxY <= minY) {
            return;
        }

        int textWidth = font.width(text);
        int textY = minY + (maxY - minY - font.lineHeight) / 2;

        if (textWidth <= availableWidth) {
            int drawX = centered ? minX + (availableWidth - textWidth) / 2 : minX;
            guiGraphics.text(font, text, drawX, textY, color, dropShadow);
            return;
        }

        int overflow = textWidth - availableWidth;
        double seconds = System.nanoTime() / 1_000_000_000.0;
        double scrollDuration = Math.max((double) overflow * 0.5, 3.0);
        double phase = Math.sin((Math.PI / 2.0) * Math.cos((Math.PI * 2.0) * seconds / scrollDuration)) / 2.0 + 0.5;
        double offset = Mth.lerp(phase, 0.0, (double) overflow);

        guiGraphics.enableScissor(minX, minY, maxX, maxY);
        guiGraphics.text(font, text, minX - (int) offset, textY, color, dropShadow);
        guiGraphics.disableScissor();
    }
}
