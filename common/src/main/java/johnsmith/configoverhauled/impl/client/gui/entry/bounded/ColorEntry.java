package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.NotNull;

import org.lwjgl.glfw.GLFW;

public abstract class ColorEntry<P extends Property<Integer>> extends BoundedEntry<Integer> {
    public ColorEntry(P property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super((DefaultProperty.Bounded<Integer>) property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    protected abstract String getHexFormatString();
    protected abstract int getMaxLength();
    protected abstract int getPreviewColor(int rawValue);

    protected abstract FormattedCharSequence formatColorString(String text, int cursorOffset);

    @Override
    protected Integer parse(String input) throws NumberFormatException {
        String cleanHex = input.startsWith("#") ? input.substring(1) : input;
        return Integer.parseUnsignedInt(cleanHex, 16);
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
        super.setupEditBox(box);
        box.setMaxLength(this.getMaxLength());
        box.setFormatter(this::formatColorString);
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setValue(String.format(this.getHexFormatString(), this.property.get()));
        this.widget.setCursorPosition(0);
        this.widget.setHighlightPos(0);
    }

    @Override
    protected Boolean isWithinBounds(Integer value) {
        return Integer.compareUnsigned(value, this.getBounds().lowerBound) >= 0 &&
                Integer.compareUnsigned(value, this.getBounds().upperBound) <= 0;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.widget.isFocused()) {
            if (!this.widget.getHighlighted().isEmpty()) {
                this.widget.setHighlightPos(this.widget.getCursorPosition());
            }
            String c = String.valueOf(codePoint);
            if (c.matches("[0-9a-fA-F]")) {
                int cursor = this.widget.getCursorPosition();
                if (cursor == 0) cursor = 1;
                String val = this.widget.getValue();
                if (cursor < this.getMaxLength() && val.length() == this.getMaxLength()) {
                    this.widget.setValue(val.substring(0, cursor) + c.toUpperCase() + val.substring(cursor + 1));

                    // TARGET: Synchronize positions to collapse selection
                    this.widget.setCursorPosition(cursor + 1);
                    this.widget.setHighlightPos(cursor + 1);
                }
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.widget.isFocused()) {
            int cursor = this.widget.getCursorPosition();
            String val = this.widget.getValue();
            boolean hasSelection = !this.widget.getHighlighted().isEmpty();

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE ||
                    keyCode == GLFW.GLFW_KEY_DELETE ||
                    (Screen.hasControlDown() && (keyCode == GLFW.GLFW_KEY_V || keyCode == GLFW.GLFW_KEY_X))) {
                if (hasSelection) {
                    this.widget.setHighlightPos(cursor);
                }
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (cursor > 1) {
                    this.widget.setValue(val.substring(0, cursor - 1) + "0" + val.substring(cursor));

                    this.widget.setCursorPosition(cursor - 1);
                    this.widget.setHighlightPos(cursor - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursor > 0 && cursor < val.length()) {
                    this.widget.setValue(val.substring(0, cursor) + "0" + val.substring(cursor + 1));

                    this.widget.setCursorPosition(cursor + 1);
                    this.widget.setHighlightPos(cursor + 1);
                }
                return true;
            }

            if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_X) {
                return true;
            }

            if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                String clean = clipboard.replaceAll("[^0-9a-fA-F]", "").toUpperCase();
                if (!clean.isEmpty()) {
                    if (cursor == 0) cursor = 1;
                    int remaining = val.length() - cursor;
                    int toCopy = Math.min(clean.length(), remaining);
                    if (toCopy > 0) {
                        this.widget.setValue(val.substring(0, cursor) + clean.substring(0, toCopy) + val.substring(cursor + toCopy));

                        this.widget.setCursorPosition(cursor + toCopy);
                        this.widget.setHighlightPos(cursor + toCopy);
                    }
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void renderColorPreview(GuiGraphics guiGraphics, int boxX, int boxY, int boxSize, int baseColor, int mouseX, int mouseY) {
        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, baseColor);

        boolean overResetButton = mouseX >= this.resetButton.getX() && mouseX < this.resetButton.getX() + this.resetButton.getWidth() &&
                mouseY >= this.resetButton.getY() && mouseY < this.resetButton.getY() + this.resetButton.getHeight();

        int nextColor = baseColor;

        if (overResetButton) {
            nextColor = this.getPreviewColor(this.property.defaultValue());
        } else {
            try {
                String input = this.widget.getValue();
                if (!this.isPartialInput(input)) {
                    nextColor = this.getPreviewColor(this.parse(input));
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
        return Component.translatable("config_overhauled.word.value").append(Component.literal(String.format(": " + this.getHexFormatString(), this.property.get())));
    }

    @Override
    protected Component getBoundsTooltipText() {
        return Component.translatable("config_overhauled.word.range").append(Component.literal(String.format(": [ " + this.getHexFormatString() + " - " + this.getHexFormatString() + " ]", this.getBounds().lowerBound, this.getBounds().upperBound)));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.translatable("config_overhauled.word.default").append(Component.literal(String.format(": " + this.getHexFormatString(), this.property.defaultValue())));
    }

    @Override
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, isHovering, partialTick);

        int boxSize = 20;
        int padding = 5;
        int boxX = this.widget.getX() - padding - boxSize;
        int boxY = this.widget.getY();

        int color = this.getPreviewColor(this.property.get());

        this.renderColorPreview(guiGraphics, boxX, boxY, boxSize, color, mouseX, mouseY);
    }
}