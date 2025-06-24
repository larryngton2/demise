package wtf.demise.gui.altmanager.repository;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.altmanager.login.AltLoginThread;
import wtf.demise.gui.altmanager.login.AltType;
import wtf.demise.gui.altmanager.login.SessionUpdatingAltLoginListener;
import wtf.demise.gui.altmanager.mslogin.Auth;
import wtf.demise.gui.altmanager.repository.credential.AltCredential;
import wtf.demise.gui.altmanager.repository.credential.MicrosoftAltCredential;
import wtf.demise.gui.altmanager.utils.FakeEntityPlayer;
import wtf.demise.gui.mainmenu.GuiMainMenu;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.misc.FilteredArrayList;
import wtf.demise.utils.misc.HttpResponse;
import wtf.demise.utils.misc.HttpUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.MainMenu;
import wtf.demise.utils.render.shader.impl.Shadow;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static net.minecraft.util.EnumChatFormatting.GREEN;
import static net.minecraft.util.EnumChatFormatting.RED;
import static wtf.demise.features.modules.impl.visual.Shaders.stencilFramebuffer;

public class AltRepositoryGUI extends GuiScreen {

    @NonNull
    private final Demise demise;
    private final Logger logger = LogManager.getLogger();
    @Getter
    @Setter
    public static Alt currentAlt;
    private GuiTextField searchField;

    private final FilteredArrayList<Alt, Alt> alts = new FilteredArrayList<>(
            ObjectLists.synchronize(new ObjectArrayList<>()), alt -> {
        if (this.searchField == null || StringUtils.isBlank(this.searchField.getText())) return alt;
        if (alt == null) return null;

        String s;

        if (alt.getPlayer() != null && alt.getPlayer().getName() != null) {
            s = alt.getPlayer().getName();
        } else if (alt.getCredential() != null) {
            s = alt.getCredential().getLogin();
        } else {
            return null;
        }

        return s.toLowerCase().startsWith(this.searchField.getText().trim().toLowerCase()) ? alt : null;
    }, EnumSort.DATE::getComparator);

    @Getter
    private String tokenContent = "";

