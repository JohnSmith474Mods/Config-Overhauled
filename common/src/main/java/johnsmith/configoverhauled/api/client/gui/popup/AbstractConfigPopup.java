package johnsmith.configoverhauled.api.client.gui.popup;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract base implementation of {@link ConfigPopup} that provides standard modal chrome:
 * dimmed backdrop, responsive centered frame, property title and description header with scope/lock badges,
 * child widget lifecycle and event routing, Tab focus cycling, and standard Reset / Cancel / Apply footer controls.
 *
 * @param <T> The value type produced when the popup is applied (may be {@link Void} for non-value dialogs).
 */
public abstract class AbstractConfigPopup<T> implements ConfigPopup {
    protected static final int BACKDROP_COLOR = 0xB0000000;
    protected static final int FRAME_BG_COLOR = 0xF5141414;
    protected static final int HEADER_BG_COLOR = 0xFF1C1C1C;
    protected static final int BORDER_COLOR = 0xFFA0A0A0;
    protected static final int DIVIDER_COLOR = 0xFF444444;
    protected static final int TITLE_COLOR = 0xFFFFFFFF;
    protected static final int DESCRIPTION_COLOR = 0xFFAAAAAA;
    protected static final int FRAME_PADDING = 12;

    protected final ConfigScreen parentScreen;
    protected final Minecraft minecraft;
    protected final Font font;
    protected final @Nullable Property<T> property;
    protected final Component title;
    protected final @Nullable Component description;
    protected final boolean isLocked;
    protected final @Nullable Consumer<T> onApply;

    protected int screenWidth;
    protected int screenHeight;
    protected int frameLeft;
    protected int frameTop;
    protected int frameWidth;
    protected int frameHeight;
    protected int headerHeight;
    protected int footerHeight;
    protected int contentLeft;
    protected int contentTop;
    protected int contentWidth;
    protected int contentHeight;

    protected @Nullable Button copyIdButton;
    protected @Nullable Button resetButton;
    protected @Nullable Button cancelButton;
    protected @Nullable Button applyButton;

    private final List<AbstractWidget> renderableWidgets = new ArrayList<>();
    private final List<GuiEventListener> children = new ArrayList<>();
    private @Nullable GuiEventListener focused;
    private boolean dragging = false;
    private long copiedFeedbackUntilMs = 0L;
    private boolean lastInputShiftDown = false;

    private List<FormattedCharSequence> fullDescriptionLines = List.of();
    private List<FormattedCharSequence> visibleDescriptionLines = List.of();
    private boolean descriptionTruncated = false;
    private int descriptionTop;
    private int descriptionBottom;

