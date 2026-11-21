package johnsmith.configoverhauled.impl.client.gui.entry;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import johnsmith.configoverhauled.api.client.gui.entry.AbstractConfigEntry;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.component.ConfigSearchQuery;
import johnsmith.configoverhauled.impl.network.NetworkManager;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.jetbrains.annotations.Nullable;

public abstract class OptionEntry<T, W extends AbstractWidget> extends AbstractConfigEntry implements ConfigEntry {
    protected final ConfigScreen parentScreen;
    protected final Property<T> property;
    protected final Button resetButton;
    protected final @Nullable Button detailButton;
    protected final W widget;
    protected final Component labelComponent;
    protected final List<FormattedCharSequence> tooltip;
    protected final Minecraft minecraft;
    protected final Runnable onValueChanged;

    protected final boolean isLocked;
    private boolean updatingLocally = false;
    private final Property.Listener propertyListener;

    public OptionEntry(Property<T> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        this.parentScreen = parentScreen;
        this.property = property;
        this.minecraft = minecraft;
        this.onValueChanged = onValueChanged;
        this.labelComponent = Component.translatable(this.property.translationKey());

        this.isLocked = determineLockState(this.minecraft, this.property);
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
        this.detailButton = this.hasDetailPopup() ? this.createDetailButton() : null;
        this.widget = createWidget();

        this.widget.active = !this.isLocked;
        updateResetButton();

        this.propertyListener = new Property.Listener() {
            @Override
            public void onPropertyInvalidated() {
            }

            @Override
            public void onPropertyChanged() {
                if (OptionEntry.this.updatingLocally) {
                    return;
                }
                OptionEntry.this.minecraft.execute(() -> {
                    OptionEntry.this.updateWidgetValue();
                    OptionEntry.this.updateResetButton();
                    OptionEntry.this.onValueChanged.run();
                });
            }
        };
        this.property.addListener(this.propertyListener);
    }

    /**
     * Whether this entry exposes a 20x20 square detail-editor button to the left of its primary widget.
     * <p>
     * Invoked during {@link OptionEntry} construction; subclasses overriding this method should return a
     * constant value without depending on subclass fields initialized after {@code super(...)}.
     */
    protected boolean hasDetailPopup() {
        return false;
    }

    /**
     * Opens the detailed modal editor popup for this entry via {@link ConfigScreen#openPopup}.
     */
    protected void openDetailPopup() {
    }

    protected Button createDetailButton() {
        return Button.builder(this.getDetailButtonLabel(), b -> this.openDetailPopup())
                .bounds(0, 0, 20, 20)
                .build();
    }

    protected Component getDetailButtonLabel() {
        return Component.literal("...");
    }

    protected Component getDetailButtonTooltip() {
        return Component.translatable(this.isLocked
                ? "config_overhauled.popup.inspect_detail"
                : "config_overhauled.popup.open_detail");
    }

    @Override
    public boolean matchesSearch(String query) {
        return ConfigSearchQuery.parse(query).matches(this.property.parentGroup(), this.property, this.isLocked);
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

    public static boolean determineLockState(Minecraft minecraft, Property<?> property) {
        if (property.scope() == ConfigScope.CLIENT) return false;
        if (minecraft == null || minecraft.player == null) return property.scope() == ConfigScope.LEVEL;
        if (minecraft.hasSingleplayerServer()) return false;
        return !minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
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

        this.updatingLocally = true;
        try {
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
        } finally {
            this.updatingLocally = false;
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

    protected int getMaxLabelWidth(int widgetX) {
        if (this.detailButton != null) {
            return widgetX - 5 - 20 - this.getX() - 5;
        }
        return widgetX - this.getX() - 5;
    }

    @Override
    public void extractContent(@NotNull GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        int y = this.getY() + (this.getHeight() - 20) / 2;

        if (this.isModified()) {
            int barColor = this.isLocked ? 0xFF888888 : 0xFFFFAA00;
            guiGraphicsExtractor.fill(this.getX() - 5, this.getY() + 2, this.getX() - 3, this.getY() + this.getHeight() - 2, barColor);
        }

        int rightEdge = this.getX() + this.getWidth();
        int resetX = rightEdge - 50 - 10;

        this.resetButton.setPosition(resetX, y);
        this.resetButton.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);

        int widgetWidth = 75;
        int widgetX = resetX - 5 - widgetWidth;

        this.widget.setX(widgetX);
        this.widget.setY(y);
        this.widget.setWidth(widgetWidth);
        this.widget.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);

        boolean overDetailButton = false;
        if (this.detailButton != null) {
            int detailX = widgetX - 5 - 20;
            this.detailButton.setPosition(detailX, y);
            this.detailButton.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);
            overDetailButton = mouseX >= detailX && mouseX < detailX + 20 && mouseY >= y && mouseY < y + 20;
        }

        int maxLabelWidth = this.getMaxLabelWidth(widgetX);
        if (maxLabelWidth > 0) {
            Component displayLabel = this.labelComponent;
            int textColor = 0xFFFFFFFF;

            boolean isConnectedToRemoteServer = this.minecraft.player != null && !this.minecraft.hasSingleplayerServer();
            boolean isRemotelyControlled = this.property.scope() != ConfigScope.CLIENT && isConnectedToRemoteServer;

            if (isRemotelyControlled) {
                boolean isAdmin = this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
                displayLabel = Component.translatable(this.property.translationKey()).withStyle(ChatFormatting.ITALIC, isAdmin ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            }

            MarqueeTextRenderer.extractScrollingText(
                    guiGraphicsExtractor,
                    this.minecraft.font,
                    displayLabel,
                    this.getX(),
                    this.getY(),
                    this.getX() + maxLabelWidth,
                    this.getY() + this.getHeight(),
                    textColor,
                    false,
                    true
            );
        }

        boolean overResetButton = mouseX >= resetX && mouseX < resetX + 50 && mouseY >= y && mouseY < y + 20;

        if (overResetButton) {
            this.parentScreen.setDeferredTooltip(List.of(this.getDefaultValueTooltip().getVisualOrderText()));
        } else if (overDetailButton) {
            this.parentScreen.setDeferredTooltip(List.of(this.getDetailButtonTooltip().getVisualOrderText()));
        } else if (isHovering) {
            this.parentScreen.setDeferredTooltip(this.tooltip);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.detailButton != null
                ? ImmutableList.of(this.detailButton, this.widget, this.resetButton)
                : ImmutableList.of(this.widget, this.resetButton);
    }

    @Override
    public @NotNull List<? extends NarratableEntry> narratables() {
        return this.detailButton != null
                ? ImmutableList.of(this.detailButton, this.widget, this.resetButton)
                : ImmutableList.of(this.widget, this.resetButton);
    }


}