package org.entervalov.shaxedchicken.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import org.entervalov.shaxedchicken.ModDamageSource;
import org.entervalov.shaxedchicken.ModEntityTypes;
import org.entervalov.shaxedchicken.ModSounds;
import org.entervalov.shaxedchicken.block.JammerTileEntity;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;
import org.entervalov.shaxedchicken.entity.sensors.DroneSensorSystem;

import java.util.List;
import java.util.Random;

public class DroneAIController {

    private final AdvancedDroneEntity drone;
    private DroneAIState currentState = DroneAIState.PATROL;

    private int stateTickCounter = 0;
    private int globalCooldown = 0;

    private LivingEntity currentTarget = null;
    private Vector3d currentDestination = null;

    private boolean detectionSoundPlayed = false;
    private int evasionCooldown = 0;

    private static final int JAMMER_SCAN_INTERVAL_TICKS = 20;
    private static final int JAMMER_SEARCH_RADIUS = 48;
    private static final int JAMMER_SCAN_STEP = 12;
    private static final double JAMMER_EFFECT_RADIUS_SQ = 40.0 * 40.0;

    // === РџР•Р Р•РњР•РќРќР«Р• Р“Р›РЈРЁРР›РљР ===
    private BlockPos detectedJammerPos = null; // РџРѕР·РёС†РёСЏ РѕР±РЅР°СЂСѓР¶РµРЅРЅРѕР№ РіР»СѓС€РёР»РєРё (РґР»СЏ СѓРјРЅРѕРіРѕ РѕР±С…РѕРґР°)
    private BlockPos cachedJammerPos = null;
    private long nextJammerScanTick = 0;

    // Р”Р»СЏ СЂР°Р·РІРµРґС‡РёРєР°
    private int scoutSummonCooldown = 0;
    private boolean hasSummonedReinforcements = false;

    public DroneAIController(AdvancedDroneEntity drone) {
        this.drone = drone;
    }