    public AltRepositoryGUI(@NonNull Demise demise) {
        this.demise = demise;
        loadAlts();
        if (StringUtils.isBlank(this.tokenContent)) {
            try {
                this.tokenContent = Strings.nullToEmpty(readApiKey());
            } catch (IOException e) {
                this.logger.error("An error occurred while reading data file", e);
                this.tokenContent = "";
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 69:
                StringSelection stringSelection = new StringSelection(mc.session.getUsername());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                break;
            case 0:
                mc.displayGuiScreen(
                        new GuiAddAlt(this, "Add Alt", "Add Alt", (gui, credentials) -> {
                            addAlt(credentials);
                            saveAlts();

                            mc.displayGuiScreen(this);
                        }));

                break;
            case 1:
                removeCurrentAlt();
                break;
            case 2:
                mc.displayGuiScreen(
                        new GuiAddAlt(this, "Login", "Alt Login", (gui, credentials) -> {
                            gui.getGroupAltInfo().updateStatus(GREEN + "Logging in...");

                            if (!(credentials instanceof MicrosoftAltCredential)) {
                                new AltLoginThread(credentials, new SessionUpdatingAltLoginListener() {

                                    @Override
                                    public void onLoginSuccess(AltType altType, Session session) {
                                        super.onLoginSuccess(altType, session);

                                        demise.getNotificationManager().post(NotificationType.INFO, "Logged in! Username: " + session.getUsername());
                                        mc.displayGuiScreen(AltRepositoryGUI.this);
                                    }

                                    @Override
                                    public void onLoginFailed() {
                                        gui.getGroupAltInfo().updateStatus(RED + "Invalid credentials!");
                                        mc.displayGuiScreen(AltRepositoryGUI.this);
                                    }
                                }).run();
                            } else {
                                try {
                                    MicrosoftAltCredential microsoftAltCredential = (MicrosoftAltCredential) credentials;
                                    Map.Entry<String, String> authRefreshTokens = Auth.refreshToken(microsoftAltCredential.getRefreshToken());
                                    String xblToken = Auth.authXBL(authRefreshTokens.getKey());
                                    Map.Entry<String, String> xstsTokenUserhash = Auth.authXSTS(xblToken);
                                    String accessToken = Auth.authMinecraft(xstsTokenUserhash.getValue(), xstsTokenUserhash.getKey());

                                    if (!Alt.accountCheck(accessToken))
                                        return;

                                    mc.session = new Session(microsoftAltCredential.getName(),
                                            microsoftAltCredential.getUUID().toString(),
                                            accessToken, "msa");
                                    mc.displayGuiScreen(AltRepositoryGUI.this);
                                    demise.getNotificationManager().post(NotificationType.SUCCESS, "Logged in! Username: " + microsoftAltCredential.getName());
                                } catch (Exception e) {
                                    demise.getNotificationManager().post(NotificationType.ERROR, "Failed to logged in");
                                }
                            }
                        }));
                break;
            case 3:
                refreshAlts();
                break;
            case 4:
                mc.displayGuiScreen(new GuiMainMenu());
                break;
            case 71:
                try {
                    JFileChooser jFileChooser = new JFileChooser() {
                        @Override
                        protected JDialog createDialog(Component parent) throws HeadlessException {
                            JDialog dialog = super.createDialog(parent);
                            dialog.setModal(true);
                            dialog.setAlwaysOnTop(true);
                            return dialog;
                        }
                    };
                    int returnVal = jFileChooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File skinFile = jFileChooser.getSelectedFile();
                        String url = "https://api.minecraftservices.com/minecraft/profile/skins";
                        Map<String, Object> keyValues = new HashMap<>();
                        Map<String, File> filePathMap = new HashMap<>();
                        Map<String, Object> headers = new HashMap<>();

                        if (!skinFile.getName().endsWith(".png")) {
                            demise.getNotificationManager().post(NotificationType.ERROR, "Its seems that the file isn't a skin..");
                            break;
                        }

                        int result = JOptionPane.showConfirmDialog((Component) null, "Is this a slim skin?", "alert", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (result == JOptionPane.CANCEL_OPTION) break;
                        String skinType;
                        if (result == JOptionPane.YES_OPTION) {
                            skinType = "slim";
                        } else {
                            skinType = "classic";
                        }

                        keyValues.put("variant", skinType);
                        filePathMap.put("file", skinFile);
                        headers.put("Accept", "*/*");
                        headers.put("Authorization", "Bearer " + mc.session.getToken());
                        headers.put("User-Agent", "MojangSharp/0.1");

                        HttpResponse response = HttpUtils.postFormData(url, filePathMap, keyValues, headers);
                        if (response.getCode() == 200 || response.getCode() == 204) {
                            demise.getNotificationManager().post(NotificationType.SUCCESS, "Skin changed!");
                        } else {
                            demise.getNotificationManager().post(NotificationType.ERROR, "Failed to change skin.");
                            logger.error(response);
                        }
                    }
                } catch (Exception e) {
                    demise.getNotificationManager().post(NotificationType.ERROR, "Failed to change skin.");
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiButton button : this.buttonList) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                this.selectedButton = button;

                button.playPressSound(mc.getSoundHandler());
                actionPerformed(button);
                return;
            }
        }

        int totalAlts = this.visibleAlts.size();
        int maxColumns = 4;
        int usedColumns = Math.min(maxColumns, totalAlts);
        int rows = (int) Math.ceil((double) totalAlts / (double) maxColumns);

        float totalGridWidth = usedColumns * 150 + (usedColumns - 1) * 3f;
        float totalGridHeight = rows * 36 + (rows - 1) * 3f;

        float startX = AltRepositoryGUI.width / 2f - totalGridWidth / 2f;
        float startY = AltRepositoryGUI.height / 2f - totalGridHeight / 2f;

        for (int i = 0; i < this.visibleAlts.size(); i++) {
            final Alt alt = this.visibleAlts.get(i);

            int column = i % maxColumns;
            int row = i / maxColumns;

            float x = startX + column * (150 + 3f);
            int y = (int) (startY + row * (36 + 3f));

            if (alt.mouseClicked(150, x, y, mouseX, mouseY)) {
                return;
            }
        }

        if (mouseX >= 3 && mouseX <= 3 + 9 && mouseY >= 40 && mouseY <= 40 + this.sliderHeight) {
            final float perAlt = (this.sliderHeight - 40) / this.alts.size();
            boolean b = mouseY >= 40 + perAlt * this.scrolled;

            if (b && mouseY <= Math.min(40 + perAlt * this.visibleAltsCount, this.sliderHeight) + 40 + perAlt * this.scrolled) {
                this.dragging = true;
            } else {
                setScrolledAndUpdate(this.scrolled + MathHelper.ceiling_double_int(this.alts.size() / 5.0D) * (b ? 1 : -1));
            }
        }

        this.searchField.mouseClicked(mouseX, mouseY, mouseButton);

    }

