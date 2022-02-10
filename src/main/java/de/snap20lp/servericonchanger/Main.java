package de.snap20lp.servericonchanger;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private final File iconFolder = new File("plugins/ServerIconChanger/Icons");
    private int tempOrder = 0;
    private boolean isChanged = false;

    private String prefix = "§8[§eServerIconChanger§8] §8> ";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (!iconFolder.exists()) {
            iconFolder.mkdir();
        }
        Bukkit.getConsoleSender().sendMessage(prefix + "§aStarted ServerIconChanger!");
        Bukkit.getConsoleSender().sendMessage(prefix + "§eSwapOrder: " + getConfig().getString("swapMode"));
        Bukkit.getConsoleSender().sendMessage(prefix + "§eSwapTime: " + getConfig().getString("swapTime"));
        Bukkit.getConsoleSender().sendMessage(prefix + "§aCreated by Furkan.#4554");
        if (getConfig().getString("swapTime").contains("MILLIS_")) {
            int millis = Integer.parseInt(getConfig().getString("swapTime").split("_")[1]);
            millis = (millis / 1000) * 20;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                try {
                    forcePing();
                } catch (IOException e) {
                    e.printStackTrace();
                    Bukkit.getConsoleSender().sendMessage(prefix + "§cThere was an Error pinging the Server.");
                }
                isChanged = true;
            }, millis, millis);
        }
    }


    private void forcePing() throws IOException {
        try (Socket socket = new Socket(Bukkit.getIp(), Bukkit.getPort())) {
            socket.setSoTimeout(1000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int version = 47;
            int nextState = 1;
            int handshakePacketId = 0x00;
            PacketUtils.writeVarInt(out, PacketUtils.getVarIntSize(version) + PacketUtils.getStringSize(Bukkit.getIp()) + 2 + PacketUtils.getVarIntSize(nextState) + PacketUtils.getVarIntSize(handshakePacketId));
            PacketUtils.writeVarInt(out, handshakePacketId);
            PacketUtils.writeVarInt(out, version);
            PacketUtils.writeString(out, Bukkit.getIp());
            out.writeShort(Bukkit.getPort());
            PacketUtils.writeVarInt(out, nextState);
            int writePacketId = 0x00;
            PacketUtils.writeVarInt(out, PacketUtils.getVarIntSize(writePacketId));
            PacketUtils.writeVarInt(out, writePacketId);
            PacketUtils.readVarInt(in);
            int pingPacketId = 0x01;
            long time = System.currentTimeMillis();
            PacketUtils.writeVarInt(out, PacketUtils.getVarIntSize(pingPacketId) + 8);
            PacketUtils.writeVarInt(out, pingPacketId);
            out.writeLong(time);
            PacketUtils.readVarInt(in);
            in.readLong();
        }
    }

    @EventHandler
    public void on(ServerListPingEvent event) {
        if (getConfig().getString("swapTime").contains("MILLIS_")) {
            if (isChanged) {
                isChanged = false;
                changeIcon(event);
            }
        } else if (getConfig().getString("swapTime").equals("ONPING")) {
            changeIcon(event);
        } else {
            Bukkit.getConsoleSender().sendMessage(prefix + "§cThere is an invalid value at swapTime in Config : " + getConfig().getString("swapTime"));
        }
    }

    private void changeIcon(ServerListPingEvent event) {
        sendConsole("");
        sendConsole("§aPreparing to Change ServerIcon...");
        if (iconFolder.listFiles().length == 0) {
            Bukkit.getConsoleSender().sendMessage(prefix + "§cCant change ServerIcon there is no Icon in 'plugins/ServerIconChanger/Icons' §cPlease make Icons are saved in a .PNG file!");
            return;
        }
        File iconFile;
        if (getConfig().getString("swapMode").equals("RANDOM_MODE")) {
            iconFile = iconFolder.listFiles()[new Random().nextInt(iconFolder.listFiles().length)];
        } else if (getConfig().getString("swapMode").equals("INORDER_MODE")) {
            if (tempOrder == iconFolder.listFiles().length) {
                tempOrder = 0;
            }
            iconFile = iconFolder.listFiles()[tempOrder];
            tempOrder += 1;
        } else {
            Bukkit.getConsoleSender().sendMessage(prefix + "§cThe swapMode is not Valid : " + getConfig().getString("swapMode"));
            return;
        }

        if (!iconFile.getName().toLowerCase().endsWith(".png")) {
            Bukkit.getConsoleSender().sendMessage(prefix + "§cThis file is not a .PNG : " + iconFile.getName());
            return;
        }
        BufferedImage big;
        try {
            big = ImageIO.read(iconFile);
            int width = big.getWidth();
            int height = big.getHeight();
            if (width != 64 || height != 64) {
                Bukkit.getConsoleSender().sendMessage(prefix + "§cCant Load ServerIcon because the Image is not 64x64 : " + iconFile.getName());
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            event.setServerIcon(Bukkit.loadServerIcon(iconFile));
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage(prefix + "§cThere was an Error changing the ServerIcon.");
        }
        sendConsole("§aChanged ServerIcon to §e" + iconFile.getName());
    }

    private void sendConsole(String message) {
        if (getConfig().getBoolean("consoleOutput")) {
            Bukkit.getConsoleSender().sendMessage(prefix + message);
        }
    }

}
