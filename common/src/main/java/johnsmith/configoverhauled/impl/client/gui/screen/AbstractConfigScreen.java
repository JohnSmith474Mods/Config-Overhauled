package johnsmith.configoverhauled.impl.client.gui.screen;

import johnsmith.configoverhauled.Config;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.popup.ConfigPopup;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.registry.DefaultWidgetRegistry;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractConfigScreen extends Screen implements ConfigScreen {
    private boolean levelConfigModified = false;
    private List<FormattedCharSequence> deferredTooltip;
    private WidgetRegistry widgetRegistry;
    private @Nullable ConfigPopup activePopup;
    private int lastPopupWidth = -1;
    private int lastPopupHeight = -1;

    protected final Screen parentScreen;

    protected AbstractConfigScreen(Component title, Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
        this.widgetRegistry = new DefaultWidgetRegistry();
    }

    protected AbstractConfigScreen(Component title, Screen parentScreen, WidgetRegistry widgetRegistry) {
        super(title);
        this.parentScreen = parentScreen;
        this.widgetRegistry = widgetRegistry;
    }

    /**
     * Utility method for subclasses to easily create UI entries from properties.
     */
    @Override
    public <T> ConfigEntry createEntry(Property<T> property) {
        return this.widgetRegistry.createEntry(property, this, this.minecraft, this::updateMasterResetButton);
    }

    @Override
    public <P extends Property<?>> void registerWidget(Class<P> propertyClass, WidgetFactory<? super P> factory) {
        this.widgetRegistry.register(propertyClass, factory);
    }

    @Override
    public void setWidgetRegistry(WidgetRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("WidgetRegistry cannot be null.");
        }
        this.widgetRegistry = registry;
    }

    @Override
    public void openPopup(ConfigPopup popup) {
        if (this.activePopup != null) {
            this.activePopup.onClose();
        }
        this.activePopup = popup;
        if (popup != null) {
            this.setFocused(null);
            this.lastPopupWidth = this.width;
            this.lastPopupHeight = this.height;
            popup.init(this.width, this.height);
        }
    }

    @Override
    public void closePopup() {
        if (this.activePopup != null) {
            ConfigPopup closing = this.activePopup;
            this.activePopup = null;
            closing.onClose();
        }
    }

    @Override
    public boolean hasActivePopup() {
        return this.activePopup != null;
    }

    /**
     * Callback triggered when a widget modifies a property's value.
     */
    protected void onValueChanged() {
        // Subclasses can override this to track specific save states or trigger immediate updates
    }

    @Override
    public void markLevelConfigModified() {
        this.levelConfigModified = true;
    }

    @Override
    public void setDeferredTooltip(List<FormattedCharSequence> tooltip) {
        this.deferredTooltip = tooltip;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.activePopup != null) {
            this.activePopup.tick();
        }
    }

    protected void repositionActivePopup() {
        if (this.activePopup != null) {
            this.lastPopupWidth = this.width;
            this.lastPopupHeight = this.height;
            this.activePopup.init(this.width, this.height);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activePopup != null) {
            this.activePopup.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.activePopup != null) {
            this.activePopup.keyReleased(event);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.activePopup != null) {
            this.activePopup.charTyped(event);
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.activePopup != null) {
            this.activePopup.mouseClicked(event, doubleClick);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.activePopup != null) {
            this.activePopup.mouseReleased(event);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.activePopup != null) {
            this.activePopup.mouseDragged(event, dragX, dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activePopup != null) {
            this.activePopup.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        boolean popupOpen = this.activePopup != null;
        int bgMouseX = popupOpen ? -1 : mouseX;
        int bgMouseY = popupOpen ? -1 : mouseY;

        // Execute the standard render pipeline (draws background, lists, and widgets)
        super.extractRenderState(guiGraphicsExtractor, bgMouseX, bgMouseY, partialTick);

        if (popupOpen) {
            if (this.lastPopupWidth != this.width || this.lastPopupHeight != this.height) {
                this.lastPopupWidth = this.width;
                this.lastPopupHeight = this.height;
                this.activePopup.init(this.width, this.height);
            }
            // Discard any background deferred tooltips before rendering the modal overlay
            this.deferredTooltip = null;
            this.activePopup.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        }

        // Render the deferred tooltip at the highest Z-index
        if (this.deferredTooltip != null && !this.deferredTooltip.isEmpty()) {
            guiGraphicsExtractor.setTooltipForNextFrame(this.font, this.deferredTooltip, mouseX, mouseY);
            // Purge the state to prevent persistence on the subsequent frame
            this.deferredTooltip = null;
        }
    }

    @Override
    public void onClose() {
        this.closePopup();
        if (this.levelConfigModified && this.hasReloadPermission() && Config.SHOW_RELOAD_ALERT.get()) {
            this.minecraft.gui.setScreen(new ReloadNotificationScreen(this.parentScreen));
        } else {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }

    protected boolean hasReloadPermission() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        return this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }
}