    public void tick() {
        if (globalCooldown > 0) {
            globalCooldown--;
            return;
        }

        // 0. РћР‘РќРћР’Р›Р•РќРР• Р”РђРќРќР«РҐ (РЎРЅР°С‡Р°Р»Р° СЃРјРѕС‚СЂРёРј, РїРѕС‚РѕРј РґСѓРјР°РµРј)
        // РћР±РЅРѕРІР»СЏРµРј СЃРµРЅСЃРѕСЂС‹ РІ РїРµСЂРІСѓСЋ РѕС‡РµСЂРµРґСЊ, С‡С‚РѕР±С‹ РІСЃРµ РїСЂРѕРІРµСЂРєРё РЅРёР¶Рµ СЂР°Р±РѕС‚Р°Р»Рё СЃ Р°РєС‚СѓР°Р»СЊРЅРѕР№ РёРЅС„РѕР№
        drone.getSensors().updateRadar();
        drone.getMemory().tick();

        // 1. РџР РРћР РРўР•Рў: Р“Р›РЈРЁР•РќРР• (Р”СЂРѕРЅ СЃР»РѕРјР°РЅ)
        if (isJammed()) {
            // РЎР±СЂР°СЃС‹РІР°РµРј С†РµР»СЊ Р°С‚Р°РєРё РІ СЃР°РјРѕР№ СЃСѓС‰РЅРѕСЃС‚Рё
            drone.setTarget(null);
            // Р•СЃР»Рё Сѓ С‚РµР±СЏ РІ AIController РµСЃС‚СЊ РєР°РєРёРµ-С‚Рѕ СЃРІРѕРё РїРµСЂРµРјРµРЅРЅС‹Рµ СЃРѕСЃС‚РѕСЏРЅРёСЏ (РЅР°РїСЂРёРјРµСЂ, С‚РѕС‡РєР° РїР°С‚СЂСѓР»РёСЂРѕРІР°РЅРёСЏ), СЃР±СЂРѕСЃСЊ РёС… С‚СѓС‚
            // this.patrolTarget = null;
            return;
        }

        stateTickCounter++;

        // РљСѓР»РґР°СѓРЅС‹
        if (evasionCooldown > 0) evasionCooldown--;
        if (scoutSummonCooldown > 0) scoutSummonCooldown--;

        // 2. РЈРњРќР«Р™ РћР‘РҐРћР” Р“Р›РЈРЁРР›РћРљ (РџСЂРµРІРµРЅС‚РёРІРЅР°СЏ РјРµСЂР°)
        // РџСЂРѕРІРµСЂСЏРµРј СЂР°Р· РІ 10 С‚РёРєРѕРІ (РѕРїС‚РёРјРёР·Р°С†РёСЏ), РµСЃР»Рё РјС‹ РЅРµ СѓРєР»РѕРЅСЏРµРјСЃСЏ РїСЂСЏРјРѕ СЃРµР№С‡Р°СЃ
        if (stateTickCounter % 20 == 0 && evasionCooldown <= 0) {
            // РўРѕР»СЊРєРѕ СѓРјРЅС‹Рµ РґСЂРѕРЅС‹ СѓРјРµСЋС‚ СЌС‚Рѕ РґРµР»Р°С‚СЊ
            DroneType type = drone.getDroneType();
            if ((type == DroneType.NIGHT || type == DroneType.NUCLEAR) && detectJammerThreat()) {
                transitionTo(DroneAIState.EVADE);
                evasionCooldown = 100; // 5 СЃРµРєСѓРЅРґ РЅР° РјР°РЅРµРІСЂ
                return; // РЎСЂР°Р·Сѓ РІС‹С…РѕРґРёРј, С‡С‚РѕР±С‹ РЅРµ СЃР±РёС‚СЊ СЃРѕСЃС‚РѕСЏРЅРёРµ РЅРёР¶Рµ
            }
        }

        // 3. РћР¦Р•РќРљРђ РЈР“Р РћР— (Р Р°РєРµС‚С‹)
        List<DroneSensorSystem.ThreatAssessment> threats = drone.getSensors().assessThreats();
        // Р•СЃР»Рё РІ РЅР°СЃ Р»РµС‚РёС‚ СЂР°РєРµС‚Р° - СѓРєР»РѕРЅСЏРµРјСЃСЏ, РґР°Р¶Рµ РµСЃР»Рё РµСЃС‚СЊ РєСѓР»РґР°СѓРЅ (СЌРєСЃС‚СЂРµРЅРЅРѕ)
        // РќРѕ С‚РѕР»СЊРєРѕ РµСЃР»Рё РјС‹ РЅРµ РїРёРєРёСЂСѓРµРј (DIVE_BOMB - СЌС‚Рѕ РєР°РјРёРєР°РґР·Рµ СЂРµР¶РёРј)
        if (shouldEvade(threats) && currentState != DroneAIState.DIVE_BOMB) {
            transitionTo(DroneAIState.EVADE);
            evasionCooldown = 40;
            return;
        }

        // 4. Р›РћР“РРљРђ
        // РћР±РЅРѕРІР»РµРЅРёРµ С†РµР»Рё (РёРіСЂРѕРєРё/РјРѕР±С‹)
        updateTarget();

        // Р’С‹РїРѕР»РЅРµРЅРёРµ РґРµР№СЃС‚РІРёР№ С‚РµРєСѓС‰РµРіРѕ СЃРѕСЃС‚РѕСЏРЅРёСЏ
        executeCurrentState();

        // 5. РџР•Р Р•РҐРћР”Р«
        // РќРµ РјРµРЅСЏРµРј СЃРѕСЃС‚РѕСЏРЅРёРµ, РµСЃР»Рё РјС‹ Р·Р°РЅСЏС‚С‹ СѓРєР»РѕРЅРµРЅРёРµРј
        if (currentState != DroneAIState.EVADE) {
            checkStateTransitions();
        }
    }


    // =============================================================
    //               Р›РћР“РРљРђ Р“Р›РЈРЁРР›РћРљ (JAMMERS)
    // =============================================================

