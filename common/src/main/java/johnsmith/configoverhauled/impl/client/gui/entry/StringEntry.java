package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StringEntry extends AbstractTextEntry<String> {
    public StringEntry(Property<String> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected void setupEditBox(EditBox box) {
        box.setResponder(s -> box.setTextColor(0xFFFFFFFF));
    }

    @Override
    protected void applyInput(String input) {
        this.setValue(input);
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
            this.parentScreen.setDeferredTooltip(boundsTooltip);
        }
    }
}