    @Getter
    private float altBoxAnimationStep, altBoxAlphaStep;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        final GuiTextField oldSearchField = this.searchField;
        this.altBoxAnimationStep = 500 / 564.0F * Alt.FHD_ANIMATION_STEP;
        this.altBoxAlphaStep = 0xFF / (200 / Alt.FHD_ANIMATION_STEP);
        this.searchField = new GuiTextField(0, mc.fontRendererObj, (width / 2) - 100, 5, 200, 20);

        if (oldSearchField != null) {
            this.searchField.setText(oldSearchField.getText());
        }

        this.sliderHeight = -40 + height + -DOWN_MARGIN;
        final int oldVisibleAltsCount = this.visibleAltsCount;
        this.visibleAltsCount = getVisibleAltsCount();

        if (oldVisibleAltsCount < this.visibleAltsCount && this.alts.size() - this.scrolled < this.visibleAltsCount) {
            setScrolledAndUpdate(this.alts.size() - this.visibleAltsCount);
        }

        updateVisibleAlts();

        float width = 75;
        float x = GuiScreen.width / 2 - (width / 2);
        float y = height - 25;

        this.buttonList.add(new GuiButton(0, x - (width * 2 + 10), y, width, 20, "Add"));
        this.buttonList.add(new GuiButton(1, x - (width + 5), y, width, 20, "Remove"));
        this.buttonList.add(new GuiButton(2, x, y, width, 20, "Direct Login"));
        this.buttonList.add(new GuiButton(69, x + (width + 5), y, width, 20, "Copy IGN"));
        this.buttonList.add(new GuiButton(71, x + (width * 2 + 10), y, width, 20, "Change Skin"));

