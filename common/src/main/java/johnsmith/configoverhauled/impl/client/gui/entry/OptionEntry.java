package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

import johnsmith.configoverhauled.api.client.gui.entry.AbstractConfigEntry;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.network.NetworkManager;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.NotNull;

public abstract class OptionEntry<T, W extends AbstractWidget> extends AbstractConfigEntry implements ConfigEntry {
    protected final ConfigScreen parentScreen;
    protected final Property<T> property;
    protected final Button resetButton;
    protected final W widget;
    protected final Component labelComponent;
    protected final List<FormattedCharSequence> tooltip;
    protected final Minecraft minecraft;
    protected final Runnable onValueChanged;

    protected final boolean isLocked;

    public OptionEntry(Property<T> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        this.parentScreen = parentScreen;
        this.property = property;
        this.minecraft = minecraft;
        this.onValueChanged = onValueChanged;
        this.labelComponent = Component.translatable(this.property.translationKey());

        this.isLocked = determineLockState();
        List<FormattedCharSequence> baseTooltip = minecraft.font.split(Component.translatable(this.property.descriptionTranslationKey()), 200);
        if (this.isLocked) {
            List<FormattedCharSequence> extendedTooltip = new ArrayList<>(baseTooltip);
            extendedTooltip.add(FormattedCharSequence.EMPTY);
            extendedTooltip.add(Component.translatable("config_overhauled.config.locked").withStyle(ChatFormatting.RED).getVisualOrderText());
            this.tooltip = extendedTooltip;
        } else {
            this.tooltip = baseTooltip;
        }

        this.resetButton = Button.builder(Component.translatable("controls.reset"), b -> resetToDefault())
                .bounds(0, 0, 50, 20).build();
        this.widget = createWidget();

        this.widget.active = !this.isLocked;
        updateResetButton();
    }

    @Override
    public boolean matchesSearch(String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        return this.labelComponent.getString().toLowerCase(Locale.ROOT).contains(lowerQuery) ||
                this.property.resourceName().toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    @Override
    public void resetToDefault() {
        if (this.isLocked) return;
        this.setValue(this.property.defaultValue());
        this.updateWidgetValue();
    }

    @Override
    public boolean isModified() {
        return !this.isDefault();
    }

    private boolean determineLockState() {
        if (this.property.scope() == ConfigScope.CLIENT) return false;
        if (this.minecraft.player == null) return this.property.scope() == ConfigScope.LEVEL;
        if (this.minecraft.hasSingleplayerServer()) return false;
        return !this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }

    protected abstract W createWidget();

    protected abstract void updateWidgetValue();

    protected void setValue(T newValue) {
        if (this.isLocked) return;

        if (java.util.Objects.equals(this.property.get(), newValue)) {
            return;
        }

        boolean isConnectedToServer = this.minecraft.player != null;
        boolean isServerConfig = this.property.scope() != ConfigScope.CLIENT;

        if (isServerConfig && isConnectedToServer) {
            ConfigDescription metadata = new ConfigDescription(
                    this.property.parentGroup().manager().modId(),
                    this.property.parentGroup().parent().id(),
                    this.property.parentGroup().id(),
                    this.property.resourceName()
            );

            Tag encoded = this.property.codec().encodeStart(NbtOps.INSTANCE, newValue)
                    .getOrThrow(error -> new IllegalStateException("Failed to encode property: " + error));

            ConfigUpdateRequestPacket packet = ConfigUpdateRequestPacket.create(metadata, encoded);
            NetworkManager.sendToServer(packet);

            this.property.onSync(newValue);
        } else {
            this.property.set(newValue);
            this.property.parentGroup().manager().saveAll();
        }

        if (this.property.scope() == ConfigScope.LEVEL) {
            this.parentScreen.markLevelConfigModified();
        }

        this.updateResetButton();
        this.onValueChanged.run();
    }

    public boolean isDefault() {
        return Objects.equals(property.get(), property.defaultValue());
    }

    public boolean canReset() {
        return !this.isLocked && !this.isDefault();
    }

    protected void updateResetButton() {
        this.resetButton.active = this.canReset();
    }

    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + this.property.defaultValue());
    }


    @Override
    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        int y = this.getY() + (this.getHeight() - 20) / 2;

        int rightEdge = this.getX() + this.getWidth();
        int resetX = rightEdge - 50 - 10;

        this.resetButton.setPosition(resetX, y);
        this.resetButton.render(guiGraphics, mouseX, mouseY, partialTick);

        int widgetWidth = 75;
        int widgetX = resetX - 5 - widgetWidth;
        this.widget.setX(widgetX);
        this.widget.setY(y);
        this.widget.setWidth(widgetWidth);
        this.widget.render(guiGraphics, mouseX, mouseY, partialTick);

        int textY = this.getY() + (this.getHeight() - this.minecraft.font.lineHeight) / 2;

        int maxLabelWidth = widgetX - this.getX() - 5;
        if (maxLabelWidth > 0) {
            Component displayLabel = this.labelComponent;
            int textColor = 0xFFFFFFFF;

            boolean isConnectedToRemoteServer = this.minecraft.player != null && !this.minecraft.hasSingleplayerServer();
            boolean isRemotelyControlled = this.property.scope() != ConfigScope.CLIENT && isConnectedToRemoteServer;

            if (isRemotelyControlled) {
                boolean isAdmin = this.minecraft.player.hasPermissions(2);
                displayLabel = Component.translatable(this.property.translationKey()).withStyle(ChatFormatting.ITALIC, isAdmin ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            }

            guiGraphics.drawString(this.minecraft.font, displayLabel, this.getX(), textY, textColor);
        }

        boolean overResetButton = mouseX >= resetX && mouseX < resetX + 50 && mouseY >= y && mouseY < y + 20;

        if (overResetButton) {
            this.parentScreen.setDeferredTooltip(List.of(this.getDefaultValueTooltip().getVisualOrderText()));
        } else if (isHovering) {
            this.parentScreen.setDeferredTooltip(this.tooltip);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() { return ImmutableList.of(widget, resetButton); }

    @Override
    public @NotNull List<? extends NarratableEntry> narratables() { return ImmutableList.of(widget, resetButton); }


}