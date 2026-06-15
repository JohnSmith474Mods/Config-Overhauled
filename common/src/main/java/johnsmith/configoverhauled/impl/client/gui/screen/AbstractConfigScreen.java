package johnsmith.configoverhauled.impl.client.gui.screen;

import johnsmith.configoverhauled.Config;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.registry.DefaultWidgetRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public abstract class AbstractConfigScreen extends Screen implements ConfigScreen {
    private boolean levelConfigModified = false;
    private List<FormattedCharSequence> deferredTooltip;
    private WidgetRegistry widgetRegistry;

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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Execute the standard render pipeline (draws background, lists, and widgets)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render the deferred tooltip at the highest Z-index
        if (this.deferredTooltip != null && !this.deferredTooltip.isEmpty()) {
            guiGraphics.setTooltipForNextFrame(this.font, this.deferredTooltip, mouseX, mouseY);
            // Purge the state to prevent persistence on the subsequent frame
            this.deferredTooltip = null;
        }
    }

    @Override
    public void onClose() {
        if (this.levelConfigModified && this.hasReloadPermission() && Config.SHOW_RELOAD_ALERT.get()) {
            this.minecraft.setScreen(new ReloadNotificationScreen(this.parentScreen));
        } else {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    protected boolean hasReloadPermission() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        return this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }
}