package johnsmith.configoverhauled.impl.client.gui.entry;

import java.util.List;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.NotNull;

public class EnumEntry<E extends Enum<E>> extends OptionEntry<E, CycleButton<E>> {
    public EnumEntry(Property<E> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected CycleButton<E> createWidget() {
        E[] enumConstants = this.property.defaultValue().getDeclaringClass().getEnumConstants();
        return CycleButton.<E>builder(e -> Component.literal(e.name()))
                .withValues(enumConstants)
                .withInitialValue(this.property.get())
                .displayOnlyValue()
                .create(0, 0, 75, 20, Component.empty(), (b, val) -> this.setValue(val));
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(this.property.get());
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + this.property.defaultValue().name());
    }

    protected Component getValueRange() {
        E[] values = this.property.defaultValue().getDeclaringClass().getEnumConstants();
        String joinedValues = java.util.Arrays.stream(values)
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));

        return Component.literal(joinedValues);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
        super.render(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovering, partialTick);

        if (this.widget.isMouseOver(mouseX, mouseY)) {
            List<FormattedCharSequence> boundsTooltip = List.of(this.getValueRange().getVisualOrderText());
            if (this.minecraft.screen instanceof ConfigScreen configScreen) {
                configScreen.deferredTooltip = boundsTooltip;
            } else {
                guiGraphics.setTooltipForNextFrame(this.minecraft.font, boundsTooltip, mouseX, mouseY);
            }
        }
    }
}