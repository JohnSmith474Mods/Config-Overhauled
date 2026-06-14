package johnsmith.configoverhauled.impl.client.gui.screen;

import johnsmith.configoverhauled.Config;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public abstract class AbstractConfigScreen extends Screen implements ConfigScreen {
    private boolean levelConfigModified = false;
    protected final Screen parentScreen;

    // Encapsulated state variable replacing the public field
    private List<FormattedCharSequence> deferredTooltip;

    protected AbstractConfigScreen(Component title, Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        // Execute the standard render pipeline (draws background, lists, and widgets)
        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);

        // Render the deferred tooltip at the highest Z-index
        if (this.deferredTooltip != null && !this.deferredTooltip.isEmpty()) {
            guiGraphicsExtractor.setTooltipForNextFrame(this.font, this.deferredTooltip, mouseX, mouseY);
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