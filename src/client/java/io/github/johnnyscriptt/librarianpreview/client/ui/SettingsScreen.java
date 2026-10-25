package io.github.johnnyscriptt.librarianpreview.client.ui;

import io.github.johnnyscriptt.librarianpreview.client.Highlights;
import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.core.HighlightRules;
import io.github.johnnyscriptt.librarianpreview.core.config.LibrarianPreviewConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod.MOD_ID;

/**
 * Sheet on/off, draw range, and the list of enchantments to write in green. Clicking an
 * enchantment cycles off, any level, max level only. Saved when the screen closes.
 */
public final class SettingsScreen extends Screen {
    private static final int W = 240;
    private static final int BTN_H = 20;
    private static final int ROW_H = 14;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int[] RANGE_CHOICES = {8, 12, 16, 24, 32};
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA0A0A0;
    private static final int GREEN = 0xFF55FF55;
    private static final int ROW_BG = 0x30000000;
    private static final int ROW_HOVER = 0x25FFFFFF;
    private static final int ROW_ON = 0x3055FF55;

    private record Entry(Holder<Enchantment> holder, String id, String name, int maxLevel) {
    }

    private final Screen parent;
    private List<Entry> all = List.of();
    private List<Entry> filtered = List.of();
    private EditBox searchBox;
    private String appliedQuery;
    private int listX;
    private int listY;
    private int visibleRows;
    private int scrollRow;

    public SettingsScreen(Screen parent) {
        super(Component.translatable(MOD_ID + ".settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LibrarianPreviewConfig cfg = ConfigIo.get();
        int x = (this.width - W) / 2;
        int y = TITLE_Y + 14;
        int half = (W - GAP) / 2;

        addRenderableWidget(Button.builder(sheetsLabel(cfg), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(sheetsLabel(cfg));
        }).bounds(x, y, half, BTN_H).build());
        addRenderableWidget(Button.builder(rangeLabel(cfg), b -> {
            cfg.rangeBlocks = next(RANGE_CHOICES, cfg.rangeBlocks);
            b.setMessage(rangeLabel(cfg));
        }).bounds(x + half + GAP, y, half, BTN_H).build());
        y += BTN_H + 14;

        searchBox = new EditBox(this.font, x, y, W, BTN_H, Component.translatable(MOD_ID + ".settings.search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable(MOD_ID + ".settings.search"));
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
        y += BTN_H + GAP;

        listX = x;
        listY = y;
        visibleRows = Math.max(1, (this.height - listY - 32) / ROW_H);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, this.height - 26, W, BTN_H).build());

        all = loadEnchantments();
        appliedQuery = null;
    }

    /** Enchantments a librarian can sell, by name. */
    private List<Entry> loadEnchantments() {
        if (this.minecraft.level == null) {
            return List.of();
        }
        return this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(h -> h.is(EnchantmentTags.TRADEABLE))
                .map(h -> new Entry(h, h.getRegisteredName(), h.value().description().getString(),
                        h.value().getMaxLevel()))
                .sorted(Comparator.comparing(Entry::name))
                .toList();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        applyQuery();
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, getTitle(), this.width / 2, TITLE_Y, TEXT);
        g.text(this.font, Component.translatable(MOD_ID + ".settings.highlight"), listX,
                searchBox.getY() - 11, MUTED);

        HighlightRules rules = Highlights.rules();
        int hovered = rowAt(mouseX, mouseY);
        for (int row = 0; row < visibleRows && scrollRow + row < filtered.size(); row++) {
            Entry e = filtered.get(scrollRow + row);
            HighlightRules.Mode mode = rules.mode(e.id());
            int top = listY + row * ROW_H;
            int bg = scrollRow + row == hovered ? ROW_HOVER : mode == HighlightRules.Mode.OFF ? ROW_BG : ROW_ON;
            g.fill(listX, top, listX + W, top + ROW_H - 1, bg);
            int colour = mode == HighlightRules.Mode.OFF ? TEXT : GREEN;
            g.text(this.font, e.name(), listX + 4, top + 3, colour);
            Component state = modeLabel(mode, e);
            g.text(this.font, state, listX + W - 4 - this.font.width(state), top + 3,
                    mode == HighlightRules.Mode.OFF ? MUTED : GREEN);
        }
        if (filtered.isEmpty()) {
            g.centeredText(this.font, Component.translatable(MOD_ID + ".settings.no_matches"),
                    this.width / 2, listY + 4, MUTED);
        }
    }

    private static Component modeLabel(HighlightRules.Mode mode, Entry e) {
        return switch (mode) {
            case OFF -> Component.translatable(MOD_ID + ".settings.mode.off");
            case ANY -> Component.translatable(MOD_ID + ".settings.mode.any");
            case MAX -> Component.translatable(MOD_ID + ".settings.mode.max",
                    Component.translatable("enchantment.level." + e.maxLevel()));
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int index = rowAt((int) event.x(), (int) event.y());
        if (index >= 0) {
            Entry e = filtered.get(index);
            Highlights.rules().cycle(e.id(), e.maxLevel());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        int max = Math.max(0, filtered.size() - visibleRows);
        scrollRow = Math.clamp(scrollRow - (int) Math.signum(deltaY), 0, max);
        return true;
    }

    private int rowAt(int mouseX, int mouseY) {
        if (mouseX < listX || mouseX >= listX + W || mouseY < listY || mouseY >= listY + visibleRows * ROW_H) {
            return -1;
        }
        int index = scrollRow + (mouseY - listY) / ROW_H;
        return index < filtered.size() ? index : -1;
    }

    private void applyQuery() {
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) {
            return;
        }
        appliedQuery = query;
        filtered = query.isEmpty() ? all
                : all.stream().filter(e -> e.name().toLowerCase(Locale.ROOT).contains(query)).toList();
        scrollRow = 0;
    }

    private static Component sheetsLabel(LibrarianPreviewConfig cfg) {
        return Component.translatable(MOD_ID + ".settings.sheets",
                cfg.enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private static Component rangeLabel(LibrarianPreviewConfig cfg) {
        return Component.translatable(MOD_ID + ".settings.range", cfg.rangeBlocks);
    }

    private static int next(int[] choices, int current) {
        for (int choice : choices) {
            if (choice > current) {
                return choice;
            }
        }
        return choices[0];
    }

    /** Centre of a visible list row in GUI coordinates, for the in-game test to click. */
    public int rowCentreX() {
        return listX + W / 2;
    }

    public int rowCentreY(int row) {
        return listY + row * ROW_H + ROW_H / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ConfigIo.save();
        this.minecraft.gui.setScreen(parent);
    }
}
