package com.restaurantmanager.client;

import com.restaurantmanager.entity.CustomerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CustomerRenderer extends HumanoidMobRenderer<CustomerEntity, PlayerModel<CustomerEntity>> {
    private static final ResourceLocation[] SKINS = {
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png"),
            new ResourceLocation("minecraft", "textures/entity/player/slim/alex.png")
    };

    public CustomerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CustomerEntity customer) {
        return SKINS[Math.floorMod(customer.getUUID().hashCode(), SKINS.length)];
    }
}
