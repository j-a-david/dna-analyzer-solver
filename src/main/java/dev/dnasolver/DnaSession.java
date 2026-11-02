/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Derived from the DNA Analyzer solver in SkyHanni
 * (https://github.com/hannibal002/SkyHanni).
 */
package dev.dnasolver;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

/** Per-screen state. One instance lives inside each AbstractContainerScreen via the mixin. */
public final class DnaSession {
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;
    public static final int CLOSE_BUTTON_SLOT = 49;

    private static final int HIGHLIGHT_ARGB = 0x9955FF55;

    private boolean fake;
    private int[] lastSignature;
    private DnaSolver.Solution solution;

    /** True while this screen is a real DNA Analyzer GUI and the feature is switched on. */
    public boolean isActive(AbstractContainerScreen<?> screen) {
        if (!DnaConfig.get().enabled || fake) {
            return false;
        }
        return screen.getTitle().getString().endsWith(" DNA");
    }

    /** Re-reads the inventory and re-solves only when the colours changed. */
    public void refresh(AbstractContainerScreen<?> screen) {
        if (!isActive(screen)) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (menu.slots.size() <= LAST_SLOT) {
            return;
        }

        int[] signature = new int[LAST_SLOT - FIRST_SLOT + 1];
        for (int i = FIRST_SLOT; i <= LAST_SLOT; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) {
                // Contents have not arrived yet (or are mid-update). Keep the previous result.
                return;
            }
            int colour = colourOf(stack);
            if (colour < 0) {
                // Something that is not a DNA item: this is not the real analyzer GUI.
                fake = true;
                solution = null;
                return;
            }
            signature[i - FIRST_SLOT] = colour;
        }
        if (Arrays.equals(signature, lastSignature)) {
            return;
        }
        lastSignature = signature;

        // slot % 9 is the strand index (0..8), slot / 9 - 1 is the position inside the strand (0..3)
        int[][] board = new int[9][4];
        for (int i = FIRST_SLOT; i <= LAST_SLOT; i++) {
            board[i % 9][i / 9 - 1] = signature[i - FIRST_SLOT];
        }
        for (int[] strand : board) {
            if (Arrays.stream(strand).distinct().count() != 4) {
                // Not every colour in every strand yet. Wait for the next update.
                return;
            }
        }
        solution = DnaSolver.solve(board, DnaConfig.get().allowEnds);
    }

    public void renderHighlights(GuiGraphicsExtractor context, AbstractContainerScreen<?> screen) {
        if (!isActive(screen) || solution == null || solution.swaps().isEmpty()) {
            return;
        }
        DnaSolver.Swap next = solution.swaps().get(0);
        highlight(context, screen.getMenu(), next.row() + (next.a() + 1) * 9);
        highlight(context, screen.getMenu(), next.row() + (next.b() + 1) * 9);
    }

    private static void highlight(GuiGraphicsExtractor context, AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.getSlot(slotIndex);
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, HIGHLIGHT_ARGB);
    }

    public static boolean isBoardSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        int idx = slot.getContainerSlot();
        return idx >= FIRST_SLOT && idx <= LAST_SLOT;
    }

    /** 0 red, 1 green, 2 blue, 3 yellow, -1 if the item is not a DNA item. */
    private static int colourOf(ItemStack stack) {
        Component name = stack.getHoverName();
        final String[] firstText = {null};
        final Style[] firstStyle = {null};
        name.visit((style, text) -> {
            if (!text.isEmpty()) {
                firstText[0] = text;
                firstStyle[0] = style;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (firstText[0] == null || !firstText[0].startsWith("DNA")) {
            return -1;
        }
        TextColor color = firstStyle[0].getColor();
        if (color == null) {
            return -1;
        }
        int rgb = color.getValue();
        if (rgb == rgb(ChatFormatting.RED)) return 0;
        if (rgb == rgb(ChatFormatting.GREEN)) return 1;
        if (rgb == rgb(ChatFormatting.BLUE)) return 2;
        if (rgb == rgb(ChatFormatting.YELLOW)) return 3;
        return -1;
    }

    private static int rgb(ChatFormatting f) {
        Integer c = f.getColor();
        return c == null ? -1 : c;
    }
}
