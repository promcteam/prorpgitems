package su.nightexpress.quantumrpg.types;

import mc.promcteam.engine.NexEngine;
import mc.promcteam.engine.items.ItemType;
import mc.promcteam.engine.items.exception.ProItemException;
import mc.promcteam.engine.items.providers.VanillaProvider;
import mc.promcteam.engine.utils.StringUT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.quantumrpg.QuantumRPG;

import java.util.HashSet;
import java.util.Set;

public enum ItemGroup {

    WEAPON("Weapon"),
    ARMOR("Armor"),
    TOOL("Tool"),
    ;

    private String        name;
    private Set<ItemType> mats;

    private ItemGroup(@NotNull String name) {
        this.setName(name);
        this.mats = new HashSet<>();
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = StringUT.color(name);
    }

    @NotNull
    public Set<ItemType> getMaterials() {
        return this.mats;
    }

    public void setMaterials(@NotNull Set<String> mats) {
        this.mats.clear();
        for (String mat : mats) {
            try {
                this.mats.add(NexEngine.get().getItemManager().getItemType(mat));
            } catch (ProItemException e) {
                QuantumRPG.getInstance().warn("Unknown item group: \""+mat+'\"');
            }
        }
    }

    public boolean isItemOfThis(@NotNull ItemStack item) {
        return this.mats.stream().anyMatch(itemType -> itemType.isInstance(item));
    }

    @Deprecated
    public boolean isItemOfThis(@NotNull Material mat) {
        return this.mats.contains(new VanillaProvider.VanillaItemType(mat));
    }

    public boolean isItemOfThis(@NotNull String mat) {
        return this.mats.stream().anyMatch(itemType -> itemType.getNamespacedID().equalsIgnoreCase(mat));
    }

    @Nullable
    public static ItemGroup getItemGroup(@NotNull ItemStack item) {
        for (ItemGroup ig : ItemGroup.values()) {
            if (ig.isItemOfThis(item)) {
                return ig;
            }
        }
        return null;
    }

    @Deprecated
    @Nullable
    public static ItemGroup getItemGroup(@NotNull Material material) {
        for (ItemGroup ig : ItemGroup.values()) {
            if (ig.isItemOfThis(material)) {
                return ig;
            }
        }
        return null;
    }

    @Nullable
    public static ItemGroup getItemGroup(@NotNull String mat) {
        for (ItemGroup ig : ItemGroup.values()) {
            if (ig.isItemOfThis(mat)) {
                return ig;
            }
        }
        return null;
    }
}
