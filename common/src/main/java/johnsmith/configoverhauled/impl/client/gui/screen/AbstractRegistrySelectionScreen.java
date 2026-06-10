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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractRegistrySelectionScreen<T> extends Screen {
    public static final Identifier SELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    public static final Identifier SELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/select");
    public static final Identifier UNSELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
    public static final Identifier UNSELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect");
    public static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up");
    public static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    public static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down");
    public static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");

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

    protected ElementEntry createEntry(AbstractElementList list, T element, Identifier sprite, Identifier highlightedSprite, Runnable onTransfer) {
        return new ElementEntry(list, element, this.nameProvider.apply(element), this.iconProvider.apply(element), sprite, highlightedSprite, onTransfer);
    }

    public abstract class AbstractElementList extends ObjectSelectionList<ElementEntry> {
        public AbstractElementList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public AbstractElementList(Minecraft minecraft, int width, int height, int y, int itemHeight, int headerHeight) {
            super(minecraft, width, height, y, itemHeight);
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
        private final Identifier sprite;
        private final Identifier highlightedSprite;
        private final Runnable onTransfer;
        private Runnable onMoveUp;
        private Runnable onMoveDown;

        public ElementEntry(AbstractElementList list, T element, Component name, ItemStack icon, Identifier sprite, Identifier highlightedSprite, Runnable onTransfer) {
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
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            if (isHovering) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + 32, this.getY() + 32, -1601138544);
            }

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(this.getX(), this.getY());
            guiGraphics.pose().scale(2.0F, 2.0F);
            guiGraphics.renderItem(this.icon, 0, 0);
            guiGraphics.pose().popMatrix();

            if (isHovering && this.sprite != null && this.highlightedSprite != null) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(0, 0);

                int j = mouseX - this.getX();
                int k = mouseY - this.getY();

                if (this.onMoveUp == null && this.onMoveDown == null) {
                    if (j < 32) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.highlightedSprite, this.getX(), this.getY(), 32, 32);
                    } else {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), 32, 32);
                    }
                } else {
                    if (j < 16) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.highlightedSprite, this.getX(), this.getY(), 32, 32);
                    } else {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), 32, 32);
                    }

                    if (this.onMoveUp != null) {
                        if (j < 32 && j > 16 && k < 16) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_SPRITE, this.getX(), this.getY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_SPRITE, this.getX(), this.getY(), 32, 32);
                        }
                    }

                    if (this.onMoveDown != null) {
                        if (j < 32 && j > 16 && k > 16) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getX(), this.getY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_SPRITE, this.getX(), this.getY(), 32, 32);
                        }
                    }
                }

                guiGraphics.pose().popMatrix();
            }

            int maxWidth = width - 38;

            Component trimmedName = AbstractRegistrySelectionScreen.this.trimComponent(this.name, maxWidth);
            guiGraphics.drawString(AbstractRegistrySelectionScreen.this.font, trimmedName, this.getX() + 34, this.getY() + 1, 0xFFFFFFFF, false);

            Identifier key = AbstractRegistrySelectionScreen.this.registry.getKey(this.element);
            if (key != null) {
                String trimmedKey = AbstractRegistrySelectionScreen.this.trimString(key.toString(), maxWidth);
                guiGraphics.drawString(AbstractRegistrySelectionScreen.this.font, trimmedKey, this.getX() + 34, this.getY() + 12, 0xFF888888, false);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
            double j = event.x() - (double) this.list.getRowLeft();
            double k = event.y() - (double) this.list.getRowTop(this.list.children().indexOf(this));

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
            return super.mouseClicked(event, pressed);
        }

        @Override
        public Component getNarration() {
            return this.name;
        }
    }
}