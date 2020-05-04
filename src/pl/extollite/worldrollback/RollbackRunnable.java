package pl.extollite.worldrollback;

import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class RollbackRunnable implements Runnable {
    private int id;
    private int counter;
    private String world;
    private String name;

    RollbackRunnable(int counter, String world, String name){
        this.counter = counter;
        this.world = world;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    @Override
    public void run(){
        WorldRollback plugin = WorldRollback.getInstance();
        if(counter == 0){
            plugin.getServer().broadcastMessage(plugin.getPrefix()+ TextFormat.RED + plugin.getRollback().replace("%world_name%", name));
            rollback();
            plugin.getServer().getScheduler().cancelTask(id);
        }
        else if(counter == 1){
            plugin.getServer().broadcastMessage(plugin.getPrefix()+ TextFormat.RED + plugin.getOneSecond().replace("%counter%", String.valueOf(counter) ).replace("%world_name%", name));
            counter--;
        }
        else{
            plugin.getServer().broadcastMessage(plugin.getPrefix()+ TextFormat.RED + plugin.getFiveSeconds().replace("%counter%", String.valueOf(counter)).replace("%world_name%", name));
            counter--;
        }
    }

    private void rollback(){
        Config worlds = WorldRollback.getInstance().getWorlds();
        String name = worlds.getString("worlds."+world+".name");
        Level level = WorldRollback.getInstance().getServer().getLevelByName(name);
        /*WorldRollback.getInstance().getServer().*/
        WorldRollback.getInstance().getServer().getLevels().remove(level.getId());
        WorldRollback.getInstance().getServer().unloadLevel(level);
        File toDelete = new File(WorldRollback.getInstance().getServer().getDataPath()+"/worlds/"+name);
        byte[] buffer = new byte[1024];
        try {
            deleteDirectoryStream(Paths.get(toDelete.getAbsolutePath()));
            toDelete.mkdirs();
            String fileZip = toDelete.getAbsolutePath().replace(name, "")+worlds.getString("worlds."+world+".backup");
            extractFolder(fileZip, toDelete.getAbsolutePath()+"/");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Date toSet = new Date(System.currentTimeMillis());
        worlds.set("worlds."+world+".last-rollback", new SimpleDateFormat(WorldRollback.getFormat()).format(toSet));
        worlds.save();
        WorldRollback.getInstance().getServer().loadLevel(name);
        WorldRollback.getInstance().getServer().dispatchCommand(WorldRollback.getInstance().getServer().getConsoleSender(), "ap reload");
    }

    void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    static public void extractFolder(String zipFile, String newPath) throws ZipException, IOException
    {
        int BUFFER = 2048;
        File file = new File(zipFile);

        ZipFile zip = new ZipFile(file);

        new File(newPath).mkdir();
        Enumeration zipFileEntries = zip.entries();

        // Process each entry
        while (zipFileEntries.hasMoreElements())
        {
            // grab a zip file entry
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(newPath, currentEntry);
            //destFile = new File(newPath, destFile.getName());
            File destinationParent = destFile.getParentFile();

            // create the parent directory structure if needed
            destinationParent.mkdirs();

            if (!entry.isDirectory())
            {
                BufferedInputStream is = new BufferedInputStream(zip
                        .getInputStream(entry));
                int currentByte;
                // establish buffer for writing file
                byte data[] = new byte[BUFFER];

                // write the current file to disk
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos,
                        BUFFER);

                // read and write until last byte is encountered
                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
            }
        }
    }
}
