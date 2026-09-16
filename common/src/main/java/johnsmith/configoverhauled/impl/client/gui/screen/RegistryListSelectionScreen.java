package johnsmith.configoverhauled.impl.client.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegistryListSelectionScreen<T> extends AbstractRegistrySelectionScreen<T> {
    private final List<T> selectedItems;
    private EditBox availableSearchBox;
    private EditBox selectedSearchBox;
    private ElementList availableList;
    private ElementList selectedList;
    private final Consumer<List<T>> onSelect;

    public RegistryListSelectionScreen(Screen parent, Component title, Registry<T> registry, List<T> initialSelection, Consumer<List<T>> onSelect, Function<T, ItemStack> iconProvider, Function<T, Component> nameProvider) {
        super(parent, title, registry, iconProvider, nameProvider);
        this.selectedItems = new ArrayList<>(initialSelection);
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int halfWidth = this.width / 2 - 12;

        this.availableList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, 8);
        this.addRenderableWidget(this.availableList);

        this.selectedList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, this.width / 2 + 4);
        this.addRenderableWidget(this.selectedList);

        this.availableSearchBox = this.addSearchBarWithClear(8, this.height - 52, halfWidth, query -> {
            this.updateAvailable(query);
            this.availableList.setScrollAmount(0);
        });
        this.selectedSearchBox = this.addSearchBarWithClear(this.width / 2 + 4, this.height - 52, halfWidth, query -> {
            this.updateSelected(query);
            this.selectedList.setScrollAmount(0);
        });

        this.refreshLists();

        this.addFooterButtons(() -> this.onSelect.accept(new ArrayList<>(this.selectedItems)));
    }

    private void refreshLists() {
        this.updateAvailable(this.availableSearchBox.getValue());
        this.updateSelected(this.selectedSearchBox.getValue());
    }

    private void updateAvailable(String query) {
        double scroll = this.availableList.scrollAmount();
        this.availableList.clearEntries();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (T element : this.registry) {
            if (this.selectedItems.contains(element)) continue;

            Component name = this.nameProvider.apply(element);
            Identifier key = this.registry.getKey(element);

            if (lowerQuery.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(lowerQuery) || (key != null && key.toString().contains(lowerQuery))) {
                this.availableList.addEntry(this.createEntry(this.availableList, element, SELECT_SPRITE, SELECT_HIGHLIGHTED_SPRITE, () -> {
                    this.selectedItems.add(element);
                    this.refreshLists();
                }));
            }
        }
        this.availableList.setScrollAmount(scroll);
    }

    private void updateSelected(String query) {
        double scroll = this.availableList.scrollAmount();
        this.selectedList.clearEntries();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (int i = 0; i < this.selectedItems.size(); i++) {
            T element = this.selectedItems.get(i);
            Component name = this.nameProvider.apply(element);
            Identifier key = this.registry.getKey(element);

            if (lowerQuery.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(lowerQuery) || (key != null && key.toString().contains(lowerQuery))) {
                AbstractRegistrySelectionScreen<T>.ElementEntry entry = this.createEntry(this.selectedList, element, UNSELECT_SPRITE, UNSELECT_HIGHLIGHTED_SPRITE, () -> {
                    this.selectedItems.remove(element);
                    this.refreshLists();
                });

                int index = i;
                Runnable onMoveUp = index > 0 ? () -> {
                    this.selectedItems.remove(index);
                    this.selectedItems.add(index - 1, element);
                    this.refreshLists();
                } : null;

                Runnable onMoveDown = index < this.selectedItems.size() - 1 ? () -> {
                    this.selectedItems.remove(index);
                    this.selectedItems.add(index + 1, element);
                    this.refreshLists();
                } : null;

                entry.setSortable(onMoveUp, onMoveDown);
                this.selectedList.addEntry(entry);
            }
        }
        this.selectedList.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw screen title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        // Draw headers for the two lists
        drawListHeader(guiGraphics, this.availableList, Component.translatable("pack.available.title"));
        drawListHeader(guiGraphics, this.selectedList,  Component.translatable("pack.selected.title"));
    }

    private void drawListHeader(GuiGraphics guiGraphics, ElementList list, MutableComponent title) {
        Component formattedTitle = title
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.UNDERLINE);

        // y is calculated based on the list's position (e.g., list.getY() - headerHeight)
        int headerY = list.getY() - 16;
        guiGraphics.drawCenteredString(this.minecraft.font, formattedTitle, list.getX() + list.getRowWidth() / 2, headerY + 2, 0xFFFFFFFF);
    }

    private class ElementList extends AbstractElementList {
        public ElementList(Minecraft minecraft, int width, int height, int y, int itemHeight, int x) {
            super(minecraft, width, height, y, itemHeight, 16);
            this.setX(x);
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 6;
        }
    }
}