    /**
     * РЎРєР°РЅРёСЂСѓРµС‚ РјРµСЃС‚РЅРѕСЃС‚СЊ РІРїРµСЂРµРґРё РЅР° РЅР°Р»РёС‡РёРµ РіР»СѓС€РёР»РѕРє.
     * Р Р°Р±РѕС‚Р°РµС‚ РўРћР›Р¬РљРћ РґР»СЏ Nuclear Рё Night Hunter.
     */
    private boolean detectJammerThreat() {
        DroneType type = drone.getDroneType();

        if (!type.isNuclear() && type != DroneType.NIGHT) {
            return false;
        }

        if (stateTickCounter % 20 != 0) return false;

        BlockPos nearbyJammer = getCurrentJammerPos(false);
        if (nearbyJammer != null) {
            detectedJammerPos = nearbyJammer;
            return true;
        }

        Vector3d velocity = drone.getDeltaMovement();
        if (velocity.lengthSqr() < 0.01) velocity = drone.getLookAngle();
        velocity = velocity.normalize();

        BlockPos center = drone.blockPosition();

        for (int i = 1; i <= 2; i++) {
            double dist = i * 24.0;
            BlockPos checkPoint = new BlockPos(
                    center.getX() + velocity.x * dist,
                    center.getY(),
                    center.getZ() + velocity.z * dist
            );

            int searchBox = 12;
            for (int x = -searchBox; x <= searchBox; x += JAMMER_SCAN_STEP) {
                for (int z = -searchBox; z <= searchBox; z += JAMMER_SCAN_STEP) {
                    for (int y = -12; y <= 12; y += JAMMER_SCAN_STEP) {
                        BlockPos pos = checkPoint.offset(x, y, z);
                        if (isActiveJammerAt(pos)) {
                            detectedJammerPos = pos;
                            cachedJammerPos = pos;
                            nextJammerScanTick = drone.level.getGameTime() + JAMMER_SCAN_INTERVAL_TICKS;
                            return true;
                        }
                    }
                }
            }
        }

        detectedJammerPos = null;
        return false;
    }

    public Vector3d getCurrentDestination() {
        return this.currentDestination;
    }

    /**
     * РџСЂРѕРІРµСЂСЏРµС‚, РїРѕРїР°Р» Р»Рё РґСЂРѕРЅ РЈР–Р• РІ Р·РѕРЅСѓ РґРµР№СЃС‚РІРёСЏ (Р Р°РґРёСѓСЃ 40).
     */
    private boolean isJammed() {
        BlockPos jammerPos = getCurrentJammerPos(false);
        if (jammerPos != null) {
            detectedJammerPos = jammerPos;
            return true;
        }
        return false;
    }

    private BlockPos getCurrentJammerPos(boolean forceRescan) {
        if (drone.level.isClientSide) {
            return null;
        }

        long gameTime = drone.level.getGameTime();

        if (cachedJammerPos != null && isActiveJammerAt(cachedJammerPos) && isInJammerRange(cachedJammerPos)) {
            if (!forceRescan && gameTime < nextJammerScanTick) {
                return cachedJammerPos;
            }
        }

        if (!forceRescan && gameTime < nextJammerScanTick) {
            return null;
        }

        nextJammerScanTick = gameTime + JAMMER_SCAN_INTERVAL_TICKS;
        cachedJammerPos = scanNearestActiveJammer();
        return cachedJammerPos;
    }

