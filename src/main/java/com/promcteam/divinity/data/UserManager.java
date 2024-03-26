package com.promcteam.divinity.data;

import com.promcteam.codex.data.users.IUserManager;
import com.promcteam.divinity.Divinity;
import com.promcteam.divinity.data.api.DivinityUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UserManager extends IUserManager<Divinity, DivinityUser> {

    public UserManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    protected DivinityUser createData(@NotNull Player player) {
        return new DivinityUser(this.plugin, player);
    }
}