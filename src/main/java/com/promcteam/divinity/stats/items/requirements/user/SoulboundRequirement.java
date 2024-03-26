package com.promcteam.divinity.stats.items.requirements.user;

import com.promcteam.codex.config.api.ILangMsg;
import com.promcteam.divinity.Divinity;
import com.promcteam.divinity.Perms;
import com.promcteam.divinity.modules.list.soulbound.SoulboundManager;
import com.promcteam.divinity.stats.items.ItemTags;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SoulboundRequirement extends AbstractOwnerRequirement {

    public SoulboundRequirement(
            @NotNull String name,
            @NotNull String format
    ) {
        super(
                "soulbound",
                name,
                format,
                ItemTags.PLACEHOLDER_REQ_USER_SOUL
        );
    }

    @Override
    @NotNull
    public String getBypassPermission() {
        return Perms.BYPASS_REQ_USER_SOULBOUND;
    }

    @Override
    public boolean canUse(@NotNull Player player, @Nullable UUID value) {
        return value == null || player.getUniqueId().equals(value);
    }

    @Override
    @NotNull
    public String formatValue(@NotNull ItemStack item, @NotNull UUID value) {
        if (value.equals(DUMMY_ID)) {
            return SoulboundManager.SOULBOUND_FORMAT_FREE;
        }
        String user = Divinity.getInstance().getServer().getOfflinePlayer(value).getName();
        if (user == null) user = "null";

        return SoulboundManager.SOULBOUND_FORMAT_APPLIED.replace("%player%", user);
    }

    @Override
    @NotNull
    public ILangMsg getDenyMessage(@NotNull Player p, @NotNull ItemStack src) {
        if (this.isRequired(src)) {
            return Divinity.getInstance().lang().Module_Item_Interact_Error_Soulbound;
        }
        UUID   value = this.getRaw(src);
        String user  = "null";
        if (value != null) {
            String name = Divinity.getInstance().getServer().getOfflinePlayer(value).getName();
            if (name != null) user = name;
        }
        return Divinity.getInstance().lang().Module_Item_Interact_Error_Owner.replace("%owner%", user);
    }
}