    /**
     * Constructs a property-backed modal popup.
     */
    protected AbstractConfigPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Property<T> property,
            boolean isLocked,
            Consumer<T> onApply
    ) {
        this(
                parentScreen,
                minecraft,
                property,
                Component.translatable(property.translationKey()),
                resolveDescription(property),
                isLocked,
                onApply
        );
    }

    /**
     * Constructs a general-purpose modal popup not bound to a specific {@link Property}.
     */
    protected AbstractConfigPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            Component title,
            @Nullable Component description,
            boolean isLocked,
            @Nullable Consumer<T> onApply
    ) {
        this(parentScreen, minecraft, null, title, description, isLocked, onApply);
    }

    private AbstractConfigPopup(
            ConfigScreen parentScreen,
            Minecraft minecraft,
            @Nullable Property<T> property,
            Component title,
            @Nullable Component description,
            boolean isLocked,
            @Nullable Consumer<T> onApply
    ) {
        this.parentScreen = parentScreen;
        this.minecraft = minecraft;
        this.font = minecraft.font;
        this.property = property;
        this.title = title;
        this.description = description;
        this.isLocked = isLocked;
        this.onApply = onApply;
    }

    private static @Nullable Component resolveDescription(Property<?> property) {
        String key = property.descriptionTranslationKey();
        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return null;
    }

    protected int getPreferredWidth() {
        return 340;
    }

    protected int getPreferredHeight() {
        return 180;
    }

    protected int getMaxDescriptionLines() {
        return 2;
    }

    protected boolean showResetButton() {
        return this.property != null;
    }

    protected boolean showFooter() {
        return true;
    }

    protected boolean showCopyIdButton() {
        return this.property != null;
    }

    protected int getHeaderRightPadding() {
        return this.showCopyIdButton() ? 54 : 0;
    }

    protected Component getApplyButtonLabel() {
        return Component.translatable("config_overhauled.popup.apply");
    }

    protected Component getCancelButtonLabel() {
        return Component.translatable("config_overhauled.popup.cancel");
    }

    protected Component getResetButtonLabel() {
        return Component.translatable("controls.reset");
    }

    protected @Nullable Component getDefaultValueTooltip() {
        if (this.property == null) {
            return null;
        }
        return Component.translatable("config_overhauled.word.default")
                .append(Component.literal(": " + this.property.defaultValue()));
    }

    @Override
    public final void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.clearWidgets();

        int maxWidth = Math.max(180, screenWidth - 24);
        int maxHeight = Math.max(110, screenHeight - 24);
        this.frameWidth = Math.min(this.getPreferredWidth(), maxWidth);
        this.frameHeight = Math.min(this.getPreferredHeight(), maxHeight);
        this.frameLeft = (screenWidth - this.frameWidth) / 2;
        this.frameTop = (screenHeight - this.frameHeight) / 2;

        int innerWidth = Math.max(40, this.frameWidth - FRAME_PADDING * 2);
        this.descriptionTop = this.frameTop + 24;
        int computedHeader = 28;

        if (this.description != null) {
            this.fullDescriptionLines = this.font.split(this.description, innerWidth);
            int maxLines = Math.max(0, this.getMaxDescriptionLines());
            int shownCount = Math.min(this.fullDescriptionLines.size(), maxLines);
            this.visibleDescriptionLines = shownCount > 0
                    ? this.fullDescriptionLines.subList(0, shownCount)
                    : List.of();
            this.descriptionTruncated = this.fullDescriptionLines.size() > shownCount;

            if (shownCount > 0) {
                int descHeight = shownCount * (this.font.lineHeight + 1);
                this.descriptionBottom = this.descriptionTop + descHeight;
                computedHeader = (this.descriptionBottom - this.frameTop) + 7;
            } else {
                this.descriptionBottom = this.descriptionTop;
            }
        } else {
            this.fullDescriptionLines = List.of();
            this.visibleDescriptionLines = List.of();
            this.descriptionTruncated = false;
            this.descriptionBottom = this.descriptionTop;
        }

        this.headerHeight = computedHeader;
        this.footerHeight = this.showFooter() ? 32 : 0;

        this.contentLeft = this.frameLeft + FRAME_PADDING;
        this.contentTop = this.frameTop + this.headerHeight + 8;
        this.contentWidth = innerWidth;
        this.contentHeight = Math.max(20, this.frameHeight - this.headerHeight - this.footerHeight - 16);

        this.initHeaderWidgets();
        this.initContent(this.contentLeft, this.contentTop, this.contentWidth, this.contentHeight);

        if (this.showFooter()) {
            this.initFooter();
        } else {
            this.resetButton = null;
            this.cancelButton = null;
            this.applyButton = null;
        }

        this.updateFooterButtons();
    }

    /**
     * Optional hook for adding interactive widgets to the popup header bar.
     */
    protected void initHeaderWidgets() {
        if (this.showCopyIdButton()) {
            int buttonW = 48;
            int buttonH = 15;
            int buttonX = this.frameLeft + this.frameWidth - FRAME_PADDING - buttonW;
            int buttonY = this.frameTop + 6;
            this.copyIdButton = this.addRenderableWidget(
                    Button.builder(this.getCopyIdButtonLabel(), b -> this.copyPropertyIdentifier())
                            .bounds(buttonX, buttonY, buttonW, buttonH)
                            .build()
            );
        } else {
            this.copyIdButton = null;
        }
    }

    private Component getCopyIdButtonLabel() {
        if (System.currentTimeMillis() < this.copiedFeedbackUntilMs) {
            return Component.translatable("config_overhauled.popup.copied").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("config_overhauled.popup.copy_id");
    }

    private void copyPropertyIdentifier() {
        if (this.property == null) {
            return;
        }
        String textToCopy = this.lastInputShiftDown ? this.property.translationKey() : this.property.getUniqueId();
        this.minecraft.keyboardHandler.setClipboard(textToCopy);
        this.copiedFeedbackUntilMs = System.currentTimeMillis() + 1500L;
        if (this.copyIdButton != null) {
            this.copyIdButton.setMessage(this.getCopyIdButtonLabel());
        }
    }

    /**
     * Initializes the popup's body widgets inside the provided inner content bounds.
     */
    protected abstract void initContent(int left, int top, int width, int height);

    /**
     * Renders custom non-widget content inside the popup body (drawn beneath child widgets).
     */
    protected void extractBody(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * Renders custom foreground overlays after child widgets have been drawn.
     */
    protected void extractForeground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * Builds the standard Reset / Cancel / Apply footer buttons.
     */
    protected void initFooter() {
        int buttonY = this.frameTop + this.frameHeight - this.footerHeight + 6;

        if (this.showResetButton()) {
            this.resetButton = this.addRenderableWidget(
                    Button.builder(this.getResetButtonLabel(), b -> {
                        if (!this.isLocked) {
                            this.resetWorkingValue();
                            this.updateFooterButtons();
                        }
                    }).bounds(this.frameLeft + FRAME_PADDING, buttonY, 58, 20).build()
            );
        } else {
            this.resetButton = null;
        }

        int buttonWidth = 68;
        int applyX = this.frameLeft + this.frameWidth - FRAME_PADDING - buttonWidth;
        int cancelX = applyX - 6 - buttonWidth;

        this.cancelButton = this.addRenderableWidget(
                Button.builder(this.getCancelButtonLabel(), b -> this.dismiss())
                        .bounds(cancelX, buttonY, buttonWidth, 20)
                        .build()
        );

        this.applyButton = this.addRenderableWidget(
                Button.builder(this.getApplyButtonLabel(), b -> this.applyAndClose())
                        .bounds(applyX, buttonY, buttonWidth, 20)
                        .build()
        );
    }

    protected boolean isWorkingValueValid() {
        return true;
    }

    protected boolean canResetWorkingValue() {
        return false;
    }

    protected void resetWorkingValue() {
    }

    protected @Nullable T buildResultValue() {
        return null;
    }

    protected void updateFooterButtons() {
        if (this.applyButton != null) {
            this.applyButton.active = !this.isLocked && this.isWorkingValueValid();
        }
        if (this.resetButton != null) {
            this.resetButton.active = !this.isLocked && this.canResetWorkingValue();
        }
    }

    protected void applyAndClose() {
        if (this.isLocked || !this.isWorkingValueValid()) {
            return;
        }
        if (this.onApply != null) {
            this.onApply.accept(this.buildResultValue());
        }
        this.parentScreen.closePopup();
    }

    protected void dismiss() {
        this.parentScreen.closePopup();
    }

    protected <W extends AbstractWidget> W addRenderableWidget(W widget) {
        this.renderableWidgets.add(widget);
        this.children.add(widget);
        return widget;
    }

    protected <L extends GuiEventListener> L addListener(L listener) {
        this.children.add(listener);
        return listener;
    }

    protected void removeWidget(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            this.renderableWidgets.remove(widget);
        }
        this.children.remove(listener);
        if (this.focused == listener) {
            this.focused.setFocused(false);
            this.focused = null;
        }
    }

    protected void clearWidgets() {
        if (this.focused != null) {
            this.focused.setFocused(false);
            this.focused = null;
        }
        this.dragging = false;
        this.renderableWidgets.clear();
        this.children.clear();
    }

    protected void setFocusedWidget(@Nullable GuiEventListener listener) {
        if (this.focused == listener) {
            if (listener != null) {
                listener.setFocused(true);
            }
            return;
        }
        if (this.focused != null) {
            this.focused.setFocused(false);
        }
        this.focused = listener;
        if (this.focused != null) {
            this.focused.setFocused(true);
        }
    }

    protected @Nullable GuiEventListener getFocusedWidget() {
        return this.focused;
    }

    protected boolean isInsideFrame(double x, double y) {
        return x >= this.frameLeft && x < this.frameLeft + this.frameWidth
                && y >= this.frameTop && y < this.frameTop + this.frameHeight;
    }

    /**
     * Renders the backdrop behind the modal frame. Defaults to a dark scrim to avoid
     * invoking Minecraft's single-pass blur shader twice in the same frame.
     */
    protected void extractBackdrop(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        guiGraphics.fill(0, 0, screenWidth, screenHeight, BACKDROP_COLOR);
    }

    protected void extractFrame(GuiGraphicsExtractor guiGraphics) {
        int right = this.frameLeft + this.frameWidth;
        int bottom = this.frameTop + this.frameHeight;

        guiGraphics.fill(this.frameLeft, this.frameTop, right, bottom, FRAME_BG_COLOR);
        guiGraphics.fill(this.frameLeft + 1, this.frameTop + 1, right - 1, this.frameTop + this.headerHeight, HEADER_BG_COLOR);

        // Header divider
        guiGraphics.fill(this.frameLeft + 1, this.frameTop + this.headerHeight, right - 1, this.frameTop + this.headerHeight + 1, DIVIDER_COLOR);

        // Footer divider
        if (this.footerHeight > 0) {
            int footerTop = bottom - this.footerHeight;
            guiGraphics.fill(this.frameLeft + 1, footerTop, right - 1, footerTop + 1, DIVIDER_COLOR);
        }

        // Crisp 1px outer border
        guiGraphics.outline(this.frameLeft, this.frameTop, this.frameWidth, this.frameHeight, BORDER_COLOR);
    }

    protected void extractHeader(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleTop = this.frameTop + 7;
        int titleBottom = this.frameTop + 21;
        int rightCursor = this.frameLeft + this.frameWidth - FRAME_PADDING - this.getHeaderRightPadding();

        // Draw scope badge on the right side of the title bar
        if (this.property != null) {
            ConfigScope scope = this.property.scope();
            Component scopeBadge = Component.literal("[" + scope.name() + "]").withStyle(getScopeColor(scope));
            int badgeWidth = this.font.width(scopeBadge);
            int badgeX = rightCursor - badgeWidth;
            int badgeY = titleTop + (titleBottom - titleTop - this.font.lineHeight) / 2;
            guiGraphics.text(this.font, scopeBadge, badgeX, badgeY, 0xFFFFFFFF, false);
            rightCursor = badgeX - 6;
        }

        // Draw locked badge if property cannot be edited
        if (this.isLocked) {
            Component lockedBadge = Component.translatable("config_overhauled.config.locked").withStyle(ChatFormatting.RED);
            int lockedWidth = this.font.width(lockedBadge);
            int lockedX = rightCursor - lockedWidth;
            int lockedY = titleTop + (titleBottom - titleTop - this.font.lineHeight) / 2;
            guiGraphics.text(this.font, lockedBadge, lockedX, lockedY, 0xFFFFFFFF, false);
            rightCursor = lockedX - 6;
        }

        // Clipped marquee title on the left
        int titleLeft = this.frameLeft + FRAME_PADDING;
        int titleMaxX = Math.max(titleLeft, rightCursor);
        Component boldTitle = this.title.copy().withStyle(ChatFormatting.BOLD);
        extractScrollingText(guiGraphics, this.font, boldTitle, titleLeft, titleTop, titleMaxX, titleBottom, TITLE_COLOR);

        // Description lines beneath the title
        if (!this.visibleDescriptionLines.isEmpty()) {
            int lineY = this.descriptionTop;
            for (FormattedCharSequence line : this.visibleDescriptionLines) {
                guiGraphics.text(this.font, line, this.frameLeft + FRAME_PADDING, lineY, DESCRIPTION_COLOR, false);
                lineY += this.font.lineHeight + 1;
            }

            if (this.descriptionTruncated
                    && mouseX >= this.frameLeft + FRAME_PADDING
                    && mouseX < this.frameLeft + this.frameWidth - FRAME_PADDING
                    && mouseY >= this.descriptionTop
                    && mouseY < this.descriptionBottom) {
                this.parentScreen.setDeferredTooltip(this.fullDescriptionLines);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.copyIdButton != null) {
            this.copyIdButton.setMessage(this.getCopyIdButtonLabel());
        }

        guiGraphics.nextStratum();
        this.extractBackdrop(guiGraphics, this.screenWidth, this.screenHeight);
        this.extractFrame(guiGraphics);
        this.extractHeader(guiGraphics, mouseX, mouseY);
        this.extractBody(guiGraphics, mouseX, mouseY, partialTick);

        for (AbstractWidget widget : this.renderableWidgets) {
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        this.extractForeground(guiGraphics, mouseX, mouseY, partialTick);

        if (this.copyIdButton != null && this.property != null && this.copyIdButton.isMouseOver(mouseX, mouseY)) {
            this.parentScreen.setDeferredTooltip(List.of(
                    Component.translatable("config_overhauled.popup.copy_id.tooltip_id", this.property.getUniqueId()).getVisualOrderText(),
                    Component.translatable("config_overhauled.popup.copy_id.tooltip_key", this.property.translationKey())
                            .withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.resetButton != null && this.resetButton.isMouseOver(mouseX, mouseY)) {
            Component defaultTooltip = this.getDefaultValueTooltip();
            if (defaultTooltip != null) {
                this.parentScreen.setDeferredTooltip(List.of(defaultTooltip.getVisualOrderText()));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.lastInputShiftDown = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        for (int i = this.children.size() - 1; i >= 0; i--) {
            GuiEventListener child = this.children.get(i);
            if (child.mouseClicked(event, doubleClick)) {
                this.setFocusedWidget(child);
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.dragging = true;
                }
                return true;
            }
        }

        this.setFocusedWidget(null);
        if (!this.isInsideFrame(event.x(), event.y())
                && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && this.closeOnOutsideClick()) {
            this.dismiss();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        if (this.focused != null && this.focused.mouseReleased(event)) {
            return true;
        }
        for (int i = this.children.size() - 1; i >= 0; i--) {
            GuiEventListener child = this.children.get(i);
            if (child != this.focused && child.mouseReleased(event)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.focused != null && this.dragging) {
            this.focused.mouseDragged(event, dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int i = this.children.size() - 1; i >= 0; i--) {
            GuiEventListener child = this.children.get(i);
            if (child.isMouseOver(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        this.lastInputShiftDown = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && this.closeOnEscape()) {
            this.dismiss();
            return true;
        }

        if (this.focused != null && this.focused.keyPressed(event)) {
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_TAB) {
            boolean reverse = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            this.cycleFocus(reverse);
            return true;
        }

        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.focused != null) {
            this.focused.keyReleased(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.focused != null) {
            this.focused.charTyped(event);
        }
        return true;
    }

    private void cycleFocus(boolean reverse) {
        List<AbstractWidget> focusable = new ArrayList<>();
        for (AbstractWidget widget : this.renderableWidgets) {
            if (widget.visible && widget.active) {
                focusable.add(widget);
            }
        }
        if (focusable.isEmpty()) {
            return;
        }

        int currentIdx = focusable.indexOf(this.focused);
        int nextIdx;
        if (currentIdx < 0) {
            nextIdx = reverse ? focusable.size() - 1 : 0;
        } else {
            nextIdx = (currentIdx + (reverse ? -1 : 1) + focusable.size()) % focusable.size();
        }
        this.setFocusedWidget(focusable.get(nextIdx));
    }

    private static ChatFormatting getScopeColor(ConfigScope scope) {
        return switch (scope) {
            case CLIENT -> ChatFormatting.AQUA;
            case GLOBAL -> ChatFormatting.GOLD;
            case LEVEL -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static void extractScrollingText(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            Component text,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color
    ) {
        int availableWidth = maxX - minX;
        if (availableWidth <= 0 || maxY <= minY) {
            return;
        }
        int textWidth = font.width(text);
        int textY = minY + (maxY - minY - font.lineHeight) / 2;

        if (textWidth <= availableWidth) {
            guiGraphics.text(font, text, minX, textY, color, true);
            return;
        }

        int overflow = textWidth - availableWidth;
        double seconds = System.nanoTime() / 1_000_000_000.0;
        double scrollDuration = Math.max((double) overflow * 0.5, 3.0);
        double phase = Math.sin((Math.PI / 2.0) * Math.cos((Math.PI * 2.0) * seconds / scrollDuration)) / 2.0 + 0.5;
        double offset = Mth.lerp(phase, 0.0, (double) overflow);

        guiGraphics.enableScissor(minX, minY, maxX, maxY);
        guiGraphics.text(font, text, minX - (int) offset, textY, color, true);
        guiGraphics.disableScissor();
    }
}
