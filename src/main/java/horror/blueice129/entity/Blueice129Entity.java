package horror.blueice129.entity;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import horror.blueice129.entity.ai.control.PlayerBodyControl;
import horror.blueice129.entity.ai.control.PlayerJumpControl;
import horror.blueice129.entity.ai.control.PlayerLookControl;
import horror.blueice129.entity.ai.control.PlayerMoveControl;
import horror.blueice129.entity.ai.pathing.Blueice129Navigation;
import horror.blueice129.entity.goals.GoalProfileRegistry;
import horror.blueice129.scheduler.Blueice129SpawnScheduler;
import horror.blueice129.utils.EntityLoginState;
import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.Map;

/**
 * Blueice129 Entity - A custom PathAwareEntity that takes the form of a player
 * character with Blueice129's skin.
 * 
 * PathAwareEntity extends MobEntity, which extends LivingEntity.
 * - LivingEntity has health and can deal damage.
 * - MobEntity has movement controls and AI capabilities.
 * - PathAwareEntity has pathfinding favor and slightly tweaked leash behavior.
 * 
 * This entity uses a state-based goal profile system where different behaviors
 * are activated based on the current EntityState.
 */
public class Blueice129Entity extends ServerPlayerEntity {

    public final static String name = "Blueice129";
    public final static GameProfile gameProfile = new GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()),
            name);

    private EntityState currentState;
    private int ticksInCurrentState = 0;
    private boolean should_logout_after_menu = false;
    private GoalProfileRegistry goalRegistry;
    private EntityState previousState = null;
    private int ticksInUnloadedChunk = 0;

    protected Blueice129Navigation navigation;
    private final Map<PathNodeType, Float> pathfindingPenalties = Maps.newEnumMap(PathNodeType.class);

    public final GoalSelector goalSelector;
    public final GoalSelector targetSelector;

    protected PlayerLookControl lookControl;
    protected PlayerMoveControl moveControl;
    protected PlayerJumpControl jumpControl;
    private final PlayerBodyControl bodyControl;

    private BlockPos positionTarget = BlockPos.ORIGIN;
    private float positionTargetRange = -1.0F;

    public enum EntityState {
        PASSIVE, // Default state, does nothing really
        PANICED, // jumps, cycles hotbar, head jerks around like mouse is being
                 // shaken, crouches randomly
        FLEEING, // runs away from player, attempts to break line of sight, occasionally looks
                 // back
        SURFACE_HIDING, // hides behind trees/other structures, peeks out occasionally, always crouched
        UNDERGROUND_BURROWING, // crouched in the ground near player, mines blocks when player does, attempts
                               // to follow player
        IN_MENUS, // stops moving, no actions taken.
        INVESTIGATING, // opens chests, furnaces, opens doors, presses buttons and levers, generally
                       // interacts with the environment
        UPGRADING_HOUSE // places blocks, breaks old blocks, clears out area, uses blocks and tools from
                        // inventory
    }

    /**
     * Change the entity's state and update AI goals accordingly
     */
    public void setState(EntityState newState) {
        if (this.currentState == newState)
            return;

        this.currentState = newState;
        this.ticksInCurrentState = 0; // Reset the timer when changing states
        updateGoals();
    }

    /**
     * Update AI goals based on the current state.
     * This method uses the GoalProfileRegistry to apply the appropriate goal
     * profile.
     */
    private void updateGoals() {
        if (goalRegistry != null) {
            goalRegistry.applyCurrentProfile();
        }
    }

    public void onStartPathfinding() {
    }

    public void onFinishPathfinding() {
    }

    public float getPathfindingPenalty(PathNodeType nodeType) {
        Float float_ = (Float)this.pathfindingPenalties.get(nodeType);
        return float_ == null ? nodeType.getDefaultPenalty() : float_;
    }

    public void setPathfindingPenalty(PathNodeType nodeType, float penalty) {
        this.pathfindingPenalties.put(nodeType, penalty);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        Blueice129SpawnScheduler.remove();
    }

    protected Blueice129Navigation createNavigation(World world) {
        return new Blueice129Navigation(this, world);
    }

    public Blueice129Navigation getNavigation() {
        return this.navigation;
    }

    @Override
    protected final void tickNewAi() {
        super.tickNewAi();
        this.despawnCounter++;
        this.getWorld().getProfiler().push("sensing");
        this.getWorld().getProfiler().pop();
        int i = this.getWorld().getServer().getTicks() + this.getId();
        if (i % 2 != 0 && this.age > 1) {
            this.getWorld().getProfiler().push("targetSelector");
            this.targetSelector.tickGoals(false);
            this.getWorld().getProfiler().pop();
            this.getWorld().getProfiler().push("goalSelector");
            this.goalSelector.tickGoals(false);
            this.getWorld().getProfiler().pop();
        } else {
            this.getWorld().getProfiler().push("targetSelector");
            this.targetSelector.tick();
            this.getWorld().getProfiler().pop();
            this.getWorld().getProfiler().push("goalSelector");
            this.goalSelector.tick();
            this.getWorld().getProfiler().pop();
        }

        this.getWorld().getProfiler().push("navigation");
        this.navigation.tick();
        this.getWorld().getProfiler().pop();
        this.getWorld().getProfiler().push("mob tick");
        this.getWorld().getProfiler().pop();
        this.getWorld().getProfiler().push("controls");
        this.getWorld().getProfiler().push("move");
        this.moveControl.tick();
        this.getWorld().getProfiler().swap("look");
        this.lookControl.tick();
        this.getWorld().getProfiler().swap("jump");
        this.jumpControl.tick();
        this.getWorld().getProfiler().pop();
        this.getWorld().getProfiler().pop();
    }

    /**
     * will handle automatic state transitions and per-tick behavior
     */
    @Override
    public void tick() {
        super.tick(); // Remove Maybe?
        // maybe add tickNewAI();

        // Only increment tick counter on server side where state transitions happen
        if (!this.getWorld().isClient) {
            ticksInCurrentState++;
            
            if (this.previousState != this.currentState) {
                HorrorMod129.LOGGER.info("Blueice129Entity: State changed to " + this.currentState + " from "
                        + this.previousState);
                this.previousState = this.currentState;
            }
        }
        
        // Check if entity is in an unloaded chunk and despawn silently after 5 seconds
        if (!this.getWorld().isClient) {
            net.minecraft.util.math.ChunkPos chunkPos = new net.minecraft.util.math.ChunkPos(this.getBlockPos());
            if (!this.getWorld().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                ticksInUnloadedChunk++;
                if (ticksInUnloadedChunk > 100) { // 5 seconds
                    HorrorMod129.LOGGER.info("Blueice129Entity: Despawning due to being in unloaded chunk for 5 seconds");
                    this.remove(RemovalReason.DISCARDED);
                    return;
                }
            } else {
                ticksInUnloadedChunk = 0; // Reset counter when chunk is loaded
            }
        }

        // Only process state transitions on server side
        if (this.getWorld().isClient) {
            return; // Client just renders, doesn't make decisions
        }
        
        boolean seesPlayer = checkEntitySeesPlayer();
        int agroMeter = 0;
        if (this.getWorld().getServer() != null) {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(this.getWorld().getServer());
            agroMeter = state.getIntValue("agroMeter", 0);
        }

        switch (currentState) {
            case PASSIVE:

                if (ticksInCurrentState < 20 * 3) {
                    break; // wait at least 3 seconds before checking for transitions
                }

                if (seesPlayer) {
                    setState(EntityState.PANICED);
                }
                // check if the player damages the entity
                if (checkPlayerDamagesEntity()) {
                    setState(EntityState.PANICED);
                }
                should_logout_after_menu = false;
                break;
            case PANICED:
                // after a certain amount of time panicing, transition to IN_MENUS
                // if agro meter high enough, increase chance to enter fleeing/ hiding /
                // aggravated states
                // if not agro, transition to fleeing after .5 seconds
                if (ticksInCurrentState > 20 * 0.5 && agroMeter < 5) {
                    setState(EntityState.FLEEING);
                    should_logout_after_menu = true;
                    // done here so it doesn't log out when implementing higher aggro actions
                    break;
                }

                // after 5 seconds of panicking, if agro is high enough, go to surface hiding
                if (ticksInCurrentState > 20 * 5 && agroMeter >= 5) {
                    setState(EntityState.SURFACE_HIDING);
                    should_logout_after_menu = false; // Don't logout when hiding
                    break;
                }
                break;

            case FLEEING:

                if (ticksInCurrentState > 20 * 5 && agroMeter < 5) {
                    setState(EntityState.IN_MENUS);
                    break;
                }

                // PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 15.0D);
                // if (nearestPlayer == null && ticksInCurrentState > 20 * 8 && agroMeter >= 5) {
                //     if (agroMeter >= 5) {
                //         setState(EntityState.SURFACE_HIDING);
                //         break;
                //     }
                // }
                // if the entity is no longer moving (stuck), go to IN_MENUS
                if (this.getVelocity().lengthSquared() < 0.01 && ticksInCurrentState > 20 * 2) {
                    setState(EntityState.IN_MENUS);
                    HorrorMod129.LOGGER
                            .info("Blueice129Entity: FLEEING state - entity stuck, transitioning to IN_MENUS");
                    should_logout_after_menu = true;
                    break;
                }
                if (checkPlayerDamagesEntity()) {
                    setState(EntityState.PANICED);
                    should_logout_after_menu = true;
                    break;
                }

                break;
            case SURFACE_HIDING:
                // Check for player proximity or damage - transition to IN_MENUS and logout
                PlayerEntity hidingNearestPlayer = this.getWorld().getClosestPlayer(this, 64.0D);

                // If damaged by player, immediately go to menus and logout
                if (checkPlayerDamagesEntity()) {
                    setState(EntityState.IN_MENUS);
                    should_logout_after_menu = true;
                    HorrorMod129.LOGGER.info(
                            "Blueice129Entity: SURFACE_HIDING state - damaged by player, transitioning to IN_MENUS");
                    break;
                }

                // If player gets within 7 blocks, go to menus and logout
                if (hidingNearestPlayer != null && ticksInCurrentState > 20 * 2) {
                    double distanceSquared = this.squaredDistanceTo(hidingNearestPlayer);
                    double actualDistance = Math.sqrt(distanceSquared);
                    
                    // Log when player is getting close (for debugging)
                    if (actualDistance <= 10.0 && ticksInCurrentState % 40 == 0) {
                        HorrorMod129.LOGGER.info(
                                "Blueice129Entity: SURFACE_HIDING state - player is " + String.format("%.1f", actualDistance) + " blocks away");
                    }
                    
                    if (distanceSquared <= 7.0 * 7.0) { // 7 * 7 = 49 (reduced from 7 blocks)
                        setState(EntityState.IN_MENUS);
                        should_logout_after_menu = true;
                        HorrorMod129.LOGGER.info(
                                "Blueice129Entity: SURFACE_HIDING state - player too close (" + String.format("%.1f", actualDistance) + " blocks), transitioning to IN_MENUS");
                    }
                }
                break;
            case UNDERGROUND_BURROWING:
                // TODO: Implement transitions from UNDERGROUND_BURROWING to other states
                break;
            case IN_MENUS:
                if (ticksInCurrentState > 20 && should_logout_after_menu) {
                    // despawn this entity and send a logout message to the chat
                    // This only runs server-side due to the check at the start of tick()
                    this.getWorld().getServer().getPlayerManager().broadcast(net.minecraft.text.Text
                            .literal("Blueice129 left the game").styled(style -> style.withColor(0xFFFF55)), false);
                    this.remove(RemovalReason.DISCARDED);
                    EntityLoginState.setEntityLoggedOut(HorrorModPersistentState.getServerState(this.getWorld().getServer()));
                    return; // Stop processing after despawn
                }
                
                // If not logging out, stay in menus briefly then transition based on aggro
                if (!should_logout_after_menu && ticksInCurrentState > 20 * 3) {
                    if (agroMeter >= 5) {
                        setState(EntityState.SURFACE_HIDING);
                    } else {
                        setState(EntityState.PASSIVE);
                    }
                    break;
                }
                break;
            case INVESTIGATING:
                // TODO: Implement transitions from INVESTIGATING to other states
                break;
            case UPGRADING_HOUSE:
                // TODO: Implement transitions from UPGRADING_HOUSE to other states
                break;
        }
    }

    /**
     * Get the current state of the entity
     */
    public EntityState getState() {
        return currentState;
    }

    /**
     * Check if the player has damaged this entity recently
     */
    private boolean checkPlayerDamagesEntity() {
        // Check if the entity has been recently damaged by a player
        return this.getRecentDamageSource() != null
                && this.getRecentDamageSource().getAttacker() instanceof PlayerEntity;
    }

    /**
     * Check if the entity can see the player within 64 blocks
     * Performs a raycast from the entity's eye position to the player's position
     * blocks
     * and checks if the player is within the entity's field of view
     */
    private boolean checkEntitySeesPlayer() {
        // Find the nearest player within 64 blocks
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 64.0);

        if (nearestPlayer == null) {
            return false;
        }

        // Check if the entity has line of sight to the player
        if (!this.canSee(nearestPlayer)) {
            return false;
        }

        // Check if the player is within the entity's field of view
        // Get the direction vector from entity to player
        double dx = nearestPlayer.getX() - this.getX();
        double dz = nearestPlayer.getZ() - this.getZ();

        // Normalize the direction to player
        double distanceToPlayer = Math.sqrt(dx * dx + dz * dz);
        if (distanceToPlayer < 0.01) {
            return true; // Player is extremely close, can definitely see them
        }

        dx /= distanceToPlayer;
        dz /= distanceToPlayer;

        // Get the entity's look direction (yaw is in degrees)
        double yawRadians = Math.toRadians(this.getYaw());
        double lookDirX = -Math.sin(yawRadians);
        double lookDirZ = Math.cos(yawRadians);

        // Calculate dot product to determine if player is in front of entity
        // Dot product > 0 means player is in front (within 180 degree arc)
        // For a narrower field of view, use a higher threshold (e.g., 0.5 for ~60
        // degrees)
        double dotProduct = dx * lookDirX + dz * lookDirZ;

        // Use a threshold of 0.0 for 180-degree FOV (can see anything in front)
        // Adjust this value for narrower FOV (0.5 ≈ 60°, 0.7 ≈ 45°, 0.866 ≈ 30°)
        return dotProduct > 0.7;
    }

    /**
     * Clear all goals from the goal selector
     * Helper method for the goal profile system
     */
    public void clearGoals() {
        this.goalSelector.clear(goal -> true); // Remove all goals
    }

    /**
     * Add a goal to the goal selector
     * Helper method for the goal profile system
     */
    public void addGoal(int priority, net.minecraft.entity.ai.goal.Goal goal) {
        this.goalSelector.add(priority, goal);
    }

    public void setForwardSpeed(float forwardSpeed) {
        this.forwardSpeed = forwardSpeed;
    }

    public void setUpwardSpeed(float upwardSpeed) {
        this.upwardSpeed = upwardSpeed;
    }

    public void setSidewaysSpeed(float sidewaysSpeed) {
        this.sidewaysSpeed = sidewaysSpeed;
    }

    @Override
    public void setMovementSpeed(float movementSpeed) {
        super.setMovementSpeed(movementSpeed);
        this.setForwardSpeed(movementSpeed);
    }

    public boolean isInWalkTargetRange() {
        return this.isInWalkTargetRange(this.getBlockPos());
    }

    public boolean isInWalkTargetRange(BlockPos pos) {
        return this.positionTargetRange == -1.0F ? true : this.positionTarget.getSquaredDistance(pos) < this.positionTargetRange * this.positionTargetRange;
    }

    public void setPositionTarget(BlockPos target, int range) {
        this.positionTarget = target;
        this.positionTargetRange = range;
    }

    public BlockPos getPositionTarget() {
        return this.positionTarget;
    }

    public float getPositionTargetRange() {
        return this.positionTargetRange;
    }

    public void clearPositionTarget() {
        this.positionTargetRange = -1.0F;
    }

    public boolean hasPositionTarget() {
        return this.positionTargetRange != -1.0F;
    }

    private PlayerBodyControl createBodyControl() {
        return  new PlayerBodyControl(this);
    }

    public PlayerBodyControl getBodyControl() {
        return  this.bodyControl;
    }

    public PlayerJumpControl getJumpControl() {
        return  this.jumpControl;
    }

    public PlayerLookControl getLookControl() {
        return  this.lookControl;
    }

    public PlayerMoveControl getMoveControl() {
        return  this.moveControl;
    }

    // WARNING: THIS IS PUBLIC TO MAKE THE SPAWN SCHEDULER BE ABLE TO ACCESS IT; DO NOT CALL IT DIRECTLY
    public Blueice129Entity(ServerWorld world) {
        super(world.getServer(), world, gameProfile);

        this.networkHandler = new FakePlayerNetworkHandler(this);

        this.lookControl = new PlayerLookControl(this);
        this.moveControl = new PlayerMoveControl(this);
        this.jumpControl = new PlayerJumpControl(this);
        this.bodyControl = this.createBodyControl();

        this.goalSelector = new GoalSelector(world.getProfilerSupplier());
        this.targetSelector = new GoalSelector(world.getProfilerSupplier());

        this.navigation = createNavigation(world);

        // Initialize the goal profile registry
        this.goalRegistry = new GoalProfileRegistry(this);

        // Set initial state based on agro meter
        if (!world.isClient) {
            HorrorModPersistentState state = HorrorModPersistentState.getServerState(world.getServer());
            int agroMeter = state.getIntValue("agroMeter", 0);

            if (agroMeter > 5) {
                this.currentState = EntityState.SURFACE_HIDING;
            } else {
                this.currentState = EntityState.PASSIVE;
            }
        } else {
            // Default to PASSIVE on client or if server is unavailable
            this.currentState = EntityState.PASSIVE;
        }

        if (goalRegistry != null) {
            goalRegistry.applyCurrentProfile();
        }
    }

    /**
     * {@return the maximum degrees which the pitch can change when looking}
     *
     * <p>This is used by the look control.
     *
     * <p>It can return from {@code 1} for entities that can hardly raise their head,
     * like axolotls or dolphins, or {@code 180} for entities that can freely raise
     * and lower their head, like guardians. The default return value is {@code 40}.
     */
    public int getMaxLookPitchChange() {
        return 40;
    }

    /**
     * {@return the maximum degrees which the head yaw can differ from the body yaw}
     *
     * <p>This is used by the body control.
     *
     * <p>It can return from {@code 1} for entities that can hardly rotate their head,
     * like axolotls or dolphins, or {@code 180} for entities that can freely rotate
     * their head, like shulkers. The default return value is {@code 75}.
     */
    public int getMaxHeadRotation() {
        return 75;
    }

    /**
     * {@return the maximum degrees which the yaw can change when looking}
     *
     * <p>This is used by the look control.
     *
     * <p>The default return value is {@code 10}.
     */
    public int getMaxLookYawChange() {
        return 10;
    }

    public float getPathfindingFavor(BlockPos pos) {
        return this.getPathfindingFavor(pos, this.getWorld());
    }

    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 1.0F;
    }

    public boolean isNavigating() {
        return !this.getNavigation().isIdle();
    }

}
