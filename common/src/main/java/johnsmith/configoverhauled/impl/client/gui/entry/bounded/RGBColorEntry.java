package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class RGBColorEntry extends ColorEntry<Property<Integer>> {
    public RGBColorEntry(Property<Integer> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
    }

    @Override
    protected String getHexFormatString() {
        return "#%06X";
    }

    @Override
    protected int getMaxLength() {
        return 7;
    }

    @Override
    protected int getPreviewColor(int rawValue) {
        return rawValue | 0xFF000000;
    }

    @Override
    protected FormattedCharSequence formatColorString(String text, int cursorOffset) {
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int color = 0xFFFFFFFF;

            int absoluteIndex = cursorOffset + i;
            if (absoluteIndex == 0) color = 0xFFAAAAAA;
            else if (absoluteIndex == 1 || absoluteIndex == 2) color = 0xFFFF5555;
            else if (absoluteIndex == 3 || absoluteIndex == 4) color = 0xFF55FF55;
            else if (absoluteIndex == 5 || absoluteIndex == 6) color = 0xFF5555FF;

            component.append(Component.literal(String.valueOf(c)).withStyle(Style.EMPTY.withColor(color)));
        }
        return component.getVisualOrderText();
    }
}