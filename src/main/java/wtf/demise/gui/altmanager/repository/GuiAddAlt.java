package wtf.demise.gui.altmanager.repository;

import lombok.Getter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjglx.Sys;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.gui.altmanager.mslogin.MicrosoftAuthCallback;
import wtf.demise.gui.altmanager.repository.credential.AltCredential;
import wtf.demise.gui.altmanager.repository.credential.MicrosoftAltCredential;
import wtf.demise.gui.altmanager.utils.Checks;
import wtf.demise.gui.button.GuiCustomButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.render.shader.impl.MainMenu;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.RED;
import static wtf.demise.utils.misc.StringUtils.*;

public final class GuiAddAlt extends GuiScreen {

    private final AltRepositoryGUI gui;
    @Getter
    private GuiGroupAltLogin groupAltInfo;
    private final BiConsumer<GuiAddAlt, ? super AltCredential> consumer;

    private final String addAltButtonName;
    private final String title;

    private GuiTextField passwordField;
    private GuiTextField usernameField;
    private GuiButton fakerMode;
    private FakerMode currFMode = FakerMode.NORMAL;

    public GuiAddAlt(@NotNull AltRepositoryGUI gui, @NotNull String addAltButtonName, @NotNull String title, @NotNull BiConsumer<GuiAddAlt, ? super AltCredential> consumer) {
        this.gui = gui;
        this.addAltButtonName = addAltButtonName;
        this.title = title;
        this.consumer = consumer;
    }

    @Override
    public void initGui() {
        int height = this.height / 2 - 30;

        this.groupAltInfo = new GuiGroupAltLogin(this, title);

        List<GuiButton> buttonList = this.buttonList;
        buttonList.add(new GuiButton(4, width / 2 - 100, height + 72 + 12 - 72, "Microsoft"));
        buttonList.add(new GuiButton(3,width / 2 - 100, height + 72 + 12 - 48, "Generate"));
        buttonList.add(fakerMode = new GuiButton(5, width / 2 - 100, height + 72 - 12, "Faker: NORMAL"));
        buttonList.add(new GuiButton(0, width / 2 - 100, height + 72 + 12, addAltButtonName));
        buttonList.add(new GuiButton(1, width / 2 - 100, height + 72 + 36, "Back"));

        this.usernameField = new GuiTextField(0, mc.fontRendererObj, width / 2 - 100, height - 65, 200, 20);
        this.passwordField = new GuiTextField(1, mc.fontRendererObj, width / 2 - 100, height - 30, 200, 20);
        usernameField.setFocused(true);

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        GuiTextField usernameField = this.usernameField;
        GuiTextField passwordField = this.passwordField;

        usernameField.drawTextBox();
        passwordField.drawTextBox();

        groupAltInfo.drawGroup(mc, mouseX, mouseY);

        if (this.title != null) {
            Fonts.interSemiBold.get(22).drawString(this.title,
                    (int) ((gui.width - this.width) / 2F) + (this.width - Fonts.interSemiBold.get(22).getStringWidth(this.title)) / 2.0F,
                    15 + 5,
                    -1);
        }

        if (StringUtils.isBlank(usernameField.getText()) && !usernameField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Username / E-Mail", width / 2F - 96, this.height / 2 - 89, 0xFF888888);
        }

        if (StringUtils.isBlank(passwordField.getText()) && !passwordField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Password", width / 2F - 96, this.height / 2 - 54, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])$"
    );

    static boolean isEmail(@NotNull String email) {
        Checks.notBlank(email, "email");
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void add_login() {
        try {
            if (usernameField.getText().trim().isEmpty()) return;

            AltCredential altCredential = new AltCredential(usernameField.getText(), passwordField.getText());
            String login = altCredential.getLogin();

            if (altCredential.getPassword() == null) {
                if (!login.matches("^[a-zA-Z0-9_]+$")) {
                    groupAltInfo.updateStatus(RED + "Invalid characters in username");
                    return;
                }

                if (login.length() > 16) {
                    groupAltInfo.updateStatus(RED + "Username is too long");
                    return;
                }
            } else if (!isEmail(login)) {
                groupAltInfo.updateStatus(RED + "Invalid e-mail");
            }

            consumer.accept(this, altCredential);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                add_login();
                break;
            case 1:
                mc.displayGuiScreen(gui);
                break;
            case 4:
                final MicrosoftAuthCallback callback = new MicrosoftAuthCallback();

                CompletableFuture<MicrosoftAltCredential> future = callback.start((s, o) -> {

                    groupAltInfo.updateStatus(String.format(s, o[0]));
                });

                Sys.openURL(MicrosoftAuthCallback.url);

                future.whenCompleteAsync((o, t) -> {
                    if (t != null) {

                        groupAltInfo.updateStatus(t.getClass().getName() + ':' + t.getMessage());
                        t.printStackTrace();
                    } else {

                        groupAltInfo.updateStatus("Done");
                        consumer.accept(this, o);
                    }
                });
                break;
            case 3:
                //wtf.demise.utils.misc.StringUtils.randomString(wtf.demise.utils.misc.StringUtils.sb, 10)
                usernameField.setText(randomName(currFMode));
                passwordField.setText("");
                break;
            case 5:
                currFMode = FakerMode.values()[(currFMode.ordinal() + 1) % FakerMode.values().length];
                fakerMode.displayString = "Faker: " + currFMode.name();
                break;
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == 1) {
            mc.displayGuiScreen(gui);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            add_login();
            return;
        }

        switch (character) {
            case '\t':
                boolean passwordFieldFocused = passwordField.isFocused();
                boolean usernameFieldFocused = usernameField.isFocused();

                if (usernameFieldFocused && !passwordFieldFocused) {
                    usernameField.setFocused(false);
                    passwordField.setFocused(true);
                    return;
                }

                usernameField.setFocused(true);
                passwordField.setFocused(false);
                break;

            case '\r':
                mc.displayGuiScreen(gui);
                break;
        }

        usernameField.textboxKeyTyped(character, key);
        passwordField.textboxKeyTyped(character, key);
    }

    @Override
    public void mouseClicked(int x2, int y2, int button) {
        try {
            super.mouseClicked(x2, y2, button);
        } catch (Throwable t) {
            Demise.LOGGER.warn(t);
        }

        usernameField.mouseClicked(x2, y2, button);
        passwordField.mouseClicked(x2, y2, button);
    }

    private @Nullable AltCredential getDataFromClipboard() {
        String s = null;

        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                s = ((String) transferable.getTransferData(DataFlavor.stringFlavor)).trim();
            }
        } catch (Throwable ignored) {
        }

        if (s == null) {
            return null;
        }

        int index = s.indexOf(':');
        return index == -1
                ? s.endsWith("@alt.com") ? new AltCredential(s, null) : null
                : new AltCredential(s.substring(0, index), s.substring(index + 1));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}