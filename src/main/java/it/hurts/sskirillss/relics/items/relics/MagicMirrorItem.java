package it.hurts.sskirillss.relics.items.relics;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.indev.*;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.utils.DurabilityUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.FOVModifierEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Random;

public class MagicMirrorItem extends RelicItem {
    public MagicMirrorItem() {
        super(RelicData.builder()
                .rarity(Rarity.RARE)
                .build());
    }

    @Override
    public RelicDataNew getNewData() {
        return RelicDataNew.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("teleport", RelicAbilityEntry.builder()
                                .stat("distance", RelicAbilityStat.builder()
                                        .initialValue(200F, 1000F)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, 500F)
                                        .build())
                                .stat("cooldown", RelicAbilityStat.builder()
                                        .initialValue(60F, 300F)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, -2F)
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 200))
                .styleData(RelicStyleData.builder()
                        .borders("#008cd7", "#0a3484")
                        .build())
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);

        if (playerIn.getCooldowns().isOnCooldown(ItemRegistry.MAGIC_MIRROR.get())
                || DurabilityUtils.isBroken(stack) || worldIn.isClientSide())
            return InteractionResultHolder.fail(stack);

        ServerPlayer serverPlayer = (ServerPlayer) playerIn;

        Pair<ServerLevel, Vec3> data = getHomePos(serverPlayer);

        if (!canTeleport(serverPlayer, data, stack))
            return InteractionResultHolder.fail(stack);

        playerIn.startUsingItem(handIn);

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (world.isClientSide() || !(entity instanceof ServerPlayer player))
            return stack;

        Pair<ServerLevel, Vec3> data = getHomePos(player);

        if (!canTeleport(player, data, stack))
            return stack;

        Vec3 pos = data.getRight();

        player.teleportTo(data.getLeft(), pos.x() + 0.5F, pos.y() + 1.0F, pos.z() + 0.5F, player.getYRot(), player.getXRot());

        if (!player.isCreative())
            player.getCooldowns().addCooldown(stack.getItem(), (int) Math.round(getAbilityValue(stack, "teleport", "cooldown") * 20));

        addExperience(stack, 1);

        world.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        return stack;
    }


    @Override
    public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
        if (player.level.isClientSide())
            return;

        ServerLevel level = (ServerLevel) player.level;

        float radius = count * 0.075F;
        double extraY = player.getY() + 1.5F - Math.log((count + getUseDuration(stack) * 0.075F) * 0.1F);

        Random random = level.getRandom();

        Color color = switch (level.dimension().location().getPath()) {
            case "overworld" -> new Color(75, 150, 255);
            case "the_nether" -> new Color(150, 0, 0);
            case "the_end" -> new Color(100, 0, 200);
            default -> new Color(50, 150, 0);
        };

        for (int i = 0; i < 5; i++) {
            float angle = (0.01F * (count * 3 + i * 125));

            double extraX = (double) (radius * Mth.sin((float) (Math.PI + angle))) + player.getX();
            double extraZ = (double) (radius * Mth.cos(angle)) + player.getZ();

            level.sendParticles(new CircleTintData(color, Math.max(0.2F, (getUseDuration(stack) - count) * 0.015F),
                    40, 0.92F, false), extraX, extraY, extraZ, 1, 0F, 0F, 0F, 0F);
        }

        level.sendParticles(new CircleTintData(color, (getUseDuration(stack) - count) * 0.005F, 10 + random.nextInt(50),
                        0.95F, false), player.getX(), player.getY() + player.getBbHeight() * 0.5F, player.getZ(),
                (int) ((getUseDuration(stack) - count) * 0.5F), 0.25F, player.getBbHeight() * 0.4F, 0.25F, 0.025F);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 40;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Nullable
    private Pair<ServerLevel, Vec3> getHomePos(ServerPlayer player) {
        if (player.level.isClientSide())
            return null;

        BlockPos pos = player.getRespawnPosition();

        MinecraftServer server = player.getServer();

        if (server == null)
            return null;

        ServerLevel world = player.getServer().getLevel(player.getRespawnDimension());

        if (world == null || pos == null)
            return null;

        return Player.findRespawnPositionAndUseSpawnBlock(world, pos, player.getRespawnAngle(), true, false)
                .map(vec3 -> Pair.of(world, vec3)).orElse(null);
    }

    private boolean canTeleport(ServerPlayer player, Pair<ServerLevel, Vec3> data, ItemStack stack) {
        if (data == null) {
            player.displayClientMessage(new TranslatableComponent("tooltip.relics.magic_mirror.invalid_location"), false);

            return false;
        }

        Vec3 pos = data.getRight();
        ServerLevel level = data.getLeft();

        if (pos.distanceTo(player.position()) * DimensionType.getTeleportationScale(player.level.dimensionType(),
                level.dimensionType()) > getAbilityValue(stack, "teleport", "distance")) {
            player.displayClientMessage(new TranslatableComponent("tooltip.relics.magic_mirror.too_far"), false);

            return false;
        }

        if (level.dimension() != player.level.dimension() && getAbilityPoints(stack, "teleport") < 5) {
            player.displayClientMessage(new TranslatableComponent("tooltip.relics.magic_mirror.invalid_dimension"), false);

            return false;
        }

        return true;
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onFovUpdate(FOVModifierEvent event) {
            Player player = event.getEntity();
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (stack.getItem() != ItemRegistry.MAGIC_MIRROR.get()
                    || !player.isUsingItem())
                return;

            int time = player.getTicksUsingItem();

            if (time > 0)
                event.setNewfov(event.getNewfov() - time * 0.02F);
        }
    }

    @Mod.EventBusSubscriber
    public static class ServerEvents {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            Item item = ItemRegistry.MAGIC_MIRROR.get();

            if (!(event.getEntityLiving() instanceof Player player) || !player.isUsingItem()
                    || (player.getMainHandItem().getItem() != item && player.getOffhandItem().getItem() != item))
                return;

            player.stopUsingItem();

            player.getCooldowns().addCooldown(item, 20);
        }
    }
}