    private BlockPos scanNearestActiveJammer() {
        BlockPos dronePos = drone.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -JAMMER_SEARCH_RADIUS; x <= JAMMER_SEARCH_RADIUS; x += JAMMER_SCAN_STEP) {
            for (int y = -JAMMER_SEARCH_RADIUS; y <= JAMMER_SEARCH_RADIUS; y += JAMMER_SCAN_STEP) {
                for (int z = -JAMMER_SEARCH_RADIUS; z <= JAMMER_SEARCH_RADIUS; z += JAMMER_SCAN_STEP) {
                    BlockPos checkPos = dronePos.offset(x, y, z);
                    if (!isActiveJammerAt(checkPos)) {
                        continue;
                    }

                    if (!isInJammerRange(checkPos)) {
                        continue;
                    }

                    double distanceSq = drone.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);
                    if (distanceSq < closestDistance) {
                        closestDistance = distanceSq;
                        closest = checkPos;
                    }
                }
            }
        }
        return closest;
    }

    private boolean isActiveJammerAt(BlockPos pos) {
        if (!drone.level.hasChunkAt(pos)) {
            return false;
        }

        TileEntity te = drone.level.getBlockEntity(pos);
        if (!(te instanceof JammerTileEntity)) {
            return false;
        }

        JammerTileEntity jammer = (JammerTileEntity) te;
        return jammer.isActive() && jammer.isJammerEnabled();
    }

    private boolean isInJammerRange(BlockPos jammerPos) {
        return drone.distanceToSqr(jammerPos.getX() + 0.5, jammerPos.getY() + 0.5, jammerPos.getZ() + 0.5) <= JAMMER_EFFECT_RADIUS_SQ;
    }

    // =============================================================
    //                  РРЎРџРћР›РќР•РќРР• РЎРћРЎРўРћРЇРќРР™
    // =============================================================

    private void executeCurrentState() {
        switch (currentState) {
            case PATROL: executePatrol(); break;
            case SEARCH: executeSearch(); break;
            case CHASE: executeChase(); break;
            case ATTACK: executeAttack(); break;
            case FLANK: executeFlank(); break;
            case RETREAT: executeRetreat(); break;
            case EVADE: executeEvade(); break;
            case ORBIT: executeOrbit(); break;
            case DIVE_BOMB: executeDiveBomb(); break;
            case COORDINATE: executeCoordinate(); break;
        }
    }

    // --- PATROL ---
    private void executePatrol() {
        if (currentDestination == null || reachedDestination()) {
            currentDestination = generatePatrolPoint();
        }
        moveToDestination(1.0);
        if (stateTickCounter % 60 == 0) drone.yRot = drone.yRot + 45;
        detectionSoundPlayed = false;
        hasSummonedReinforcements = false;
    }

    // --- SEARCH ---
    private void executeSearch() {
        Vector3d lastKnown = drone.getMemory().getLastKnownTargetPosition();
        if (lastKnown == null) {
            if (currentTarget != null) drone.getMemory().forgetTarget(currentTarget.getUUID());
            transitionTo(DroneAIState.PATROL);
            return;
        }
        if (currentDestination == null) currentDestination = lastKnown;
        moveToDestination(1.5);

        if (reachedDestination()) {
            double angle = stateTickCounter * 0.1;
            double radius = 5 + (stateTickCounter % 100) * 0.3;
            currentDestination = lastKnown.add(Math.cos(angle) * radius, 5 + Math.sin(angle * 0.5) * 3, Math.sin(angle) * radius);
        }
        if (stateTickCounter > 400) {
            if (currentTarget != null) drone.getMemory().forgetTarget(currentTarget.getUUID());
            transitionTo(DroneAIState.PATROL);
        }
    }

    // --- CHASE ---
    private void executeChase() {
        if (currentTarget == null) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        DroneType type = drone.getDroneType();
        if (type.isScout() && !hasSummonedReinforcements && scoutSummonCooldown <= 0) {
            summonReinforcements();
            hasSummonedReinforcements = true;
            scoutSummonCooldown = 600;
        }
        currentDestination = currentTarget.position().add(0, 1.5, 0);
        double speedMod = type.getSpeedMultiplier();
        moveToDestination(3.0 * speedMod);
        if (drone.getSensors().canSeeTarget(currentTarget)) drone.getMemory().rememberTarget(currentTarget);
        drone.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);

        double distance = drone.distanceTo(currentTarget);
        if (type.isScout()) {
            if (distance < 15) transitionTo(DroneAIState.RETREAT);
        } else if (distance < 20) {
            transitionTo(DroneAIState.ATTACK);
        }
    }

    // --- ATTACK ---
    private void executeAttack() {
        if (currentTarget == null) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        DroneType type = drone.getDroneType();
        if (type.isScout()) {
            transitionTo(DroneAIState.RETREAT);
            return;
        }
        Vector3d targetPos = currentTarget.position().add(0, 1.0, 0);
        currentDestination = targetPos;
        double distance = drone.distanceTo(currentTarget);
        double speedMod = type.getSpeedMultiplier();

        moveToDestination(4.0 * speedMod);

        Vector3d direction = targetPos.subtract(drone.position()).normalize();
        double boost = Math.min(0.3 * speedMod, distance * 0.02);
        drone.setDeltaMovement(drone.getDeltaMovement().add(direction.scale(boost)));

        double explosionDistance = 2.5 * type.getSizeMultiplier();
        if (distance < explosionDistance) {
            performExplosion();
            return;
        }
        if (distance > 35) transitionTo(DroneAIState.CHASE);
        drone.getLookControl().setLookAt(currentTarget, 60.0F, 60.0F);
    }

    // --- FLANK ---
    private void executeFlank() {
        if (currentTarget == null) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        currentDestination = drone.getTactics().calculateFlankPosition(currentTarget);
        moveToDestination(2.5 * drone.getDroneType().getSpeedMultiplier());
        if (reachedDestination() || stateTickCounter > 60) transitionTo(DroneAIState.ATTACK);
        drone.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);
    }

    // --- RETREAT ---
    private void executeRetreat() {
        currentDestination = drone.getTactics().calculateRetreatPosition();
        moveToDestination(2.5);
        if (drone.getHealth() > drone.getMaxHealth() * 0.7f) transitionTo(DroneAIState.PATROL);
        if (stateTickCounter > 200) transitionTo(DroneAIState.PATROL);
    }

    // --- EVADE (РћР‘РҐРћР” Р“Р›РЈРЁРР›РћРљ) ---
    private void executeEvade() {
        // 1. РЈРєР»РѕРЅРµРЅРёРµ РѕС‚ РіР»СѓС€РёР»РєРё (РџСЂРёРѕСЂРёС‚РµС‚ в„–1)
        if (detectedJammerPos != null) {
            Vector3d jammerVec = Vector3d.atCenterOf(detectedJammerPos);
            Vector3d toJammer = jammerVec.subtract(drone.position());
            Vector3d away = toJammer.scale(-1).normalize();
            Vector3d sideStep = new Vector3d(-away.z, 0, away.x);
            if (toJammer.lengthSqr() < 45 * 45) sideStep = sideStep.add(0, 0.8, 0);

            Vector3d evadePath = away.scale(0.6).add(sideStep.scale(0.4)).normalize().scale(15.0);
            currentDestination = drone.position().add(evadePath);
            moveToDestination(3.0 * drone.getDroneType().getSpeedMultiplier());

            if (drone.distanceToSqr(jammerVec) > 70 * 70) {
                detectedJammerPos = null;
                transitionTo(DroneAIState.PATROL);
            }
            return;
        }

        // 2. РЎС‚Р°РЅРґР°СЂС‚РЅРѕРµ СѓРєР»РѕРЅРµРЅРёРµ (СѓРіСЂРѕР·С‹)
        List<DroneSensorSystem.ThreatAssessment> threats = drone.getSensors().assessThreats();
        if (threats.isEmpty()) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        if (currentDestination == null || reachedDestination() || stateTickCounter % 40 == 0) {
            currentDestination = drone.getTactics().calculateEvasionVector(threats);
        }
        if (currentDestination != null) moveToDestination(2.0 * drone.getDroneType().getSpeedMultiplier());
        if (stateTickCounter > 100) transitionTo(DroneAIState.PATROL);
    }

    // --- ORBIT ---
    private void executeOrbit() {
        if (currentTarget == null) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        Vector3d orbitPos = drone.getTactics().calculateOrbitPosition(currentTarget, 12.0, 1.5);
        orbitPos = new Vector3d(orbitPos.x, currentTarget.getY() + 5, orbitPos.z);
        currentDestination = orbitPos;
        moveToDestination(2.0);
        drone.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);
        if (stateTickCounter % 40 == 0) transitionTo(DroneAIState.ATTACK);
    }

    // --- DIVE BOMB ---
    private void executeDiveBomb() {
        if (currentTarget == null) {
            transitionTo(DroneAIState.PATROL);
            return;
        }
        Vector3d targetPos = currentTarget.position().add(0, 1, 0);
        double altitude = drone.getY() - currentTarget.getY();
        double horizontalDist = horizontalDistanceTo(currentTarget);

        if (altitude < 10 && horizontalDist > 15) {
            currentDestination = new Vector3d(drone.getX(), currentTarget.getY() + 20, drone.getZ());
            moveToDestination(2.0);
        } else {
            currentDestination = targetPos;
            moveToDestination(4.0);
            drone.setDeltaMovement(drone.getDeltaMovement().add(0, -0.2, 0));
            Vector3d toTarget = targetPos.subtract(drone.position()).normalize();
            drone.setDeltaMovement(drone.getDeltaMovement().add(toTarget.scale(0.2)));
        }
        DroneType type = drone.getDroneType();
        double explosionDistance = 2.5 * type.getSizeMultiplier();
        if (drone.distanceTo(currentTarget) < explosionDistance) performExplosion();
        drone.getLookControl().setLookAt(currentTarget, 60.0F, 60.0F);
    }

    // --- COORDINATE ---
    private void executeCoordinate() {
        List<AdvancedDroneEntity> nearbyDrones = findNearbyDrones();
        if (nearbyDrones.isEmpty() || stateTickCounter > 40) transitionTo(DroneAIState.ATTACK);
    }

    // =============================================================
    //                  Р’РЎРџРћРњРћР“РђРўР•Р›Р¬РќР«Р• РњР•РўРћР”Р«
    // =============================================================

    private void summonReinforcements() {
        if (drone.level.isClientSide) return;
        int count = 3 + drone.getRandom().nextInt(3);
        for (int i = 0; i < count; i++) {
            AdvancedDroneEntity reinforcement = ModEntityTypes.DRONE_CHICKEN.get().create(drone.level);
            if (reinforcement != null) {
                double angle = (2 * Math.PI * i) / count;
                double radius = 5.0;
                reinforcement.setPos(drone.getX() + Math.cos(angle) * radius, drone.getY() + 2, drone.getZ() + Math.sin(angle) * radius);
                reinforcement.setDroneType(DroneType.BASIC);
                drone.level.addFreshEntity(reinforcement);
            }
        }
        drone.level.playSound(null, drone.blockPosition(), SoundEvents.RAID_HORN, SoundCategory.HOSTILE, 2.0F, 1.5F);
        if (currentTarget instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) currentTarget).displayClientMessage(new StringTextComponent("§c§l⚠ Разведчик вызвал подкрепление!"), true);
        }
    }

    private void checkStateTransitions() {
        switch (currentState) {
            case PATROL:
                if (currentTarget != null && drone.getSensors().canSeeTarget(currentTarget)) {
                    playDetectionSound();
                    transitionTo(DroneAIState.CHASE);
                } else {
                    LivingEntity heardTarget = drone.getSensors().detectBySound();
                    if (heardTarget != null) {
                        currentTarget = heardTarget;
                        playDetectionSound();
                        transitionTo(DroneAIState.CHASE);
                    }
                }
                break;
            case CHASE:
                if (currentTarget == null) transitionTo(DroneAIState.PATROL);
                else if (!drone.getSensors().canSeeTarget(currentTarget)) transitionTo(DroneAIState.SEARCH);
                break;
            case SEARCH:
                if (currentTarget != null && drone.getSensors().canSeeTarget(currentTarget)) {
                    playDetectionSound();
                    transitionTo(DroneAIState.CHASE);
                }
                break;
            case ATTACK:
            case FLANK:
            case DIVE_BOMB:
                if (drone.getHealth() < drone.getMaxHealth() * 0.3f) transitionTo(DroneAIState.RETREAT);
                break;
        }
    }

    private void playDetectionSound() {
        if (detectionSoundPlayed) return;
        if (drone.level.isClientSide) return;
        try {
            drone.level.playSound(null, drone.blockPosition(), ModSounds.CHICKEN_PRIME.get(), SoundCategory.HOSTILE, 5.0F, 0.8F + drone.getRandom().nextFloat() * 0.4F);
            if (currentTarget != null) {
                drone.level.playSound(null, currentTarget.blockPosition(), ModSounds.CHICKEN_PRIME.get(), SoundCategory.HOSTILE, 3.0F, 0.9F + drone.getRandom().nextFloat() * 0.2F);
            }
            detectionSoundPlayed = true;
            drone.setAttacking(true);
        } catch (Exception e) {
            System.err.println("[ShaxedChicken] Error playing detection sound: " + e.getMessage());
        }
    }

    private void transitionTo(DroneAIState newState) {
        if (newState == currentState) return;
        currentState = newState;
        stateTickCounter = 0;
        currentDestination = null;
        if (newState == DroneAIState.PATROL) {
            detectionSoundPlayed = false;
            drone.setAttacking(false);
        }
    }

    private void updateTarget() {
        LivingEntity newTarget = drone.getSensors().selectBestTarget();
        if (newTarget != null) {
            currentTarget = newTarget;
            if (drone.getSensors().canSeeTarget(currentTarget)) {
                // Р’РЅСѓС‚СЂРё rememberTarget Р°РЅР°Р»РёР· Р·Р°РїСѓСЃРєР°РµС‚СЃСЏ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё
                drone.getMemory().rememberTarget(currentTarget);
            }
        } else if (drone.getMemory().getTicksSinceTargetSeen() > 200) {
            if (currentTarget != null) drone.getMemory().forgetTarget(currentTarget.getUUID());
            currentTarget = null;
        }
    }

    private boolean shouldEvade(List<DroneSensorSystem.ThreatAssessment> threats) {
        if (threats.isEmpty()) return false;
        if (currentTarget != null && drone.distanceTo(currentTarget) < 5) return false;
        if (currentState == DroneAIState.DIVE_BOMB) return false;
        int maxDanger = threats.stream().mapToInt(t -> t.dangerLevel).max().orElse(0);
        return maxDanger > 50 && currentState != DroneAIState.EVADE;
    }

    private void moveToDestination(double speed) {
        if (currentDestination == null) return;
        drone.getMoveControl().setWantedPosition(currentDestination.x, currentDestination.y, currentDestination.z, speed);
    }

    private boolean reachedDestination() {
        if (currentDestination == null) return true;
        return drone.position().distanceToSqr(currentDestination) < 9.0;
    }

    private Vector3d generatePatrolPoint() {
        Random rand = drone.getRandom();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double dist = 20 + rand.nextDouble() * 30;
        double x = drone.getX() + Math.cos(angle) * dist;
        double z = drone.getZ() + Math.sin(angle) * dist;
        double y = drone.getY() + (rand.nextDouble() - 0.5) * 10;
        if (y < 65) y = 65 + rand.nextInt(10);
        if (y > 120) y = 120 - rand.nextInt(10);
        return new Vector3d(x, y, z);
    }

    private double horizontalDistanceTo(Entity entity) {
        double dx = drone.getX() - entity.getX();
        double dz = drone.getZ() - entity.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private List<AdvancedDroneEntity> findNearbyDrones() {
        return drone.level.getEntitiesOfClass(AdvancedDroneEntity.class, drone.getBoundingBox().inflate(50), d -> d != drone && d.isAlive());
    }

    private void performExplosion() {
        if (!drone.level.isClientSide) {
            DroneType type = drone.getDroneType();
            float power = type.getExplosionPower();
            if (type.isScout()) {
                drone.remove();
                return;
            }
            if (currentTarget != null && currentTarget.isAlive()) {
                float damage = power * 5;
                currentTarget.hurt(ModDamageSource.droneExplosion(drone), damage);
            }
            Explosion.Mode mode = type.isNuclear() ? Explosion.Mode.DESTROY : Explosion.Mode.BREAK;
            drone.level.explode(null, drone.getX(), drone.getY(), drone.getZ(), power, type.isFire(), mode);
            if (type.isFire()) igniteArea();
            if (currentTarget instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) currentTarget;
                String deathMessage = type.isNuclear() ? "вў РўР•Р РњРћРЇР”Р•Р РќР«Р™ РЈР”РђР ! вў" : "в  Р‘РџР›Рђ РЈРќРР§РўРћР–РР› Р’РђРЎ в ";
                player.connection.send(new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent(deathMessage).withStyle(type.getColor(), TextFormatting.BOLD)));
            }
            drone.remove();
        }
    }

    private void igniteArea() {
        BlockPos center = drone.blockPosition();
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (drone.level.isEmptyBlock(pos) && drone.level.getBlockState(pos.below()).isSolidRender(drone.level, pos.below())) {
                        if (drone.getRandom().nextFloat() < 0.3f) {
                            drone.level.setBlockAndUpdate(pos, net.minecraft.block.Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    public DroneAIState getCurrentState() {
        return currentState;
    }
}


