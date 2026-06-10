package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import java.util.List;

import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.AbstractTextEntry;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.NotNull;

public abstract class BoundedEntry<T extends Number & Comparable<T>> extends AbstractTextEntry<T> {
    public BoundedEntry(PropertyImpl.Bounded<T> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
    }

    protected PropertyImpl.Bounded<T> getBounds() {
        return (PropertyImpl.Bounded<T>) this.property;
    }

    protected T clamp(T value) {
        if (value.compareTo(this.getBounds().lowerBound) < 0) {
            return this.getBounds().lowerBound;
        }
        if (value.compareTo(this.getBounds().upperBound) > 0) {
            return this.getBounds().upperBound;
        }
        return value;
    }

    protected abstract T parse(String input) throws NumberFormatException;

    protected abstract String getRegexFilter();

    protected abstract boolean isPartialInput(String input);

    @Override
    protected void setupEditBox(EditBox box) {
        PropertyImpl.Bounded<T> bounds = this.getBounds();
        box.setFilter(s -> s.matches(this.getRegexFilter()));
        box.setResponder(s -> {
            try {
                if (this.isPartialInput(s)) {
                    box.setTextColor(0xFFFFFFFF);
                    return;
                }
                T val = this.parse(s);

                if (val.compareTo(bounds.lowerBound) >= 0 && val.compareTo(bounds.upperBound) <= 0) {
                    box.setTextColor(0xFFFFFFFF);
                } else {
                    box.setTextColor(0xFFFF0000);
                }
            } catch (NumberFormatException ignored) {
                box.setTextColor(0xFFFF0000);
            }
        });
    }

    @Override
    protected void applyInput(String input) {
        try {
            if (this.isPartialInput(input)) {
                this.restoreCurrentValue();
                return;
            }
            T val = this.parse(input);

            if (val.compareTo(this.getBounds().lowerBound) >= 0 && val.compareTo(this.getBounds().upperBound) <= 0) {
                this.setValue(val);
                this.widget.setTextColor(0xFFFFFFFF);
            } else {
                this.restoreCurrentValue();
            }
        } catch (NumberFormatException ignored) {
            this.restoreCurrentValue();
        }
    }

    protected Component getCurrentValueTooltipText() {
        return Component.translatable("config_overhauled.word.value").append(Component.literal(": " + this.property.get()));
    }

    protected Component getBoundsTooltipText() {
        return Component.translatable("config_overhauled.word.range").append(Component.literal(String.format(": [ %s - %s ]", this.getBounds().lowerBound, this.getBounds().upperBound)));
    }

    @Override
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);

        if (this.widget.isMouseOver(mouseX, mouseY)) {
            List<FormattedCharSequence> boundsTooltip = List.of(this.getBoundsTooltipText().getVisualOrderText(), this.getCurrentValueTooltipText().getVisualOrderText());
            if (this.minecraft.screen instanceof ConfigScreen configScreen) {
                configScreen.deferredTooltip = boundsTooltip;
            } else {
                guiGraphics.setTooltipForNextFrame(this.minecraft.font, boundsTooltip, mouseX, mouseY);
            }
        }
    }
}