package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.ai.PlayerFuzzyTargeting;
import horror.blueice129.entity.ai.PlayerNoPenaltyTargeting;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Goal that makes the entity wander around randomly using vanilla WanderAroundFarGoal
 */
public class WanderGoal extends BaseBlueice129Goal {
    public static final int DEFAULT_CHANCE = 120;
    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected final double speed;
    protected int chance;
    protected boolean ignoringChance;
    private final boolean canDespawn;

    public static final float CHANCE = 0.001F;
    protected final float probability;

    public WanderGoal(Blueice129Entity entity, double speed) {
        super(entity);
        this.speed = speed;
        this.chance = 120;
        this.canDespawn = false;
        this.setControls(EnumSet.of(Goal.Control.MOVE));

        this.probability = CHANCE;
    }

    @Override
    public boolean shouldStart() {
        if (this.entity.hasPassengers()) {
            return false;
        } else {
            if (!this.ignoringChance) {
                if (this.canDespawn && this.entity.getDespawnCounter() >= 100) {
                    return false;
                }

                if (this.entity.getRandom().nextInt(toGoalTicks(this.chance)) != 0) {
                    return false;
                }
            }

            Vec3d vec3d = this.getWanderTarget();
            if (vec3d == null) {
                return false;
            } else {
                this.targetX = vec3d.x;
                this.targetY = vec3d.y;
                this.targetZ = vec3d.z;
                this.ignoringChance = false;
                return true;
            }
        }
    }

    @Override
    public void start() {
        this.entity.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
    }

    @Override
    public void stop() {
        this.entity.getNavigation().stop();
        super.stop();
    }

    @Override
    public void tick() {

    }

    protected Vec3d getWanderTarget() {
        Vec3d vec3d = PlayerFuzzyTargeting.find(this.entity, 15, 7);
        return vec3d == null ? superGetWanderTarget() : vec3d;
    }

    protected Vec3d superGetWanderTarget() {
        return PlayerNoPenaltyTargeting.find(this.entity, 10, 7);
    }

    public void ignoreChanceOnce() {
        this.ignoringChance = true;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }
}