package org.entervalov.shaxedchicken.entity.tactics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.sensors.DroneSensorSystem;

import java.util.List;
import java.util.Random;

public class DroneTacticsModule {

    private final AdvancedDroneEntity drone;
    private final Random random = new Random();

    private FlankDirection currentFlankDirection;

    public enum FlankDirection { LEFT, RIGHT, ABOVE, BEHIND }

    public DroneTacticsModule(AdvancedDroneEntity drone) {
        this.drone = drone;
    }

    public Vector3d calculateFlankPosition(LivingEntity target) {
        Vector3d targetPos = target.position();
        Vector3d targetLook = target.getViewVector(1.0F);

        if (currentFlankDirection == null || random.nextInt(100) < 20) {
            currentFlankDirection = FlankDirection.values()[random.nextInt(4)];
        }

        Vector3d flankOffset;
        switch (currentFlankDirection) {
            case LEFT:
                flankOffset = new Vector3d(-targetLook.z, 0, targetLook.x).normalize().scale(15);
                break;
            case RIGHT:
                flankOffset = new Vector3d(targetLook.z, 0, -targetLook.x).normalize().scale(15);
                break;
            case ABOVE:
                flankOffset = new Vector3d(0, 20, 0);
                break;
            case BEHIND:
            default:
                flankOffset = targetLook.scale(-15);
                break;
        }

        Vector3d flankPos = targetPos.add(flankOffset);

        BlockPos flankBlock = new BlockPos(flankPos);
        if (drone.getMemory().isDangerZone(flankBlock)) {
            currentFlankDirection = FlankDirection.values()[(currentFlankDirection.ordinal() + 1) % 4];
            flankPos = targetPos.add(0, 15, 0);
        }

        return flankPos;
    }

    public Vector3d calculateOrbitPosition(LivingEntity target, double radius, double speed) {
        Vector3d targetPos = target.position();
        Vector3d dronePos = drone.position();

        double dx = dronePos.x - targetPos.x;
        double dz = dronePos.z - targetPos.z;
        double currentAngle = Math.atan2(dz, dx);

        double nextAngle = currentAngle + speed * 0.05;

        double newX = targetPos.x + Math.cos(nextAngle) * radius;
        double newZ = targetPos.z + Math.sin(nextAngle) * radius;
        double newY = targetPos.y + 10 + Math.sin(nextAngle * 2) * 3;

        Vector3d orbitPos = new Vector3d(newX, newY, newZ);

        BlockPos orbitBlock = new BlockPos(orbitPos);
        if (drone.getMemory().isDangerZone(orbitBlock)) {
            newX = targetPos.x + Math.cos(nextAngle) * (radius + 10);
            newZ = targetPos.z + Math.sin(nextAngle) * (radius + 10);
            newY = newY + 10;
            orbitPos = new Vector3d(newX, newY, newZ);
        }

        return orbitPos;
    }

    public Vector3d calculateEvasionVector(List<DroneSensorSystem.ThreatAssessment> threats) {
        if (threats.isEmpty()) return null;

        Vector3d evasionVector = Vector3d.ZERO;

        for (DroneSensorSystem.ThreatAssessment threat : threats) {
            Vector3d awayFromThreat = drone.position().subtract(threat.position).normalize();
            double weight = threat.dangerLevel / 100.0;
            evasionVector = evasionVector.add(awayFromThreat.scale(weight));
        }

        if (evasionVector.lengthSqr() > 0.01) {
            evasionVector = evasionVector.normalize().scale(10);
        }

        evasionVector = evasionVector.add(
                (random.nextDouble() - 0.5) * 5,
                random.nextDouble() * 3,
                (random.nextDouble() - 0.5) * 5
        );

        Vector3d evasionPos = drone.position().add(evasionVector);

        BlockPos evasionBlock = new BlockPos(evasionPos);
        if (drone.getMemory().isDangerZone(evasionBlock)) {
            evasionPos = evasionPos.add(0, 15, 0);
        }

        return evasionPos;
    }

    public Vector3d calculateRetreatPosition() {
        Vector3d dronePos = drone.position();
        Vector3d retreatDir = Vector3d.ZERO;

        for (LivingEntity target : drone.getSensors().getDetectedTargets()) {
            Vector3d away = dronePos.subtract(target.position()).normalize();
            retreatDir = retreatDir.add(away);
        }

        if (retreatDir.lengthSqr() < 0.01) {
            retreatDir = drone.getViewVector(1.0F).scale(-1).add(0, 1, 0);
        }

        retreatDir = retreatDir.normalize().scale(40);

        Vector3d retreatPos = dronePos.add(retreatDir);

        if (retreatPos.y < dronePos.y + 10) {
            retreatPos = new Vector3d(retreatPos.x, dronePos.y + 15, retreatPos.z);
        }

        BlockPos retreatBlock = new BlockPos(retreatPos);
        if (drone.getMemory().isDangerZone(retreatBlock)) {
            retreatPos = dronePos.add(-retreatDir.x, retreatDir.y + 10, -retreatDir.z);
        }

        return retreatPos;
    }
}