package ru.fiw.smoothzoom;

import java.io.File;
import java.io.IOException;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Mod(modid = "smoothzoom", name = "Smooth Zoom", version = "1.0")
public class SmoothZoom {
    private long systemTime = getSystemTime();
    private Minecraft mc;
    private float origSens;

    private float sens = 0;
    private float fov = 0;
    private float fovPrev = 0;

    public static boolean stab = true;
    public static boolean zoomOnWheel = true;
    public static boolean hideHand = true;
    public static boolean smooth = true;
    public static int speed = 5;

    public static Configuration config;
    public static KeyBinding[] keyBindings = new KeyBinding[3];

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        String catergory = "Smooth Zoom";
        keyBindings[0] = new KeyBinding("Zoom -", Keyboard.KEY_V, catergory);
        keyBindings[1] = new KeyBinding("Zoom +", Keyboard.KEY_R, catergory);
        keyBindings[2] = new KeyBinding("Reset zoom", Keyboard.KEY_LMENU, catergory);
        for (int i = 0; i < keyBindings.length; ++i) {
            ClientRegistry.registerKeyBinding(keyBindings[i]);
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new SmoothZoomCommand());
        loadConfig();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        origSens = mc.gameSettings.mouseSensitivity;
    }

    @SubscribeEvent
    public void onFOVModifier(EntityViewRenderEvent.FOVModifier e) {
        if (e.getEntity().equals(mc.player)) {
            if (!isHandFov(Thread.currentThread().getStackTrace())) {
                if (smooth) {
                    e.setFOV((float) (fovPrev + ((fov - fovPrev) * e.getRenderPartialTicks())));
                } else {
                    e.setFOV(fov);
                }
            }
        }
    }

    private boolean isHandFov(StackTraceElement[] sts) {
        for (StackTraceElement st : sts) {
            if (st.getMethodName().equals("renderHand")) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) throws IOException {
        if (e.phase == Phase.START) {
            fovPrev = fov;
            if (mc.currentScreen == null && mc.player != null) {
                if (keyBindings[2].isPressed()) {
                    resetOpt();
                } else if (keyBindings[0].isKeyDown()) {
                    zoom(-1);
                } else if (keyBindings[1].isKeyDown()) {
                    zoom(1);
                }
                runTickMouse();
            }
        } else if (e.phase == Phase.END) {
            systemTime = getSystemTime();
        }
    }

    @SubscribeEvent
    public void guiOpen(GuiScreenEvent e) throws IOException {
        if (sens != mc.gameSettings.mouseSensitivity) {
            origSens = mc.gameSettings.mouseSensitivity;
        }
        resetOpt();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void hideHand(RenderHandEvent e) throws IOException {
        if (mc.gameSettings.fovSetting != fov && hideHand) {
            e.setCanceled(true);
        }
    }

    private void zoom(int sig) {
        if (mc.currentScreen != null) {
            return;
        }
        float mod = speed;
        float fov = this.fov;
        float newFov = fov - mod * sig;
        if (newFov < 2.0) {
            newFov = 2.0F;
        }

        if (newFov != fov) {
            if (newFov >= mc.gameSettings.fovSetting) {
                resetOpt();
            } else {
                this.fov = newFov;
                if (stab) {
                    float newSens = origSens * (newFov / mc.gameSettings.fovSetting);
                    mc.gameSettings.mouseSensitivity = newSens;
                    sens = newSens;
                }
            }
        }
    }

    private void loadConfig() {
        try {
            config = new Configuration(new File(Minecraft.getMinecraft().mcDataDir + "/config/SmoothZoom.cfg"));
            config.load();
            int speed = config.get("Options", "Zoom speed", 5).getInt();
            boolean hideHand = config.get("Options", "Hide hand", true).getBoolean();
            boolean smooth = config.get("Options", "Smooth zoom", true).getBoolean();
            boolean stab = config.get("Options", "Sensitivity stabilization", true).getBoolean();
            boolean zoomOnWheel = config.get("Options", "Zoom on mouse wheel", true).getBoolean();
            if (speed < 1 || speed > 10) {
                speed = 5;
            }
            this.stab = stab;
            this.speed = speed;
            this.smooth = smooth;
            this.hideHand = hideHand;
            this.zoomOnWheel = zoomOnWheel;
        } catch (Exception e) {
            System.out.println("Error loading config");
        } finally {
            config.save();
        }
    }

    private void resetOpt() {
        mc.gameSettings.mouseSensitivity = origSens;

        sens = mc.gameSettings.mouseSensitivity;
        fov = mc.gameSettings.fovSetting;
    }

    private static long getSystemTime() {
        return Sys.getTime() * 1000L / Sys.getTimerResolution();
    }

    private void runTickMouse() throws IOException {
        while (Mouse.next()) {
            if (net.minecraftforge.client.ForgeHooksClient.postMouseEvent()) {
                continue;
            }

            int i = Mouse.getEventButton();
            KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());

            if (Mouse.getEventButtonState()) {
                if (mc.player.isSpectator() && i == 2) {
                    mc.ingameGUI.getSpectatorGui().onMiddleClick();
                } else {
                    KeyBinding.onTick(i - 100);
                }
            }

            long j = getSystemTime() - this.systemTime;

            if (j <= 200L) {
                int k = Mouse.getEventDWheel();
                if (zoomOnWheel) {
                    zoom(Integer.signum(k));
                }

                if (k != 0) {
                    if (mc.player.isSpectator()) {
                        k = k < 0 ? -1 : 1;

                        if (mc.ingameGUI.getSpectatorGui().isMenuActive()) {
                            mc.ingameGUI.getSpectatorGui().onMouseScroll(-k);
                        } else {
                            float f = MathHelper.clamp(mc.player.capabilities.getFlySpeed() + k * 0.005F, 0.0F, 0.2F);
                            mc.player.capabilities.setFlySpeed(f);
                        }
                    } else {
                        if (!zoomOnWheel) {
                            mc.player.inventory.changeCurrentItem(k);
                        }
                    }
                }

                if (mc.currentScreen == null) {
                    if (!mc.inGameHasFocus && Mouse.getEventButtonState()) {
                        mc.setIngameFocus();
                    }
                } else if (mc.currentScreen != null) {
                    mc.currentScreen.handleMouseInput();
                }
            }
            net.minecraftforge.fml.common.FMLCommonHandler.instance().fireMouseInput();
        }
    }
}
