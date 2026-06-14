package johnsmith.configoverhauled.impl.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ReloadNotificationScreen extends Screen {
    private final Screen previousScreen;
    private final Component message = Component.translatable("config_overhauled.warning.reload_required.message");

    public ReloadNotificationScreen(Screen previousScreen) {
        super(Component.translatable("config_overhauled.warning.reload_required.title"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height / 2 + 30;

        MultiLineTextWidget textWidget = new MultiLineTextWidget(
                0, this.height / 2 - 40, Component.empty(), this.font
        );
        textWidget.setMessage(this.message);
        textWidget.setMaxWidth(this.width - 50);
        textWidget.setCentered(true);
        textWidget.setX(this.width / 2 - textWidget.getWidth() / 2);

        this.addRenderableWidget(textWidget);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_ACKNOWLEDGE, button -> this.onClose())
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        guiGraphicsExtractor.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }
}