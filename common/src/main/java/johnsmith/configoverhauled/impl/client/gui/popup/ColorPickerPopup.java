package johnsmith.configoverhauled.impl.client.gui.popup;

import johnsmith.configoverhauled.api.client.gui.popup.AbstractConfigPopup;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Detailed modal color picker supporting both {@code RGB} ({@code #RRGGBB}) and
 * {@code ARGB} ({@code #AARRGGBB}) properties.
 * <p>
 * Features a 2D Saturation/Value square, vertical Hue spectrum bar, optional Alpha bar with
 * transparency checkerboard, clickable Old/New comparison swatches, and bidirectionally
 * synchronized Hex and R/G/B/(A) numeric fields.
 */
public class ColorPickerPopup extends AbstractConfigPopup<Integer> {
    private static final int PICKER_SIZE = 124;
    private static final int BAR_WIDTH = 14;
    private static final int BAR_GAP = 8;

    private static final int CHECKER_LIGHT = 0xFFCCCCCC;
    private static final int CHECKER_DARK = 0xFF888888;
    private static final int BORDER_IDLE = 0xFFAAAAAA;
    private static final int BORDER_ACTIVE = 0xFFFFFFFF;
    private static final int VALID_TEXT_COLOR = 0xFFFFFFFF;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int INFO_TEXT_COLOR = 0xFF999999;

    private static final int[] HUE_STOPS = {
            0xFFFF0000, // 0°   Red
            0xFFFFFF00, // 60°  Yellow
            0xFF00FF00, // 120° Green
            0xFF00FFFF, // 180° Cyan
            0xFF0000FF, // 240° Blue
            0xFFFF00FF, // 300° Magenta
            0xFFFF0000  // 360° Red
    };

    private final DefaultProperty.Bounded<Integer> boundedProperty;
    private final boolean hasAlpha;
    private final int originalValue;

    // Canonical HSVA working state (persists across window resizes; keeps Hue stable at S=0 or V=0)
    private float hue;
    private float saturation;
    private float value;
    private int alpha;
    private int workingPackedColor;
    private boolean inputsValid = true;

    // Layout geometry computed during initContent
    private int pickerSize = PICKER_SIZE;
    private int svLeft, svTop;
    private int hueLeft, hueTop;
    private int alphaLeft, alphaTop;
    private int oldSwatchLeft, newSwatchLeft, swatchTop, swatchHalfWidth, swatchHeight;
    private int hexLabelX, hexLabelY;
    private int rLabelX, gLabelX, bLabelX, aLabelX;
    private int rgRowY, baRowY;
    private int infoY, errorY, rightPanelLeft, rightPanelWidth;

    private EditBox hexBox;
    private EditBox redBox;
    private EditBox greenBox;
    private EditBox blueBox;
    private @Nullable EditBox alphaBox;

    private boolean syncingInputs = false;
    private DragTarget activeDrag = DragTarget.NONE;

    public ColorPickerPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            DefaultProperty.Bounded<Integer> property,
            int initialColor,
            boolean hasAlpha,
            boolean isLocked,
            Consumer<Integer> onApply
    ) {
        super(parentScreen, minecraft, property, isLocked, onApply);
        this.boundedProperty = property;
        this.hasAlpha = hasAlpha;
        this.originalValue = property.get();
        this.setWorkingColorFromPacked(initialColor, true);
    }

    @Override
    protected int getPreferredWidth() {
        return 356;
    }

    @Override
    protected int getPreferredHeight() {
        return 238;
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": " + this.formatHex(this.boundedProperty.defaultValue())));
    }

    @Override
    protected void initContent(int left, int top, int width, int height) {
        this.pickerSize = Mth.clamp(height - 4, 72, PICKER_SIZE);
        this.svLeft = left;
        this.svTop = top + 2;

        this.hueLeft = this.svLeft + this.pickerSize + BAR_GAP;
        this.hueTop = this.svTop;

        int pickerRight = this.hueLeft + BAR_WIDTH;
        if (this.hasAlpha) {
            this.alphaLeft = pickerRight + BAR_GAP;
            this.alphaTop = this.svTop;
            pickerRight = this.alphaLeft + BAR_WIDTH;
        }

        this.rightPanelLeft = pickerRight + 12;
        this.rightPanelWidth = Math.max(110, (left + width) - this.rightPanelLeft);

        // Old / New comparison swatches
        this.swatchTop = this.svTop + 11;
        this.swatchHeight = 20;
        this.swatchHalfWidth = this.rightPanelWidth / 2;
        this.oldSwatchLeft = this.rightPanelLeft;
        this.newSwatchLeft = this.rightPanelLeft + this.swatchHalfWidth;

        // Hex EditBox row
        int hexY = this.swatchTop + this.swatchHeight + 7;
        int hexLabelWidth = 24;
        this.hexLabelX = this.rightPanelLeft;
        this.hexLabelY = hexY + (18 - this.font.lineHeight) / 2;

        int hexBoxWidth = Math.max(60, this.rightPanelWidth - hexLabelWidth - 4);
        this.hexBox = this.createCommitEditBox(this.rightPanelLeft + hexLabelWidth + 4, hexY, hexBoxWidth, 18, Component.translatable("config_overhauled.popup.color.hex"));
        this.hexBox.setMaxLength(this.hasAlpha ? 9 : 7);
        this.hexBox.addFormatter(this::formatHexDisplay);
        this.hexBox.setResponder(this::onHexChanged);
        this.addRenderableWidget(this.hexBox);

        // 2x2 RGBA numeric grid
        int channelGap = 8;
        int channelLabelW = 10;
        int colWidth = (this.rightPanelWidth - channelGap) / 2;
        int numBoxWidth = Math.max(32, colWidth - channelLabelW - 3);
        int col2X = this.rightPanelLeft + colWidth + channelGap;

        int rgY = hexY + 24;
        this.rgRowY = rgY + (18 - this.font.lineHeight) / 2;
        this.rLabelX = this.rightPanelLeft;
        this.gLabelX = col2X;

        this.redBox = this.createChannelBox(this.rightPanelLeft + channelLabelW + 3, rgY, numBoxWidth, Component.literal("R"));
        this.greenBox = this.createChannelBox(col2X + channelLabelW + 3, rgY, numBoxWidth, Component.literal("G"));

        int baY = rgY + 22;
        this.baRowY = baY + (18 - this.font.lineHeight) / 2;
        this.bLabelX = this.rightPanelLeft;
        this.aLabelX = col2X;

        this.blueBox = this.createChannelBox(this.rightPanelLeft + channelLabelW + 3, baY, numBoxWidth, Component.literal("B"));
        if (this.hasAlpha) {
            this.alphaBox = this.createChannelBox(col2X + channelLabelW + 3, baY, numBoxWidth, Component.literal("A"));
        } else {
            this.alphaBox = null;
        }

        this.infoY = baY + 22;
        this.errorY = this.infoY + this.font.lineHeight + 2;

        this.syncAllInputsFromWorkingColor();
        this.updateFooterButtons();
    }

    private EditBox createCommitEditBox(int x, int y, int width, int height, Component narration) {
        EditBox box = new EditBox(this.font, x, y, width, height, narration) {
            @Override
            public boolean keyPressed(KeyEvent event) {
                if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
                        && !ColorPickerPopup.this.isLocked
                        && ColorPickerPopup.this.isWorkingValueValid()) {
                    ColorPickerPopup.this.applyAndClose();
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        box.setEditable(!this.isLocked);
        return box;
    }

    private EditBox createChannelBox(int x, int y, int width, Component narration) {
        EditBox box = this.createCommitEditBox(x, y, width, 18, narration);
        box.setMaxLength(3);
        box.setResponder(s -> this.onChannelInputChanged());
        return this.addRenderableWidget(box);
    }

    private void onHexChanged(String text) {
        if (this.syncingInputs) {
            return;
        }
        String clean = text.startsWith("#") ? text.substring(1) : text;
        int expectedDigits = this.hasAlpha ? 8 : 6;
        if (clean.length() != expectedDigits || !clean.matches("[0-9a-fA-F]+")) {
            this.inputsValid = false;
            this.hexBox.setTextColor(INVALID_TEXT_COLOR);
            this.updateFooterButtons();
            return;
        }

        try {
            int parsed = Integer.parseUnsignedInt(clean, 16);
            this.setWorkingColorFromPacked(parsed, false);
            this.inputsValid = true;
            this.hexBox.setTextColor(VALID_TEXT_COLOR);
            this.syncChannelBoxesFromWorkingColor();
            this.updateFooterButtons();
        } catch (NumberFormatException e) {
            this.inputsValid = false;
            this.hexBox.setTextColor(INVALID_TEXT_COLOR);
            this.updateFooterButtons();
        }
    }

    private void onChannelInputChanged() {
        if (this.syncingInputs) {
            return;
        }
        Integer r = parseChannel(this.redBox);
        Integer g = parseChannel(this.greenBox);
        Integer b = parseChannel(this.blueBox);
        Integer a = this.hasAlpha && this.alphaBox != null ? parseChannel(this.alphaBox) : Integer.valueOf(255);

        if (r == null || g == null || b == null || a == null) {
            this.inputsValid = false;
            this.updateFooterButtons();
            return;
        }

        int rgb = (r << 16) | (g << 8) | b;
        int packed = this.hasAlpha ? ((a << 24) | rgb) : rgb;
        this.setWorkingColorFromPacked(packed, false);
        this.inputsValid = true;

        this.syncingInputs = true;
        try {
            this.hexBox.setValue(this.formatHex(this.workingPackedColor));
            this.hexBox.setTextColor(VALID_TEXT_COLOR);
        } finally {
            this.syncingInputs = false;
        }
        this.updateFooterButtons();
    }

    private static @Nullable Integer parseChannel(EditBox box) {
        String text = box.getValue().trim();
        if (text.isEmpty() || !text.matches("\\d{1,3}")) {
            box.setTextColor(INVALID_TEXT_COLOR);
            return null;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 0 || value > 255) {
                box.setTextColor(INVALID_TEXT_COLOR);
                return null;
            }
            box.setTextColor(VALID_TEXT_COLOR);
            return value;
        } catch (NumberFormatException e) {
            box.setTextColor(INVALID_TEXT_COLOR);
            return null;
        }
    }

    private void setWorkingColorFromPacked(int packed, boolean resetHueFallback) {
        int masked = this.hasAlpha ? packed : (packed & 0x00FFFFFF);
        this.workingPackedColor = masked;
        this.alpha = this.hasAlpha ? ((masked >>> 24) & 0xFF) : 255;

        float[] hsv = rgbToHsv(masked & 0x00FFFFFF, resetHueFallback ? 0.0f : this.hue);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    private void recomputePackedFromHsva() {
        int rgb = hsvToRgb(this.hue, this.saturation, this.value);
        this.workingPackedColor = this.hasAlpha ? ((this.alpha << 24) | rgb) : rgb;
        this.inputsValid = true;
        this.syncAllInputsFromWorkingColor();
        this.updateFooterButtons();
    }

    private void syncAllInputsFromWorkingColor() {
        if (this.hexBox == null) {
            return;
        }
        this.syncingInputs = true;
        try {
            this.hexBox.setValue(this.formatHex(this.workingPackedColor));
            this.hexBox.setTextColor(VALID_TEXT_COLOR);
            this.hexBox.setCursorPosition(0);
            this.hexBox.setHighlightPos(0);
            this.syncChannelBoxesInternal();
        } finally {
            this.syncingInputs = false;
        }
    }

    private void syncChannelBoxesFromWorkingColor() {
        if (this.redBox == null) {
            return;
        }
        this.syncingInputs = true;
        try {
            this.syncChannelBoxesInternal();
        } finally {
            this.syncingInputs = false;
        }
    }

    private void syncChannelBoxesInternal() {
        int r = (this.workingPackedColor >> 16) & 0xFF;
        int g = (this.workingPackedColor >> 8) & 0xFF;
        int b = this.workingPackedColor & 0xFF;

        this.setChannelBox(this.redBox, r);
        this.setChannelBox(this.greenBox, g);
        this.setChannelBox(this.blueBox, b);
        if (this.hasAlpha && this.alphaBox != null) {
            this.setChannelBox(this.alphaBox, this.alpha);
        }
    }

    private void setChannelBox(EditBox box, int val) {
        box.setValue(String.valueOf(val));
        box.setTextColor(VALID_TEXT_COLOR);
        box.setCursorPosition(0);
        box.setHighlightPos(0);
    }

    @Override
    protected boolean isWorkingValueValid() {
        return this.inputsValid && this.isWithinUnsignedBounds(this.workingPackedColor);
    }

    @Override
    protected boolean canResetWorkingValue() {
        return !this.inputsValid || this.workingPackedColor != this.boundedProperty.defaultValue();
    }

    @Override
    protected void resetWorkingValue() {
        this.setWorkingColorFromPacked(this.boundedProperty.defaultValue(), true);
        this.inputsValid = true;
        this.syncAllInputsFromWorkingColor();
        this.updateFooterButtons();
    }

    @Override
    protected Integer buildResultValue() {
        return this.workingPackedColor;
    }

    private boolean isWithinUnsignedBounds(int color) {
        return Integer.compareUnsigned(color, this.boundedProperty.lowerBound) >= 0
                && Integer.compareUnsigned(color, this.boundedProperty.upperBound) <= 0;
    }

    private int toPreviewArgb(int packed) {
        return this.hasAlpha ? packed : (packed | 0xFF000000);
    }

    private String formatHex(int packed) {
        return this.hasAlpha
                ? String.format(Locale.ROOT, "#%08X", packed)
                : String.format(Locale.ROOT, "#%06X", packed & 0x00FFFFFF);
    }

    private FormattedCharSequence formatHexDisplay(String text, int cursorOffset) {
        if (!this.inputsValid) {
            return Component.literal(text).withStyle(Style.EMPTY.withColor(INVALID_TEXT_COLOR)).getVisualOrderText();
        }
        String fullValue = this.hexBox != null ? this.hexBox.getValue() : text;
        boolean hasPrefix = fullValue.startsWith("#");
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int absoluteIndex = cursorOffset + i;
            int color = 0xFFFFFFFF;
            if (hasPrefix && absoluteIndex == 0) {
                color = 0xFFAAAAAA;
            } else {
                int digitIndex = hasPrefix ? absoluteIndex - 1 : absoluteIndex;
                if (this.hasAlpha) {
                    if (digitIndex == 0 || digitIndex == 1) color = 0xFFFFFFFF;
                    else if (digitIndex == 2 || digitIndex == 3) color = 0xFFFF5555;
                    else if (digitIndex == 4 || digitIndex == 5) color = 0xFF55FF55;
                    else if (digitIndex == 6 || digitIndex == 7) color = 0xFF5555FF;
                } else {
                    if (digitIndex == 0 || digitIndex == 1) color = 0xFFFF5555;
                    else if (digitIndex == 2 || digitIndex == 3) color = 0xFF55FF55;
                    else if (digitIndex == 4 || digitIndex == 5) color = 0xFF5555FF;
                }
            }
            component.append(Component.literal(String.valueOf(c)).withStyle(Style.EMPTY.withColor(color)));
        }
        return component.getVisualOrderText();
    }

    @Override
    protected void extractBody(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractSvSquare(guiGraphics, mouseX, mouseY);
        this.extractHueBar(guiGraphics, mouseX, mouseY);
        if (this.hasAlpha) {
            this.extractAlphaBar(guiGraphics, mouseX, mouseY);
        }
        this.extractComparisonSwatches(guiGraphics, mouseX, mouseY);
        this.extractLabelsAndInfo(guiGraphics);
    }

    private void extractSvSquare(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int size = this.pickerSize;
        int svRight = this.svLeft + size;
        int svBottom = this.svTop + size;

        // Vertical value interpolation from HSV(H, S, 1) to pure black is mathematically linear in RGB.
        for (int col = 0; col < size; col++) {
            float sat = (float) col / (float) (size - 1);
            int topColor = hsvToRgb(this.hue, sat, 1.0f) | 0xFF000000;
            guiGraphics.fillGradient(this.svLeft + col, this.svTop, this.svLeft + col + 1, svBottom, topColor, 0xFF000000);
        }

        // Crosshair reticle clipped strictly inside the SV box
        int cx = this.svLeft + Math.round(this.saturation * (size - 1));
        int cy = this.svTop + Math.round((1.0f - this.value) * (size - 1));
        guiGraphics.enableScissor(this.svLeft, this.svTop, svRight, svBottom);
        guiGraphics.outline(cx - 3, cy - 3, 7, 7, 0xFF000000);
        guiGraphics.outline(cx - 2, cy - 2, 5, 5, 0xFFFFFFFF);
        guiGraphics.disableScissor();

        boolean active = this.activeDrag == DragTarget.SV || this.isOverSv(mouseX, mouseY);
        guiGraphics.outline(this.svLeft - 1, this.svTop - 1, size + 2, size + 2, active ? BORDER_ACTIVE : BORDER_IDLE);
    }

    private void extractHueBar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int size = this.pickerSize;
        int segments = HUE_STOPS.length - 1;
        for (int i = 0; i < segments; i++) {
            int segTop = this.hueTop + (i * size) / segments;
            int segBottom = this.hueTop + ((i + 1) * size) / segments;
            guiGraphics.fillGradient(this.hueLeft, segTop, this.hueLeft + BAR_WIDTH, segBottom, HUE_STOPS[i], HUE_STOPS[i + 1]);
        }

        boolean active = this.activeDrag == DragTarget.HUE || this.isOverHue(mouseX, mouseY);
        guiGraphics.outline(this.hueLeft - 1, this.hueTop - 1, BAR_WIDTH + 2, size + 2, active ? BORDER_ACTIVE : BORDER_IDLE);

        int markerY = this.hueTop + Math.round((this.hue / 360.0f) * (size - 1));
        this.extractBarMarker(guiGraphics, this.hueLeft, BAR_WIDTH, markerY);
    }

    private void extractAlphaBar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int size = this.pickerSize;
        drawCheckerboard(guiGraphics, this.alphaLeft, this.alphaTop, BAR_WIDTH, size, 7);

        int rgb = hsvToRgb(this.hue, this.saturation, this.value);
        int opaqueTop = 0xFF000000 | rgb;
        int transparentBottom = rgb & 0x00FFFFFF;
        guiGraphics.fillGradient(this.alphaLeft, this.alphaTop, this.alphaLeft + BAR_WIDTH, this.alphaTop + size, opaqueTop, transparentBottom);

        boolean active = this.activeDrag == DragTarget.ALPHA || this.isOverAlpha(mouseX, mouseY);
        guiGraphics.outline(this.alphaLeft - 1, this.alphaTop - 1, BAR_WIDTH + 2, size + 2, active ? BORDER_ACTIVE : BORDER_IDLE);

        int markerY = this.alphaTop + Math.round((1.0f - (this.alpha / 255.0f)) * (size - 1));
        this.extractBarMarker(guiGraphics, this.alphaLeft, BAR_WIDTH, markerY);
    }

    private void extractBarMarker(GuiGraphicsExtractor guiGraphics, int barLeft, int barWidth, int centerY) {
        guiGraphics.fill(barLeft - 2, centerY - 1, barLeft + barWidth + 2, centerY + 2, 0xFF000000);
        guiGraphics.fill(barLeft - 1, centerY, barLeft + barWidth + 1, centerY + 1, 0xFFFFFFFF);
    }

    private void extractComparisonSwatches(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int labelY = this.svTop;
        Component oldLabel = Component.translatable("config_overhauled.popup.color.old");
        Component newLabel = Component.translatable("config_overhauled.popup.color.new");

        guiGraphics.text(this.font, oldLabel, this.oldSwatchLeft, labelY, INFO_TEXT_COLOR, false);
        guiGraphics.text(this.font, newLabel, this.newSwatchLeft + 2, labelY, INFO_TEXT_COLOR, false);

        int totalW = this.swatchHalfWidth * 2;
        if (this.hasAlpha) {
            drawCheckerboard(guiGraphics, this.oldSwatchLeft, this.swatchTop, totalW, this.swatchHeight, 5);
        }

        guiGraphics.fill(this.oldSwatchLeft, this.swatchTop, this.newSwatchLeft, this.swatchTop + this.swatchHeight, this.toPreviewArgb(this.originalValue));
        guiGraphics.fill(this.newSwatchLeft, this.swatchTop, this.oldSwatchLeft + totalW, this.swatchTop + this.swatchHeight, this.toPreviewArgb(this.workingPackedColor));

        guiGraphics.outline(this.oldSwatchLeft, this.swatchTop, totalW, this.swatchHeight, BORDER_IDLE);
        guiGraphics.fill(this.newSwatchLeft, this.swatchTop, this.newSwatchLeft + 1, this.swatchTop + this.swatchHeight, BORDER_IDLE);

        boolean overOld = !this.isLocked && this.isOverOldSwatch(mouseX, mouseY);
        if (overOld) {
            guiGraphics.outline(this.oldSwatchLeft, this.swatchTop, this.swatchHalfWidth + 1, this.swatchHeight, BORDER_ACTIVE);
            this.parentScreen.setDeferredTooltip(List.of(
                    Component.translatable("config_overhauled.popup.color.revert_old", this.formatHex(this.originalValue)).getVisualOrderText()
            ));
        }
    }

    private void extractLabelsAndInfo(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.text(this.font, Component.translatable("config_overhauled.popup.color.hex"), this.hexLabelX, this.hexLabelY, 0xFFCCCCCC, false);
        guiGraphics.text(this.font, "R", this.rLabelX, this.rgRowY, 0xFFFF5555, false);
        guiGraphics.text(this.font, "G", this.gLabelX, this.rgRowY, 0xFF55FF55, false);
        guiGraphics.text(this.font, "B", this.bLabelX, this.baRowY, 0xFF5555FF, false);
        if (this.hasAlpha) {
            guiGraphics.text(this.font, "A", this.aLabelX, this.baRowY, 0xFFFFFFFF, false);
        }

        Component defaultText = Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": " + this.formatHex(this.boundedProperty.defaultValue())));
        guiGraphics.text(this.font, defaultText, this.rightPanelLeft, this.infoY, INFO_TEXT_COLOR, false);

        if (!this.isWithinUnsignedBounds(this.workingPackedColor)) {
            Component rangeErr = Component.translatable(
                    "config_overhauled.popup.numeric.out_of_range",
                    this.formatHex(this.boundedProperty.lowerBound),
                    this.formatHex(this.boundedProperty.upperBound)
            );
            String trimmed = this.font.plainSubstrByWidth(rangeErr.getString(), this.rightPanelWidth);
            guiGraphics.text(this.font, trimmed, this.rightPanelLeft, this.errorY, INVALID_TEXT_COLOR, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !this.isLocked) {
            double mx = event.x();
            double my = event.y();

            if (this.isOverSv(mx, my)) {
                this.setFocusedWidget(null);
                this.activeDrag = DragTarget.SV;
                this.updateFromSvMouse(mx, my);
                return true;
            }
            if (this.isOverHue(mx, my)) {
                this.setFocusedWidget(null);
                this.activeDrag = DragTarget.HUE;
                this.updateFromHueMouse(my);
                return true;
            }
            if (this.hasAlpha && this.isOverAlpha(mx, my)) {
                this.setFocusedWidget(null);
                this.activeDrag = DragTarget.ALPHA;
                this.updateFromAlphaMouse(my);
                return true;
            }
            if (this.isOverOldSwatch(mx, my)) {
                this.setFocusedWidget(null);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.setWorkingColorFromPacked(this.originalValue, true);
                this.inputsValid = true;
                this.syncAllInputsFromWorkingColor();
                this.updateFooterButtons();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.activeDrag != DragTarget.NONE && !this.isLocked) {
            switch (this.activeDrag) {
                case SV -> this.updateFromSvMouse(event.x(), event.y());
                case HUE -> this.updateFromHueMouse(event.y());
                case ALPHA -> this.updateFromAlphaMouse(event.y());
                case NONE -> {}
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.activeDrag != DragTarget.NONE) {
            this.activeDrag = DragTarget.NONE;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateFromSvMouse(double mx, double my) {
        int denom = Math.max(1, this.pickerSize - 1);
        this.saturation = Mth.clamp((float) (mx - this.svLeft) / (float) denom, 0.0f, 1.0f);
        this.value = Mth.clamp(1.0f - (float) (my - this.svTop) / (float) denom, 0.0f, 1.0f);
        this.recomputePackedFromHsva();
    }

    private void updateFromHueMouse(double my) {
        int denom = Math.max(1, this.pickerSize - 1);
        float ratio = Mth.clamp((float) (my - this.hueTop) / (float) denom, 0.0f, 1.0f);
        // Clamp slightly below 360 so red at bottom doesn't snap back to 0° visual jump
        this.hue = Math.min(359.9f, ratio * 360.0f);
        this.recomputePackedFromHsva();
    }

    private void updateFromAlphaMouse(double my) {
        int denom = Math.max(1, this.pickerSize - 1);
        float ratio = Mth.clamp(1.0f - (float) (my - this.alphaTop) / (float) denom, 0.0f, 1.0f);
        this.alpha = Mth.clamp(Math.round(ratio * 255.0f), 0, 255);
        this.recomputePackedFromHsva();
    }

    private boolean isOverSv(double mx, double my) {
        return mx >= this.svLeft && mx < this.svLeft + this.pickerSize
                && my >= this.svTop && my < this.svTop + this.pickerSize;
    }

    private boolean isOverHue(double mx, double my) {
        return mx >= this.hueLeft && mx < this.hueLeft + BAR_WIDTH
                && my >= this.hueTop && my < this.hueTop + this.pickerSize;
    }

    private boolean isOverAlpha(double mx, double my) {
        return mx >= this.alphaLeft && mx < this.alphaLeft + BAR_WIDTH
                && my >= this.alphaTop && my < this.alphaTop + this.pickerSize;
    }

    private boolean isOverOldSwatch(double mx, double my) {
        return mx >= this.oldSwatchLeft && mx < this.newSwatchLeft
                && my >= this.swatchTop && my < this.swatchTop + this.swatchHeight;
    }

    public static void drawCheckerboard(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int cellSize) {
        for (int cy = 0; cy < height; cy += cellSize) {
            int drawH = Math.min(cellSize, height - cy);
            for (int cx = 0; cx < width; cx += cellSize) {
                int drawW = Math.min(cellSize, width - cx);
                boolean light = ((cx / cellSize) + (cy / cellSize)) % 2 == 0;
                guiGraphics.fill(x + cx, y + cy, x + cx + drawW, y + cy + drawH, light ? CHECKER_LIGHT : CHECKER_DARK);
            }
        }
    }

    private static int hsvToRgb(float h, float s, float v) {
        float hueNorm = ((h % 360.0f) + 360.0f) % 360.0f;
        float sat = Mth.clamp(s, 0.0f, 1.0f);
        float val = Mth.clamp(v, 0.0f, 1.0f);

        float c = val * sat;
        float x = c * (1.0f - Math.abs((hueNorm / 60.0f) % 2.0f - 1.0f));
        float m = val - c;

        float r1, g1, b1;
        if (hueNorm < 60.0f) {
            r1 = c; g1 = x; b1 = 0.0f;
        } else if (hueNorm < 120.0f) {
            r1 = x; g1 = c; b1 = 0.0f;
        } else if (hueNorm < 180.0f) {
            r1 = 0.0f; g1 = c; b1 = x;
        } else if (hueNorm < 240.0f) {
            r1 = 0.0f; g1 = x; b1 = c;
        } else if (hueNorm < 300.0f) {
            r1 = x; g1 = 0.0f; b1 = c;
        } else {
            r1 = c; g1 = 0.0f; b1 = x;
        }

        int r = Mth.clamp(Math.round((r1 + m) * 255.0f), 0, 255);
        int g = Mth.clamp(Math.round((g1 + m) * 255.0f), 0, 255);
        int b = Mth.clamp(Math.round((b1 + m) * 255.0f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static float[] rgbToHsv(int rgb, float fallbackHue) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0.0f) {
            h = fallbackHue;
        } else if (max == r) {
            h = 60.0f * (((g - b) / delta) % 6.0f);
        } else if (max == g) {
            h = 60.0f * (((b - r) / delta) + 2.0f);
        } else {
            h = 60.0f * (((r - g) / delta) + 4.0f);
        }
        if (h < 0.0f) {
            h += 360.0f;
        }

        float s = max == 0.0f ? 0.0f : (delta / max);
        return new float[]{h, s, max};
    }

    private enum DragTarget {
        NONE,
        SV,
        HUE,
        ALPHA
    }
}
