package johnsmith.configoverhauled.impl.client.gui.screen;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractRegistrySelectionScreen<T> extends Screen {
    public static final ResourceLocation SELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    public static final ResourceLocation SELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select");
    public static final ResourceLocation UNSELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    public static final ResourceLocation UNSELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    public static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    public static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    public static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    public static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");

    protected final Screen parent;
    protected final Registry<T> registry;
    protected final Function<T, ItemStack> iconProvider;
    protected final Function<T, Component> nameProvider;

    protected AbstractRegistrySelectionScreen(Screen parent, Component title, Registry<T> registry, Function<T, ItemStack> iconProvider, Function<T, Component> nameProvider) {
        super(title);
        this.parent = parent;
        this.registry = registry;
        this.iconProvider = iconProvider;
        this.nameProvider = nameProvider;
    }

    protected EditBox addSearchBarWithClear(int x, int y, int totalWidth, Consumer<String> responder) {
        int clearWidth = 20;
        int boxWidth = totalWidth - clearWidth - 4;

        EditBox searchBox = new EditBox(this.font, x, y, boxWidth, 20, Component.empty());
        searchBox.setHint(Component.translatable("config_overhauled.config.search_by_name_or_namespace").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        searchBox.setResponder(responder);
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
            searchBox.setValue("");
            searchBox.setFocused(true);
        }).bounds(x + boxWidth + 4, y, clearWidth, 20).build());

        return searchBox;
    }

    protected void addFooterButtons(Runnable onDone) {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 154, this.height - 26, 150, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            onDone.run();
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }

    protected ElementEntry createEntry(AbstractElementList list, T element, ResourceLocation sprite, ResourceLocation highlightedSprite, Runnable onTransfer) {
        return new ElementEntry(list, element, this.nameProvider.apply(element), this.iconProvider.apply(element), sprite, highlightedSprite, onTransfer);
    }

    public abstract class AbstractElementList extends ObjectSelectionList<ElementEntry> {
        public AbstractElementList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public AbstractElementList(Minecraft minecraft, int width, int height, int y, int itemHeight, int headerHeight) {
            super(minecraft, width, height, y, itemHeight, headerHeight);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        public int addEntry(ElementEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowTop(int index) {
            return super.getRowTop(index);
        }
    }

    protected Component trimComponent(Component component, int maxWidth) {
        if (this.font.width(component) <= maxWidth) {
            return component;
        }
        int ellipsisWidth = this.font.width("...");
        if (maxWidth <= ellipsisWidth) return Component.literal("...").withStyle(component.getStyle());
        String truncated = this.font.plainSubstrByWidth(component.getString(), maxWidth - ellipsisWidth);
        return Component.literal(truncated + "...").withStyle(component.getStyle());
    }

    protected String trimString(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = this.font.width("...");
        if (maxWidth <= ellipsisWidth) return "...";
        return this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    public class ElementEntry extends ObjectSelectionList.Entry<ElementEntry> {
        public final AbstractElementList list;
        public final T element;
        public final Component name;
        public final ItemStack icon;
        private final ResourceLocation sprite;
        private final ResourceLocation highlightedSprite;
        private final Runnable onTransfer;
        private Runnable onMoveUp;
        private Runnable onMoveDown;

        public ElementEntry(AbstractElementList list, T element, Component name, ItemStack icon, ResourceLocation sprite, ResourceLocation highlightedSprite, Runnable onTransfer) {
            this.list = list;
            this.element = element;
            this.name = name;
            this.icon = icon;
            this.sprite = sprite;
            this.highlightedSprite = highlightedSprite;
            this.onTransfer = onTransfer;
        }

        public ElementEntry setSortable(Runnable onMoveUp, Runnable onMoveDown) {
            this.onMoveUp = onMoveUp;
            this.onMoveDown = onMoveDown;
            return this;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            if (isMouseOver) {
                guiGraphics.fill(left, top, left + 32, top + 32, -1601138544);
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left, top, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(this.icon, 0, 0);
            guiGraphics.pose().popPose();

            if (isMouseOver && this.sprite != null && this.highlightedSprite != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200.0F);

                int j = mouseX - left;
                int k = mouseY - top;

                if (this.onMoveUp == null && this.onMoveDown == null) {
                    if (j < 32) {
                        guiGraphics.blitSprite(RenderType::guiTextured, this.highlightedSprite, left, top, 32, 32);
                    } else {
                        guiGraphics.blitSprite(RenderType::guiTextured, this.sprite, left, top, 32, 32);
                    }
                } else {
                    if (j < 16) {
                        guiGraphics.blitSprite(RenderType::guiTextured, this.highlightedSprite, left, top, 32, 32);
                    } else {
                        guiGraphics.blitSprite(RenderType::guiTextured, this.sprite, left, top, 32, 32);
                    }

                    if (this.onMoveUp != null) {
                        if (j < 32 && j > 16 && k < 16) {
                            guiGraphics.blitSprite(RenderType::guiTextured, MOVE_UP_HIGHLIGHTED_SPRITE, left, top, 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderType::guiTextured, MOVE_UP_SPRITE, left, top, 32, 32);
                        }
                    }

                    if (this.onMoveDown != null) {
                        if (j < 32 && j > 16 && k > 16) {
                            guiGraphics.blitSprite(RenderType::guiTextured, MOVE_DOWN_HIGHLIGHTED_SPRITE, left, top, 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderType::guiTextured, MOVE_DOWN_SPRITE, left, top, 32, 32);
                        }
                    }
                }

                guiGraphics.pose().popPose();
            }

            int maxWidth = 203;

            Component trimmedName = AbstractRegistrySelectionScreen.this.trimComponent(this.name, maxWidth);
            guiGraphics.drawString(AbstractRegistrySelectionScreen.this.font, trimmedName, left + 34, top + 1, 0xFFFFFFFF, false);

            ResourceLocation key = AbstractRegistrySelectionScreen.this.registry.getKey(this.element);
            if (key != null) {
                String trimmedKey = AbstractRegistrySelectionScreen.this.trimString(key.toString(), maxWidth);
                guiGraphics.drawString(AbstractRegistrySelectionScreen.this.font, trimmedKey, left + 34, top + 12, 0xFF888888, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double j = mouseX - (double) this.list.getRowLeft();
            double k = mouseY - (double) this.list.getRowTop(this.list.children().indexOf(this));

            if (j <= 32.0D) {
                if (this.onMoveUp == null && this.onMoveDown == null) {
                    if (this.onTransfer != null) this.onTransfer.run();
                    return true;
                }

                if (j < 16.0D && this.onTransfer != null) {
                    this.onTransfer.run();
                    return true;
                }

                if (j > 16.0D && k < 16.0D && this.onMoveUp != null) {
                    this.onMoveUp.run();
                    return true;
                }

                if (j > 16.0D && k > 16.0D && this.onMoveDown != null) {
                    this.onMoveDown.run();
                    return true;
                }
            }

            this.list.setSelected(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return this.name;
        }
    }
}