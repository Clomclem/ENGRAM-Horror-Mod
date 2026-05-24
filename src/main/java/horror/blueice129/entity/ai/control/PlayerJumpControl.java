package horror.blueice129.entity.ai.control;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.control.Control;
import net.minecraft.entity.mob.MobEntity;

public class PlayerJumpControl implements Control {
    private final Blueice129Entity entity;
    protected boolean active;

    public PlayerJumpControl(Blueice129Entity entity) {
        this.entity = entity;
    }

    public void setActive() {
        this.active = true;
    }

    public void tick() {
        this.entity.setJumping(this.active);
        this.active = false;
    }
}