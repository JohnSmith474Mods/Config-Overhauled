package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;

public class DoubleEntry extends BoundedEntry<Double, EditBox> {

    public DoubleEntry(PropertyImpl.Bounded<Double> type, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(type, parentScreen, minecraft, onValueChanged);
        this.widget.setValue(this.formatValue(type.get()));
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Double parse(String input) throws NumberFormatException {
        return Double.parseDouble(input);
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

    private String formatValue(Double value) {
        String plain = BigDecimal.valueOf(value).toPlainString();
        if (plain.contains(".")) {
            plain = plain.replaceAll("0*$", "");
            if (plain.endsWith(".")) {
                plain += "0";
            }
        }
        return plain;
    }
}