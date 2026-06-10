package johnsmith.configoverhauled.impl.client.gui.screen;

import java.util.List;

import johnsmith.configoverhauled.Config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;


public abstract class AbstractConfigScreen extends Screen {
    private boolean levelConfigModified = false;

    protected final Screen parentScreen;

    public List<FormattedCharSequence> deferredTooltip;

    protected AbstractConfigScreen(Component title, Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
    }

    public void markLevelConfigModified() {
        this.levelConfigModified = true;
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