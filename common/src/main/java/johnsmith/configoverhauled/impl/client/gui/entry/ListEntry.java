package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

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

public class ListEntry<E> extends OptionEntry<List<E>, EditBox> {
    public ListEntry(Property<List<E>> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected EditBox createWidget() {
        EditBox box = new EditBox(this.minecraft.font, 0, 0, 75, 20, Component.empty());
        box.setMaxLength(32767);
        box.setResponder(s -> {
            try {
                JsonElement element = JsonParser.parseString(s);
                DataResult<List<E>> result = this.property.codec().parse(JsonOps.INSTANCE, element);
                result.result().ifPresentOrElse(list -> {
                    this.setValue(list);
                    box.setTextColor(0xFFFFFFFF);
                }, () -> box.setTextColor(0xFFFF0000));
            } catch (Exception e) {
                box.setTextColor(0xFFFF0000);
            }
        });
        return box;
    }

    @Override
    protected void updateWidgetValue() {
        DataResult<JsonElement> result = this.property.codec().encodeStart(JsonOps.INSTANCE, this.property.get());
        result.result().ifPresent(json -> {
            this.widget.setValue(json.toString());
            this.widget.setCursorPosition(0);
            this.widget.setHighlightPos(0);
        });
    }

    @Override
    protected Component getDefaultValueTooltip() {
        DataResult<JsonElement> result = this.property.codec().encodeStart(JsonOps.INSTANCE, this.property.defaultValue());
        return Component.literal("Default: " + result.result().map(JsonElement::toString).orElse("[]"));
    }

    protected Component getContentPreview() {
        DataResult<JsonElement> result = this.property.codec().encodeStart(JsonOps.INSTANCE, this.property.get());
        return Component.literal("\"" + result.result().map(JsonElement::toString).orElse("[]") + "\"");
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