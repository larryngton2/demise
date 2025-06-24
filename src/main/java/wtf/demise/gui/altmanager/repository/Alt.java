package wtf.demise.gui.altmanager.repository;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.altmanager.login.AltLoginThread;
import wtf.demise.gui.altmanager.login.AltType;
import wtf.demise.gui.altmanager.login.SessionUpdatingAltLoginListener;
import wtf.demise.gui.altmanager.mslogin.Auth;
import wtf.demise.gui.altmanager.repository.credential.AltCredential;
import wtf.demise.gui.altmanager.repository.credential.MicrosoftAltCredential;
import wtf.demise.gui.altmanager.utils.FakeEntityPlayer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN;

@Getter
@Setter
public class Alt {
    private final AltRepositoryGUI repository;
    private final AltCredential credential;
    private FakeEntityPlayer player;
    private boolean invalid;
    public static boolean shader;

    public Alt(@NotNull AltCredential credential, @NotNull FakeEntityPlayer player, @NotNull AltRepositoryGUI repository, boolean invalid) {
        this.repository = repository;
        this.credential = credential;
        this.player = player;
        this.invalid = invalid;
    }

    protected boolean mouseClicked(float width, float x, float y, int mouseX, int mouseY) {
        if (!MouseUtils.isHovered(x, y, width, 36, mouseX, mouseY)) return false;

        if (Minecraft.getSystemTime() - lastClickTime < 250L) {
            logIn();
        } else {
            select();
        }

        this.lastClickTime = Minecraft.getSystemTime();
        return true;
    }

    public void drawAlt(float width, float x, int y, int mouseX, int mouseY) {
        if (!shader) {
            RoundedUtils.drawRound(x, y, width, 36, 8, new Color(!isSelected() ? DEFAULT_COLOR : SELECTED_COLOR, true));

            if (triedAuthorizing() && alpha > 0) {
                RoundedUtils.drawRound(x, y, animationX, 36, 8, new Color((int) Math.max(0, alpha) << 24 | (isLoginSuccessful() ? SUCCESS_LOGIN_COLOR : FAILED_LOGIN_COLOR), true));
                renderAltBox(width, mouseX, mouseY);
            }

            drawSkull(player, y, x + 2);
            Fonts.interSemiBold.get(20).drawString((invalid ? EnumChatFormatting.STRIKETHROUGH : "") + player.getName(), x + 37, y + 3, TEXT_SELECTED_COLOR);

            Fonts.interSemiBold.get(12).drawString("Email: " + credential.getLogin(), x + 37, y + 17, TEXT_SELECTED_COLOR);

            String password = credential.getPassword();

            if (StringUtils.isNotBlank(password)) {
                Fonts.interSemiBold.get(12).drawString("Password: ", x + 37, y + 23, TEXT_SELECTED_COLOR);
                Fonts.interSemiBold.get(12).drawString(new String(new char[password.length()]).replace('\0', '*'), Fonts.interMedium.get(12).getStringWidth("Password: ") + x + 37, y + 25, TEXT_SELECTED_COLOR);
            }

            if (AltRepositoryGUI.getCurrentAlt() == this) {
                Fonts.interSemiBold.get(20).drawString("Logged", x + width - 45, y + 36 / 2F - 5, new Color(255, 255, 255, 50).getRGB());
            }
        } else {
            RoundedUtils.drawShaderRound(x, y, width, 36, 8, Color.black);

            if (triedAuthorizing() && alpha > 0) {
                RoundedUtils.drawShaderRound(x, y, animationX, 36, 8, new Color((int) Math.max(0, alpha) << 24 | (isLoginSuccessful() ? SUCCESS_LOGIN_COLOR : FAILED_LOGIN_COLOR), true));
                renderAltBox(width, mouseX, mouseY);
            }
        }
    }

    private void drawSkull(@NotNull FakeEntityPlayer player, int scrolled, float x) {
        RenderUtils.renderPlayerHead(player, x, scrolled + 2, 36 - 4, 12);
    }

    private final TimerUtils timer = new TimerUtils();
    private float alpha = 255;
    private float animationX = 0;

    private void renderAltBox(float width, int mouseX, int mouseY) {
        float altBoxAlphaStep = repository.getAltBoxAlphaStep();

        if (timer.hasTimeElapsed(UPDATE_MILLIS_DELAY) && alpha > 0) {
            this.alpha -= altBoxAlphaStep;
            timer.reset();
        }

        if (animationX < width) {
            this.animationX = Math.min(animationX + repository.getAltBoxAnimationStep(), width);
        }
    }

    private long lastTimeAlreadyLogged;

