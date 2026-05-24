package horror.blueice129.client.entity.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

/**
 * Renderer for the Blueice129 entity.
 * Provides the model and texture for rendering the entity in the world.
 */
public class Blueice129EntityRenderer extends PlayerEntityRenderer {

    /**
     * Constructor for the Blueice129EntityRenderer.
     * 
     * @param context The entity renderer factory context
     */
    public Blueice129EntityRenderer(EntityRendererFactory.Context context) {
        // false = standard Steve arms (4px wide), true = Alex arms (3px wide)
        super(context, false);
    }
}
