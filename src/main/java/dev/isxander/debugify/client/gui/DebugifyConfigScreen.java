package dev.isxander.debugify.client.gui;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.client.DebugifyClient;
import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.fixes.FixCategory;
import dev.isxander.debugify.mixinplugin.DebugifyErrorHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Vanilla-style in-game config screen for Debugify (no YACL dependency).
 * Lists every registered bug fix grouped by environment and category, with an
 * on/off toggle per fix plus the misc/global options.
 */
public class DebugifyConfigScreen extends Screen {
    private static final int LIST_TOP = 40;
    private static final int LIST_BOTTOM_OFFSET = 36;
    private static final int TOGGLE_WIDTH = 90;

    private final Screen parent;
    private BugFixList list;

    public DebugifyConfigScreen(Screen parent) {
        super(Component.translatable("debugify.name"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = new BugFixList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_OFFSET, 24);
        addWidget(list);

        rebuildEntries();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .pos(width / 2 - 100, height - 28)
                .size(200, 20)
                .build());
    }

    private void rebuildEntries() {
        list.clearAll();
        DebugifyConfig config = Debugify.CONFIG;

        for (BugFix.Env env : BugFix.Env.values()) {
            list.addHeader(Component.translatable(env.getDisplayName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            for (FixCategory category : FixCategory.values()) {
                list.addHeader(Component.translatable(category.getDisplayName())
                        .withStyle(ChatFormatting.YELLOW));

                if (category == FixCategory.GAMEPLAY) {
                    list.addHeader(Component.translatable("debugify.gameplay.warning")
                            .withStyle(ChatFormatting.RED));
                    list.addToggle(
                            Component.translatable("debugify.gameplay.enable_in_multiplayer"),
                            config.gameplayFixesInMultiplayer,
                            false,
                            value -> config.gameplayFixesInMultiplayer = value,
                            null);
                }

                config.getBugFixes().forEach((bug, enabled) -> {
                    if (bug.env() == env && bug.category() == category) {
                        list.addBugFix(bug, enabled);
                    }
                });
            }
        }

        // Misc section
        list.addHeader(Component.translatable("debugify.misc").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        list.addToggle(
                Component.translatable("debugify.misc.default_disabled"),
                config.defaultDisabled,
                false,
                value -> config.defaultDisabled = value,
                Component.translatable("debugify.misc.default_disabled.description"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        list.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Debugify.LOGGER.info("Saving Debugify from config screen");
        Debugify.CONFIG.save();
        minecraft.setScreen(parent);
    }

    // ── Scrollable list ──────────────────────────────────────────────────

    private final class BugFixList extends ContainerObjectSelectionList<BugFixList.Entry> {
        BugFixList(net.minecraft.client.Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        protected int getScrollbarPosition() {
            return width - 7;
        }

        @Override
        public int getRowWidth() {
            return Math.min(380, width - 40);
        }

        void clearAll() {
            clearEntries();
        }

        void addHeader(Component text) {
            addEntry(new HeaderEntry(text));
        }

        void addToggle(Component name, boolean initialValue, boolean unavailable,
                        Consumer<Boolean> onChange, Component tooltip) {
            addEntry(new ToggleEntry(name, initialValue, unavailable, onChange, tooltip));
        }

        void addBugFix(BugFixData bug, boolean enabled) {
            addEntry(new BugFixEntry(bug, enabled));
        }

        // ── Entry base ────────────────────────────────────────────────────

        abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        }

        // ── Header / label row ────────────────────────────────────────────

        final class HeaderEntry extends Entry {
            private final Component text;

            HeaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int rowTop, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float delta) {
                graphics.drawString(font, text, left + 4, rowTop + 4, 0xFFFFFF);
            }

            public Component getNarration() {
                return text;
            }

            @Override
            public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }

        // ── Generic on/off toggle row ─────────────────────────────────────

        final class ToggleEntry extends Entry {
            private final CycleButton<Boolean> button;

            ToggleEntry(Component name, boolean initialValue, boolean unavailable,
                        Consumer<Boolean> onChange, Component tooltip) {
                CycleButton.Builder<Boolean> builder = CycleButton.booleanBuilder(
                        Component.translatable("debugify.fix.enabled"),
                        Component.translatable("debugify.fix.disabled"))
                        .withInitialValue(initialValue);
                this.button = builder.create(0, 0, TOGGLE_WIDTH, 20, name, (b, v) -> onChange.accept(v));
                if (unavailable) button.active = false;
                if (tooltip != null) button.setTooltip(Tooltip.create(tooltip));
            }

            @Override
            public void render(GuiGraphics graphics, int index, int rowTop, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float delta) {
                button.setX(left + rowWidth - TOGGLE_WIDTH);
                button.setY(rowTop);
                button.render(graphics, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return List.of(button);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(button);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            public Component getNarration() {
                return button.getMessage();
            }
        }

        // ── Per-bug-fix toggle row ────────────────────────────────────────

        final class BugFixEntry extends Entry {
            private final BugFixData bug;
            private final Component name;
            private final CycleButton<Boolean> button;
            private final boolean unavailable;

            BugFixEntry(BugFixData bug, boolean enabled) {
                this.bug = bug;
                this.unavailable = computeUnavailable(bug);
                this.name = Component.literal(bug.bugId());
                List<Component> description = buildDescription(bug);

                CycleButton.Builder<Boolean> builder = CycleButton.booleanBuilder(
                        Component.translatable("debugify.fix.enabled"),
                        Component.translatable("debugify.fix.disabled"))
                        .withInitialValue(enabled);
                this.button = builder.create(0, 0, TOGGLE_WIDTH, 20, Component.literal(""),
                        (b, v) -> Debugify.CONFIG.getBugFixes().replace(bug, v));
                if (unavailable) {
                    button.active = false;
                }
                if (!description.isEmpty()) {
                    button.setTooltip(Tooltip.create(joinLines(description)));
                }
            }

            private boolean computeUnavailable(BugFixData bug) {
                Set<String> conflicts = bug.getActiveConflicts();
                boolean satisfiesOS = bug.satisfiesOSRequirement();
                boolean errored = DebugifyErrorHandler.hasErrored(bug);
                return !conflicts.isEmpty() || !satisfiesOS || errored;
            }

            private List<Component> buildDescription(BugFixData bug) {
                List<Component> description = new ArrayList<>();
                Set<String> conflicts = bug.getActiveConflicts();
                boolean satisfiesOS = bug.satisfiesOSRequirement();
                boolean errored = DebugifyErrorHandler.hasErrored(bug);

                if (errored) {
                    description.add(Component.translatable("debugify.error.mixin_error", bug.bugId())
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }
                for (String conflictMod : conflicts) {
                    String modName = ModList.get().getModContainerById(conflictMod)
                            .map(c -> c.getModInfo().getDisplayName())
                            .orElse(conflictMod);
                    description.add(Component.translatable("debugify.error.conflict", bug.bugId(), modName)
                            .withStyle(ChatFormatting.RED));
                }
                if (!satisfiesOS) {
                    description.add(Component.translatable("debugify.error.os", bug.bugId(),
                            Component.translatable(bug.requiredOs().getDisplayName()))
                            .withStyle(ChatFormatting.RED));
                }
                if (DebugifyClient.bugFixDescriptionCache != null && DebugifyClient.bugFixDescriptionCache.has(bug.bugId())) {
                    description.add(Component.literal(DebugifyClient.bugFixDescriptionCache.get(bug.bugId()))
                            .withStyle(ChatFormatting.WHITE));
                }
                String explanationKey = "debugify.fix_explanation." + bug.bugId().toLowerCase();
                if (Language.getInstance().has(explanationKey)) {
                    description.add(Component.translatable(explanationKey).withStyle(ChatFormatting.GRAY));
                }
                String effectKey = "debugify.fix_effect." + bug.bugId().toLowerCase();
                if (Language.getInstance().has(effectKey)) {
                    description.add(Component.translatable(effectKey).withStyle(ChatFormatting.GOLD));
                }
                return description;
            }

            private Component joinLines(List<Component> lines) {
                MutableComponent combined = Component.empty();
                for (int i = 0; i < lines.size(); i++) {
                    combined.append(lines.get(i));
                    if (i < lines.size() - 1) combined.append("\n");
                }
                return combined;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int rowTop, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float delta) {
                Component displayName = unavailable
                        ? name.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH)
                        : name.copy();
                graphics.drawString(font, displayName, left + 4, rowTop + 5, 0xFFFFFF);

                button.setX(left + rowWidth - TOGGLE_WIDTH);
                button.setY(rowTop);
                button.render(graphics, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return List.of(button);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(button);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            public Component getNarration() {
                return name;
            }
        }
    }
}
