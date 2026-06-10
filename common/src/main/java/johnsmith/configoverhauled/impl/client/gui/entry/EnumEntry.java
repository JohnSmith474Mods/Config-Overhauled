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
        @SuppressWarnings("unchecked")
        E[] enumConstants = (E[]) this.property.defaultValue().getClass().getEnumConstants();

        CycleButton<E> button = CycleButton.builder((E e) -> Component.literal(e.name()), this.property.get())
                .withValues(enumConstants)
                .displayOnlyValue()
                .create(0, 0, 75, 20, Component.empty(), (b, val) -> this.setValue(val));

        button.setValue(this.property.get());

        return button;
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
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);

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