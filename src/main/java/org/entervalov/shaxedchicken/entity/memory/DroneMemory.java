package org.entervalov.shaxedchicken.entity.memory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;

public class DroneMemory {

    // Храним цели
    private final Map<UUID, TargetMemory> targetMemories = new HashMap<>();

    // Храним опасные зоны (Центр -> Радиус + Время исчезновения)
    private final List<DangerZone> dangerZones = new ArrayList<>();

    private Vector3d lastKnownTargetPosition;
    private int ticksSinceTargetSeen;

    // === КЛАСС ПАМЯТИ О ЦЕЛИ ===
    public static class TargetMemory {
        public UUID targetId;
        public Vector3d lastPosition;
        public Vector3d velocity; // Вектор движения за 1 тик
        public long lastSeenTime;
        public int threatLevel; // 0 - 100+
        public boolean isHighPriority;

        // Анализ движения
        public MovementPattern movementPattern = MovementPattern.UNKNOWN;
        public double averageSpeed;
        public LinkedList<Vector3d> positionHistory = new LinkedList<>(); // Хвост движения (последние 5 тиков)

        // Экипировка
        public boolean usesShield;
        public boolean hasRangedWeapon;
        public boolean hasHeavyArmor; // Алмаз/Незерит

        public TargetMemory(UUID id) {
            this.targetId = id;
            this.threatLevel = 0;
            this.lastSeenTime = System.currentTimeMillis();
            this.velocity = Vector3d.ZERO;
        }

        public void updatePosition(Vector3d newPos) {
            if (lastPosition != null) {
                // Считаем мгновенную скорость
                this.velocity = newPos.subtract(lastPosition);

                // Обновляем историю (храним последние 5 точек)
                positionHistory.addFirst(this.velocity);
                if (positionHistory.size() > 5) positionHistory.removeLast();

                // Считаем среднюю скорость (сглаживание рывков)
                double totalSpeed = 0;
                for (Vector3d v : positionHistory) totalSpeed += v.length();
                this.averageSpeed = totalSpeed / positionHistory.size();
            }
            this.lastPosition = newPos;
            this.lastSeenTime = System.currentTimeMillis();
        }
    }

    // === КЛАСС ОПАСНОЙ ЗОНЫ ===
    public static class DangerZone {
        public BlockPos center;
        public double radius;
        public long expirationTime;

        public DangerZone(BlockPos center, double radius, long durationMs) {
            this.center = center;
            this.radius = radius;
            // ОШИБКА: Ты не прибавил durationMs!
            this.expirationTime = System.currentTimeMillis() + durationMs;
        }
    }

    public enum MovementPattern {
        UNKNOWN,
        STATIONARY, // Стоит
        LINEAR,     // Бежит прямо
        EVASIVE,    // Зигзаги/Стрейф
        JUMPING,    // Прыгает (сложнее попасть в ноги)
        FLEEING     // Убегает от дрона
    }

    // === МЕТОДЫ ПАМЯТИ ===

    public void rememberTarget(LivingEntity target) {
        UUID id = target.getUUID();
        TargetMemory memory = targetMemories.computeIfAbsent(id, TargetMemory::new);

        memory.updatePosition(target.position());
        memory.usesShield = target.isBlocking();
        memory.hasRangedWeapon = hasRangedWeapon(target);
        memory.hasHeavyArmor = checkHeavyArmor(target);

        // Пересчет уровня угрозы
        calculateThreat(memory);
        // Анализ движения
        analyzeMovementPattern(memory);

        this.lastKnownTargetPosition = target.position();
        this.ticksSinceTargetSeen = 0;
    }

    private void calculateThreat(TargetMemory memory) {
        int score = 10; // База

        // Если у цели мощное оружие
        if (memory.hasRangedWeapon) score += 30;

        // Если цель хорошо защищена (приоритет для бронебойных атак)
        if (memory.hasHeavyArmor) score += 20;

        // Если цель смотрит прямо на дрона (готовится атаковать)
        // (Упрощенная проверка, можно добавить Vector Math)

        memory.threatLevel = score;
    }

    public void forgetTarget(UUID id) {
        targetMemories.remove(id);
    }

    // Добавляем зону (например, где только что взорвалась ракета)
    public void addDangerZone(BlockPos pos, double radius, long durationMs) {
        dangerZones.add(new DangerZone(pos, radius, durationMs));
    }

    // Проверка, опасно ли в этой точке (для Pathfinding)
    public boolean isPositionDangerous(BlockPos pos) {
        long now = System.currentTimeMillis();
        dangerZones.removeIf(z -> now > z.expirationTime);

        for (DangerZone zone : dangerZones) {
            if (pos.distSqr(zone.center) < zone.radius * zone.radius) {
                return true;
            }
        }
        return false;
    }

    // === АНАЛИТИКА ===

    public void analyzeMovementPattern(TargetMemory memory) {
        if (memory.averageSpeed < 0.05) {
            memory.movementPattern = MovementPattern.STATIONARY;
            return;
        }

        // Проверка на зигзаги (EVASIVE)
        // Если векторы движения сильно меняют направление
        if (memory.positionHistory.size() >= 3) {
            Vector3d v1 = memory.positionHistory.get(0).normalize();
            Vector3d v2 = memory.positionHistory.get(1).normalize();

            // Скалярное произведение: если близко к 1, то движение прямое. Если < 0.5, резкий поворот.
            double dot = v1.dot(v2);

            if (dot < 0.7) {
                memory.movementPattern = MovementPattern.EVASIVE;
                return;
            }
        }

        // Если движется быстро и прямо
        if (memory.averageSpeed > 0.25) {
            memory.movementPattern = MovementPattern.LINEAR;
        } else {
            memory.movementPattern = MovementPattern.UNKNOWN;
        }
    }

    public void tick() {
        ticksSinceTargetSeen++;

        // Очистка старых воспоминаний (если не видели больше 30 секунд)
        long now = System.currentTimeMillis();
        targetMemories.entrySet().removeIf(entry -> (now - entry.getValue().lastSeenTime) > 30000);
    }

    // === УТИЛИТЫ ===

    private boolean hasRangedWeapon(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        return isRanged(main) || isRanged(off);
    }

    private boolean isRanged(ItemStack stack) {
        return stack.getItem() instanceof BowItem ||
                stack.getItem() instanceof CrossbowItem ||
                stack.getItem() instanceof TridentItem;
    }

    private boolean checkHeavyArmor(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlotType.CHEST);
        Item item = chest.getItem();
        return item instanceof ArmorItem &&
                (((ArmorItem) item).getMaterial() == ArmorMaterial.NETHERITE ||
                        ((ArmorItem) item).getMaterial() == ArmorMaterial.DIAMOND);
    }

    // === ГЕТТЕРЫ ===

    public Vector3d getLastKnownTargetPosition() { return lastKnownTargetPosition; }
    public int getTicksSinceTargetSeen() { return ticksSinceTargetSeen; }
    public TargetMemory getTargetMemory(UUID id) { return targetMemories.get(id); }
    public boolean isDangerZone(BlockPos pos) {
        return isPositionDangerous(pos);
    }
}
