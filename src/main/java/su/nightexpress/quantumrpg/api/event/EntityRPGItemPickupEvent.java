package su.nightexpress.quantumrpg.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quantumrpg.modules.api.QModuleDrop;

public class EntityRPGItemPickupEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;

    private ItemStack item;

    private LivingEntity li;

    private QModuleDrop<?> module;

    public EntityRPGItemPickupEvent(@NotNull ItemStack item, @NotNull LivingEntity li, @NotNull QModuleDrop<?> module) {
        this.item = item;
        this.li = li;
        this.module = module;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    public ItemStack getItem() {
        return this.item;
    }

    @NotNull
    public LivingEntity getEntity() {
        return this.li;
    }

    @NotNull
    public QModuleDrop<?> getModule() {
        return this.module;
    }
}
