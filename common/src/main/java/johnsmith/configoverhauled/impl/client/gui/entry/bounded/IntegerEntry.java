package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class IntegerEntry extends BoundedEntry<Integer> {
    public IntegerEntry(DefaultProperty.Integer property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.widget.setValue(String.valueOf(property.get()));
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Integer parse(String input) throws NumberFormatException {
        return Integer.parseInt(input);
    }

    @Override
    protected String getRegexFilter() {
        return "-?\\d*";
    }

    @Override
    protected boolean isPartialInput(String input) {
        return input.isEmpty() || input.equals("-");
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(String.valueOf(this.getBounds().get()));
        this.widget.setTextColor(0xFFFFFFFF);
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.translatable("config_overhauled.word.default").append(Component.literal(": " + this.property.defaultValue()));
    }
}