    public void logIn() {
        CompletableFuture<Session> sessionCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Session session = null;

            if (!isLoggingIn() && !isLoginSuccessful()) {
                setLoggingIn(true);

                if (credential instanceof MicrosoftAltCredential cast) {
                    try {

                        Map.Entry<String, String> authRefreshTokens = Auth.refreshToken(cast.getRefreshToken());
                        String xblToken = Auth.authXBL(authRefreshTokens.getKey());
                        Map.Entry<String, String> xstsTokenUserhash = Auth.authXSTS(xblToken);
                        String accessToken = Auth.authMinecraft(xstsTokenUserhash.getValue(), xstsTokenUserhash.getKey());

                        if (Alt.accountCheck(accessToken)) {
                            session = new Session(cast.getName(), cast.getUUID().toString(), accessToken, "mojang");

                            Minecraft.getMinecraft().session = session;

                            repository.getAlts().forEach(Alt::resetLogged);
                            AltRepositoryGUI.setCurrentAlt(Alt.this);
                            setGameProfile(session.getProfile());
                            setLoginProperty(true);
                            setInvalid(false);

                            Demise.INSTANCE.getNotificationManager().post(NotificationType.SUCCESS, "Logged in! " + Alt.this);
                        }
                    } catch (Throwable e) {
                        setLoginProperty(false);
                        setInvalid(true);
                        Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, e.getClass().getName() + ':' + e.getMessage());
                    }
                } else {
                    session = new AltLoginThread(credential, new SessionUpdatingAltLoginListener() {

                        @Override
                        public void onLoginSuccess(AltType type, Session session) {
                            super.onLoginSuccess(type, session);

                            repository.getAlts().forEach(Alt::resetLogged);
                            AltRepositoryGUI.setCurrentAlt(Alt.this);
                            setGameProfile(session.getProfile());
                            setLoginProperty(true);
                            setInvalid(false);

                            Demise.INSTANCE.getNotificationManager().post(NotificationType.SUCCESS, "Logged in! " + Alt.this);
                        }

                        @Override
                        public void onLoginFailed() {
                            setLoginProperty(false);
                            setInvalid(true);
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "Invalid credentials!");
                        }
                    }).run();
                }

                setLoggingIn(false);

