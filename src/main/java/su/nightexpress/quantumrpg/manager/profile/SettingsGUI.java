package su.nightexpress.quantumrpg.manager.profile;

import mc.promcteam.engine.config.api.JYML;
import mc.promcteam.engine.manager.api.gui.ContentType;
import mc.promcteam.engine.manager.api.gui.GuiClick;
import mc.promcteam.engine.manager.api.gui.GuiItem;
import mc.promcteam.engine.manager.api.gui.NGUI;
import mc.promcteam.engine.utils.CollectionsUT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quantumrpg.QuantumRPG;
import su.nightexpress.quantumrpg.data.api.RPGUser;
import su.nightexpress.quantumrpg.data.api.UserProfile;

import java.util.List;

public class SettingsGUI extends NGUI<QuantumRPG> {

    private ProfileManager profileManager;

    public SettingsGUI(@NotNull ProfileManager profileManager, @NotNull JYML cfg) {
        super(profileManager.plugin, cfg, "");
        this.profileManager = profileManager;

        GuiClick click = (p, type, e) -> {
            if (type == null) return;
            Class<?> clazz = type.getClass();

            if (clazz.equals(ContentType.class)) {
                ContentType type2 = (ContentType) type;
                switch (type2) {
                    case RETURN: {
                        this.profileManager.getProfileGUI().open(p, 1);
                        break;
                    }
                    case EXIT: {
                        p.closeInventory();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                return;
            }

            if (clazz.equals(ItemType.class)) {
                ItemType type2 = (ItemType) type;

                RPGUser user = plugin.getUserManager().getOrLoadUser(p);
                if (user == null) return;

                UserProfile profile = user.getActiveProfile();

                switch (type2) {
                    case SETTING_ENTITY_NAMES: {
                        profile.setNamesMode(CollectionsUT.toggleEnum(profile.getNamesMode()));
                        break;
                    }
                    case SETTING_PLAYER_HELMET: {
                        profile.setHideHelmet(!profile.isHideHelmet());
                        break;
                    }
                    default: {
                        return;
                    }
                }
                this.profileManager.getSettingsGUI().open(p, 1);
                return;
            }
        };

        for (String id : cfg.getSection("content")) {
            GuiItem guiItem = cfg.getGuiItem("content." + id, ItemType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(click);
            }
            this.addButton(guiItem);
        }
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

    }

    @Override
    protected boolean ignoreNullClick() {
        return true;
    }

    @Override
    protected boolean cancelClick(int slot) {
        return true;
    }

    @Override
    protected boolean cancelPlayerClick() {
        return true;
    }

    @Override
    protected void replaceMeta(@NotNull Player player, @NotNull ItemStack item, @NotNull GuiItem guiItem) {
        super.replaceMeta(player, item, guiItem);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        RPGUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile profile = user.getActiveProfile();

        lore.replaceAll(line -> line
                .replace("%player-helmet%", plugin.lang().getBool(profile.isHideHelmet()))
                .replace("%entity-names%", plugin.lang().getEnum(profile.getNamesMode()))
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private static enum ItemType {
        SETTING_ENTITY_NAMES,
        SETTING_PLAYER_HELMET,
        ;
    }
}