        this.buttonList.add(new GuiButton(4, GuiScreen.width - width - 5, y, width, 20, "Back"));
        this.buttonList.add(new GuiButton(3, GuiScreen.width - width * 2 - 10, y, width, 20, "Refresh"));
    }

    private static final float DOWN_MARGIN = 5;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            GlStateManager.disableCull();

            MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-1f, -1f);
            GL11.glVertex2f(-1f, 1f);
            GL11.glVertex2f(1f, 1f);
            GL11.glVertex2f(1f, -1f);
            GL11.glEnd();
            GL20.glUseProgram(0);
            final int altsCount = this.alts.size();

            if (this.dragging) {
                int sliderValue = MathHelper
                        .clamp_int(mouseY - 40, 0, (int) this.sliderHeight - 40);
                final int altIndex = (int) (sliderValue / this.sliderHeight * this.alts.size());

                setScrolledAndUpdate(altIndex);
            }

            this.searchField.drawTextBox();

            if (StringUtils.isBlank(searchField.getText()) && !searchField.isFocused()) {
                mc.fontRendererObj.drawStringWithShadow("Search...", width / 2f - 94, 11, 0xFF888888);
            }

            if (!this.alts.isEmpty()) {
                if (altsCount > this.visibleAltsCount) scrollWithWheel(altsCount);

                if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
                    Alt.shader = true;
                    Blur.startBlur();
                    drawAlts(mouseX, mouseY);
                    Blur.endBlur(25, 1);
                }

                if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
                    Alt.shader = true;
                    stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
                    stencilFramebuffer.framebufferClear();
                    stencilFramebuffer.bindFramebuffer(true);
                    drawAlts(mouseX, mouseY);
                    stencilFramebuffer.unbindFramebuffer();
                    Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);
                }

                Alt.shader = false;
                drawAlts(mouseX, mouseY);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Throwable t) {
            this.logger.warn("scrolled: " + this.scrolled, t);
        }
    }

    private void drawAlts(int mouseX, int mouseY) {
        int totalAlts = this.visibleAlts.size();
        int maxColumns = 4;
        int usedColumns = Math.min(maxColumns, totalAlts);
        int rows = (int) Math.ceil((double) totalAlts / (double) maxColumns);

        float totalGridWidth = usedColumns * 150 + (usedColumns - 1) * 3f;
        float totalGridHeight = rows * 36 + (rows - 1) * 3f;

        float startX = AltRepositoryGUI.width / 2f - totalGridWidth / 2f;
        float startY = AltRepositoryGUI.height / 2f - totalGridHeight / 2f;

        for (int i = 0; i < totalAlts; i++) {
            final Alt alt = this.visibleAlts.get(i);

            int column = i % maxColumns;
            int row = i / maxColumns;

            float x = startX + column * (150 + 3f);
            int y = (int) (startY + row * (36 + 3f));

            alt.drawAlt(150, x, y, mouseX, mouseY);
        }
    }

    private int scrolled;
    private List<Alt> visibleAlts;
    private int visibleAltsCount;

    private float sliderHeight;
    private boolean dragging;

    private void setScrolledAndUpdate(int value) {
        setScrolled(value);
        updateVisibleAlts();
    }

    private void setScrolled(int value) {
        this.scrolled = MathHelper.clamp_int(value, 0, Math.max(0, this.alts.size() - this.visibleAltsCount));
    }

    private void updateVisibleAlts() {
        if (this.alts.size() - this.scrolled < this.visibleAltsCount) {
            setScrolled(this.alts.size() - this.visibleAltsCount);
        }

        final int size = this.alts.size();
        this.visibleAlts = this.alts.subList(MathHelper.clamp_int(this.scrolled, 0, size), MathHelper.clamp_int(this.scrolled + this.visibleAltsCount, 0, size));
    }

    private void scrollWithWheel(int altsCount) {
        final int mouse = Mouse.getDWheel();
        final int newValue;

        if (mouse == 0) {
            return;
        } else if (mouse < 0) {
            newValue = this.scrolled + 1;
        } else {
            newValue = this.scrolled - 1;
        }

        if (newValue >= 0 && newValue <= altsCount - this.visibleAltsCount) {
            setScrolledAndUpdate(newValue);
        }
    }

    private int getVisibleAltsCount() {
        final int columns = 4;
        final float rows = (height - 40) / (36 + 3f);
        return MathHelper.floor_float(rows) * columns;
    }

    @Nullable
    public String readApiKey() throws IOException {
        final Path dataPath = this.demise.getDataFolder().resolve("Data.txt");

        if (Files.notExists(dataPath)) {
            return null;
        }

        final List<String> lines = Files.readAllLines(dataPath);
        return !lines.isEmpty() ? this.tokenContent = lines.get(0) : null;
    }

    public Alt getSelectedAlt() {
        return this.alts.stream().filter(Alt::isSelected).findAny().orElse(null);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);

        saveAlts();

        try {
            Files.writeString(this.demise.getDataFolder().resolve("Data.txt"),
                    this.tokenContent, CREATE, TRUNCATE_EXISTING);
        } catch (Throwable t) {
            this.logger.error("Unable to reach clients folder", t);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isKeyComboCtrlV(keyCode)) {
            pasteAltsFromClipboard();
        } else if (isKeyComboCtrlC(keyCode)) {
            Alt selectedAlt = getSelectedAlt();

            if (selectedAlt == null) return;

            final String credential = selectedAlt.getCredential().toString();
            final StringSelection selection = new StringSelection(credential);

            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        } else {
            try {
                switch (keyCode) {
                    case Keyboard.KEY_NUMPAD8:
                    case Keyboard.KEY_UP: {
                        Alt previous = null;

                        for (int i = 0; i < this.alts.size(); i++) {
                            final Alt alt = this.alts.get(i);

                            if (alt.isSelected()) {
                                if (previous != null) selectAlt(alt, previous, i - 1);
                                return;
                            } else {
                                previous = alt;
                            }
                        }

                        break;
                    }

                    case Keyboard.KEY_NUMPAD2:
                    case Keyboard.KEY_DOWN:
                        final int size = this.alts.size();

                        for (int i = 0; i < size; i++) {
                            final Alt alt = this.alts.get(i);

                            if (alt.isSelected()) {
                                if (i + 1 < size) selectAlt(alt, this.alts.get(i + 1), i + 1);
                                return;
                            }
                        }

                        break;

                    case Keyboard.KEY_F5:
                        refreshAlts();
                        break;

                    case Keyboard.KEY_NUMPADENTER:
                    case Keyboard.KEY_RETURN: {
                        final Alt alt = getSelectedAlt();
                        if (alt != null) alt.logIn();

                        break;
                    }

                    case Keyboard.KEY_DELETE: {
                        removeCurrentAlt();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (this.searchField.textboxKeyTyped(typedChar, keyCode) && this.searchField.isEnabled) {
            this.alts.update();
            setScrolledAndUpdate(0);
        }

        super.keyTyped(typedChar, keyCode);
    }

    void selectAlt(@Nullable Alt oldValue, @NonNull Alt newValue, Integer newValueIndex) {
        if (newValueIndex == null) {
            if ((newValueIndex = this.alts.indexOf(newValue)) == -1) {
                return;
            }
        }

        if (oldValue != null) {
            oldValue.setSelectedProperty(false);
        }

        newValue.setSelectedProperty(true);

        if (newValueIndex < this.scrolled) {
            setScrolledAndUpdate(newValueIndex);
        } else if (newValueIndex >= this.scrolled + this.visibleAltsCount) {
            setScrolledAndUpdate(newValueIndex - this.visibleAltsCount + 1);
        }
    }

    private void refreshAlts() {
        loadAlts();
        setScrolledAndUpdate(0);
    }

    @SuppressWarnings("unchecked")
    private void pasteAltsFromClipboard() {
        try {
            final Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents == null) return;

            final Stream<String> stream;

            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                stream = Arrays.stream(((String) contents.getTransferData(DataFlavor.stringFlavor)).split("\n"));
            } else if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                stream = ((List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor)).stream().map(file -> {
                    try {
                        return Files.lines(file.toPath());
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).flatMap(s -> s);
            } else {
                return;
            }

            final Set<Object> seen = ConcurrentHashMap.newKeySet();

            stream.map(s -> {
                        if (!s.endsWith("@alt.com")) {
                            final int index = s.indexOf(':');
                            return index == -1 ? null : new String[]{s.substring(0, index), s.substring(index + 1)};
                        } else {
                            return new String[]{s, new String(new char[ThreadLocalRandom.current().nextInt(7) + 1]).replace(
                                    '\0', 'a')};
                        }
                    }).filter(Objects::nonNull).filter(alt -> !alt[0].trim().isEmpty() && !alt[1].trim().isEmpty())
                    .filter(t -> seen.add(t[0]))
                    .sorted(Comparator.comparing(o -> o[0])).forEach(alt -> addAlt(new AltCredential(alt[0], alt[1])));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeCurrentAlt() {
        Alt previous = null;

        for (Alt alt : this.alts) {
            if (alt.isSelected()) {
                removeAlt(alt);

                if (previous != null) previous.setSelectedProperty(true);
                return;
            } else {
                previous = alt;
            }
        }

        saveAlts();
    }

    public boolean hasAlt(@NonNull Alt credential) {
        return this.alts.getUnfiltered().stream()
                .anyMatch(alt -> alt.getPlayer().getName().equals(credential.getPlayer().getName()));
    }

    public void addAlt(@NonNull AltCredential credential) {
        final Alt alt;

        if (credential instanceof MicrosoftAltCredential m) {
            alt = new Alt(credential, new FakeEntityPlayer(new GameProfile(m.getUUID(), m.getName()), null), this, false);
        } else {
            final String login = credential.getLogin();
            final String name = GuiAddAlt.isEmail(login) ? "<Unknown Name>" : login;

            alt = new Alt(credential, new FakeEntityPlayer(new GameProfile(new UUID(0, 0), name), null), this, false);
        }

        if (!hasAlt(alt)) {
            this.alts.add(alt);
            updateVisibleAlts();

            alt.select();
        } else {
            demise.getNotificationManager().post(NotificationType.ERROR, "Account is already added!");
        }
    }

    public void removeAlt(@NonNull Alt alt) {
        if (this.alts.remove(alt)) {
            updateVisibleAlts();
        }
    }

    public void loadAlts() {
        this.alts.clear();

        try {
            final NBTTagCompound tagCompound = CompressedStreamTools
                    .read(new File(Demise.INSTANCE.getMainDir(), "alts.ml"));
            if (tagCompound == null) return;

            final NBTTagList altListTag = tagCompound.getTagList("alts", 10);

            for (int i = 0; i < altListTag.tagCount(); ++i) {
                final Alt alt;

                try {
                    alt = Alt.fromNBT(this, altListTag.getCompoundTagAt(i));
                } catch (Throwable t) {
                    this.logger.error("Failed to parse account: {}", altListTag.getCompoundTagAt(i).toString(), t);
                    continue;
                }

                this.alts.add(alt);
            }
        } catch (Exception e) {
            this.logger.error("Couldn't load alt list", e);
        }

        updateVisibleAlts();
    }

    public void saveAlts() {
        try {
            final NBTTagList tagList = new NBTTagList();

            for (Alt alt : this.alts.getUnfiltered()) {
                tagList.appendTag(alt.asNBTCompound());
            }

            final NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("alts", tagList);

            CompressedStreamTools.safeWrite(tagCompound, new File(Demise.INSTANCE.getMainDir(), "alts.ml"));
        } catch (Exception e) {
            this.logger.error("Couldn't save alt list", e);
        }
    }

    private enum EnumSort {
        DATE("Date", (o1, o2) -> 0);
        private final String criteria;
        @Getter
        private final Comparator<Alt> comparator;

        EnumSort(String criteria, Comparator<Alt> comparator) {
            this.criteria = criteria;
            this.comparator = comparator;
        }
    }

    public List<Alt> getAlts() {
        return (List<Alt>) this.alts.getUnfiltered();
    }
}
