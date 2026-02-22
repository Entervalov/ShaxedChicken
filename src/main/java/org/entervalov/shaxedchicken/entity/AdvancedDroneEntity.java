package org.entervalov.shaxedchicken.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.entervalov.shaxedchicken.entity.ai.DroneAIController;
import org.entervalov.shaxedchicken.entity.ai.DroneAIState;
import org.entervalov.shaxedchicken.entity.controller.AdvancedFlyingController;
import org.entervalov.shaxedchicken.entity.memory.DroneMemory;
import org.entervalov.shaxedchicken.entity.sensors.DroneSensorSystem;
import org.entervalov.shaxedchicken.entity.tactics.DroneTacticsModule;
import org.entervalov.shaxedchicken.network.HackCameraPacket;
import org.entervalov.shaxedchicken.network.ModNetwork;
import org.entervalov.shaxedchicken.utils.DebugSystem;
import org.entervalov.shaxedchicken.Main;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AdvancedDroneEntity extends FlyingEntity implements IMob {

    private static final DataParameter<Boolean> IS_ATTACKING = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> STATE_ID = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> DRONE_TYPE = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> IS_JAMMED = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> EMERGENCY_TICKS_SYNC = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> CHUNK_LOCKED_SYNC = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.BOOLEAN);

    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> JAMMED_TICKS_SYNC = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> JAMMER_POS_SYNC = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<BlockPos>> DEBUG_TARGET_POS_SYNC = EntityDataManager.defineId(AdvancedDroneEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);

    private final DroneMemory memory;
    private final DroneSensorSystem sensors;
    private final DroneTacticsModule tactics;
    private final DroneAIController aiController;

    private static final int MAX_LIFETIME = 24000;
    private static final int HACK_RANGE = 30;
    private static final int HACK_TIME = 140;
    private static final int HACK_COOLDOWN = 200;
    private static final int HACK_DURATION = 2400;
    private static final int MARK_MAX_TICKS = 200;
    private static final float RESIST_CHANCE = 0.35f;
    private static final int RESIST_PENALTY = 20;

    private int jammedTicks = 0;
    private BlockPos jammerPos = null;
    private DroneType cachedType = null;

    private UUID hackingPlayer = null;
    private int hackingTicks = 0;
    private int hackingCooldown = 0;

    private UUID hackedOwner = null;
    private int hackedTicks = 0;

    private int markTicks = 0;
    private UUID markOwner = null;
    private int commandTargetId = -1;

    private boolean forcedChunkActive = false;
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;

    private static final int STUCK_TRIGGER_TICKS = 80;
    private static final int EMERGENCY_COUNTDOWN_TICKS = 60;
    private int stuckAirTicks = 0;
    private int emergencyTicks = 0;

    public enum HackResult { STARTED, COOLDOWN, TOO_FAR, ALREADY_HACKED }

    public AdvancedDroneEntity(EntityType<? extends FlyingEntity> type, World world) {
        super(type, world);
        this.moveControl = new AdvancedFlyingController(this);
        this.memory = new DroneMemory();
        this.sensors = new DroneSensorSystem(this);
        this.tactics = new DroneTacticsModule(this);
        this.aiController = new DroneAIController(this);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ATTACKING, false);
        this.entityData.define(STATE_ID, 0);
        this.entityData.define(DRONE_TYPE, 0);
        this.entityData.define(IS_JAMMED, false);
        this.entityData.define(EMERGENCY_TICKS_SYNC, 0);
        this.entityData.define(CHUNK_LOCKED_SYNC, false);

        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(JAMMED_TICKS_SYNC, 0);
        this.entityData.define(JAMMER_POS_SYNC, Optional.empty());
        this.entityData.define(DEBUG_TARGET_POS_SYNC, Optional.empty());
    }

    @Override
    public ITextComponent getName() {
        if (hasCustomName()) return super.getName();
        return getDroneType().getFormattedName();
    }

    @Override
    public net.minecraft.entity.EntitySize getDimensions(@Nonnull net.minecraft.entity.Pose pose) {
        DroneType type = getDroneType();
        float multiplier = type.getSizeMultiplier();
        return super.getDimensions(pose).scale(multiplier);
    }

    @Override
    public void onSyncedDataUpdated(@Nonnull DataParameter<?> key) {
        super.onSyncedDataUpdated(key);
        if (DRONE_TYPE.equals(key)) {
            this.cachedType = null;
            this.refreshDimensions();
        }
    }

    public void setDroneType(DroneType type) {
        this.entityData.set(DRONE_TYPE, type.ordinal());
        this.cachedType = type;
        applyTypeAttributes();
        this.refreshDimensions();
    }

    public DroneType getDroneType() {
        if (cachedType == null) {
            cachedType = DroneType.fromId(this.entityData.get(DRONE_TYPE));
        }
        return cachedType;
    }

    private void applyTypeAttributes() {
        DroneType type = getDroneType();
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(type.getHealth());
        this.setHealth((float) type.getHealth());
        double baseSpeed = 4.0;
        Objects.requireNonNull(this.getAttribute(Attributes.FLYING_SPEED)).setBaseValue(baseSpeed * type.getSpeedMultiplier());
        Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.8 * type.getSpeedMultiplier());
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return FlyingEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.FLYING_SPEED, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.8D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 120.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    public static boolean canSpawn(EntityType<AdvancedDroneEntity> type, net.minecraft.world.IServerWorld world, net.minecraft.entity.SpawnReason reason, BlockPos pos, java.util.Random random) {
        net.minecraft.world.server.ServerWorld level = world.getLevel();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (pos.getY() < 60 || pos.getY() > 180) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        return !level.getBlockState(pos).getMaterial().isLiquid();
    }

    @Override
    public void tick() {
        if (DebugSystem.FREEZE_DRONES) {
            this.setDeltaMovement(0, 0, 0);
            if (!this.level.isClientSide) {
                this.getNavigation().stop();
                this.moveControl.setWantedPosition(this.getX(), this.getY(), this.getZ(), 0);
            }
        }

        super.tick();

        if (DebugSystem.FREEZE_DRONES) {
            this.setDeltaMovement(0, 0, 0);
            this.setNoGravity(true);
            return;
        }

        if (!this.level.isClientSide) {

            if (this.tickCount % 5 == 0) {
                Vector3d dest = aiController.getCurrentDestination();
                if (dest != null) this.entityData.set(DEBUG_TARGET_POS_SYNC, Optional.of(new BlockPos(dest)));
                else this.entityData.set(DEBUG_TARGET_POS_SYNC, Optional.empty());
            }

            LivingEntity target = getTarget();
            this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
            this.entityData.set(JAMMED_TICKS_SYNC, this.jammedTicks);
            this.entityData.set(JAMMER_POS_SYNC, Optional.ofNullable(jammerPos));

            if (markTicks > 0) {
                markTicks--;
                if (markTicks == 0 && markOwner != null) {
                    setGlowing(false);
                    markOwner = null;
                }
            }

            if (hackingCooldown > 0) hackingCooldown--;
            if (hackedTicks > 0) {
                hackedTicks--;
                if (hackedTicks == 0) {
                    if (hackedOwner != null) {
                        ServerPlayerEntity p = (ServerPlayerEntity) level.getPlayerByUUID(hackedOwner);
                        if (p != null) sendCameraPacket(p, false);
                    }
                    clearHackedOwner();
                    releaseForcedChunkTicket();
                }
            }

            boolean jammedHandled = false;
            if (this.jammedTicks > 0) {
                this.jammedTicks--;
                this.entityData.set(IS_JAMMED, true);
                if (jammerPos != null) {
                    double dist = Math.sqrt(this.distanceToSqr(jammerPos.getX(), jammerPos.getY(), jammerPos.getZ()));
                    if (dist < 10.0) {
                        if (isHacked() && hackedOwner != null) {
                            ServerPlayerEntity p = (ServerPlayerEntity) level.getPlayerByUUID(hackedOwner);
                            if (p != null) {
                                sendCameraPacket(p, false);
                                p.displayClientMessage(new TranslationTextComponent("drone.shaxedchicken.jammer_disconnect").withStyle(TextFormatting.RED), true);
                            }
                            clearHackedOwner();
                        }
                        handleCriticalFailure();
                    } else {
                        if (!isHacked()) {
                            handleInterference();
                        }

                    }
                } else {
                    if (!isHacked()) handleInterference();
                }
                jammedHandled = true;
            } else {
                if (this.entityData.get(IS_JAMMED)) this.entityData.set(IS_JAMMED, false);
                this.jammerPos = null;
            }

            if (jammedHandled) {
                if (isHacked()) updateForcedChunkTicket();
                else releaseForcedChunkTicket();
            }

            updateEmergencyTimer();

            if (hackingPlayer != null) {
                tickHacking();
                return;
            }
            if (jammedHandled) return;

            if (isHacked()) {
                updateForcedChunkTicket();
                if (handleHackedBehavior()) return;
            } else {
                releaseForcedChunkTicket();
                if (this.tickCount > MAX_LIFETIME) { this.remove(); return; }
                if (this.tickCount % 3 == 0) aiController.tick();
            }

            this.entityData.set(STATE_ID, aiController.getCurrentState().ordinal());
            this.entityData.set(IS_ATTACKING, aiController.getCurrentState() == DroneAIState.ATTACK || aiController.getCurrentState() == DroneAIState.DIVE_BOMB);

            if (this.tickCount % 5 == 0 && this.isInWater()) this.setDeltaMovement(this.getDeltaMovement().add(0, 0.5, 0));

            if (getDroneType().isNight() && this.tickCount % 200 == 0) {
                boolean isNight = this.level.getDayTime() % 24000 > 13000;
                if (isNight && !this.hasEffect(Effects.INVISIBILITY)) {
                    this.addEffect(new EffectInstance(Effects.INVISIBILITY, 220, 0, false, false));
                }
            }
        } else {
            if (this.tickCount % 5 == 0) handleClientEffects();
            if (isJammed() && random.nextFloat() < 0.3f) {
                this.level.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.05, 0);
            }
        }
    }

    private void handleInterference() {
        this.getNavigation().stop();
        this.setTarget(null);
        this.yRot += (random.nextFloat() - 0.5f) * 20.0f;
        this.yBodyRot = this.yRot;
        Vector3d motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.95 + (random.nextDouble()-0.5)*0.05, -0.05, motion.z * 0.95 + (random.nextDouble()-0.5)*0.05);
    }

    private void handleCriticalFailure() {
        this.getNavigation().stop();
        this.setTarget(null);
        this.setNoGravity(false);
        this.yRot += 45.0f;
        this.yBodyRot = this.yRot;
        this.setDeltaMovement(0, -0.8, 0);
        this.moveControl.setWantedPosition(this.getX(), this.getY() - 1.0, this.getZ(), 0);
        if (this.isOnGround() && !this.level.isClientSide) {
            this.level.explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, net.minecraft.world.Explosion.Mode.BREAK);
            this.remove();
        }
    }

    private boolean handleHackedBehavior() {
        if (!(this.level instanceof ServerWorld) || hackedOwner == null) return false;

        if (commandTargetId != -1) {
            Entity target = level.getEntity(commandTargetId);
            if (target == null || !target.isAlive()) {
                commandTargetId = -1;
                return false;
            }
            Vector3d dir = target.position().add(0, target.getBbHeight()/2, 0).subtract(this.position());
            double dist = dir.length();
            if (dist < 2.0) {
                disconnectHackedOwner(null);
                this.level.explode(this, getX(), getY(), getZ(), 3.0f, net.minecraft.world.Explosion.Mode.BREAK);
                this.remove();
                return true;
            }
            this.setDeltaMovement(dir.normalize().scale(0.5));
            return true;
        }
        return false;
    }

    private void tickHacking() {
        if (!(this.level instanceof ServerWorld) || hackingPlayer == null) return;
        ServerWorld world = (ServerWorld) this.level;
        ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUUID(hackingPlayer);

        if (player == null || !player.isAlive()) { cancelHacking(false); return; }
        if (player.distanceToSqr(this) > HACK_RANGE * HACK_RANGE || !player.canSee(this)) { cancelHacking(true); return; }

        if (this.getTarget() != player) this.setTarget(player);

        hackingTicks++;

        if (hackingTicks % 4 == 0) {
            double px = player.getX() + (this.getX() - player.getX()) * random.nextDouble();
            double py = player.getEyeY() + (this.getY() - player.getEyeY()) * random.nextDouble();
            double pz = player.getZ() + (this.getZ() - player.getZ()) * random.nextDouble();
            world.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 2, 0.1, 0.1, 0.1, 0.01);
        }

        if (hackingTicks % 20 == 0) {
            world.playSound(null, this.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundCategory.HOSTILE, 0.5f, 1.5f + random.nextFloat() * 0.5f);

            player.addEffect(new EffectInstance(Effects.CONFUSION, 40, 0, false, false));
            player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 40, 0, false, false));

            int percent = Math.min(100, (hackingTicks * 100) / HACK_TIME);
            int filled = percent / 10;
            StringBuilder bar = new StringBuilder("\u00A7b[");
            for (int i = 0; i < 10; i++) {
                bar.append(i < filled ? "\u00A7a\u2593" : "\u00A78\u2591");
            }
            bar.append("\u00A7b] \u00A7f").append(percent).append("% \u00A77— \u00A7b");
            bar.append(new TranslationTextComponent("drone.shaxedchicken.hack_progress").getString());
            player.displayClientMessage(new StringTextComponent(bar.toString()), true);

            if (this.random.nextFloat() < RESIST_CHANCE) {
                hackingTicks = Math.max(0, hackingTicks - RESIST_PENALTY);
                player.addEffect(new EffectInstance(Effects.WEAKNESS, 40, 0, false, false));

                world.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundCategory.HOSTILE, 0.8f, 0.5f);

                int newPercent = Math.min(100, (hackingTicks * 100) / HACK_TIME);
                player.displayClientMessage(new StringTextComponent(
                        "\u00A7c\u26A0 " + new TranslationTextComponent("drone.shaxedchicken.hack_resist").getString()
                                + " \u00A77(-" + (percent - newPercent) + "%)"
                ), true);
                this.setDeltaMovement(this.getDeltaMovement().add((random.nextDouble()-0.5)*0.3, 0.1, (random.nextDouble()-0.5)*0.3));
            }
        }
        if (hackingTicks >= HACK_TIME) completeHacking(player);
    }

    public HackResult startHacking(ServerPlayerEntity player) {
        if (player.distanceToSqr(this) > HACK_RANGE * HACK_RANGE || !player.canSee(this)) return HackResult.TOO_FAR;
        if (isHacked()) return HackResult.ALREADY_HACKED;
        if (hackingPlayer != null || hackingCooldown > 0) return HackResult.COOLDOWN;

        hackingPlayer = player.getUUID();
        hackingTicks = 0;
        player.displayClientMessage(new TranslationTextComponent("drone.shaxedchicken.hack_started").withStyle(TextFormatting.AQUA), true);
        player.addEffect(new EffectInstance(Effects.CONFUSION, 60, 0, false, false));

        level.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1.0f, 1.8f);

        return HackResult.STARTED;
    }

    private void completeHacking(ServerPlayerEntity player) {
        hackingPlayer = null;
        hackingTicks = 0;
        hackingCooldown = HACK_COOLDOWN;
        hackedOwner = player.getUUID();
        hackedTicks = HACK_DURATION;
        commandTargetId = -1;

        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundCategory.HOSTILE, 1.0f, 1.5f);
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.5, getZ(), 15, 0.5, 0.5, 0.5, 0.1);
        }

        player.displayClientMessage(new StringTextComponent(
                "\u00A7a\u2713 " + new TranslationTextComponent("drone.shaxedchicken.hack_success").getString()
                        + " \u00A77— WASD/Space/LMB"
        ), true);
        sendCameraPacket(player, true);
    }

    private void cancelHacking(boolean notify) {
        if (hackingPlayer != null && level instanceof ServerWorld && notify) {
            ServerPlayerEntity p = (ServerPlayerEntity) level.getPlayerByUUID(hackingPlayer);
            if (p != null) {
                p.displayClientMessage(new TranslationTextComponent("drone.shaxedchicken.hack_failed").withStyle(TextFormatting.RED), true);
                level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundCategory.HOSTILE, 0.5f, 0.5f);
            }
        }
        hackingPlayer = null;
        hackingTicks = 0;
        hackingCooldown = HACK_COOLDOWN;
    }

    public boolean isHacked() {
        return hackedOwner != null && hackedTicks > 0;
    }

    public boolean isHackedBy(UUID uuid) {
        return isHacked() && hackedOwner.equals(uuid);
    }

    public void handleInput(boolean attack, boolean exit, float yRot, float xRot, float forward, float strafe, boolean jump, boolean sneak) {
        if (!isHacked()) return;

        if (isJammed() && getDistanceToJammer() < 10.0) {
            disconnectHackedOwner(new TranslationTextComponent("drone.shaxedchicken.jammer_disconnect").withStyle(TextFormatting.RED));
            return;
        }

        this.yRot = yRot;
        this.xRot = xRot;
        this.yHeadRot = yRot;
        this.yBodyRot = yRot;

        if (exit) {
            if (hackedOwner != null && this.level instanceof ServerWorld) {
                ServerPlayerEntity player = (ServerPlayerEntity) this.level.getPlayerByUUID(hackedOwner);
                if (player != null) {
                    sendCameraPacket(player, false);
                    player.displayClientMessage(new TranslationTextComponent("drone.shaxedchicken.disconnect"), true);
                }
                clearHackedOwner();
            }
            releaseForcedChunkTicket();
            return;
        }

        if (attack) {
            disconnectHackedOwner(null);
            this.level.explode(this, this.getX(), this.getY(), this.getZ(), 3.0F, net.minecraft.world.Explosion.Mode.BREAK);
            this.remove();
            return;
        }

        Vector3d look = Vector3d.directionFromRotation(0, yRot);
        Vector3d left = Vector3d.directionFromRotation(0, yRot - 90);

        double speed = 0.5;

        Vector3d motion = Vector3d.ZERO;
        if (Math.abs(forward) > 0) motion = motion.add(look.scale(forward * speed));
        if (Math.abs(strafe) > 0) motion = motion.add(left.scale(strafe * speed));

        if (jump) {
            motion = motion.add(0, 0.4, 0);
        } else if (sneak) {
            motion = motion.add(0, -0.3, 0);
        } else {
            motion = motion.add(0, -0.05, 0);
        }

        this.setDeltaMovement(motion);
        this.hasImpulse = true;
    }

    @Override
    public void travel(@Nonnull Vector3d travelVector) {
        if (DebugSystem.FREEZE_DRONES) {
            this.setDeltaMovement(0,0,0); return;
        }
        super.travel(travelVector);
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide) {
            disconnectHackedOwner(null);
        }
        super.remove();
    }

    private void updateForcedChunkTicket() {
        if (!(this.level instanceof ServerWorld)) return;
        BlockPos pos = this.blockPosition();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (forcedChunkActive && (chunkX != forcedChunkX || chunkZ != forcedChunkZ)) {
            ForgeChunkManager.forceChunk((ServerWorld) this.level, Main.MOD_ID, this, forcedChunkX, forcedChunkZ, false, true);
            forcedChunkActive = false;
        }

        if (!forcedChunkActive || chunkX != forcedChunkX || chunkZ != forcedChunkZ) {
            if (ForgeChunkManager.forceChunk((ServerWorld) this.level, Main.MOD_ID, this, chunkX, chunkZ, true, true)) {
                forcedChunkActive = true;
                forcedChunkX = chunkX;
                forcedChunkZ = chunkZ;
                this.entityData.set(CHUNK_LOCKED_SYNC, true);
            }
        }
    }

    private void releaseForcedChunkTicket() {
        if (!forcedChunkActive || !(this.level instanceof ServerWorld)) return;
        ForgeChunkManager.forceChunk((ServerWorld) this.level, Main.MOD_ID, this, forcedChunkX, forcedChunkZ, false, true);
        forcedChunkActive = false;
        forcedChunkX = Integer.MIN_VALUE;
        forcedChunkZ = Integer.MIN_VALUE;
        this.entityData.set(CHUNK_LOCKED_SYNC, false);
    }

    private void clearHackedOwner() {
        hackedOwner = null;
        hackedTicks = 0;
        commandTargetId = -1;
        stuckAirTicks = 0;
        emergencyTicks = 0;
        if (!this.level.isClientSide) {
            this.entityData.set(EMERGENCY_TICKS_SYNC, 0);
        }
    }

    private void disconnectHackedOwner(ITextComponent message) {
        if (this.level instanceof ServerWorld && hackedOwner != null) {
            ServerPlayerEntity player = (ServerPlayerEntity) this.level.getPlayerByUUID(hackedOwner);
            if (player != null) {
                sendCameraPacket(player, false);
                if (message != null) {
                    player.displayClientMessage(message, true);
                }
            }
        }
        clearHackedOwner();
        releaseForcedChunkTicket();
    }

    private void updateEmergencyTimer() {
        if (this.level.isClientSide) return;

        if (!isHacked()) {
            stuckAirTicks = 0;
            if (emergencyTicks != 0) {
                emergencyTicks = 0;
                this.entityData.set(EMERGENCY_TICKS_SYNC, 0);
            }
            return;
        }

        if (this.isOnGround() || this.isInWater() || this.isInLava()) {
            stuckAirTicks = 0;
            if (emergencyTicks != 0) {
                emergencyTicks = 0;
                this.entityData.set(EMERGENCY_TICKS_SYNC, 0);
            }
            return;
        }

        Vector3d motion = this.getDeltaMovement();
        double horiz = motion.x * motion.x + motion.z * motion.z;
        double vert = Math.abs(motion.y);
        boolean stuck = horiz < 0.0004 && vert < 0.02;

        if (stuck) {
            stuckAirTicks++;
            if (emergencyTicks == 0 && stuckAirTicks >= STUCK_TRIGGER_TICKS) {
                emergencyTicks = EMERGENCY_COUNTDOWN_TICKS;
            }
        } else {
            stuckAirTicks = 0;
            if (emergencyTicks != 0) {
                emergencyTicks = 0;
            }
        }

        if (emergencyTicks > 0) {
            emergencyTicks--;
            if (emergencyTicks == 0) {
                disconnectHackedOwner(null);
                this.level.explode(this, this.getX(), this.getY(), this.getZ(), 3.0F, net.minecraft.world.Explosion.Mode.BREAK);
                this.remove();
                return;
            }
        }

        this.entityData.set(EMERGENCY_TICKS_SYNC, emergencyTicks);
    }

    private void sendCameraPacket(ServerPlayerEntity player, boolean enable) {
        ModNetwork.CHANNEL.send(
                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                new HackCameraPacket(this.getId(), enable)
        );
    }

    public void setJammed(BlockPos sourcePos, int ticks) { this.jammedTicks = ticks; this.jammerPos = sourcePos; }
    public boolean isJammed() { return this.level.isClientSide ? this.entityData.get(IS_JAMMED) : jammedTicks > 0; }
    public double getDistanceToJammer() {
        if(level.isClientSide) return entityData.get(JAMMER_POS_SYNC).map(p -> Math.sqrt(distanceToSqr(p.getX(), p.getY(), p.getZ()))).orElse(999.0);
        return jammerPos == null ? 999.0 : Math.sqrt(distanceToSqr(jammerPos.getX(), jammerPos.getY(), jammerPos.getZ()));
    }
    public void setCommandTarget(int id) { this.commandTargetId = id; }
    public void setMarkedBy(UUID owner, int ticks) { this.markOwner=owner; this.markTicks=ticks; if(ticks>0) setGlowing(true); }
    public BlockPos getDebugTargetPos() { return this.entityData.get(DEBUG_TARGET_POS_SYNC).orElse(null); }
    public int getDebugTargetId() { return this.entityData.get(TARGET_ID); }
    public int getDebugJammedTicks() { return this.entityData.get(JAMMED_TICKS_SYNC); }
    public int getEmergencyTicks() { return this.level.isClientSide ? this.entityData.get(EMERGENCY_TICKS_SYNC) : emergencyTicks; }
    public boolean isChunkLockActive() { return this.level.isClientSide ? this.entityData.get(CHUNK_LOCKED_SYNC) : forcedChunkActive; }
    public DroneAIState getAIState() { return DroneAIState.values()[this.entityData.get(STATE_ID)]; }
    public void setAttacking(boolean attacking) { this.entityData.set(IS_ATTACKING, attacking); }

    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 65536; }
    @Override public boolean isPickable() { return true; }
    @Override protected void registerGoals() {}
    @Override public void onAddedToWorld() { super.onAddedToWorld(); this.setDeltaMovement(0, 1.0, 0); if (!this.level.isClientSide && this.entityData.get(DRONE_TYPE) == 0) setDroneType(DroneType.getRandomType(this.random)); }
    @Override public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (source == DamageSource.DROWN) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() != null) {
            memory.addDangerZone(blockPosition(), 5.0, 10000);
            if (source.getEntity() instanceof LivingEntity) {
                DroneMemory.TargetMemory mem = memory.getTargetMemory(source.getEntity().getUUID());
                if (mem != null) { mem.isHighPriority = true; mem.threatLevel += 30; }
            }
        }
        return hurt;
    }
    @Override public boolean causeFallDamage(float distance, float damageMultiplier) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GENERIC_EXPLODE; }
    @Override public void addAdditionalSaveData(@Nonnull CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("DroneType", getDroneType().ordinal());
        nbt.putInt("JammedTicks", jammedTicks);
        if (hackedOwner != null) nbt.putUUID("HackedOwner", hackedOwner);
        nbt.putInt("HackedTicks", hackedTicks);
        nbt.putInt("HackCooldown", hackingCooldown);
        nbt.putInt("CommandTargetId", commandTargetId);
    }
    @Override public void readAdditionalSaveData(@Nonnull CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("DroneType")) setDroneType(DroneType.fromId(nbt.getInt("DroneType")));
        this.jammedTicks = nbt.getInt("JammedTicks");
        if (nbt.hasUUID("HackedOwner")) this.hackedOwner = nbt.getUUID("HackedOwner");
        this.hackedTicks = nbt.getInt("HackedTicks");
        this.hackingCooldown = nbt.getInt("HackCooldown");
        this.commandTargetId = nbt.getInt("CommandTargetId");
    }
    private void handleClientEffects() {
        if (isJammed()) {
            level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    getX() + (random.nextDouble() - 0.5) * 0.5,
                    getY() + 0.3 + random.nextDouble() * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0, 0.02, 0);
            if (random.nextFloat() < 0.2f) {
                level.addParticle(ParticleTypes.CRIT,
                        getX(), getY() + 0.5, getZ(),
                        (random.nextDouble() - 0.5) * 0.3, 0.1, (random.nextDouble() - 0.5) * 0.3);
            }
        }

        DroneType type = getDroneType();
        if (type == DroneType.FIRE && random.nextFloat() < 0.3f) {
            level.addParticle(ParticleTypes.FLAME,
                    getX(), getY() - 0.1, getZ(),
                    0, -0.05, 0);
        } else if (type == DroneType.NUCLEAR && random.nextFloat() < 0.15f) {
            level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    getX() + (random.nextDouble() - 0.5) * 0.4,
                    getY() + random.nextDouble() * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.4,
                    0, 0.05, 0);
        }
    }

    public DroneMemory getMemory() { return memory; }
    public DroneSensorSystem getSensors() { return sensors; }
    public DroneTacticsModule getTactics() { return tactics; }
    public DroneAIController getAIController() { return aiController; }
}