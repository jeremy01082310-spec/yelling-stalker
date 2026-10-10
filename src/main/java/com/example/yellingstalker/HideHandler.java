package com.example.yellingstalker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.BedBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Esconderse bajo la cama: agachate (Shift) + clic derecho en una cama.
 * Para salir: clic derecho en la cama otra vez (o si te hacen dano).
 */
@Mod.EventBusSubscriber(modid = YellingStalkerMod.MOD_ID)
public class HideHandler {
    private static final Map<UUID, BlockPos> HIDDEN = new HashMap<>();

    public static boolean isHidden(UUID id) {
        return HIDDEN.containsKey(id);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BedBlock)) return;

        if (isHidden(sp.getUUID())) {
            unhide(sp);
            cancel(event);
        } else if (sp.isShiftKeyDown()) {
            hide(sp, event.getPos());
            cancel(event);
        }
        // clic normal = dormir como siempre
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // Si te golpean, sales del escondite
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && isHidden(sp.getUUID())) {
            unhide(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) unhide(sp);
    }

    // Si te alejas de la cama o la cama desaparece, sales
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || HIDDEN.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (Map.Entry<UUID, BlockPos> e : new ArrayList<>(HIDDEN.entrySet())) {
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            if (sp == null) {
                HIDDEN.remove(e.getKey());
                continue;
            }
            boolean bedGone = !(sp.level().getBlockState(e.getValue()).getBlock() instanceof BedBlock);
            if (bedGone || sp.blockPosition().distSqr(e.getValue()) > 9) unhide(sp);
        }
    }

    private static void hide(ServerPlayer p, BlockPos bed) {
        HIDDEN.put(p.getUUID(), bed);
        p.teleportTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
        int d = MobEffectInstance.INFINITE_DURATION;
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, d, 0, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, d, 255, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, d, 128, false, false, false));
        p.displayClientMessage(
                Component.literal("Te escondes bajo la cama... no hagas ruido. (Clic derecho para salir)"), true);
    }

    private static void unhide(ServerPlayer p) {
        if (HIDDEN.remove(p.getUUID()) == null) return;
        p.removeEffect(MobEffects.INVISIBILITY);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.JUMP);
    }
}
