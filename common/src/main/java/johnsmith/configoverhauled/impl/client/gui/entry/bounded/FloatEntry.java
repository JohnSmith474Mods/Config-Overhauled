package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;

public class FloatEntry extends BoundedEntry<Float, EditBox> {

    public FloatEntry(PropertyImpl.Bounded<Float> type, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(type, parentScreen, minecraft, onValueChanged);
        this.widget.setValue(this.formatValue(type.get()));
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Float parse(String input) throws NumberFormatException {
        return Float.parseFloat(input);
    }

    @Override
    protected String getRegexFilter() {
        return "-?\\d*\\.?\\d*";
    }

    @Override
    protected boolean isPartialInput(String input) {
        return input.isEmpty() || input.equals("-") || input.equals(".");
    }

    @Override
    protected EditBox createWidget() {
        return this.buildNumericBox();
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(this.formatValue(this.getBounds().get()));
        this.widget.setTextColor(0xFFFFFF);
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }


    @Override
    protected Component getCurrentValueTooltipText() {
        return Component.translatable("config_overhauled.word.value").append(Component.literal(": " + this.formatValue(this.property.get())));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.translatable("config_overhauled.word.default").append(Component.literal(": " + this.formatValue(this.property.defaultValue())));
    }

    private String formatValue(Float value) {
        String plain = new BigDecimal(String.valueOf(value)).toPlainString();
        if (plain.contains(".")) {
            plain = plain.replaceAll("0*$", "");
            if (plain.endsWith(".")) {
                plain += "0";
            }
        }
        return plain;
    }
}