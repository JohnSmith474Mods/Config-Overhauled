package johnsmith.configoverhauled.impl.client.gui.entry;

import johnsmith.configoverhauled.api.Group;

import com.google.common.collect.ImmutableList;

import johnsmith.configoverhauled.api.client.gui.entry.AbstractConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CategoryEntry extends AbstractConfigEntry {
    private final Minecraft minecraft;
    private final Component label;
    private final int textWidth;

    public CategoryEntry(Group category, Minecraft minecraft) {
        this.minecraft = minecraft;
        this.label = Component.translatable(category.translationKey())
                .withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
        this.textWidth = minecraft.font.width(this.label);
    }

    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, boolean isHovering, float partialTick) {
        int textY = this.getY() + (this.getHeight() - minecraft.font.lineHeight) / 2;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        guiGraphicsExtractor.text(minecraft.font, this.label, screenWidth / 2 - this.textWidth / 2, textY, 0xFFFFFFFF, false);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() { return Collections.emptyList(); }

    @Override
    public @NotNull List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(new NarratableEntry() {
            @Override public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.HOVERED; }
            @Override public void updateNarration(@NotNull NarrationElementOutput output) { output.add(NarratedElementType.TITLE, label); }
        });
    }
}