package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListEntry<E> extends AbstractTextEntry<List<E>> {
    private String cachedSerializedValue = "[]";
    private List<FormattedCharSequence> cachedPreviewTooltip = List.of();

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
                        this.refreshPreviewCache();
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
        result.result().ifPresentOrElse(
                json -> {
                    String serialized = json.toString();
                    this.widget.setValue(serialized);
                    this.widget.setCursorPosition(0);
                    this.widget.setHighlightPos(0);
                    this.updatePreviewCache(serialized);
                },
                () -> this.updatePreviewCache("[]")
        );
    }

    private void refreshPreviewCache() {
        DataResult<JsonElement> result = this.property.codec().encodeStart(JsonOps.INSTANCE, this.property.get());
        this.updatePreviewCache(result.result().map(JsonElement::toString).orElse("[]"));
    }

    private void updatePreviewCache(String serialized) {
        this.cachedSerializedValue = serialized;
        this.cachedPreviewTooltip = List.of(this.getContentPreview().getVisualOrderText());
    }

    @Override
    protected Component getDefaultValueTooltip() {
        DataResult<JsonElement> result = this.property.codec().encodeStart(JsonOps.INSTANCE, this.property.defaultValue());
        return Component.literal("Default: " + result.result().map(JsonElement::toString).orElse("[]"));
    }

    protected Component getContentPreview() {
        return Component.literal("\"" + this.cachedSerializedValue + "\"");
    }

    @Override
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);

        if (this.widget.isMouseOver(mouseX, mouseY)) {
            this.parentScreen.setDeferredTooltip(this.cachedPreviewTooltip);
        }
    }
}