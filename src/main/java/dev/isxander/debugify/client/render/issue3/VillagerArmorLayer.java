package dev.isxander.debugify.client.render.issue3;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue3Tweak;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerArmorLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    private static final ResourceLocation VILLAGER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    private final HumanoidModel<Villager> poseModel;
    private final HumanoidArmorLayer<Villager, HumanoidModel<Villager>, HumanoidModel<Villager>> armorLayer;

    public VillagerArmorLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent,
                              EntityRendererProvider.Context context) {
        super(parent);
        this.poseModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        RenderLayerParent<Villager, HumanoidModel<Villager>> bridge = new RenderLayerParent<>() {
            @Override
            public HumanoidModel<Villager> getModel() {
                return poseModel;
            }

            @Override
            public ResourceLocation getTextureLocation(Villager entity) {
                return VILLAGER_TEXTURE;
            }
        };
        this.armorLayer = new HumanoidArmorLayer<>(bridge,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!Debugify.CONFIG.isIssue3TweakEnabled(Issue3Tweak.VISIBLE_VILLAGER_ARMOR) || villager.isInvisible()) {
            return;
        }
        poseArmorModel(this.getParentModel());
        this.armorLayer.render(poseStack, buffer, packedLight, villager,
                limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
    }

    private void poseArmorModel(VillagerModel<Villager> villagerModel) {
        HumanoidModel<Villager> model = this.poseModel;
        model.young = false;
        model.riding = villagerModel.riding;
        model.attackTime = villagerModel.attackTime;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

        ModelPart villagerHead = villagerModel.getHead();
        model.head.copyFrom(villagerHead);
        model.head.yScale *= 1.25F;
        model.hat.copyFrom(villagerHead);
        model.hat.yScale *= 1.25F;

        ModelPart villagerRoot = villagerModel.root();
        model.body.copyFrom(villagerRoot.getChild("body"));
        model.body.zScale *= 1.5F;
        model.rightLeg.copyFrom(villagerRoot.getChild("right_leg"));
        model.leftLeg.copyFrom(villagerRoot.getChild("left_leg"));

        model.rightArm.x = -5.0F;
        model.rightArm.y = 2.0F;
        model.rightArm.z = 0.0F;
        model.rightArm.xRot = -1.0F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.6F;
        model.rightArm.xScale = 1.0F;
        model.rightArm.yScale = 1.0F;
        model.rightArm.zScale = 1.0F;

        model.leftArm.x = 5.0F;
        model.leftArm.y = 2.0F;
        model.leftArm.z = 0.0F;
        model.leftArm.xRot = -1.0F;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = -0.6F;
        model.leftArm.xScale = 1.0F;
        model.leftArm.yScale = 1.0F;
        model.leftArm.zScale = 1.0F;
    }
}
