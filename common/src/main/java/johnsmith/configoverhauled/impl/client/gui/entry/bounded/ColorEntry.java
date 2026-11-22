package johnsmith.configoverhauled.impl.client.gui.entry.bounded;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.popup.ColorPickerPopup;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ColorEntry<P extends Property<Integer>> extends BoundedEntry<Integer> {
    private final ColorSwatchButton swatchButton = new ColorSwatchButton();

    public ColorEntry(P property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super((DefaultProperty.Bounded<Integer>) property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected boolean hasDetailPopup() {
        return false;
    }

    protected boolean hasAlpha() {
        return false;
    }

    protected abstract String getHexFormatString();
    protected abstract int getMaxLength();
    protected abstract int getPreviewColor(int rawValue);

    protected abstract FormattedCharSequence formatColorString(String text, int cursorOffset);

    protected void openColorPicker() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        int initialColor = this.property.get();
        try {
            String input = this.widget.getValue();
            if (!this.isPartialInput(input)) {
                int candidate = this.parse(input);
                if (this.isWithinBounds(candidate)) {
                    initialColor = candidate;
                }
            }
        } catch (NumberFormatException ignored) {
        }

        this.parentScreen.openPopup(new ColorPickerPopup(
                this.parentScreen,
                this.minecraft,
                this.getBounds(),
                initialColor,
                this.hasAlpha(),
                this.isLocked,
                this::applyPopupValue
        ));
    }

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

            boolean isCut = event.isCut();

            // Handle mass deletion/cutting of selected text
            if (hasSelection && (keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE || isCut)) {
                if (isCut) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.widget.getHighlighted());
                }

                this.widget.insertText(""); // Erase selection natively
                this.enforceHexFormatting(); // Immediately self-heal

                return true;
            }

            // Handle single character backspace
            if (keyCode == InputConstants.KEY_BACKSPACE) {
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
            if (keyCode == InputConstants.KEY_DELETE) {
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
            if (event.isCopy()) {
                if (hasSelection) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.widget.getHighlighted());
                }
                return true;
            }

            // Handle controlled pasting
            if (event.isPaste()) {
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

    protected void renderColorPreview(GuiGraphicsExtractor guiGraphicsExtractor, int boxX, int boxY, int boxSize, int baseColor, int mouseX, int mouseY, boolean highlighted) {
        if (this.hasAlpha()) {
            ColorPickerPopup.drawCheckerboard(guiGraphicsExtractor, boxX, boxY, boxSize, boxSize, 5);
        }

        guiGraphicsExtractor.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, baseColor);

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
            this.renderNextColorPreviewTriangle(guiGraphicsExtractor, boxX, boxY, boxSize, nextColor);
        }

        guiGraphicsExtractor.outline(boxX, boxY, boxSize, boxSize, highlighted ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    protected void renderNextColorPreviewTriangle(GuiGraphicsExtractor guiGraphicsExtractor, int boxX, int boxY, int boxSize, int color) {
        for (int row = 0; row < boxSize; row++) {
            int y1 = boxY + row;
            int y2 = y1 + 1;
            int x1 = boxX + boxSize - row - 1;
            int x2 = boxX + boxSize;
            guiGraphicsExtractor.fill(x1, y1, x2, y2, color);
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
    protected int getMaxLabelWidth(int widgetX) {
        int boxSize = 20;
        int padding = 5;
        return widgetX - padding - boxSize - this.getX() - 5;
    }

    @Override
    public void extractContent(@NotNull GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        super.extractContent(guiGraphicsExtractor, mouseX, mouseY, isHovering, partialTick);

        int boxSize = 20;
        int padding = 5;
        int boxX = this.widget.getX() - padding - boxSize;
        int boxY = this.widget.getY();

        this.swatchButton.updateBounds(boxX, boxY, boxSize, boxSize);
        boolean overSwatch = this.swatchButton.isMouseOver(mouseX, mouseY);
        this.swatchButton.hovered = overSwatch;

        int color = this.getPreviewColor(this.property.get());
        this.renderColorPreview(guiGraphicsExtractor, boxX, boxY, boxSize, color, mouseX, mouseY, overSwatch || this.swatchButton.isFocused());

        if (overSwatch) {
            Component hint = Component.translatable(this.isLocked
                    ? "config_overhauled.popup.color.inspect_picker"
                    : "config_overhauled.popup.color.open_picker");
            this.parentScreen.setDeferredTooltip(List.of(
                    hint.getVisualOrderText(),
                    this.getCurrentValueTooltipText().getVisualOrderText()
            ));
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return ImmutableList.of(this.swatchButton, this.widget, this.resetButton);
    }

    @Override
    public @NotNull List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.swatchButton, this.widget, this.resetButton);
    }

    private final class ColorSwatchButton implements GuiEventListener, NarratableEntry {
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean focused;
        private boolean hovered;

        private void updateBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.isMouseOver(event.x(), event.y())) {
                ColorEntry.this.openColorPicker();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (this.focused && (event.key() == InputConstants.KEY_RETURN
                    || event.key() == InputConstants.KEY_NUMPADENTER
                    || event.key() == InputConstants.KEY_SPACE)) {
                ColorEntry.this.openColorPicker();
                return true;
            }
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            if (this.focused) {
                return NarrationPriority.FOCUSED;
            }
            return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.translatable(ColorEntry.this.isLocked
                    ? "config_overhauled.popup.color.inspect_picker"
                    : "config_overhauled.popup.color.open_picker"));
        }
    }
}