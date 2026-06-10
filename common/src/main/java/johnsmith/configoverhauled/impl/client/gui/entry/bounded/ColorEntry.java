package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

public class ColorEntry extends BoundedEntry<Integer> {
    public ColorEntry(PropertyImpl.Color property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected Integer parse(String input) throws NumberFormatException {
        String cleanHex = input.startsWith("#") ? input.substring(1) : input;
        return Integer.parseInt(cleanHex, 16);
    }

    @Override
    protected String getRegexFilter() {
        return "^#?[0-9a-fA-F]*$";
    }

    @Override
    protected boolean isPartialInput(String input) {
        return input.isEmpty() || input.equals("#");
    }

    @Override
    protected void setupEditBox(EditBox box) {
        super.setupEditBox(box); // Applies the BoundedEntry regex responder
        box.setMaxLength(7);
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(String.format("#%06X", this.getBounds().get()));
        this.widget.setTextColor(0xFFFFFFFF);
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    protected void renderColorPreview(GuiGraphics guiGraphics, int boxX, int boxY, int boxSize, int baseColor, int mouseX, int mouseY) {
        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, baseColor);

        boolean overResetButton = mouseX >= this.resetButton.getX() && mouseX < this.resetButton.getX() + this.resetButton.getWidth() &&
                mouseY >= this.resetButton.getY() && mouseY < this.resetButton.getY() + this.resetButton.getHeight();

        int nextColor = baseColor;

        if (overResetButton) {
            nextColor = this.property.defaultValue() | 0xFF000000;
        } else {
            try {
                String input = this.widget.getValue();
                if (!this.isPartialInput(input)) {
                    nextColor = this.parse(input) | 0xFF000000;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (nextColor != baseColor) {
            this.renderNextColorPreviewTriangle(guiGraphics, boxX, boxY, boxSize, nextColor);
        }

        guiGraphics.submitOutline(boxX, boxY, boxSize, boxSize, 0xFFAAAAAA);
    }

    protected void renderNextColorPreviewTriangle(GuiGraphics guiGraphics, int boxX, int boxY, int boxSize, int color) {
        for (int row = 0; row < boxSize; row++) {
            int y1 = boxY + row;
            int y2 = y1 + 1;
            int x1 = boxX + boxSize - row - 1;
            int x2 = boxX + boxSize;
            guiGraphics.fill(x1, y1, x2, y2, color);
        }
    }

    @Override
    protected Component getCurrentValueTooltipText() {
        return Component.translatable("config_overhauled.word.value").append(Component.literal(String.format(": #%06X", this.property.get())));
    }

    @Override
    protected Component getBoundsTooltipText() {
        return Component.translatable("config_overhauled.word.range").append(Component.literal(String.format(": [ #%06X - #%06X ]", this.getBounds().lowerBound, this.getBounds().upperBound)));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.translatable("config_overhauled.word.default").append(Component.literal(String.format(": #%06X", this.property.defaultValue())));
    }

    @Override
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);

        int boxSize = 20;
        int padding = 5;
        int boxX = this.widget.getX() - padding - boxSize;
        int boxY = this.widget.getY();

        int color = this.getBounds().get() | 0xFF000000;

        this.renderColorPreview(guiGraphics, boxX, boxY, boxSize, color, mouseX, mouseY);
    }
}