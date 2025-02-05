package wtf.demise.gui.click.neverlose.panel.config;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.Demise;
import wtf.demise.features.config.Config;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;


import java.awt.*;

import static wtf.demise.gui.click.neverlose.NeverLose.*;

@Getter
public class ConfigRect extends Component {
    private final Config config;
    @Setter
    private float posX, posY, scroll;
    @Setter
    private boolean selected;
    private final Animation hover = new DecelerateAnimation(250,1);
    private final Animation select = new DecelerateAnimation(250,1);
    public ConfigRect(Config config) {
        this.config = config;
        setHeight(36);
        hover.setDirection(Direction.BACKWARDS);
        select.setDirection(Direction.BACKWARDS);
    }
    @Override
    public void drawScreen(int mouseX, int mouseY) {
        //coordinate
        float y = getPosY() + scroll;
        //anim
        hover.setDirection(MouseUtils.isHovered2(getPosX() + 290,y + 20,60,18,mouseX,mouseY) ? Direction.FORWARDS : Direction.BACKWARDS);
        select.setDirection(selected ? Direction.FORWARDS : Direction.BACKWARDS);
        //render
        String name = config.getName().replace(".json","") + (Demise.INSTANCE.getConfigManager().getCurrentConfig().equals(config.getName()) ? " (Current Config)" : "");
        RoundedUtils.drawRoundOutline(getPosX(),y + 10,358,getHeight(),4,0.1f,bgColor,new Color(ColorUtils.interpolateColor2(categoryBgColor, categoryBgColor.brighter().brighter(), (float) select.getOutput())));
        Fonts.interSemiBold.get(17).drawString(name,posX + 8,y + 17,-1);
        //button
        RoundedUtils.drawRoundOutline(getPosX() + 290,y + 20,60,18,2,0.1f, new Color(ColorUtils.interpolateColor2(categoryBgColor, categoryBgColor.brighter().brighter(), (float) hover.getOutput())), new Color(iconRGB));
        Fonts.neverlose.get(20).drawString("k",getPosX() + 296,y + 27,-1);
        Fonts.interSemiBold.get(16).drawString("Save",getPosX() + 302 + Fonts.neverlose.get(20).getStringWidth("k"),y + 27,-1);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered2(getPosX() + 290,getPosY() + scroll + 20,60,18,mouseX,mouseY) && mouseButton == 0) {
            config.saveConfig();
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }
    public int getMaxScroll() {
        return (int) (INSTANCE.getNeverLose().getPosY() + 80 + getHeight());
    }
    public boolean isHovered(int mouseX,int mouseY) {
        return MouseUtils.isHovered2(getPosX(),getPosY() + scroll + 10,358,getHeight(),mouseX,mouseY);
    }
}
