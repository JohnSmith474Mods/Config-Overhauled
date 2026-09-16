package johnsmith.configoverhauled.impl.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegistrySelectionScreen<T> extends AbstractRegistrySelectionScreen<T> {
    private T currentSelection;
    private EditBox searchBox;
    private ElementList list;
    private final Consumer<T> onSelect;

    public RegistrySelectionScreen(Screen parent, Component title, Registry<T> registry, T initialSelection, Consumer<T> onSelect, Function<T, ItemStack> iconProvider, Function<T, Component> nameProvider) {
        super(parent, title, registry, iconProvider, nameProvider);
        this.currentSelection = initialSelection;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        this.list = new ElementList(this.minecraft, this.width, this.height - 124, 64, 36);
        this.addRenderableWidget(this.list);

        this.searchBox = this.addSearchBarWithClear(this.width / 2 - 154, this.height - 52, 308, query -> {
            this.updateSearch(query);
            this.list.setScrollAmount(0);
        });
        this.updateSearch("");

        this.addFooterButtons(() -> {
            if (this.currentSelection != null) {
                this.onSelect.accept(this.currentSelection);
            }
        });
    }

    private void updateSearch(String query) {
        double scroll = this.list.getScrollAmount();
        this.list.clearEntries();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (T element : this.registry) {
            if (this.currentSelection != null && this.currentSelection.equals(element)) continue;

            Component name = this.nameProvider.apply(element);
            ResourceLocation key = this.registry.getKey(element);

            if (lowerQuery.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(lowerQuery) || (key != null && key.toString().contains(lowerQuery))) {
                this.list.addEntry(this.createEntry(this.list, element, SELECT_SPRITE, SELECT_HIGHLIGHTED_SPRITE, () -> {
                    this.currentSelection = element;
                    this.updateSearch(this.searchBox.getValue());
                }));
            }
        }
        this.list.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        if (this.currentSelection != null) {
            int left = this.width / 2 - 118;
            int top = 24;

            guiGraphics.fill(left - 2, top - 2, left + 238, top + 34, 0xFFFFFFFF);
            guiGraphics.fill(left - 1, top - 1, left + 237, top + 33, 0xFF000000);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left, top, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(this.iconProvider.apply(this.currentSelection), 0, 0);
            guiGraphics.pose().popPose();

            int maxWidth = 203;
            Component name = this.nameProvider.apply(this.currentSelection);

            guiGraphics.drawString(this.font, this.trimComponent(name, maxWidth), left + 34, top + 1, 0xFFFFFFFF, false);

            ResourceLocation key = this.registry.getKey(this.currentSelection);
            if (key != null) {
                guiGraphics.drawString(this.font, this.trimString(key.toString(), maxWidth), left + 34, top + 12, 0xFF888888, false);
            }
        }
    }

    private class ElementList extends AbstractElementList {
        public ElementList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 240;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 124;
        }
    }
}