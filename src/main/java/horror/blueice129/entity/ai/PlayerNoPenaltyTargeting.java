package horror.blueice129.entity.ai;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Similar to {@link FuzzyTargeting}, but the positions this class' utility methods
 * find never have pathfinding penalties.
 */
public class PlayerNoPenaltyTargeting {
    /**
     * Paths to a random reachable position with no penalty.
     *
     * @return the chosen end position or null if no valid positions could be found
     *
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param entity the entity doing the pathing
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     */
    @Nullable
    public static Vec3d find(Blueice129Entity entity, int horizontalRange, int verticalRange) {
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return PlayerFuzzyPositions.guessBestPathTarget(entity, () -> {
            BlockPos blockPos = PlayerFuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
            return tryMake(entity, horizontalRange, bl, blockPos);
        });
    }

    /**
     * Paths to a position leading towards a given end-point.
     *
     * @return the chosen end position or null if no valid positions could be found
     *
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     * @param entity the entity doing the pathing
     * @param angleRange the minimum angle of approach
     * @param end the position to path towards
     */
    @Nullable
    public static Vec3d findTo(Blueice129Entity entity, int horizontalRange, int verticalRange, Vec3d end, double angleRange) {
        Vec3d vec3d = end.subtract(entity.getX(), entity.getY(), entity.getZ());
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return PlayerFuzzyPositions.guessBestPathTarget(entity, () -> {
            BlockPos blockPos = PlayerFuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange, 0, vec3d.x, vec3d.z, angleRange);
            return blockPos == null ? null : tryMake(entity, horizontalRange, bl, blockPos);
        });
    }

    /**
     * Paths to a position leading away from a given starting point.
     *
     * @return the chosen end position or null if no valid positions could be found
     *
     * @param start the position to path away from
     * @param verticalRange the vertical pathing range (how far the point can be from the entity's starting position on the Y range)
     * @param horizontalRange the horizontal pathing range (how far the point can be from the entity's starting position on the X or Z range)
     * @param entity the entity doing the pathing
     */
    @Nullable
    public static Vec3d findFrom(Blueice129Entity entity, int horizontalRange, int verticalRange, Vec3d start) {
        Vec3d vec3d = entity.getPos().subtract(start);
        boolean bl = PlayerNavigationConditions.isPositionTargetInRange(entity, horizontalRange);
        return PlayerFuzzyPositions.guessBestPathTarget(entity, () -> {
            BlockPos blockPos = FuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange, 0, vec3d.x, vec3d.z, (float) (Math.PI / 2));
            return blockPos == null ? null : tryMake(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    private static BlockPos tryMake(Blueice129Entity entity, int horizontalRange, boolean posTargetInRange, BlockPos fuzz) {
        BlockPos blockPos = PlayerFuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), fuzz);
        return !PlayerNavigationConditions.isHeightInvalid(blockPos, entity)
                && !PlayerNavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, blockPos)
                && !PlayerNavigationConditions.isInvalidPosition(entity.getNavigation(), blockPos)
                && !PlayerNavigationConditions.hasPathfindingPenalty(entity, blockPos)
                ? blockPos
                : null;
    }
}
