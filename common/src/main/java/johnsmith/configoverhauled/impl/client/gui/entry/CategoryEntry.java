package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.common.collect.ImmutableList;

import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.client.gui.entry.AbstractConfigEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CategoryEntry extends AbstractConfigEntry {
    private static final int HOVER_BG_COLOR = 0x22FFFFFF;
    private static final int FOCUS_OUTLINE_COLOR = 0x88FFFF55;
    private static final int EXPANDED_LINE_COLOR = 0x88FFFF55;

    private final Group group;
    private final Minecraft minecraft;
    private final Runnable onToggle;
    private final HeaderToggle headerToggle;
    private final Component groupLabel;
    private boolean collapsed;

    public CategoryEntry(Group group, Minecraft minecraft) {
        this(group, minecraft, false, () -> {});
    }

    public CategoryEntry(Group group, Minecraft minecraft, boolean collapsed, Runnable onToggle) {
        this.group = group;
        this.minecraft = minecraft;
        this.collapsed = collapsed;
        this.onToggle = onToggle;
        this.groupLabel = Component.translatable(this.group.translationKey())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        this.headerToggle = new HeaderToggle();
    }

    public Group getGroup() {
        return this.group;
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void focusToggleButton() {
        this.setFocused(this.headerToggle);
        this.headerToggle.setFocused(true);
    }

    private Component buildIndicator(boolean hoveredOrFocused) {
        ChatFormatting color = hoveredOrFocused ? ChatFormatting.WHITE : ChatFormatting.YELLOW;
        return Component.literal(this.collapsed ? "[+]" : "[-]")
                .withStyle(color, ChatFormatting.BOLD);
    }

    private MutableComponent buildNarration() {
        String stateKey = this.collapsed
                ? "config_overhauled.config.group.collapsed"
                : "config_overhauled.config.group.expanded";
        return Component.translatable(stateKey, Component.translatable(this.group.translationKey()));
    }

    private void triggerToggle() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.onToggle.run();
    }

    @Override
    public void extractContent(@NotNull GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int width = Math.max(0, this.getWidth() - 10);
        int height = this.getHeight();

        this.headerToggle.updateBounds(x, y, width, height);

        boolean hovered = this.headerToggle.isMouseOver(mouseX, mouseY);
        this.headerToggle.hovered = hovered;
        boolean focused = this.headerToggle.isFocused();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (hovered) {
            guiGraphicsExtractor.fill(x, y, x + width, y + height, HOVER_BG_COLOR);
        }
        if (focused) {
            guiGraphicsExtractor.outline(x, y, width, height, FOCUS_OUTLINE_COLOR);
        }

        Component indicator = this.buildIndicator(hovered || focused);
        int indicatorWidth = this.minecraft.font.width(indicator);
        int indicatorX = x + width - indicatorWidth - 2;
        int textY = y + (height - this.minecraft.font.lineHeight) / 2;

        guiGraphicsExtractor.text(this.minecraft.font, indicator, indicatorX, textY, 0xFFFFFFFF, true);

        int labelMaxX = indicatorX - 6;
        if (labelMaxX > x) {
            MarqueeTextRenderer.extractScrollingText(
                    guiGraphicsExtractor,
                    this.minecraft.font,
                    this.groupLabel,
                    x,
                    y,
                    labelMaxX,
                    y + height,
                    0xFFFFFFFF,
                    false,
                    true
            );
        }

        if (!this.collapsed) {
            guiGraphicsExtractor.fill(x, y + height - 1, x + width, y + height, EXPANDED_LINE_COLOR);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return ImmutableList.of(this.headerToggle);
    }

    @Override
    public @NotNull List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.headerToggle);
    }

    private final class HeaderToggle implements GuiEventListener, NarratableEntry {
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
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.isMouseOver(event.x(), event.y())) {
                CategoryEntry.this.triggerToggle();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (this.focused && (event.key() == GLFW.GLFW_KEY_ENTER
                    || event.key() == GLFW.GLFW_KEY_KP_ENTER
                    || event.key() == GLFW.GLFW_KEY_SPACE)) {
                CategoryEntry.this.triggerToggle();
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
        public @NotNull ScreenRectangle getRectangle() {
            return new ScreenRectangle(this.x, this.y, this.width, this.height);
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            if (this.focused) return NarrationPriority.FOCUSED;
            if (this.hovered) return NarrationPriority.HOVERED;
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, CategoryEntry.this.buildNarration());
        }
    }
}