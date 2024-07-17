package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EggViewerClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("eggviewer");

    @Override
    public void onInitializeClient() {
        // Register the tooltip event
        ItemTooltipCallback.EVENT.register(this::onItemTooltip);

        HudRenderCallback.EVENT.register(this::onRenderHud);

    }

    private void onRenderHud(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() == Items.TURTLE_EGG) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                NbtCompound polymer = nbt.getCompound("Polymer$itemTag");
                int cyclesLeft = Math.max(polymer.getInt("currentEggCycle"), 0);
                double stepsLeft = polymer.getDouble("stepsLeftInCycle");

                int green = (int) (255 * (cyclesLeft / 30.0));
                int red = 255 - green;
                int color = (red << 16) | (green << 8);

                MutableText steps = Text.literal("Cycles: " +cyclesLeft)
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)).setStyle(Style.EMPTY.withBold(true))
                        .append(Text.literal(" Steps: " + (int) stepsLeft).setStyle(Style.EMPTY.withBold(false))
                                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));

                int screenWidth = client.getWindow().getScaledWidth();
                int screenHeight = client.getWindow().getScaledHeight();
                int textWidth = client.textRenderer.getWidth(steps);
                int x = (screenWidth / 2) - (textWidth / 2);
                int y = screenHeight - 70;

                drawContext.drawText(client.textRenderer, steps, x, y, 0xFFFFFF, true);
            }
        }
    }

    private void onItemTooltip(ItemStack stack, TooltipContext context, List<Text> lines) {
        if (stack.getItem() == Items.TURTLE_EGG) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                NbtCompound polymer = nbt.getCompound("Polymer$itemTag");
                if (polymer != null) {
                    NbtCompound ivs = polymer.getCompound("IVs");
                    NbtList eggMoves = polymer.getList("BenchedMoves", 10);
                    String ability = polymer.getCompound("Ability").getString("AbilityName");

                    String gender = polymer.getString("Gender");

                    String genderSymbol;
                    Style genderColor;

                    if (gender.equals("FEMALE")) {
                        genderSymbol = "♀";
                        genderColor = Style.EMPTY.withColor(Formatting.LIGHT_PURPLE);
                    } else if (gender.equals("MALE")) {
                        genderSymbol = "♂";
                        genderColor = Style.EMPTY.withColor(Formatting.AQUA);
                    } else {
                        genderSymbol = "○";
                        genderColor = Style.EMPTY.withColor(Formatting.GRAY);
                    }

                    String shinySymbol = "";
                    if (polymer.getBoolean("Shiny")) {
                        shinySymbol = " ★";
                    }
                    String Form = polymer.getString("FormId") + " ";

                    String AbilityPrio = polymer.getCompound("Ability").getString("AbilityPriority");
                    String HA = "No";
                    if (!AbilityPrio.equals("LOWEST")) {
                        HA = "yes";
                    }

                    lines.add(Text.literal("Form: ").append(Text.literal(Form)).setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE))
                            .append(Text.literal(genderSymbol).setStyle(genderColor)
                                    .append(Text.literal(shinySymbol).setStyle(Style.EMPTY.withColor(Formatting.GOLD)))));

                    lines.add(Text.literal("Ability: ").setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(Text.literal(String.valueOf(ability)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                            .append(Text.literal(" | ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)))
                            .append(Text.literal("HA: ").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                            .append(Text.literal(HA).setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    int HPtype = this.CalcHPType(ivs);
                    int HPDamage = this.CalcHPDamage(ivs);

                    lines.add(Text.literal("Hidden power: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN))
                            .append(Text.literal(HPDamage + " ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                            .append(Text.literal(Type.values()[HPtype].name()).setStyle(Style.EMPTY.withColor(Type.values()[HPtype].getColor()))));
                    String ball = polymer.getString("CaughtBall").split(":")[1].replace('_', ' ');
                    lines.add(Text.literal("Pokeball: ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B1A1A))).append(Text.literal(ball).setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("\n"));

                    lines.add(Text.literal("HP: ").setStyle(Style.EMPTY.withColor(Formatting.RED))
                            .append(Text.literal(String.valueOf(ivs.getInt("hp")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("Attack: ").setStyle(Style.EMPTY.withColor(Formatting.BLUE))
                            .append(Text.literal(String.valueOf(ivs.getInt("attack")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("Defense: ").setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                            .append(Text.literal(String.valueOf(ivs.getInt("defence")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("Special Attack: ").setStyle(Style.EMPTY.withColor(Formatting.AQUA))
                            .append(Text.literal(String.valueOf(ivs.getInt("special_attack")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("Special Defence: ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
                            .append(Text.literal(String.valueOf(ivs.getInt("special_defence")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                    lines.add(Text.literal("Speed: ").setStyle(Style.EMPTY.withColor(Formatting.GREEN))
                            .append(Text.literal(String.valueOf(ivs.getInt("speed")))
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))));


                    if (!eggMoves.isEmpty()) {
                        lines.add(Text.literal("\n"));

                        lines.add(Text.literal("Egg moves: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE)));

                        for (int i = 0; i < eggMoves.size(); i++) {
                            NbtCompound moveTag = eggMoves.getCompound(i);
                            lines.add(Text.literal("  " + moveTag.getString("MoveName")).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
                            if (i < eggMoves.size() - 1) {
                                lines.add(Text.literal(", ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
                            }
                        }
                    }

                }
            }
        }
    }

    private int CalcHPType(NbtCompound ivs) {
        int a = ivs.getInt("hp") % 2;
        int b = ivs.getInt("attack") % 2;
        int c = ivs.getInt("defence") % 2;
        int d = ivs.getInt("speed") % 2;
        int e = ivs.getInt("special_attack") % 2;
        int f = ivs.getInt("special_defence") % 2;

        int typeBits = a + 2 * b + 4 * c + 8 * d + 16 * e + 32 * f;
        return (int) Math.floor((typeBits * 15) / 63.0);
    }

    private int CalcHPDamage(NbtCompound ivs) {
        int u = (ivs.getInt("hp") % 4) / 2;
        int v = (ivs.getInt("attack") % 4) / 2;
        int w = (ivs.getInt("defence") % 4) / 2;
        int x = (ivs.getInt("speed") % 4) / 2;
        int y = (ivs.getInt("special_attack") % 4) / 2;
        int z = (ivs.getInt("special_defence") % 4) / 2;

        int powerBits = u + 2 * v + 4 * w + 8 * x + 16 * y + 32 * z;

        return (int) Math.floor(((powerBits * 40) / 63.0) + 30);
    }

    private enum Type {
        FIGHTING(0xC22E28),
        FLYING(0xA98FF3),
        POISON(0xA33EA1),
        GROUND(0xE2BF65),
        ROCK(0xB6A136),
        BUG(0xA6B91A),
        GHOST(0x735797),
        STEEL(0xB7B7CE),
        FIRE(0xEE8130),
        WATER(0x6390F0),
        GRASS(0x7AC74C),
        ELECTRIC(0xF7D02C),
        PSYCHIC(0xF95587),
        ICE(0x96D9D6),
        DRAGON(0x6F35FC),
        DARK(0x705746);

        private final TextColor color;

        Type(int colorCode) {
            this.color = TextColor.fromRgb(colorCode);
        }

        public TextColor getColor() {
            return color;
        }
    }
}