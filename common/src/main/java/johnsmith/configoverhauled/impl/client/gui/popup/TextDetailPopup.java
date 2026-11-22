package johnsmith.configoverhauled.impl.client.gui.popup;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.popup.AbstractConfigPopup;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable modal popup base for editing single text-backed configuration properties in a spacious
 * centered {@link EditBox} with live validation, inline error feedback, and default/metadata hints.
 *
 * @param <T> The property value type.
 */
public abstract class TextDetailPopup<T> extends AbstractConfigPopup<T> {
    protected static final int VALID_TEXT_COLOR = 0xFFFFFFFF;
    protected static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    protected static final int INFO_TEXT_COLOR = 0xFF999999;

    protected String workingText;
    protected String lastAcceptedText;
    protected @Nullable T parsedValue;
    protected @Nullable Component validationError;

    protected EditBox editBox;
    protected Button clearButton;

    private boolean syncingEditBox = false;
    private boolean initialFocusApplied = false;

    protected TextDetailPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Property<T> property,
            @Nullable String initialText,
            boolean isLocked,
            Consumer<T> onApply
    ) {
        super(parentScreen, minecraft, property, isLocked, onApply);
        String seed = initialText != null ? initialText : this.formatValue(property.get());
        this.workingText = seed;
        this.lastAcceptedText = seed;
    }

    protected abstract ValidationResult<T> validateInput(String text);

    protected abstract String formatValue(T value);

    protected @Nullable String getRegexFilter() {
        return null;
    }

    protected int getMaxLength() {
        return 32767;
    }

    protected @Nullable Component getRightInfoText() {
        return null;
    }

    protected Component getDefaultInfoText() {
        return Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": " + this.formatValue(this.property.defaultValue())));
    }

    protected void initExtraContent(int left, int top, int width, int height) {
    }

    protected void onWorkingValueChanged(@Nullable T newValue, boolean valid) {
    }

    @Override
    protected int getPreferredWidth() {
        return 360;
    }

    @Override
    protected int getPreferredHeight() {
        return 164;
    }

    @Override
    protected @Nullable Component getDefaultValueTooltip() {
        return this.getDefaultInfoText();
    }

    @Override
    protected void initContent(int left, int top, int width, int height) {
        int inputY = top + 2;
        int clearSize = 20;
        int clearGap = 4;
        int boxWidth = Math.max(80, width - clearSize - clearGap);

        this.editBox = new EditBox(this.font, left, inputY, boxWidth, 20, this.title) {
            @Override
            public boolean keyPressed(KeyEvent event) {
                if ((event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)
                        && !TextDetailPopup.this.isLocked
                        && TextDetailPopup.this.isWorkingValueValid()) {
                    TextDetailPopup.this.applyAndClose();
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        this.editBox.setMaxLength(this.getMaxLength());
        this.editBox.setEditable(!this.isLocked);
        this.editBox.setValue(this.workingText);
        this.editBox.moveCursorToEnd(false);
        this.editBox.setResponder(this::handleTextChanged);
        this.addRenderableWidget(this.editBox);

        this.clearButton = Button.builder(Component.literal("X"), b -> {
            if (!this.isLocked) {
                this.setWorkingText("");
                this.setFocusedWidget(this.editBox);
            }
        }).bounds(left + boxWidth + clearGap, inputY, clearSize, clearSize).build();
        this.addRenderableWidget(this.clearButton);

        int extraTop = inputY + 20 + 16;
        int infoReserve = this.font.lineHeight + 4;
        int extraHeight = Math.max(0, (top + height) - extraTop - infoReserve);
        this.initExtraContent(left, extraTop, width, extraHeight);

        this.revalidate(false);

        if (!this.initialFocusApplied) {
            this.initialFocusApplied = true;
            this.setFocusedWidget(this.editBox);
        }
    }

    protected void setWorkingText(String newText) {
        this.workingText = newText;
        this.lastAcceptedText = newText;
        if (this.editBox != null) {
            this.syncingEditBox = true;
            try {
                this.editBox.setValue(newText);
                this.editBox.moveCursorToEnd(false);
            } finally {
                this.syncingEditBox = false;
            }
        }
        this.revalidate(true);
    }

    private void handleTextChanged(String text) {
        if (this.syncingEditBox) {
            return;
        }
        String regex = this.getRegexFilter();
        if (regex != null && !text.matches(regex)) {
            this.syncingEditBox = true;
            try {
                this.editBox.setValue(this.lastAcceptedText);
            } finally {
                this.syncingEditBox = false;
            }
            return;
        }

        this.workingText = text;
        this.lastAcceptedText = text;
        this.revalidate(true);
    }

    protected void revalidate(boolean notifySubclass) {
        ValidationResult<T> result = this.validateInput(this.workingText);
        if (result.valid()) {
            this.parsedValue = result.value();
            this.validationError = null;
            if (this.editBox != null) {
                this.editBox.setTextColor(VALID_TEXT_COLOR);
            }
        } else {
            this.parsedValue = null;
            this.validationError = result.error();
            if (this.editBox != null) {
                this.editBox.setTextColor(INVALID_TEXT_COLOR);
            }
        }

        if (this.clearButton != null) {
            this.clearButton.active = !this.isLocked && !this.workingText.isEmpty();
        }

        if (notifySubclass) {
            this.onWorkingValueChanged(this.parsedValue, result.valid());
        }

        this.updateFooterButtons();
    }

    @Override
    protected void extractBody(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int errorY = this.contentTop + 26;
        if (this.validationError != null) {
            String trimmedError = this.trimToWidth(this.validationError.getString(), this.contentWidth);
            guiGraphics.text(this.font, trimmedError, this.contentLeft, errorY, INVALID_TEXT_COLOR, false);
        }

        int infoY = this.contentTop + this.contentHeight - this.font.lineHeight - 1;
        Component rightInfo = this.getRightInfoText();
        int rightWidth = 0;

        if (rightInfo != null) {
            String rightStr = this.trimToWidth(rightInfo.getString(), this.contentWidth / 2);
            rightWidth = this.font.width(rightStr);
            int rightX = this.contentLeft + this.contentWidth - rightWidth;
            guiGraphics.text(this.font, rightStr, rightX, infoY, INFO_TEXT_COLOR, false);
        }

        int leftAvailable = this.contentWidth - (rightWidth > 0 ? rightWidth + 8 : 0);
        if (leftAvailable > 16) {
            String leftStr = this.trimToWidth(this.getDefaultInfoText().getString(), leftAvailable);
            guiGraphics.text(this.font, leftStr, this.contentLeft, infoY, INFO_TEXT_COLOR, false);
        }
    }

    @Override
    protected boolean isWorkingValueValid() {
        return this.validationError == null && this.parsedValue != null;
    }

    @Override
    protected boolean canResetWorkingValue() {
        if (this.property == null) {
            return false;
        }
        String defaultFormatted = this.formatValue(this.property.defaultValue());
        return !Objects.equals(this.workingText, defaultFormatted);
    }

    @Override
    protected void resetWorkingValue() {
        if (this.property != null) {
            this.setWorkingText(this.formatValue(this.property.defaultValue()));
        }
    }

    @Override
    protected @Nullable T buildResultValue() {
        return this.parsedValue;
    }

    protected String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = this.font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return "...";
        }
        return this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    public record ValidationResult<T>(boolean valid, @Nullable T value, @Nullable Component error) {
        public static <T> ValidationResult<T> ok(T value) {
            return new ValidationResult<>(true, value, null);
        }

        public static <T> ValidationResult<T> error(Component error) {
            return new ValidationResult<>(false, null, error);
        }
    }
}
