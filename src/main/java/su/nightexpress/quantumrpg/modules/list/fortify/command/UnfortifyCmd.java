package su.nightexpress.quantumrpg.modules.list.fortify.command;

import mc.promcteam.engine.utils.ItemUT;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quantumrpg.Perms;
import su.nightexpress.quantumrpg.modules.command.MCmd;
import su.nightexpress.quantumrpg.modules.list.fortify.FortifyManager;

public class UnfortifyCmd extends MCmd<FortifyManager> {

    public UnfortifyCmd(@NotNull FortifyManager module) {
        super(module, new String[]{"unfortify"}, Perms.FORTIFY_CMD_UNFORTIFY);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Fortify_Cmd_Unfortify_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player    p    = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) {
            plugin.lang().Error_NoItem.send(p);
            return;
        }

        module.unfortifyItem(item);
        p.getInventory().setItemInMainHand(item);

        plugin.lang().Command_Modify_Done.send(p);
    }

}
