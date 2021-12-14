package su.nightexpress.quantumrpg.nms.packets.versions;

import com.mojang.datafixers.util.Pair;
import mc.promcteam.engine.NexEngine;
import mc.promcteam.engine.hooks.Hooks;
import mc.promcteam.engine.nms.packets.IPacketHandler;
import mc.promcteam.engine.nms.packets.events.EnginePlayerPacketEvent;
import mc.promcteam.engine.nms.packets.events.EngineServerPacketEvent;
import mc.promcteam.engine.utils.ItemUT;
import mc.promcteam.engine.utils.Reflex;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quantumrpg.QuantumRPG;
import su.nightexpress.quantumrpg.api.event.EntityEquipmentChangeEvent;
import su.nightexpress.quantumrpg.config.EngineCfg;
import su.nightexpress.quantumrpg.data.api.RPGUser;
import su.nightexpress.quantumrpg.data.api.UserEntityNamesMode;
import su.nightexpress.quantumrpg.data.api.UserProfile;
import su.nightexpress.quantumrpg.manager.EntityManager;
import su.nightexpress.quantumrpg.modules.list.itemhints.ItemHintsManager;
import su.nightexpress.quantumrpg.nms.packets.PacketManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class V1_17_R1 extends UniversalPacketHandler implements IPacketHandler {

    private static final String PACKET_LOCATION = "net.minecraft.network.protocol.game";

    public V1_17_R1(@NotNull QuantumRPG plugin) {
        super(plugin);
    }

    @Override
    public void managePlayerPacket(@NotNull EnginePlayerPacketEvent e) {
        Class playoutParticles        = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutWorldParticles");
        Class playoutSpawnEntity      = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutSpawnEntity");
        Class playoutUpdateAttributes = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutUpdateAttributes");
        Class playoutEntityMetadata   = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutEntityMetadata");
        Class playOutEntityEquipment  = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutEntityEquipment");

        Object packet = e.getPacket();

        if (EngineCfg.PACKETS_REDUCE_COMBAT_PARTICLES && playoutParticles.isInstance(packet)) {
            this.manageDamageParticle(e, packet);
            return;
        }
        if (EngineCfg.PACKETS_MOD_GLOW_COLOR && playoutSpawnEntity.isInstance(packet)) {
            this.manageCustomGlow(e, packet);
            return;
        }

        if (playoutUpdateAttributes.isInstance(packet)) {
            this.manageEquipmentChanges(e, packet);
            return;
        }
        if (playoutEntityMetadata.isInstance(packet)) {
            this.manageEntityNames(e, packet);
            return;
        }
        if (playOutEntityEquipment.isInstance(packet)) {
            this.managePlayerHelmet(e, packet);
            return;
        }
    }

    @Override
    public void manageEquipmentChanges(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Class playoutUpdateAttributes = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutUpdateAttributes");
            Class craftServerClass        = Reflex.getCraftClass("CraftServer");
            Class nmsEntityClass          = Reflex.getClass("net.minecraft.world.entity", "Entity");
            Class worldServerClass        = Reflex.getClass("net.minecraft.server.level", "WorldServer");

            Object equip = playoutUpdateAttributes.cast(packet);

            Integer entityId = (Integer) Reflex.getFieldValue(equip, "a");
            if (entityId == null) return;

            Object server    = craftServerClass.cast(Bukkit.getServer());
            Object nmsEntity = null;

            Object dedicatedServer = Reflex.invokeMethod(
                    Reflex.getMethod(craftServerClass, "getServer"),
                    server
            );

            Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                    Reflex.getMethod(dedicatedServer.getClass(), "getWorlds"),
                    dedicatedServer
            );

            Method getEntity = Reflex.getMethod(worldServerClass, "getEntity", int.class);
            for (Object worldServer : worlds) {
                nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
                if (nmsEntity != null) {
                    break;
                }
            }

            if (nmsEntity == null) return;


            Method getUniqueId = Reflex.getMethod(nmsEntityClass, "getUniqueID");

            Entity bukkitEntity = NexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));
            if (!(bukkitEntity instanceof LivingEntity)) return;
            if (EntityManager.isPacketDuplicatorFixed(bukkitEntity)) return;

            EntityEquipmentChangeEvent event = new EntityEquipmentChangeEvent((LivingEntity) bukkitEntity);
            plugin.getServer().getPluginManager().callEvent(event);
        });
    }

    @Override
    protected void manageDamageParticle(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Class packetParticlesClass = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutWorldParticles");
        Class particleParamClass   = Reflex.getClass("net.minecraft.core.particles", "ParticleParam");

        Object p = packetParticlesClass.cast(packet);

        Object j = Reflex.getFieldValue(p, "j");
        if (j == null) return;

        Method a = Reflex.getMethod(particleParamClass, "a");

        String name = (String) Reflex.invokeMethod(a, j);
        if (name.contains("damage_indicator")) {
            Reflex.setFieldValue(p, "h", 20);
        }
    }

    @Override
    protected void manageCustomGlow(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Object oId = Reflex.getFieldValue(packet, "d"); // Entity UUID
        if (oId == null) return;

        // Do a tick delay to let entity be spawned in the world before we can get it by UUID
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            UUID id = (UUID) oId;

            // Get entity and check if it's a dropped item
            Entity entity = plugin.getServer().getEntity(id);
            if (!(entity instanceof Item)) return;

            // Check if Glow setting is applicable to this item stack.
            Item             item        = (Item) entity;
            ItemHintsManager hintManager = plugin.getModuleCache().getItemHintsManager();
            if (hintManager == null || !hintManager.isGlow(item)) return;

            // Set item custom hint via HintManager before apply glowing
            //hintManager.setItemHint(item, 0);

            // Get glowing color depends on hint color.
            ChatColor cc   = ChatColor.WHITE;
            String    name = ItemUT.getItemName(item.getItemStack());
            if (name.length() > 2) {
                String ss = String.valueOf(cc.getChar());
                if (name.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                    ss = name.substring(1, 2);
                }
                ChatColor c2 = ChatColor.getByChar(ss);
                if (c2 != null && c2.isColor()) cc = c2;
            }
            try {
                Class<?> en = Reflex.getClass("net.minecraft", "EnumChatFormat");
                Method   b  = Reflex.getMethod(en, "b", String.class);
                //TODO Uncomment this
//                Enum ec = (Enum) b.invoke(null, cc.name());

                EnumChatFormat ec = EnumChatFormat.b(cc.name());

                Player p = e.getReciever();

                // Check if team for this color is already created
                // Also Check team per player in case of logout
                boolean        newTeam = true;
                Set<ChatColor> hash    = PacketManager.COLOR_CACHE.get(p);
                if (hash != null) {
                    if (hash.contains(cc)) {
                        newTeam = false;
                    }
                } else {
                    hash = new HashSet<>();
                }
                hash.add(cc);
                PacketManager.COLOR_CACHE.put(p, hash);

                // Set team name for each color
                String teamId = "GLOW_" + ec.name();
                if (teamId.length() > 16) teamId = teamId.substring(0, 16);

                Class       chatComponentClass = Reflex.getClass("net.minecraft.network.chat", "ChatComponentText");
                Constructor ctor               = Reflex.getConstructor(chatComponentClass, String.class);

                // ########### Create team
                Class boardClass = Reflex.getClass("net.minecraft.world.scores", "Scoreboard");
                Class teamClass  = Reflex.getClass("net.minecraft.world.scores", "ScoreboardTeam");
                Class playOutScoreboardTeamPacket = Reflex.getClass("net.minecraft.network.protocol.game",
                        "PacketPlayOutScoreboardTeam");
                Object board = Reflex.getConstructor(boardClass).newInstance();
//                Scoreboard board = new Scoreboard();
                Object team = Reflex.getConstructor(teamClass, boardClass, String.class)
                        .newInstance(board, teamId);
//                ScoreboardTeam team = new ScoreboardTeam(board, teamId);

                Reflex.invokeMethod(teamClass.getMethod("setColor", EnumChatFormat.class), team, ec);
//                team.setColor(ec);
                Reflex.invokeMethod(teamClass.getMethod("setDisplayName", IChatBaseComponent.class), team, IChatBaseComponent.a(teamId));
//                team.setDisplayName(IChatBaseComponent.a(teamId));
                Reflex.invokeMethod(teamClass.getMethod("setPrefix", IChatBaseComponent.class), team, IChatBaseComponent.a(""));
//                team.setPrefix(IChatBaseComponent.a(""));
                Reflex.invokeMethod(boardClass.getMethod("addPlayerToTeam", String.class, teamClass), board, id.toString(), team);
//                board.addPlayerToTeam(id.toString(), team);

                Object pTeam = Reflex.invokeMethod(playOutScoreboardTeamPacket.getMethod("a", teamClass, boolean.class),
                        null, team, newTeam);
//                PacketPlayOutScoreboardTeam pTeam = PacketPlayOutScoreboardTeam.a(team, newTeam);

                // ==========================================

//                Collection<String> entities = pTeam.e();
//                if (entities == null) return;

//                entities.add(id.toString());
                // ###########

                // Get list of fake team entities to add our item into it
                //TODO Fix this.
//                Class scoreboardPacket = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutScoreboardTeam");
//                //TODO Construct using PacketPlayOutScoreboardTeam.a(ScoreboardTeam, boolean);
//                Object pTeam = Reflex.invokeConstructor(Reflex.getConstructor(scoreboardPacket));
//                Object oEntities = Reflex.getFieldValue(pTeam, "j"); // List of team entities
//                if (oEntities == null) return;
//
//                @SuppressWarnings("unchecked")
//                Collection<String> entities = (Collection<String>) oEntities;
//                entities.add(id.toString());
//
//                // Set team fields
//                Reflex.setFieldValue(pTeam, "h", newTeam ? 0 : 3); // 0 = new team, 3 = add entity, 4 = remove entity
//                Reflex.setFieldValue(pTeam, "i", teamId); // Internal team name
//
//                if (newTeam) {
//                    //TODO Update Parameters list
//                    Reflex.setFieldValue(pTeam, "f", ec); // Team color (Parameters, 'f')
//                    Reflex.setFieldValue(pTeam, "a", Reflex.invokeConstructor(ctor, teamId)); // Team display name (Parameters, 'a')
//                    Reflex.setFieldValue(pTeam, "b", Reflex.invokeConstructor(ctor, "")); // Team prefix (Parameters, 'b')
//                }
                // Send packet to a player
                plugin.getPacketManager().sendPacket(e.getReciever(), pTeam);
                // Activate colored glowing
                entity.setGlowing(true);
            } catch (Exception err) { //IllegalAccessException | InvocationTargetException err) {
                System.err.println("Could not set entity glowing.");
                err.printStackTrace();
            }
        });
    }

    @Override
    protected void manageEntityNames(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        RPGUser user = plugin.getUserManager().getOrLoadUser(e.getReciever());
        if (user == null) return;

        UserProfile         profile   = user.getActiveProfile();
        UserEntityNamesMode namesMode = profile.getNamesMode();
        if (namesMode == UserEntityNamesMode.DEFAULT) return;

        Class pClass = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutEntityMetadata");

        Object p = pClass.cast(packet);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) Reflex.getFieldValue(p, "b");
        if (list == null) return;

        // Hide or show custom entity names
        if (list.size() > 13) {
            Object index3 = list.get(13);

            Method bMethod = Reflex.getMethod(index3.getClass(), "b");

            Object b = Reflex.invokeMethod(bMethod, index3);
            if (b == null || !b.getClass().equals(Boolean.class)) return;
            //Object nameVisible = Reflex.getFieldValue(index3, "b");

            boolean visibility = namesMode == UserEntityNamesMode.ALWAYS_VISIBLE;
            Reflex.setFieldValue(index3, "b", visibility);
        }
    }

    @Override
    protected void managePlayerHelmet(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Bukkit.getScheduler().runTask(QuantumRPG.getInstance(), () -> {
            Class playOutEntityEquipment = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutEntityEquipment");
            Class enumItemSlotClass      = Reflex.getClass("net.minecraft.world.entity", "EnumItemSlot");

            Object p = playOutEntityEquipment.cast(packet);

            @SuppressWarnings("unchecked")
            List<Pair<Object, Object>> slots = (List<Pair<Object, Object>>) Reflex.getFieldValue(p, "c");
            boolean contains = false;
            for (Pair<Object, Object> pair : slots) {
                Enum head = (Enum) Reflex.invokeMethod(
                        Reflex.getMethod(enumItemSlotClass, "fromName", String.class),
                        null, "head");
                if (pair.getFirst() == head) {
                    contains = true;
                    break;
                }
            }
            if (slots == null || !contains) return;

            Integer entityId = (Integer) Reflex.getFieldValue(p, "b");
            if (entityId == null) return;

            Class craftServerClass = Reflex.getCraftClass("CraftServer");
            Class nmsEntityClass   = Reflex.getClass("net.minecraft.world.entity", "Entity");
            Class worldServerClass = Reflex.getClass("net.minecraft.server.level", "WorldServer");

            Object server    = craftServerClass.cast(Bukkit.getServer());
            Object nmsEntity = null;
            Object dedicatedServer = Reflex.invokeMethod(
                    Reflex.getMethod(craftServerClass, "getServer"),
                    server
            );

            Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                    Reflex.getMethod(dedicatedServer.getClass(), "getWorlds"),
                    dedicatedServer
            );

            Method getEntity = Reflex.getMethod(worldServerClass, "getEntity", int.class);
            for (Object worldServer : worlds) {
                nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
                if (nmsEntity != null) {
                    break;
                }
            }

            if (nmsEntity == null) return;


            Method getUniqueId = Reflex.getMethod(nmsEntityClass, "getUniqueID");

            Entity bukkitEntity = NexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));
            if (bukkitEntity == null || Hooks.isNPC(bukkitEntity) || !(bukkitEntity instanceof Player)) return;

            Player  player = (Player) bukkitEntity;
            RPGUser user   = plugin.getUserManager().getOrLoadUser(player);
            if (user == null) return;

            UserProfile profile = user.getActiveProfile();
            if (profile.isHideHelmet()) {
                Reflex.setFieldValue(p, "c", Reflex.getFieldValue(Reflex.getClass("net.minecraft.world.item", "ItemStack"), "a"));
            }
        });
    }

    @Override
    public void manageServerPacket(@NotNull EngineServerPacketEvent e) {
    }
}
