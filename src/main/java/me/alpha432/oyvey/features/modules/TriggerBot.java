package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Triggerbot extends Module {
    
    public enum CooldownMode {
        SWORD, MACE, AXE
    }
    
    private final Setting<CooldownMode> cooldownMode = mode("Cooldown Mode", CooldownMode.SWORD);
    private final Setting<Boolean> playersOnly = bool("Players Only", true);
    private final Setting<Boolean> requireCooldown = bool("Require Cooldown", true);
    private final Setting<Float> minCooldown = slider("Min Cooldown %", 0.9f, 0.0f, 1.0f);
    private final Setting<Boolean> ignoreTeam = bool("Ignore Team", true);
    private final Setting<Boolean> weaponCheck = bool("Weapon Check", true);
    
    public Triggerbot() {
        super("Triggerbot", "Automatically attacks entities when looking at them with proper cooldown timing", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        
        // Check if we're looking at an entity
        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.ENTITY) return;
        
        EntityHitResult entityResult = (EntityHitResult) result;
        if (!(entityResult.getEntity() instanceof LivingEntity target)) return;
        
        // Players only check
        if (playersOnly.getValue() && !(target instanceof Player)) return;
        
        // Team check
        if (ignoreTeam.getValue() && target instanceof Player targetPlayer) {
            if (mc.player.isAlliedTo(targetPlayer)) return;
        }
        
        // Check if target is alive
        if (!target.isAlive() || target.isRemoved()) return;
        
        // Weapon check
        if (weaponCheck.getValue() && !isHoldingWeapon()) return;
        
        // Cooldown check
        if (requireCooldown.getValue()) {
            float cooldown = getCooldownForCurrentWeapon();
            if (cooldown < minCooldown.getValue()) return;
        }
        
        // Attack
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(mc.player.getUsedItemHand());
    }
    
    private boolean isHoldingWeapon() {
        ItemStack mainHand = mc.player.getMainHandItem();
        Item item = mainHand.getItem();
        
        return switch (cooldownMode.getValue()) {
            case SWORD -> item instanceof SwordItem;
            case MACE -> item instanceof MaceItem;
            case AXE -> item instanceof AxeItem;
        };
    }
    
    private float getCooldownForCurrentWeapon() {
        ItemStack mainHand = mc.player.getMainHandItem();
        Item item = mainHand.getItem();
        
        // Get the attack speed based on weapon type
        float attackSpeed = getAttackSpeed(item);
        
        // Calculate cooldown progress (1.0 = fully charged)
        return mc.player.getAttackStrengthScale(0.5f);
    }
    
    private float getAttackSpeed(Item item) {
        // Base attack speeds for different weapon types
        // These values represent attacks per second
        if (item instanceof SwordItem) {
            // Swords typically have 1.6 attack speed
            return 1.6f;
        } else if (item instanceof MaceItem) {
            // Maces typically have slower attack speed
            return 1.2f;
        } else if (item instanceof AxeItem) {
            // Axes have varying speeds based on material
            if (item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE) {
                return 1.0f;
            } else if (item == Items.STONE_AXE) {
                return 0.8f;
            } else {
                return 0.9f; // Iron, Diamond, Netherite
            }
        }
        
        return 4.0f; // Default hand attack speed
    }
    
    @Override
    public String getDisplayInfo() {
        return cooldownMode.getValue().toString();
    }
}
