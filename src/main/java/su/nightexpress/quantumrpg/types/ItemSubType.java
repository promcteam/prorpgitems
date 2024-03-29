package su.nightexpress.quantumrpg.types;

import mc.promcteam.engine.NexEngine;
import mc.promcteam.engine.items.ItemType;
import mc.promcteam.engine.items.exception.ProItemException;
import mc.promcteam.engine.items.providers.VanillaProvider;
import mc.promcteam.engine.utils.StringUT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quantumrpg.QuantumRPG;

import java.util.HashSet;
import java.util.Set;

public class ItemSubType {

    private String      id;
    private String        name;
    private Set<ItemType> mats;

    public ItemSubType(@NotNull String id, @NotNull String name, @NotNull Set<String> mats) {
        this.id = id.toLowerCase();
        this.setName(name);

        this.mats = new HashSet<>();
        for (String mat : mats) {
            try {
                this.mats.add(NexEngine.get().getItemManager().getItemType(mat));
            } catch (ProItemException e) {
                QuantumRPG.getInstance().warn("Unknown item sub type: \""+mat+'\"');
            }
        }
    }

    @NotNull
    public String getId() {
        return this.id;
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
}
