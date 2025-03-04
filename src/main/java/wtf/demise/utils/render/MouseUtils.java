package wtf.demise.utils.render;

public class MouseUtils {
    public static boolean isHovered(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
