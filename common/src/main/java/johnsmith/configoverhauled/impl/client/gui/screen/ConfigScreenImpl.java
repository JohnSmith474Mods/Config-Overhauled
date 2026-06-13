package johnsmith.configoverhauled.impl.client.gui.screen;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.screen.component.ConfigTab;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

public class ConfigScreenImpl extends AbstractConfigScreen implements ConfigScreen {
    public static final ResourceLocation TAB_HEADER_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/tab_header_background.png");

    private final ConfigManager manager;
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private TabNavigationBar tabNavigationBar;
    private Tab lastTab;
    private Button resetButton;
    private EditBox searchBox;

    public ConfigScreenImpl(Screen parentScreen, ConfigManager manager) {
        super(Component.translatable("config." + manager.modId() + ".title"), parentScreen);
        this.manager = manager;
        this.layout.setHeaderHeight(24);
    }

    @Override
    protected void init() {
        TabNavigationBar.Builder tabBuilder = TabNavigationBar.builder(this.tabManager, this.width);

        for (Category category : this.manager.getCategories()) {
            ConfigTab tab = new ConfigTab(this, category, this.manager);
            if (tab.isAvailable()) {
                tabBuilder.addTabs(tab);
            }
        }

        this.tabNavigationBar = tabBuilder.build();
        this.addRenderableWidget(this.tabNavigationBar);

        LinearLayout footerColumn = LinearLayout.vertical().spacing(6);

        LinearLayout searchRow = LinearLayout.horizontal().spacing(4);
        this.searchBox = new EditBox(this.font, 0, 0, 284, 20, Component.empty());
        this.searchBox.setHint(Component.translatable("config_overhauled.config.search_by_name").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(this::onSearchChange);
        searchRow.addChild(this.searchBox);

        Button clearButton = Button.builder(Component.literal("X"), button -> {
            this.searchBox.setValue("");
            this.searchBox.setFocused(true);
        }).width(20).build();
        searchRow.addChild(clearButton);

        footerColumn.addChild(searchRow);

        String resetKey = Constants.MOD_ID + ".config.reset";
        LinearLayout buttonRow = LinearLayout.horizontal().spacing(8);
        this.resetButton = buttonRow.addChild(Button.builder(Component.translatable(resetKey), button -> this.resetCurrentTab()).width(150).build());
        buttonRow.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(150).build());

        footerColumn.addChild(buttonRow);

        this.layout.addToFooter(footerColumn);
        this.layout.setFooterHeight(58);
        this.layout.visitWidgets(this::addRenderableWidget);

        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        this.updateMasterResetButton();
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            this.layout.setHeaderHeight(this.tabNavigationBar.getRectangle().bottom());
        }

        this.layout.arrangeElements();

        int contentWidth = Math.min(400, this.width);
        int contentLeft = (this.width - contentWidth) / 2;
        int tabTop = this.layout.getHeaderHeight();
        int tabHeight = this.layout.getContentHeight() - 2;

        ScreenRectangle tabArea = new ScreenRectangle(contentLeft, tabTop, contentWidth, tabHeight);
        this.tabManager.setTabArea(tabArea);
    }

    private void resetCurrentTab() {
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            for (var entry : configTab.getList().children()) {
                if (entry instanceof ConfigEntry option) {
                    option.resetToDefault();
                }
            }
        }
        this.updateMasterResetButton();
    }

    public void updateMasterResetButton() {
        boolean canReset = false;
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            for (var entry : configTab.getList().children()) {
                if (entry instanceof ConfigEntry option && option.isModified()) {
                    canReset = true;
                    break;
                }
            }
        }
        if (this.resetButton != null) {
            this.resetButton.active = canReset;
        }
    }

    private void onSearchChange(String query) {
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            configTab.getList().filter(query);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.tabManager.getCurrentTab() != this.lastTab) {
            this.lastTab = this.tabManager.getCurrentTab();
            this.updateMasterResetButton();
            if (this.searchBox != null) {
                this.onSearchChange(this.searchBox.getValue());
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(guiGraphics, 0, this.layout.getHeaderHeight(), this.width, this.height);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }
}