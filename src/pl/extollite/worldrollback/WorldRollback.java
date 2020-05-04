package pl.extollite.worldrollback;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorldRollback extends PluginBase {
    private static final String format = "yyyy-MM-dd HH:mm:ss Z";

    private static WorldRollback instance;

    private Config worlds;
    private Map<String, TaskHandler> worldTasks = new HashMap<>();
    private String prefix;
    private String fiveSeconds;
    private String oneSecond;
    private String rollback;
    private int warnBefore;

    static WorldRollback getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        instance = this;
        List<String> authors = this.getDescription().getAuthors();
        this.getLogger().info(TextFormat.DARK_GREEN + "Plugin by " + authors.get(0));
        parseConfig();
        parseWorlds();
    }

    private void parseConfig() {
        Config cfg = getConfig();
        prefix = cfg.getString("prefix");
        fiveSeconds = cfg.getString("5+seconds");
        oneSecond = cfg.getString("1second");
        rollback = cfg.getString("rollback");
        warnBefore = cfg.getInt("warnBefore");
    }

    private void parseWorlds(){
        worlds = new Config(this.getDataFolder()+"/worlds.yml", Config.YAML);
        for(String world : worlds.getSection("worlds").getKeys(false)){
            long duration = worlds.getInt("worlds."+world+".duration")*60*60*1000;
            Date now = new Date(System.currentTimeMillis());
            Date lastRollback;
            try {
                lastRollback = new SimpleDateFormat(format).parse(worlds.getString("worlds."+world+".last-rollback"));
            } catch (ParseException e) {
                lastRollback = now;
            }
            int period = (int) ((lastRollback.getTime() + duration - now.getTime())/50);
            if(period < 0)
                period = 1;
            String name = worlds.getString("worlds."+world+".name");
            worldTasks.put(name, this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, ()->{
                RollbackRunnable runnable = new RollbackRunnable(warnBefore, world, name);
                runnable.setId(this.getServer().getScheduler().scheduleRepeatingTask(WorldRollback.getInstance(), runnable, 20).getTaskId());
            }, period, (int) (duration/50)));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().toLowerCase().equals("rollbacknow")) {
            return true;
        }
        Player p = (Player)sender;
        if(!p.isOp() && !p.hasPermission("rollbacknow.command"))
            return true;
        for(String world : worlds.getSection("worlds").getKeys(false)){
            if(worlds.getString("worlds."+world+".name").equals(p.getLevel().getName())){
                worldTasks.get(p.getLevel().getName()).setNextRunTick(this.getServer().getTick()+worldTasks.get(p.getLevel().getName()).getPeriod());
                worldTasks.get(p.getLevel().getName()).run(this.getServer().getTick());
                p.sendMessage(prefix+"World will rollback now!");
                return true;
            }
        }
        return false;
    }

    public Config getWorlds() {
        return worlds;
    }

    public static String getFormat() {
        return format;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFiveSeconds() {
        return fiveSeconds;
    }

    public String getOneSecond() {
        return oneSecond;
    }

    public String getRollback() {
        return rollback;
    }
}
