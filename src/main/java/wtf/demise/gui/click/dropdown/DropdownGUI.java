package wtf.demise.gui.click.dropdown;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.shader.Framebuffer;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.click.dropdown.configs.GuiConfig;
import wtf.demise.gui.click.dropdown.panel.CategoryPanel;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.Shadow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DropdownGUI extends GuiScreen {

    @Getter
    private final Animation openingAnimation = new EaseOutSine(400, 1);
    private boolean closing;
    private final List<CategoryPanel> panels = new ArrayList<>();
    private static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public DropdownGUI() {
        openingAnimation.setDirection(Direction.BACKWARDS);
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new CategoryPanel(category));
            float width = 0;
            for (CategoryPanel panel : panels) {
                panel.setX(50 + width);
                panel.setY(20);
                width += panel.getWidth() + 15;
            }
        }
    }

    @Override
    public void initGui() {
        closing = false;
        openingAnimation.setDirection(Direction.FORWARDS);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, 10, height - 30, 100, 20, "Configs"));
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == 0) {
            mc.displayGuiScreen(new GuiConfig());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mc.fontRendererObj.drawStringWithShadow(Minecraft.getDebugFPS() + "fps", 2, 2, -1);

        if (closing) {
            openingAnimation.setDirection(Direction.BACKWARDS);
            if (openingAnimation.finished(Direction.BACKWARDS)) {
                mc.displayGuiScreen(null);
            }
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
            CategoryPanel.shader = true;
            Blur.startBlur();
            panels.forEach(panel -> panel.drawScreen(mouseX, mouseY));
            Blur.endBlur(25, 1);
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
            CategoryPanel.shader = true;
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            panels.forEach(panel -> panel.drawScreen(mouseX, mouseY));
            stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);
        }

        CategoryPanel.shader = false;
        panels.forEach(panel -> panel.drawScreen(mouseX, mouseY));
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
            CategoryPanel.shader = true;
            panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, mouseButton));
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
            CategoryPanel.shader = true;
            panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, mouseButton));
        }

        CategoryPanel.shader = false;
        panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
            CategoryPanel.shader = true;
            panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, state));
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
            CategoryPanel.shader = true;
            panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, state));
        }

        CategoryPanel.shader = false;
        panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closing = true;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
