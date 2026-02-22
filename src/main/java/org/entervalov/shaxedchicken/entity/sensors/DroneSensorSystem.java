package org.entervalov.shaxedchicken.entity.sensors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.memory.DroneMemory;

import java.util.ArrayList;
import java.util.List;

public class DroneSensorSystem {

    private final AdvancedDroneEntity drone;

    private static final double VISION_RANGE = 80.0;
    private static final double VISION_ANGLE = 120.0;
    private static final double HEARING_RANGE = 40.0;
    private static final double RADAR_RANGE = 100.0;
    private static final double PROJECTILE_DETECTION_RANGE = 40.0;
    private static final double MAX_JAMMER_RANGE = 30.0;
    private static final double CRITICAL_RANGE = 10.0;

    private final List<LivingEntity> detectedTargets = new ArrayList<>();
    private final List<Entity> detectedProjectiles = new ArrayList<>();

    public DroneSensorSystem(AdvancedDroneEntity drone) {
        this.drone = drone;
    }


    public boolean canSeeTarget(LivingEntity target) {
        if (drone.isJammed()) {
            double dist = drone.getDistanceToJammer();

            if (dist < CRITICAL_RANGE) return false;

            double signalQuality = (dist - CRITICAL_RANGE) / (MAX_JAMMER_RANGE - CRITICAL_RANGE);

            if (drone.getRandom().nextFloat() > signalQuality) {
                return false;
            }
        }

        if (target == null) return false;

        double distance = drone.distanceTo(target);
        if (distance > VISION_RANGE) return false;

        Vector3d lookVec = drone.getViewVector(1.0F);
        Vector3d toTarget = target.position().subtract(drone.position()).normalize();
        double dot = lookVec.dot(toTarget);
        double angle = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, dot))));

        if (angle > VISION_ANGLE / 2) return false;

        return hasLineOfSight(target);
    }

    public void updateRadar() {
        detectedTargets.clear();
        detectedProjectiles.clear();

        if (drone.isJammed()) {
            double dist = drone.getDistanceToJammer();
            if (dist < CRITICAL_RANGE) return;

            double signalQuality = (dist - CRITICAL_RANGE) / (MAX_JAMMER_RANGE - CRITICAL_RANGE);
            if (drone.getRandom().nextFloat() > signalQuality) {
                return;
            }
        }

        List<PlayerEntity> players = drone.level.getEntitiesOfClass(
                PlayerEntity.class,
                drone.getBoundingBox().inflate(RADAR_RANGE),
                this::isValidTarget
        );
        detectedTargets.addAll(players);

        List<Entity> projectiles = drone.level.getEntitiesOfClass(
                Entity.class,
                drone.getBoundingBox().inflate(PROJECTILE_DETECTION_RANGE),
                this::isProjectile
        );
        detectedProjectiles.addAll(projectiles);
    }

    public boolean hasLineOfSight(LivingEntity target) {
        if (drone.isJammed()) return false;

        RayTraceResult result = drone.level.clip(new RayTraceContext(
                drone.getEyePosition(1.0F),
                target.getEyePosition(1.0F),
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                drone
        ));
        return result.getType() == RayTraceResult.Type.MISS;
    }

    public LivingEntity detectBySound() {
        if (drone.isJammed()) return null;

        List<PlayerEntity> nearbyPlayers = drone.level.getEntitiesOfClass(
                PlayerEntity.class,
                drone.getBoundingBox().inflate(HEARING_RANGE),
                this::isValidTarget
        );

        for (PlayerEntity player : nearbyPlayers) {
            double distance = drone.distanceTo(player);

            boolean isSprinting = player.isSprinting();
            double noiseRange = isSprinting ? HEARING_RANGE : HEARING_RANGE * 0.5;

            if (player.isCrouching()) {
                noiseRange *= 0.3;
            }

            if (distance <= noiseRange) {
                return player;
            }
        }
        return null;
    }

    private boolean isProjectile(Entity entity) {
        return entity instanceof ProjectileEntity ||
                entity instanceof ThrowableEntity;
    }

    public List<ThreatAssessment> assessThreats() {
        List<ThreatAssessment> threats = new ArrayList<>();
        if (drone.isJammed()) return threats;

        Vector3d dronePos = drone.position();
        Vector3d droneCenter = dronePos.add(0, drone.getBbHeight() / 2, 0);

        for (Entity projectile : detectedProjectiles) {
            ThreatAssessment threat = analyzeProjectileThreat(projectile, droneCenter);
            if (threat != null) {
                threats.add(threat);
            }
        }

        for (LivingEntity target : detectedTargets) {
            if (target.isBlocking() || hasRangedWeapon(target)) {
                double distance = drone.distanceTo(target);
                int danger = (int) ((50 - distance) * 2);
                if (danger > 0) {
                    threats.add(new ThreatAssessment(target.position(), danger));
                }
            }
        }

        return threats;
    }

    private ThreatAssessment analyzeProjectileThreat(Entity projectile, Vector3d droneCenter) {
        Vector3d projectilePos = projectile.position();
        Vector3d velocity = projectile.getDeltaMovement();

        if (velocity.lengthSqr() < 0.01) return null;

        double distanceToDrone = projectilePos.distanceTo(droneCenter);

        if (distanceToDrone < 3.0) return new ThreatAssessment(projectilePos, 100);

        Vector3d velocityNorm = velocity.normalize();
        Vector3d towardsDrone = droneCenter.subtract(projectilePos);
        double projectionLength = towardsDrone.dot(velocityNorm);

        if (projectionLength < 0) return null;

        Vector3d closestPoint = projectilePos.add(velocityNorm.scale(projectionLength));
        double distanceToTrajectory = closestPoint.distanceTo(droneCenter);
        double hitRadius = 2.5;

        if (distanceToTrajectory < hitRadius) {
            double dangerFromDistance = (hitRadius - distanceToTrajectory) / hitRadius * 50;
            double dangerFromProximity = Math.max(0, (15 - distanceToDrone) * 5);
            double speedMultiplier = Math.min(velocity.length() * 2, 2.0);
            int totalDanger = (int) ((dangerFromDistance + dangerFromProximity) * speedMultiplier);

            if (totalDanger > 10) return new ThreatAssessment(projectilePos, Math.min(totalDanger, 100));
        }

        double dotProduct = velocityNorm.dot(towardsDrone.normalize());
        if (dotProduct > 0.7 && distanceToDrone < 20) {
            int danger = (int) ((dotProduct - 0.7) * 100 + (20 - distanceToDrone) * 3);
            if (danger > 15) return new ThreatAssessment(projectilePos, Math.min(danger, 80));
        }
        return null;
    }

    public LivingEntity selectBestTarget() {
        if (drone.isJammed()) return null;
        if (detectedTargets.isEmpty()) return null;

        LivingEntity bestTarget = null;
        double bestScore = Double.MIN_VALUE;

        for (LivingEntity target : detectedTargets) {
            double score = calculateTargetScore(target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    private double calculateTargetScore(LivingEntity target) {
        double score = 100;

        double distance = drone.distanceTo(target);
        score -= distance * 0.5;

        float healthPercent = target.getHealth() / target.getMaxHealth();
        score += (1 - healthPercent) * 30;

        if (canSeeTarget(target)) {
            score += 50;
        }

        if (hasRangedWeapon(target)) {
            score += 20;
        }

        DroneMemory.TargetMemory memory = drone.getMemory().getTargetMemory(target.getUUID());
        if (memory != null && memory.isHighPriority) {
            score += 40;
        }
        return score;
    }

    private boolean isValidTarget(PlayerEntity player) {
        return player != null &&
                !player.isCreative() &&
                !player.isSpectator() &&
                player.isAlive();
    }

    private boolean hasRangedWeapon(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        return mainHand.getItem() instanceof BowItem ||
                mainHand.getItem() instanceof CrossbowItem;
    }

    public List<LivingEntity> getDetectedTargets() {
        return detectedTargets;
    }

    public static class ThreatAssessment {
        public final Vector3d position;
        public final int dangerLevel;

        public ThreatAssessment(Vector3d pos, int danger) {
            this.position = pos;
            this.dangerLevel = danger;
        }
    }
}