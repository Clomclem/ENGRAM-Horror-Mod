package horror.blueice129.entity.ai;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToDoubleFunction;

public class PlayerFuzzyTargeting {
    /**
     * Paths to a random reachable position with positive path-finding favorability.
     *
     * @return chosen position or null if none could be found
     *
     * @param entity the entity doing the pathing
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     */
    @Nullable
    public static Vec3d find(Blueice129Entity entity, int horizontalRange, int verticalRange) {
        return find(entity, horizontalRange, verticalRange, entity::getPathfindingFavor);
    }

    /**
     * Paths to a random reachable position with positive path-finding favorability computed by a given function.
     *
     * @return the chosen position or null if none could be found
     *
     * @param scorer function to compute the path-finding favorability of a candidate position
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     * @param entity the entity doing the pathing
     */
    @Nullable
    public static Vec3d find(Blueice129Entity entity, int horizontalRange, int verticalRange, ToDoubleFunction<BlockPos> scorer) {
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return PlayerFuzzyPositions.guessBest(() -> {
            BlockPos blockPos = PlayerFuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
            BlockPos blockPos2 = towardTarget(entity, horizontalRange, bl, blockPos);
            return blockPos2 == null ? null : validate(entity, blockPos2);
        }, scorer);
    }

    /**
     * Paths to a random reachable position leading towards a given end-point.
     *
     * @return the chosen position or null if none could be found
     *
     * @param end the position to path towards
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     */
    @Nullable
    public static Vec3d findTo(Blueice129Entity entity, int horizontalRange, int verticalRange, Vec3d end) {
        Vec3d vec3d = end.subtract(entity.getX(), entity.getY(), entity.getZ());
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return findValid(entity, horizontalRange, verticalRange, vec3d, bl);
    }

    /**
     * Paths to a random reachable position leading away from a given starting point.
     *
     * @return the chosen position or null if none could be found
     *
     * @param entity the entity doing the pathing
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     * @param start the position to path away from
     */
    @Nullable
    public static Vec3d findFrom(Blueice129Entity entity, int horizontalRange, int verticalRange, Vec3d start) {
        Vec3d vec3d = entity.getPos().subtract(start);
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return findValid(entity, horizontalRange, verticalRange, vec3d, bl);
    }

    @Nullable
    private static Vec3d findValid(Blueice129Entity entity, int horizontalRange, int verticalRange, Vec3d direction, boolean posTargetInRange) {
        return PlayerFuzzyPositions.guessBestPathTarget(entity, () -> {
            BlockPos blockPos = PlayerFuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange, 0, direction.x, direction.z, (float) (Math.PI / 2));
            if (blockPos == null) {
                return null;
            } else {
                BlockPos blockPos2 = towardTarget(entity, horizontalRange, posTargetInRange, blockPos);
                return blockPos2 == null ? null : validate(entity, blockPos2);
            }
        });
    }

    /**
     * Checks whether a given position is a valid pathable target.
     *
     * @return the input position, or null if validation failed
     *
     * @param pos the candidate position
     * @param entity the entity doing the pathing
     */
    @Nullable
    public static BlockPos validate(Blueice129Entity entity, BlockPos pos) {
        pos = PlayerFuzzyPositions.upWhile(pos, entity.getWorld().getTopY(), currentPos -> PlayerNavigationConditions.isSolidAt(entity, currentPos));
        return !PlayerNavigationConditions.isWaterAt(entity, pos) && !PlayerNavigationConditions.hasPathfindingPenalty(entity, pos) ? pos : null;
    }

    /**
     * Paths to a random reachable position approaching an entity's chosen {@link net.minecraft.entity.mob.MobEntity#getPositionTarget() position target}.
     *
     * @return the chosen position or null if none could be found
     *
     * @param entity the entity doing the pathing
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     */
    @Nullable
    public static BlockPos towardTarget(Blueice129Entity entity, int horizontalRange, boolean posTargetInRange, BlockPos relativeInRangePos) {
        BlockPos blockPos = PlayerFuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), relativeInRangePos);
        return !PlayerNavigationConditions.isHeightInvalid(blockPos, entity)
                && !PlayerNavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, blockPos)
                && !PlayerNavigationConditions.isInvalidPosition(entity.getNavigation(), blockPos)
                ? blockPos
                : null;
    }
}
