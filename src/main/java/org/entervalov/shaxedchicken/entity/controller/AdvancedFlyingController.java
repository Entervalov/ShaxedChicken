package org.entervalov.shaxedchicken.entity.controller;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.ai.DroneAIState;
import org.entervalov.shaxedchicken.entity.DroneType;

public class AdvancedFlyingController extends FlyingMovementController {

    private final AdvancedDroneEntity drone;
    private int stuckTicks = 0; // Счетчик застревания

    public AdvancedFlyingController(AdvancedDroneEntity drone) {
        super(drone, 20, true);
        this.drone = drone;
    }

    @Override
    public void tick() {
        if (this.operation != Action.MOVE_TO) {
            drone.setDeltaMovement(drone.getDeltaMovement().scale(0.95));
            return;
        }

        Vector3d current = drone.position();
        Vector3d target = new Vector3d(wantedX, wantedY, wantedZ);

        // === 1. АНТИ-ЗАСТРЕВАНИЕ (UNSTUCK) ===
        // Если дрон пытается лететь, но фактическая скорость почти 0
        if (drone.getDeltaMovement().lengthSqr() < 0.0001) {
            stuckTicks++;
            if (stuckTicks > 10) { // Застрял на 0.5 сек
                // Резкий прыжок назад и вверх
                Vector3d escape = current.subtract(target).normalize().scale(0.3);
                drone.setDeltaMovement(escape.x, 0.4, escape.z);
                stuckTicks = 0;
                return; // Пропускаем остальную логику в этом тике
            }
        } else {
            stuckTicks = 0;
        }

        // === 2. ИЗБЕГАНИЕ ПРЕПЯТСТВИЙ (RAYTRACE) ===
        // Проверяем блок перед носом (1.5 блока вперед)
        Vector3d look = drone.getViewVector(1.0F).scale(1.5);
        BlockPos frontPos = new BlockPos(current.add(look));
        BlockState frontBlock = drone.level.getBlockState(frontPos);

        // Если впереди стена или густая листва
        if ((frontBlock.getMaterial().isSolid() || frontBlock.getMaterial().isReplaceable())
                && !frontBlock.isAir()) {
            // Подменяем цель: летим ВВЕРХ над препятствием
            // Это заставит дрон набирать высоту раньше, чем он врежется
            wantedY = current.y + 3.0;
            target = new Vector3d(wantedX, wantedY, wantedZ); // Обновляем цель для расчетов ниже
        }

        // === ТВОЙ ОРИГИНАЛЬНЫЙ КОД НИЖЕ ===
        Vector3d direction = target.subtract(current);
        double distanceSq = direction.lengthSqr();

        if (distanceSq < 1.0) {
            this.operation = Action.WAIT;
            drone.setDeltaMovement(drone.getDeltaMovement().scale(0.85));
            return;
        }

        double distance = Math.sqrt(distanceSq);
        direction = direction.scale(1.0 / distance);

        DroneAIState state = drone.getAIController().getCurrentState();

        double baseSpeed;
        double verticalMult;

        if (state == DroneAIState.ATTACK || state == DroneAIState.DIVE_BOMB) {
            baseSpeed = 0.35;
            verticalMult = 1.8;
        } else if (state == DroneAIState.CHASE) {
            baseSpeed = 0.28;
            verticalMult = 1.4;
        } else if (state == DroneAIState.EVADE || state == DroneAIState.RETREAT) {
            baseSpeed = 0.32;
            verticalMult = 1.5;
        } else {
            baseSpeed = 0.18;
            verticalMult = 1.0;
        }

        DroneType type = drone.getDroneType();
        double finalSpeed = speedModifier * baseSpeed * type.getSpeedMultiplier();

        double moveX = direction.x * finalSpeed;
        double moveY = direction.y * finalSpeed * verticalMult;
        double moveZ = direction.z * finalSpeed;

        if (direction.y < 0 && (state == DroneAIState.ATTACK || state == DroneAIState.DIVE_BOMB)) {
            moveY = direction.y * finalSpeed * 2.5;
        }

        Vector3d newVelocity = new Vector3d(moveX, moveY, moveZ);
        Vector3d currentVelocity = drone.getDeltaMovement();

        Vector3d smoothedVelocity = currentVelocity.scale(0.65)
                .add(newVelocity.scale(0.35));

        Vector3d velocityDelta = smoothedVelocity.subtract(currentVelocity);
        double deltaLengthSq = velocityDelta.lengthSqr();

        if (deltaLengthSq > 0.0025) {
            double deltaLength = Math.sqrt(deltaLengthSq);
            velocityDelta = velocityDelta.scale(0.05 / deltaLength);
            smoothedVelocity = currentVelocity.add(velocityDelta);
        }

        double maxSpeed = 0.6 * type.getSpeedMultiplier();
        double speedSq = smoothedVelocity.lengthSqr();
        double maxSpeedSq = maxSpeed * maxSpeed;

        if (speedSq > maxSpeedSq) {
            double currentSpeed = Math.sqrt(speedSq);
            smoothedVelocity = smoothedVelocity.scale(maxSpeed / currentSpeed);
        }

        drone.setDeltaMovement(smoothedVelocity);

        if (drone.tickCount % 2 == 0) {
            float targetYaw = (float) (MathHelper.atan2(direction.z, direction.x) * 57.2957795) - 90F;
            drone.yRot = rotlerp(drone.yRot, targetYaw, 10);
            drone.yBodyRot = drone.yRot;

            double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            float targetPitch = (float) (-Math.atan2(direction.y, horizontalDist) * 57.2957795);
            drone.xRot = rotlerp(drone.xRot, targetPitch, 6);
        }
    }

    protected float rotlerp(float current, float target, float maxTurn) {
        float delta = MathHelper.wrapDegrees(target - current);
        if (delta > maxTurn) delta = maxTurn;
        else if (delta < -maxTurn) delta = -maxTurn;
        return current + delta;
    }
}