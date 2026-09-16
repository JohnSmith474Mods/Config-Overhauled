package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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
        box.addFormatter(this::formatColorString);
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

    protected void enforceHexFormatting() {
        // 1. Self-heal the prefix if a mass-deletion wiped it out
        if (!this.widget.getValue().startsWith("#")) {
            int tempCursor = this.widget.getCursorPosition();
            this.widget.setCursorPosition(0);
            this.widget.insertText("#");
            // Shift the cursor right to account for the restored symbol
            this.widget.setCursorPosition(tempCursor + 1);
        }

        // 2. Pad any missing spaces with '0' to enforce strict length
        int missing = this.getMaxLength() - this.widget.getValue().length();
        if (missing > 0) {
            int savedCursor = this.widget.getCursorPosition();
            // Move cursor to the absolute end to apply the padding
            this.widget.setCursorPosition(this.widget.getValue().length());
            this.widget.insertText("0".repeat(missing));

            // Restore cursor to the user's active position
            this.widget.setCursorPosition(savedCursor);
            this.widget.setHighlightPos(savedCursor);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.widget.isFocused()) {
            char typedChar = (char) event.codepoint();
            String c = String.valueOf(typedChar);

            if (c.matches("[0-9a-fA-F]")) {
                boolean hasSelection = !this.widget.getHighlighted().isEmpty();
                int cursor = this.widget.getCursorPosition();

                if (cursor == 0 && !hasSelection) {
                    this.widget.setCursorPosition(1); // Protect the '#' prefix
                    cursor = 1;
                }

                if (hasSelection) {
                    this.widget.insertText(c.toUpperCase());
                    this.enforceHexFormatting();
                } else {
                    if (cursor < this.getMaxLength()) {
                        // Select the single character ahead and natively overwrite it
                        this.widget.setHighlightPos(cursor + 1);
                        this.widget.insertText(c.toUpperCase());
                        this.enforceHexFormatting();
                    }
                }
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.widget.isFocused()) {
            boolean hasSelection = !this.widget.getHighlighted().isEmpty();
            int cursor = this.widget.getCursorPosition();
            int keyCode = event.key();

            boolean isControlDown = (event.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;

            // Handle mass deletion/cutting of selected text
            if (hasSelection && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE || (isControlDown && keyCode == GLFW.GLFW_KEY_X))) {
                if (isControlDown && keyCode == GLFW.GLFW_KEY_X) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.widget.getHighlighted());
                }

                this.widget.insertText(""); // Erase selection natively
                this.enforceHexFormatting(); // Immediately self-heal

                return true;
            }

            // Handle single character backspace
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (cursor > 1) { // Prevents backspacing into the '#'
                    this.widget.setCursorPosition(cursor - 1);
                    this.widget.setHighlightPos(cursor);
                    this.widget.insertText("0");

                    this.widget.setCursorPosition(cursor - 1);
                    this.widget.setHighlightPos(cursor - 1);
                }
                return true;
            }

            // Handle single character delete
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursor > 0 && cursor < this.getMaxLength()) { // Prevents deleting the '#'
                    this.widget.setCursorPosition(cursor);
                    this.widget.setHighlightPos(cursor + 1);
                    this.widget.insertText("0");

                    this.widget.setCursorPosition(cursor);
                    this.widget.setHighlightPos(cursor);
                }
                return true;
            }

            // Allow copying
            if (isControlDown && keyCode == GLFW.GLFW_KEY_C) {
                if (hasSelection) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.widget.getHighlighted());
                }
                return true;
            }

            // Handle controlled pasting
            if (isControlDown && keyCode == GLFW.GLFW_KEY_V) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                String clean = clipboard.replaceAll("[^0-9a-fA-F]", "").toUpperCase();

                if (!clean.isEmpty()) {
                    if (hasSelection) {
                        this.widget.insertText(""); // Clear selection

                        // If clearing the selection wiped the '#', restore it before pasting
                        if (!this.widget.getValue().startsWith("#")) {
                            this.widget.setCursorPosition(0);
                            this.widget.insertText("#");
                            this.widget.setCursorPosition(1);
                        }
                    } else if (cursor == 0) {
                        this.widget.setCursorPosition(1); // Protect '#'
                    }

                    int missing = this.getMaxLength() - this.widget.getValue().length();
                    int charsToPaste = Math.min(clean.length(), missing);

                    if (charsToPaste > 0) {
                        this.widget.insertText(clean.substring(0, charsToPaste));
                    }

                    this.enforceHexFormatting();
                }
                return true;
            }
        }
        return super.keyPressed(event);
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