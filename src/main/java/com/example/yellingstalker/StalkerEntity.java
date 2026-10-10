package com.example.yellingstalker;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Stalker: persigue y ataca, pero NUNCA rompe bloques ni puertas
 * (no tiene ningun BreakDoorGoal y no modifica bloques).
 */
public class StalkerEntity extends Monster {

    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Ignora a los jugadores escondidos bajo la cama
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class,
                10, true, false, e -> !HideHandler.isHidden(e.getUUID())));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Si su objetivo se esconde, lo pierde
        if (getTarget() != null && HideHandler.isHidden(getTarget().getUUID())) {
            setTarget(null);
            getNavigation().stop();
        }

        // Grito mientras persigue
        if (getTarget() != null && this.tickCount % 160 == 0) {
            yell();
        }

        // Si hay un jugador escondido cerca, merodea y grita pero no lo encuentra
        if (this.tickCount % 80 == 0) {
            for (Player p : this.level().players()) {
                if (HideHandler.isHidden(p.getUUID()) && this.distanceToSqr(p) < 64.0) {
                    getLookControl().setLookAt(p);
                    yell();
                    break;
                }
            }
        }
    }

    private void yell() {
        this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_SCREAM,
                SoundSource.HOSTILE, 1.6f, 0.5f);
    }
}
