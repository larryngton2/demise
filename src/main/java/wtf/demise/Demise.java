package wtf.demise;

import de.florianmichael.viamcp.ViaMCP;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglx.LWJGLUtil;
import org.lwjglx.opengl.Display;
import wtf.demise.events.EventManager;
import wtf.demise.features.command.CommandManager;
import wtf.demise.features.config.ConfigManager;
import wtf.demise.features.friend.FriendManager;
import wtf.demise.features.modules.ModuleManager;
import wtf.demise.gui.altmanager.repository.AltRepositoryGUI;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.ingame.CustomWidgets;
import wtf.demise.gui.notification.NotificationManager;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.gui.widget.WidgetManager;
import wtf.demise.utils.discord.DiscordInfo;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.packet.LagUtils;
import wtf.demise.utils.player.ClickHandler;
import wtf.demise.utils.player.rotation.BasicRotations;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Objects;

@Getter
public class Demise {
    public static final Logger LOGGER = LogManager.getLogger(Demise.class);
    public static final Demise INSTANCE = new Demise();
    public final String clientName = "demise";
    public final String version = "beta";
    public final String cloud = "https://larryngton2.github.io/demise-cloud/";

    private final File mainDir = new File(Minecraft.getMinecraft().mcDataDir, clientName);

    private EventManager eventManager;
    private NotificationManager notificationManager;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private WidgetManager widgetManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private PanelGui panelGui;
    private AltRepositoryGUI altRepositoryGUI;
    private DiscordInfo discordRP;

    // System Tray icon
    private TrayIcon trayIcon;

    // Start time tracking
    private int startTime;
    private long startTimeLong;

    // Load status
    private boolean loaded;

    private Path dataFolder;

    public void init() {
        loaded = false;

        setupMainDirectory();
        setupDisplayTitle();
        initializeManagers();
        registerEventHandlers();
        initializeStartTime();
        initializeViaMCP();
        setupDiscordRPC();
        setupSystemTray();
        handleFastRender();

        // linux multiplayer fix
        if (LWJGLUtil.getPlatform() == 1) {
            Minecraft.getMinecraft().gameSettings.useNativeTransport = false;
        }

        loaded = true;

        dataFolder = Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath()).resolve(clientName);
        LOGGER.info("{} {} initialized successfully.", clientName, version);

        SoundUtil.playSound("demise.boot");
    }

    private void setupMainDirectory() {
        if (!mainDir.exists()) {
            boolean dirCreated = mainDir.mkdir();
            if (dirCreated) {
                LOGGER.info("Created main directory at {}", mainDir.getAbsolutePath());
            } else {
                LOGGER.warn("Failed to create main directory at {}", mainDir.getAbsolutePath());
            }
            Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
        } else {
            LOGGER.info("Main directory already exists at {}", mainDir.getAbsolutePath());
        }

        this.dataFolder = Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath()).resolve(clientName);
    }

    private void setupDisplayTitle() {
        String title = String.format("%s %s", clientName, version);
        Display.setTitle(title);
        LOGGER.info("Display title set to: {}", title);
    }

    private void initializeManagers() {
        eventManager = new EventManager();
        notificationManager = new NotificationManager();
        moduleManager = new ModuleManager();
        widgetManager = new WidgetManager();
        configManager = new ConfigManager();
        commandManager = new CommandManager();
        friendManager = new FriendManager();
        panelGui = new PanelGui();
        altRepositoryGUI = new AltRepositoryGUI(this);
    }

    private void registerEventHandlers() {
        eventManager.register(new RotationUtils());
        eventManager.register(new LagUtils());
        eventManager.register(new BlinkComponent());
        eventManager.register(new SpoofSlotUtils());
        eventManager.register(new CustomWidgets());
        eventManager.register(new ClickHandler());
        eventManager.register(new MathUtils());
        eventManager.register(new RotationManager());
        eventManager.register(new BasicRotations());

        LOGGER.info("Event handlers registered.");
    }

    private void initializeStartTime() {
        startTime = (int) System.currentTimeMillis();
        startTimeLong = System.currentTimeMillis();
        LOGGER.info("Start time initialized: {} ms", startTime);
    }

    private void initializeViaMCP() {
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        LOGGER.info("ViaMCP initialized.");
    }

    private void setupDiscordRPC() {
        try {
            discordRP = new DiscordInfo();
            discordRP.init();
            LOGGER.info("Discord Rich Presence initialized.");
        } catch (Throwable throwable) {
            LOGGER.error("Failed to set up Discord RPC.", throwable);
        }
    }

    private void setupSystemTray() {
        if (isWindows() && SystemTray.isSupported()) {
            try {
                Image trayImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/minecraft/demise/img/logo.png")));
                trayIcon = new TrayIcon(trayImage, clientName);
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip(clientName);

                SystemTray.getSystemTray().add(trayIcon);
                trayIcon.displayMessage(clientName, "Client started successfully.", TrayIcon.MessageType.INFO);

                LOGGER.info("System tray icon added.");
            } catch (IOException | AWTException | NullPointerException e) {
                LOGGER.error("Failed to create or add TrayIcon.", e);
            }
        } else {
            LOGGER.warn("System tray not supported or not running on Windows.");
        }
    }

    private void handleFastRender() {
        if (Minecraft.getMinecraft().gameSettings.ofFastRender) {
            notificationManager.post(NotificationType.INFO, "Fast Rendering has been disabled", "due to compatibility issues");
            Minecraft.getMinecraft().gameSettings.ofFastRender = false;
            LOGGER.info("Fast Rendering was disabled due to compatibility issues.");
        }
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows");
    }

    public void onStop() {
        if (discordRP != null) {
            discordRP.stop();
            LOGGER.info("Discord Rich Presence stopped.");
        }
        configManager.saveConfigs();
        LOGGER.info("All configurations saved.");
    }

    public static class HWID {
        public static String getHWID() {
            try {
                String toEncrypt = System.getenv("COMPUTERNAME") + System.getProperty("user.name") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL");
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(toEncrypt.getBytes());
                StringBuilder hexString = new StringBuilder();

                byte[] byteData = md.digest();

                for (byte aByteData : byteData) {
                    String hex = Integer.toHexString(0xff & aByteData);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }

                return hexString.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }
}
