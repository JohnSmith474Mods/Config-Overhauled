package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListEntry<E> extends AbstractTextEntry<List<E>> {
    public ListEntry(Property<List<E>> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected void setupEditBox(EditBox box) {
        box.setResponder(s -> {
            try {
                JsonElement element = JsonParser.parseString(s);
                DataResult<List<E>> result = this.property.codec().parse(JsonOps.INSTANCE, element);
                if (result.result().isPresent()) {
                    box.setTextColor(0xFFFFFFFF);
                } else {
                    box.setTextColor(0xFFFF0000);
                }
            } catch (Exception e) {
                box.setTextColor(0xFFFF0000);
            }
        });
    }

    @Override
    protected void applyInput(String input) {
        try {
            JsonElement element = JsonParser.parseString(input);
            DataResult<List<E>> result = this.property.codec().parse(JsonOps.INSTANCE, element);
            result.result().ifPresentOrElse(
                    list -> {
                        this.setValue(list);
                        this.widget.setTextColor(0xFFFFFFFF);
                    },
                    this::restoreCurrentValue
            );
        } catch (Exception e) {
            this.restoreCurrentValue();
        }
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
    public void extractContent(@NotNull GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.extractContent(guiGraphicsExtractor, mouseX, mouseY, isHovering, partialTick);

        if (this.widget.isMouseOver(mouseX, mouseY)) {
            List<FormattedCharSequence> boundsTooltip = List.of(this.getContentPreview().getVisualOrderText());
            this.parentScreen.setDeferredTooltip(boundsTooltip);
        }
    }
}