                this.alpha = 255;
                this.animationX = 0;
            } else if (isLoggingIn()) {
                if (System.currentTimeMillis() > lastTimeAlreadyLogged + 150) {
                    Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Already trying logging in!");
                    this.lastTimeAlreadyLogged = System.currentTimeMillis();
                }
            } else if (isLoginSuccessful()) {
                if (System.currentTimeMillis() > lastTimeAlreadyLogged + 150) {
                    Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Already logged in!");
                    this.lastTimeAlreadyLogged = System.currentTimeMillis();
                }
            }

            return session;
        }, ForkJoinPool.commonPool());

        sessionCompletableFuture.whenCompleteAsync((session, throwable) -> {
            if (throwable != null) {
                Demise.LOGGER.warn("An error occurred while logging in!", throwable);
                return;
            }

            if (isLoginSuccessful() && session != null && session.getProfile() != null && session.getProfile()
                    .getId() != null) {

                try {
                    Demise.INSTANCE.getNotificationManager().post(NotificationType.SUCCESS, "Logged in! " + this);

                    repository.saveAlts();
                } catch (Throwable t) {
                    Demise.LOGGER.warn("An unexpected error occurred while loading Hypixel profile!", t);
                }
            }
        });
    }

    private static final byte SELECTED_POSITION = 0;
    private static final byte AUTHORIZED_POSITION = 1;
    private static final byte LOGGED_POSITION = 2;
    private static final byte LOGGING_IN_POSITION = 3;

    private byte state = 0b000;

    private void modifyState(byte pos, boolean b) {
        byte mask = (byte) (1 << pos);

        if (!b) {
            this.state = (byte) (state & ~mask);
        } else {
            this.state = (byte) (state & ~mask | 1 << pos & mask);
        }
    }

    private boolean state(byte pos) {
        byte mask = (byte) (1 << pos);
        return (state & mask) == mask;
    }

    public void resetLogged() {
        modifyState(AUTHORIZED_POSITION, false);
        modifyState(LOGGED_POSITION, false);
    }

    private void setLoginProperty(boolean b) {
        modifyState(AUTHORIZED_POSITION, true);
        modifyState(LOGGED_POSITION, b);
    }

    public boolean isLoginSuccessful() {
        return triedAuthorizing() && state(LOGGED_POSITION);
    }

    public boolean isLoginUnsuccessful() {
        return triedAuthorizing() && !state(LOGGED_POSITION);
    }

    public boolean triedAuthorizing() {
        return state(AUTHORIZED_POSITION);
    }

    void setSelectedProperty(boolean b) {
        modifyState(SELECTED_POSITION, b);
    }

    public boolean isSelected() {
        return state(SELECTED_POSITION);
    }

    public boolean isLoggingIn() {
        return state(LOGGING_IN_POSITION);
    }

    private void setLoggingIn(boolean b) {
        modifyState(LOGGING_IN_POSITION, b);
    }


    public void setGameProfile(@NotNull GameProfile gameProfile) {
        setupPlayer(gameProfile, null);

        Minecraft mc = Minecraft.getMinecraft();

        gameProfile.getProperties().clear();
        gameProfile.getProperties().putAll(mc.fillSessionProfileProperties());

        MinecraftProfileTexture profileTexture = mc.getSessionService().getTextures(gameProfile, false).get(SKIN);

        if (profileTexture != null) {
            mc.addScheduledTask(() -> mc.getSkinManager().loadSkin(profileTexture, SKIN, (type, skinLocation, texture) -> setupPlayer(gameProfile, skinLocation)));
        }
    }

    void setupPlayer(@NotNull GameProfile gameProfile, @Nullable ResourceLocation skinLocation) {
        Minecraft mc = Minecraft.getMinecraft();
        this.player = new FakeEntityPlayer(gameProfile, skinLocation);

        mc.getRenderManager().cacheActiveRenderInfo(player.worldObj, mc.fontRendererObj, player, player, mc.gameSettings, 0.0F);
    }

    long lastClickTime;

    public void select() {
        Alt selected = repository.getAlts().stream().filter(Alt::isSelected).findAny().orElse(null);
        if (selected != null) selected.setSelectedProperty(false);

        setSelectedProperty(true);
        repository.selectAlt(selected, this, null);
    }

    static final float FHD_ANIMATION_STEP = 5;
    private static final int UPDATES_PER_SECOND = 100;

    private static final int UPDATE_MILLIS_DELAY = 1_000 / UPDATES_PER_SECOND;

    private static final int DEFAULT_COLOR = Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor();
    private static final int SELECTED_COLOR = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor()).darker().getRGB();
    private static final int TEXT_SELECTED_COLOR = new Color(198, 198, 198).getRGB();
    private static final int SUCCESS_LOGIN_COLOR = 0x6E8D3D;
    private static final int FAILED_LOGIN_COLOR = 0x9E3939;

    @NotNull
    public static Alt fromNBT(AltRepositoryGUI gui, @NotNull NBTTagCompound tagCompound) {
        String login = tagCompound.getString("login");
        String password = tagCompound.getString("password", null);

        NBTTagCompound profileTag = tagCompound.getCompoundTag("profile", null);
        GameProfile profile = NBTUtil.readGameProfileFromNBT(profileTag);
        FakeEntityPlayer fakeEntityPlayer = new FakeEntityPlayer(Objects.requireNonNull(profile), null);

        boolean invalid = false;
        if (tagCompound.hasKey("invalid")) {
            invalid = tagCompound.getBoolean("invalid");
        }

        final AltCredential credential;

        if (tagCompound.hasKey("Microsoft") && tagCompound.getBoolean("Microsoft")) {
            credential = new MicrosoftAltCredential(tagCompound.getString("Name"), tagCompound.getString("RefreshToken"), UUID.fromString(tagCompound.getString("UUID")));
        } else {
            credential = new AltCredential(login, password);
        }

        return new Alt(credential, fakeEntityPlayer, gui, invalid);
    }

    public NBTBase asNBTCompound() {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setString("login", credential.getLogin());
        compound.setBoolean("invalid", invalid);
        if (credential.getPassword() != null) compound.setString("password", credential.getPassword());
        compound.setTag("profile", NBTUtil.writeGameProfile(new NBTTagCompound(), player.getGameProfile()));

        if (credential instanceof MicrosoftAltCredential cast) {

            compound.setBoolean("Microsoft", true);
            compound.setString("Name", cast.getName());
            compound.setString("UUID", cast.getUUID().toString());
            compound.setString("RefreshToken", cast.getRefreshToken());
        }

        return compound;
    }

    @Override
    public String toString() {
        return "Username: " + player.getGameProfile().getName();
    }

    public static boolean accountCheck(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("User-Agent", "MojangSharp/0.1");
        headers.put("Charset", "UTF-8");
        headers.put("connection", "keep-alive");

        try {
            String attributesRaw = HttpUtil.get(new URL("https://api.minecraftservices.com/player/attributes"), headers);
            JSONObject attributes = new JSONObject(attributesRaw);

            JSONObject privileges = attributes.getJSONObject("privileges");
            JSONObject multiPlayerServerPrivilege = privileges.getJSONObject("multiplayerServer");
            if (!multiPlayerServerPrivilege.getBoolean("enabled")) {
                Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "Oops, this player don't have privilege to play online server.");
                return false;
            }

            if (attributes.has("banStatus")) {
                JSONObject bannedScopes = attributes.getJSONObject("banStatus").getJSONObject("bannedScopes");
                if (bannedScopes.has("MULTIPLAYER")) {
                    JSONObject multiplayerBan = bannedScopes.getJSONObject("MULTIPLAYER");
                    if (!bannedScopes.has("expires") ||
                            multiplayerBan.get("expires") == null ||
                            multiplayerBan.getLong("expires") >= System.currentTimeMillis()) {
                        Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "Oops, this player got banned from mojang.");
                        return false;
                    }
                }
            }
        } catch (Throwable e) {
            Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "Failed to get player attributes.");
            e.printStackTrace();
        }

        return true;
    }
}
