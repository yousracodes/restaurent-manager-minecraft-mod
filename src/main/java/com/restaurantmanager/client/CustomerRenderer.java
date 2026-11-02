package com.restaurantmanager.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.restaurantmanager.entity.CustomerPatience;
import com.restaurantmanager.entity.CustomerEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CustomerRenderer<T extends CustomerEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final ResourceLocation[] SKINS = {
            customerSkin("customer_01.png"),
            customerSkin("customer_02.png"),
            customerSkin("customer_03.png"),
            customerSkin("customer_04.png"),
            customerSkin("customer_05.png"),
            customerSkin("customer_06.png"),
            customerSkin("customer_07.png"),
            customerSkin("customer_08.png"),
            customerSkin("customer_09.png"),
            customerSkin("customer_10.png"),
            customerSkin("customer_11.png"),
            customerSkin("customer_12.png"),
            customerSkin("customer_13.png"),
            customerSkin("customer_14.png"),
            customerSkin("customer_15.png")
    };
    private static final float ICON_SIZE = 0.55F;
    private static final float LABEL_SCALE = 0.025F;
    private static final float LABEL_X = 16.0F;
    private static final float LABEL_Y = -4.0F;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int LABEL_BACKGROUND = 0x66000000;
    private final EntityRenderDispatcher renderDispatcher;

    public CustomerRenderer(EntityRendererProvider.Context context) {
        super(context, new SittingCustomerModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.renderDispatcher = context.getEntityRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(T customer) {
        return SKINS[Math.floorMod(customer.getUUID().hashCode(), SKINS.length)];
    }

    @Override
    public void render(T customer, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight) {
        super.render(customer, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderPatienceIcon(customer, poseStack, buffer, packedLight);
    }

    @Override
    protected void renderNameTag(T customer, Component name, PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight) {
    }

    private void renderPatienceIcon(T customer, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation texture = getPatienceTexture(customer.getPatienceForRendering());
        float halfSize = ICON_SIZE * 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.0D, customer.getBbHeight() + 0.65D, 0.0D);
        poseStack.mulPose(renderDispatcher.cameraOrientation());
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        addIconVertex(consumer, positionMatrix, normalMatrix, -halfSize, -halfSize, 0.0F, 0.0F, 0.0F, packedLight);
        addIconVertex(consumer, positionMatrix, normalMatrix, halfSize, -halfSize, 0.0F, 1.0F, 0.0F, packedLight);
        addIconVertex(consumer, positionMatrix, normalMatrix, halfSize, halfSize, 0.0F, 1.0F, 1.0F, packedLight);
        addIconVertex(consumer, positionMatrix, normalMatrix, -halfSize, halfSize, 0.0F, 0.0F, 1.0F, packedLight);
        poseStack.popPose();

        renderStateLabel(customer, poseStack, buffer, packedLight);
    }

    private void renderStateLabel(T customer, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String label = customer.getStateLabelForRendering();
        if (label.isBlank()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, customer.getBbHeight() + 0.65D, 0.0D);
        poseStack.mulPose(renderDispatcher.cameraOrientation());
        poseStack.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        Font font = getFont();
        Matrix4f positionMatrix = poseStack.last().pose();
        font.drawInBatch(label, LABEL_X, LABEL_Y, LABEL_COLOR, false, positionMatrix, buffer,
                Font.DisplayMode.NORMAL, LABEL_BACKGROUND, packedLight);
        poseStack.popPose();
    }

    private static void addIconVertex(VertexConsumer consumer, Matrix4f positionMatrix, Matrix3f normalMatrix,
                                      float x, float y, float z, float u, float v, int packedLight) {
        consumer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static ResourceLocation getPatienceTexture(CustomerPatience patience) {
        return new ResourceLocation(
                "restaurantmanager",
                "textures/entity/customer_patience/" + patience.name().toLowerCase(java.util.Locale.ROOT) + ".png"
        );
    }

    private static ResourceLocation customerSkin(String fileName) {
        return new ResourceLocation("restaurantmanager", "textures/entity/customer_skins/" + fileName);
    }

    private static class SittingCustomerModel<T extends CustomerEntity> extends PlayerModel<T> {
        private static final float LEG_FORWARD_ROTATION = -80.0F * Mth.DEG_TO_RAD;
        private static final float LEG_SIDE_ROTATION = 8.0F * Mth.DEG_TO_RAD;
        private static final float ARM_REST_ROTATION = -25.0F * Mth.DEG_TO_RAD;
        private static final float BODY_LEAN_ROTATION = 5.0F * Mth.DEG_TO_RAD;

        SittingCustomerModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(T customer, float limbSwing, float limbSwingAmount, float ageInTicks,
                              float netHeadYaw, float headPitch) {
            super.setupAnim(customer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (!customer.isSeatedForRendering()) {
                return;
            }

            this.body.xRot = BODY_LEAN_ROTATION;
            this.rightLeg.xRot = LEG_FORWARD_ROTATION;
            this.leftLeg.xRot = LEG_FORWARD_ROTATION;
            this.rightLeg.yRot = LEG_SIDE_ROTATION;
            this.leftLeg.yRot = -LEG_SIDE_ROTATION;
            this.rightArm.xRot = ARM_REST_ROTATION;
            this.leftArm.xRot = ARM_REST_ROTATION;
            this.rightArm.zRot = 4.0F * Mth.DEG_TO_RAD;
            this.leftArm.zRot = -4.0F * Mth.DEG_TO_RAD;

            // PlayerModel copies these layers before our sitting pose is applied.
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftPants.copyFrom(this.leftLeg);
            this.jacket.copyFrom(this.body);
        }
    }
}
