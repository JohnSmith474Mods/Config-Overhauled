package johnsmith.configoverhauled.impl.client.gui.screen;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.popup.ConfirmPopup;
import johnsmith.configoverhauled.impl.client.gui.screen.component.ConfigList;
import johnsmith.configoverhauled.impl.client.gui.screen.component.ConfigTab;
import johnsmith.configoverhauled.impl.client.gui.screen.component.SearchSuggestionPopup;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DefaultConfigScreen extends AbstractConfigScreen implements ConfigScreen {
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");

    private final ConfigManager manager;
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private TabNavigationBar tabNavigationBar;
    private Tab lastTab;
    private Button resetButton;
    private Button collapseToggleButton;
    private EditBox searchBox;
    private SearchSuggestionPopup suggestionPopup;

    public DefaultConfigScreen(Screen parentScreen, ConfigManager manager) {
        super(Component.translatable("config." + manager.modId() + ".title"), parentScreen);
        this.manager = manager;
        this.layout.setHeaderHeight(24);
    }

    public DefaultConfigScreen(Screen parentScreen, ConfigManager manager, WidgetRegistry widgetRegistry) {
        super(Component.translatable("config." + manager.modId() + ".title"), parentScreen, widgetRegistry);
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
        this.collapseToggleButton = Button.builder(Component.literal("[-]"), button -> this.toggleAllGroups())
                .width(20)
                .build();
        searchRow.addChild(this.collapseToggleButton);

        this.searchBox = new EditBox(this.font, 0, 0, 260, 20, Component.empty());
        this.searchBox.setHint(Component.translatable("config_overhauled.config.search_by_name").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(this::onSearchChange);
        searchRow.addChild(this.searchBox);

        this.suggestionPopup = new SearchSuggestionPopup(this.minecraft, this.font, this.searchBox, this::getActiveConfigList);

        Button clearButton = Button.builder(Component.literal("X"), button -> {
            this.searchBox.setValue("");
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            this.suggestionPopup.onSearchTextChanged();
        }).width(20).build();
        searchRow.addChild(clearButton);

        footerColumn.addChild(searchRow);

        String resetKey = Constants.MOD_ID + ".config.reset";
        LinearLayout buttonRow = LinearLayout.horizontal().spacing(8);
        this.resetButton = buttonRow.addChild(Button.builder(Component.translatable(resetKey), button -> this.confirmResetCurrentTab()).width(150).build());
        buttonRow.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(150).build());

        footerColumn.addChild(buttonRow);

        this.layout.addToFooter(footerColumn);
        this.layout.setFooterHeight(58);
        this.layout.visitWidgets(this::addRenderableWidget);

        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        this.updateMasterResetButton();
        this.updateCollapseToggleButton();
    }

    private ConfigList getActiveConfigList() {
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            return configTab.getList();
        }
        return null;
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.updateWidth(this.width);
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
        this.repositionActivePopup();
    }

    private void toggleAllGroups() {
        ConfigList list = this.getActiveConfigList();
        if (list == null || !list.hasVisibleGroups()) {
            return;
        }
        boolean allCollapsed = list.areAllVisibleGroupsCollapsed();
        list.setAllVisibleGroupsCollapsed(!allCollapsed);
        this.updateCollapseToggleButton();
    }

    private void updateCollapseToggleButton() {
        if (this.collapseToggleButton == null) {
            return;
        }
        ConfigList list = this.getActiveConfigList();
        boolean hasGroups = list != null && list.hasVisibleGroups();
        boolean allCollapsed = hasGroups && list.areAllVisibleGroupsCollapsed();
        this.collapseToggleButton.active = hasGroups;
        this.collapseToggleButton.setMessage(Component.literal(allCollapsed ? "[+]" : "[-]"));
    }

    private void confirmResetCurrentTab() {
        ConfigList list = this.getActiveConfigList();
        if (list == null) {
            return;
        }
        int resettableCount = list.countResettableEntries();
        if (resettableCount <= 0) {
            return;
        }

        this.openPopup(new ConfirmPopup(
                this,
                this.minecraft,
                Component.translatable("config_overhauled.popup.confirm_reset.title"),
                Component.translatable("config_overhauled.popup.confirm_reset.message", resettableCount),
                Component.translatable(Constants.MOD_ID + ".config.reset"),
                this::resetCurrentTab
        ));
    }

    private void resetCurrentTab() {
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            configTab.getList().resetMatchingEntries();
        }
        this.updateMasterResetButton();
    }

    public void updateMasterResetButton() {
        boolean canReset = false;
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            canReset = configTab.getList().hasModifiedEntries();
        }
        if (this.resetButton != null) {
            this.resetButton.active = canReset;
        }
    }

    private void onSearchChange(String query) {
        if (this.tabManager.getCurrentTab() instanceof ConfigTab configTab) {
            configTab.getList().filter(query);
            this.updateMasterResetButton();
            this.updateCollapseToggleButton();
        }
        if (this.suggestionPopup != null) {
            this.suggestionPopup.onSearchTextChanged();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.hasActivePopup() && this.suggestionPopup != null && this.suggestionPopup.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.hasActivePopup() && this.suggestionPopup != null && this.suggestionPopup.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.hasActivePopup() && this.suggestionPopup != null && this.suggestionPopup.mouseClicked(event)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.hasActivePopup() && this.suggestionPopup != null && this.suggestionPopup.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.tabManager.getCurrentTab() != this.lastTab) {
            this.lastTab = this.tabManager.getCurrentTab();
            if (this.searchBox != null) {
                this.onSearchChange(this.searchBox.getValue());
            } else {
                this.updateMasterResetButton();
            }
            if (this.suggestionPopup != null) {
                this.suggestionPopup.onTabChanged();
            }
        }

        this.updateCollapseToggleButton();

        boolean modalOpen = this.hasActivePopup();
        boolean overSuggestion = !modalOpen && this.suggestionPopup != null && this.suggestionPopup.isMouseOver(mouseX, mouseY);
        int bgMouseX = overSuggestion ? -1 : mouseX;
        int bgMouseY = overSuggestion ? -1 : mouseY;

        if (!modalOpen && !overSuggestion && this.collapseToggleButton != null
                && this.collapseToggleButton.active && this.collapseToggleButton.isMouseOver(mouseX, mouseY)) {
            ConfigList list = this.getActiveConfigList();
            boolean allCollapsed = list != null && list.areAllVisibleGroupsCollapsed();
            Component tooltip = Component.translatable(allCollapsed
                    ? "config_overhauled.config.groups.expand_all"
                    : "config_overhauled.config.groups.collapse_all");
            this.setDeferredTooltip(List.of(tooltip.getVisualOrderText()));
        }

        super.extractRenderState(guiGraphics, bgMouseX, bgMouseY, partialTick);

        if (!modalOpen && this.suggestionPopup != null) {
            this.suggestionPopup.extractRenderState(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(guiGraphics, 0, this.layout.getHeaderHeight(), this.width, this.height);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }
}