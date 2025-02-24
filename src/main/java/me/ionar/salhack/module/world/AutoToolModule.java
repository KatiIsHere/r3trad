package me.ionar.salhack.module.world;

import me.ionar.salhack.events.network.EventNetworkPacketEvent;
import me.ionar.salhack.events.player.EventPlayerDamageBlock;
import me.ionar.salhack.events.player.EventPlayerUpdate;
import me.ionar.salhack.module.Module;
import me.ionar.salhack.module.Value;
import me.ionar.salhack.util.Timer;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Author Seth 4/24/2019 @ 3:04 PM.
 */
public final class AutoToolModule extends Module {

    public final Value<Boolean> silent = new Value<Boolean>("Silent", new String[]
            {"Sil"}, "Hold any item and spoof your mining tool.", true);
    public final Value<Boolean> GoBack = new Value<Boolean>("GoBack", new String[]{"ToolBack"}, "Replaces back to your original hotbar item, if you finish mining something", false);
    public BlockPos position;
    public EnumFacing facing;
    private boolean send;
    private int _previousSlot = -1;
    private final Timer _timer = new Timer();
    @EventHandler
    private final Listener<EventPlayerDamageBlock> DamageBlock = new Listener<>(event ->
    {
        if (this.silent.getValue()) {
            final int slot = getToolInventory(event.getPos());
            if (slot != -1) {
                mc.playerController.curBlockDamageMP += blockStrength(event.getPos(), mc.player.inventoryContainer.getSlot(slot).getStack());
            } else {
                final int hotbar = getToolHotbar(event.getPos());
                if (hotbar != -1) {
                    mc.playerController.curBlockDamageMP += blockStrength(event.getPos(), mc.player.inventory.getStackInSlot(hotbar));
                }
            }
        } else {
            _timer.reset();
            final int slot = getToolHotbar(event.getPos());
            if (slot != -1 && mc.player.inventory.currentItem != slot) {
                if (_previousSlot != slot)
                    _previousSlot = mc.player.inventory.currentItem;

                mc.player.inventory.currentItem = slot;
                mc.playerController.updateController();
            }
        }
    });
    @EventHandler
    private final Listener<EventPlayerUpdate> PlayerUpdate = new Listener<>(event ->
    {
        if (_previousSlot != -1 && _timer.passed(250) && !mc.playerController.isHittingBlock) {
            _timer.reset();
            if (GoBack.getValue()) {
                mc.player.inventory.currentItem = _previousSlot;
                mc.playerController.updateController();
            }
            _previousSlot = -1;
        }
    });
    @EventHandler
    private final Listener<EventNetworkPacketEvent> PacketEvent = new Listener<>(event ->
    {
        if (this.send) {
            this.send = false;
            return;
        }
        if (event.getPacket() instanceof CPacketPlayerDigging && this.silent.getValue()) {
            final CPacketPlayerDigging packet = (CPacketPlayerDigging) event.getPacket();
            if (packet.getAction() == CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                this.position = packet.getPosition();
                this.facing = packet.getFacing();

                if (this.position != null && this.facing != null) {
                    final int slot = getToolInventory(packet.getPosition());
                    if (slot != -1) {
                        event.cancel();
                        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, slot,
                                mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                        send = true;
                        mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.position, this.facing));
                        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, slot,
                                mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                    } else {
                        final int hotbar = getToolHotbar(packet.getPosition());
                        if (hotbar != -1) {
                            event.cancel();
                            mc.playerController.windowClick(mc.player.inventoryContainer.windowId, hotbar,
                                    mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                            send = true;
                            mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.position, this.facing));
                            mc.playerController.windowClick(mc.player.inventoryContainer.windowId, hotbar,
                                    mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                        }
                    }
                }
            }
        }
    });

    public AutoToolModule() {
        super("AutoTool", new String[]
                {"Tool"}, "Automatically switches to the best tool", "NONE", 0x70DB24, ModuleType.WORLD);
    }

    private float blockStrength(BlockPos pos, ItemStack stack) {
        float hardness = mc.world.getBlockState(pos).getBlockHardness(mc.world, pos);

        if (hardness < 0.0F) {
            return 0.0F;
        }

        if (!canHarvestBlock(mc.world.getBlockState(pos).getBlock(), pos, stack)) {
            return getDigSpeed(mc.world.getBlockState(pos), pos, stack) / hardness / 100F;
        } else {
            return getDigSpeed(mc.world.getBlockState(pos), pos, stack) / hardness / 30F;
        }
    }

    private boolean canHarvestBlock(Block block, BlockPos pos, ItemStack stack) {
        IBlockState state = mc.world.getBlockState(pos);
        state = state.getBlock().getActualState(state, mc.world, pos);

        if (state.getMaterial().isToolNotRequired()) {
            return true;
        }

        String tool = block.getHarvestTool(state);

        if (stack.isEmpty() || tool == null) {
            return mc.player.canHarvestBlock(state);
        }

        final int toolLevel = stack.getItem().getHarvestLevel(stack, tool, mc.player, state);

        if (toolLevel < 0) {
            return mc.player.canHarvestBlock(state);
        }

        return toolLevel >= block.getHarvestLevel(state);
    }

    private float getDestroySpeed(IBlockState state, ItemStack stack) {
        float f = 1.0F;

        f *= stack.getDestroySpeed(state);

        return f;
    }

    private float getDigSpeed(IBlockState state, BlockPos pos, ItemStack stack) {
        float f = getDestroySpeed(state, stack);

        if (f > 1.0F) {
            int i = EnchantmentHelper.getEfficiencyModifier(mc.player);

            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }

        if (mc.player.isPotionActive(MobEffects.HASTE)) {
            f *= 1.0F + (float) (mc.player.getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }

        if (mc.player.isPotionActive(MobEffects.MINING_FATIGUE)) {
            float f1;

            switch (mc.player.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    f1 = 0.3F;
                    break;
                case 1:
                    f1 = 0.09F;
                    break;
                case 2:
                    f1 = 0.0027F;
                    break;
                case 3:
                default:
                    f1 = 8.1E-4F;
            }

            f *= f1;
        }

        if (mc.player.isInsideOfMaterial(Material.WATER) && !EnchantmentHelper.getAquaAffinityModifier(mc.player)) {
            f /= 5.0F;
        }

        if (!mc.player.onGround) {
            f /= 5.0F;
        }

        f = net.minecraftforge.event.ForgeEventFactory.getBreakSpeed(mc.player, state, f, pos);
        return (f < 0 ? 0 : f);
    }

    private int getToolInventory(BlockPos pos) {
        int index = -1;

        float speed = 1.0f;

        for (int i = 9; i < 36; i++) {
            final ItemStack stack = mc.player.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack != ItemStack.EMPTY) {
                final float digSpeed = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
                final float destroySpeed = stack.getDestroySpeed(mc.world.getBlockState(pos));

                if ((digSpeed + destroySpeed) > speed) {
                    speed = (digSpeed + destroySpeed);
                    index = i;
                }
            }
        }

        return index;
    }

    private int getToolHotbar(BlockPos pos) {
        int index = -1;

        float speed = 1.0f;

        for (int i = 0; i <= 9; i++) {
            final ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack != ItemStack.EMPTY) {
                final float digSpeed = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
                final float destroySpeed = stack.getDestroySpeed(mc.world.getBlockState(pos));

                if ((digSpeed + destroySpeed) > speed) {
                    speed = (digSpeed + destroySpeed);
                    index = i;
                }
            }
        }

        return index;
    }

}
