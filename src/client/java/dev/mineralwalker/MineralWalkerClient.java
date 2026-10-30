package dev.mineralwalker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;

/** Client-side mineral-colored health hearts while the complete avatar is active. */
public final class MineralWalkerClient implements ClientModInitializer {
    private static final int[][] HEART_PIXELS = {
            {0, 1, 1, 0, 1, 1, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0}
    };

    @Override
    public void onInitializeClient() {
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                original.extractRenderState(graphics, deltaTracker);
                return;
            }

            Block form = MineralBlocks.uniformMineral(player);
            if (form == null || player.isCreative() || player.isSpectator()) {
                original.extractRenderState(graphics, deltaTracker);
                return;
            }

            renderMineralHearts(graphics, player, MineralBlocks.colorFor(form));
        });
    }

    private static void renderMineralHearts(GuiGraphicsExtractor graphics, LocalPlayer player, int rgb) {
        int color = 0xFF000000 | (rgb & 0xFFFFFF);
        int dark = 0xFF242424;
        int maxHearts = Math.max(1, (int) Math.ceil(player.getMaxHealth() / 2.0F));
        float health = player.getHealth();

        int left = graphics.guiWidth() / 2 - 91;
        int bottom = graphics.guiHeight() - 39;

        for (int i = 0; i < maxHearts; i++) {
            int row = i / 10;
            int column = i % 10;
            int x = left + column * 8;
            int y = bottom - row * 10;
            float remaining = health - i * 2.0F;
            drawHeart(graphics, x, y, remaining >= 2.0F ? 1.0F : remaining >= 1.0F ? 0.5F : 0.0F,
                    color, dark);
        }
    }

    private static void drawHeart(GuiGraphicsExtractor graphics, int x, int y, float fill, int color, int empty) {
        for (int py = 0; py < HEART_PIXELS.length; py++) {
            for (int px = 0; px < HEART_PIXELS[py].length; px++) {
                if (HEART_PIXELS[py][px] == 0) continue;

                // A dark one-pixel edge keeps the colored hearts readable over bright scenes.
                graphics.fill(x + px + 1, y + py + 1, x + px + 2, y + py + 2, 0x99000000);
                int pixelColor = px < 3 ? (fill >= 1.0F ? color : fill >= 0.5F ? color : empty) : (fill >= 1.0F ? color : empty);
                if (fill <= 0.0F) pixelColor = empty;
                graphics.fill(x + px, y + py, x + px + 1, y + py + 1, pixelColor);
            }
        }
    }
}
