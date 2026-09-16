package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.Property;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

public class BooleanEntry extends OptionEntry<Boolean, CycleButton<Boolean>> {
    public BooleanEntry(Property<Boolean> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
    }

    @Override
    protected CycleButton<Boolean> createWidget() {
        return CycleButton.onOffBuilder(property.get())
                .withInitialValue(this.property.get())
                .displayOnlyValue()
                .create(0, 0, 75, 20, Component.empty(), (b, val) -> this.setValue(val));
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(property.get());
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + (this.property.defaultValue() ? "ON" : "OFF"));
    }
}