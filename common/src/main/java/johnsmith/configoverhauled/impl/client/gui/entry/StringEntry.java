package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StringEntry extends AbstractTextEntry<String> {
    public StringEntry(Property<String> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected EditBox createWidget() {
        EditBox box = new EditBox(this.minecraft.font, 0, 0, 75, 20, Component.empty());
        box.setMaxLength(32767);
        box.setResponder(s -> {
            this.setValue(s);
            box.setTextColor(0xFFFFFFFF);
        });
        return box;
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(this.property.get());
        this.widget.setTextColor(0xFFFFFFFF);
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: \"" + this.property.defaultValue() + "\"");
    }

    protected Component getContentPreview() {
        return Component.literal("\"" + this.property.get() + "\"");
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
        super.render(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovering, partialTick);

        if (this.widget.isMouseOver(mouseX, mouseY)) {
            List<FormattedCharSequence> boundsTooltip = List.of(this.getContentPreview().getVisualOrderText());
            if (this.minecraft.screen instanceof ConfigScreen configScreen) {
                configScreen.deferredTooltip = boundsTooltip;
            } else {
                guiGraphics.setTooltipForNextFrame(this.minecraft.font, boundsTooltip, mouseX, mouseY);
            }
        }
    }
}