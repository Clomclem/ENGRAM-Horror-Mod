package horror.blueice129.entity.ai.control;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.control.Control;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

public class PlayerBodyControl implements Control {
    private final Blueice129Entity entity;
    private static final int BODY_KEEP_UP_THRESHOLD = 15;
    private static final int ROTATE_BODY_START_TICK = 10;
    private static final int ROTATION_INCREMENTS = 10;
    private int bodyAdjustTicks;
    private float lastHeadYaw;

    public PlayerBodyControl(Blueice129Entity entity) {
        this.entity = entity;
    }

    /**
     * Ticks the body control.
     *
     * @implSpec If the entity {@linkplain #isMoving() has moved}, its body yaw
     * adjusts to its head yaw. Otherwise, if the entity is {@linkplain
     * #isIndependent() not steered}, its head yaw adjusts to its body yaw.
     */
    public void tick() {
        if (this.isMoving()) {
            this.entity.bodyYaw = this.entity.getYaw();
            this.keepUpHead();
            this.lastHeadYaw = this.entity.headYaw;
            this.bodyAdjustTicks = 0;
        } else {
            if (this.isIndependent()) {
                if (Math.abs(this.entity.headYaw - this.lastHeadYaw) > 15.0F) {
                    this.bodyAdjustTicks = 0;
                    this.lastHeadYaw = this.entity.headYaw;
                    this.keepUpBody();
                }
            }
        }
    }

    /**
     * Keeps up the body yaw by ensuring it is within the {@linkplain
     * MobEntity#getMaxHeadRotation max head rotation} from the head yaw.
     */
    private void keepUpBody() {
        this.entity.bodyYaw = MathHelper.clampAngle(this.entity.bodyYaw, this.entity.headYaw, this.entity.getMaxHeadRotation());
    }

    /**
     * Keeps up the head yaw by ensuring it is within the {@linkplain
     * MobEntity#getMaxHeadRotation max head rotation} from the body yaw.
     */
    private void keepUpHead() {
        this.entity.headYaw = MathHelper.clampAngle(this.entity.headYaw, this.entity.bodyYaw, this.entity.getMaxHeadRotation());
    }

    private boolean isIndependent() {
        return !(this.entity.getFirstPassenger() instanceof MobEntity);
    }

    private boolean isMoving() {
        double d = this.entity.getX() - this.entity.prevX;
        double e = this.entity.getZ() - this.entity.prevZ;
        return d * d + e * e > 2.5000003E-7F;
    }
}