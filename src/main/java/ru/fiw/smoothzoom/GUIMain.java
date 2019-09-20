package ru.fiw.smoothzoom;

import java.io.IOException;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;

public class GUIMain extends GuiScreen {
    private GuiButton buttonStab;
    private GuiButton buttonMouse;
    private GuiButton buttonHideHand;
    private GuiButton buttonSmooth;
    private GuiSlider sliderSpeed;

    private Integer[] positionY;
    private int positionX;

    @Override
    public void initGui() {
        int buttonLength = 180;
        centerButtons(5, buttonLength);
        this.buttonList.add(this.sliderSpeed = new GuiSlider(1, positionX, positionY[0], buttonLength, 20,
                "Zoom speed: ", "", 1, 10, SmoothZoom.speed, false, true));
        this.buttonList.add(this.buttonMouse = new GuiButton(2, positionX, positionY[1], buttonLength, 20,
                "Zoom on mouse wheel: " + getTextBoolean(SmoothZoom.zoomOnWheel)));
        this.buttonList.add(this.buttonHideHand = new GuiButton(3, positionX, positionY[2], buttonLength, 20,
                "Hide hand: " + getTextBoolean(SmoothZoom.hideHand)));
        this.buttonList.add(this.buttonSmooth = new GuiButton(4, positionX, positionY[3], buttonLength, 20,
                "Smooth zoom: " + getTextBoolean(SmoothZoom.smooth)));
        this.buttonList.add(this.buttonStab = new GuiButton(5, positionX, positionY[4], buttonLength, 20,
                "Sensitivity stabilization: " + getTextBoolean(SmoothZoom.stab)));
    }

    private String getTextBoolean(boolean a) {
        if (a) {
            return ChatFormatting.DARK_GREEN + "enabled";
        } else {
            return ChatFormatting.RED + "disabled";
        }
    }

    public void centerButtons(int amount, int buttonLength) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new Integer[amount];
        int center = (this.height + amount * 24) / 2;
        int buttonStarts = center - (amount * 24);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (24 * i);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, ticks);
        if (this.sliderSpeed.dragging) {
            SmoothZoom.speed = this.sliderSpeed.getValueInt();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case 2:
            SmoothZoom.zoomOnWheel = !SmoothZoom.zoomOnWheel;
            button.displayString = "Zoom on mouse wheel: " + getTextBoolean(SmoothZoom.zoomOnWheel);
            break;
        case 3:
            SmoothZoom.hideHand = !SmoothZoom.hideHand;
            button.displayString = "Hide hand: " + getTextBoolean(SmoothZoom.hideHand);
            break;
        case 4:
            SmoothZoom.smooth = !SmoothZoom.smooth;
            button.displayString = "Smooth zoom: " + getTextBoolean(SmoothZoom.smooth);
            break;
        case 5:
            SmoothZoom.stab = !SmoothZoom.stab;
            button.displayString = "Sensitivity stabilization: " + getTextBoolean(SmoothZoom.stab);
            break;
        }
    }

    @Override
    public void onGuiClosed() {
        SmoothZoom.config.get("Options", "Zoom speed", 5).set(this.sliderSpeed.getValueInt());
        SmoothZoom.config.get("Options", "Zoom on mouse wheel", true).set(SmoothZoom.zoomOnWheel);
        SmoothZoom.config.get("Options", "Stabilize sensitivity", true).set(SmoothZoom.stab);
        SmoothZoom.config.get("Options", "Hide hand", true).set(SmoothZoom.hideHand);
        SmoothZoom.config.get("Options", "Smooth zoom", true).set(SmoothZoom.smooth);
        SmoothZoom.config.save();
    }
}
