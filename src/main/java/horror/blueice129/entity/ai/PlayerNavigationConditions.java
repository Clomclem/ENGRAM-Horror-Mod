package horror.blueice129.entity.ai;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.ai.pathing.Blueice129Navigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class PlayerNavigationConditions {
     public static boolean isPositionTargetInRange(Blueice129Entity entity, int extraDistance) {
        return entity.hasPositionTarget() && entity.getPositionTarget().isWithinDistance(entity.getPos(), entity.getPositionTargetRange() + extraDistance + 1.0);
    }

    public static boolean isHeightInvalid(BlockPos pos, Blueice129Entity entity) {
        return pos.getY() < entity.getWorld().getBottomY() || pos.getY() > entity.getWorld().getTopY();
    }

    public static boolean isPositionTargetOutOfWalkRange(boolean posTargetInRange, Blueice129Entity entity, BlockPos pos) {
        return posTargetInRange && !entity.isInWalkTargetRange(pos);
    }

    public static boolean isInvalidPosition(Blueice129Navigation navigation, BlockPos pos) {
        return !navigation.isValidPosition(pos);
    }

    public static boolean isWaterAt(Blueice129Entity entity, BlockPos pos) {
        return entity.getWorld().getFluidState(pos).isIn(FluidTags.WATER);
    }

    public static boolean hasPathfindingPenalty(Blueice129Entity entity, BlockPos pos) {
        return entity.getPathfindingPenalty(LandPathNodeMaker.getLandNodeType(entity.getWorld(), pos.mutableCopy())) != 0.0F;
    }

    public static boolean isSolidAt(Blueice129Entity entity, BlockPos pos) {
        return entity.getWorld().getBlockState(pos).isSolid();
    }
}
