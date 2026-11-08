package com.idir.magiclives.client.nightmare;

import com.idir.magiclives.nightmare.NightmareReg;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrungClient {
    static final Logger LOG = LoggerFactory.getLogger((String)"magiclives-strung");
    static final ResourceLocation TEX = new ResourceLocation("magiclives", "textures/entity/strung_torso.png");
    static final ResourceLocation MASK = new ResourceLocation("magiclives", "textures/entity/strung_torso_mask.png");
    static final ResourceLocation GLOW = new ResourceLocation("magiclives", "textures/entity/strung_torso_glow.png");
    static boolean chasing;
    static int chaseId = -1;
    static int chaseTicks;
    static double chaseX, chaseZ;
    static float lockYaw, lockPitch;
    static float previousYaw, previousPitch;
    static CameraType previousCamera;
    static int black;
    static float alpha;
    static float alpha0;
    static int fadeT;
    static int fadeLen;
    static int primeLost;
    static int holdT;
    static int homeT;
    static boolean dream;
    static boolean shown;
    static boolean rumble;
    static double tx;
    static double tz;
    static int rumbleT;
    static int rumbleLen;
    static int lieT;
    static int ticks;
    static int nextThunder;
    static int nextBoom;
    static Loop wind;
    static Loop rumbleA;
    static Loop rumbleB;
    static final List<SoundInstance> SHOTS;
    static final float LOOP_CAP = 0.48f;
    static final float SHOT_CAP = 0.45f;
    static final float SHAKE_CAP = 0.75f;
    static final double SHELL = 20.0;
    static final double MIN_DIST = 3400.0;
    static final float TEXW = 512.0f;
    static final float TEXH = 1024.0f;
    static final float[][] ANCH;
    static final double HH = 2000.0;
    static final double HW = 1000.0;
    static final double TEXEL = 3.90625;
    static float[] HZ_TOP;

    public static void reset() {
        chasing = false;
        chaseId = -1;
        chaseTicks = 0;
        if (previousCamera != null) {
            Minecraft.m_91087_().f_91066_.m_92157_(previousCamera);
            previousCamera = null;
        }
        if (black != 4) {
            black = 0;
            alpha = 0.0f;
        }
        rumble = false;
        shown = false;
        dream = false;
        rumbleT = 0;
        StrungClient.stopLoops();
    }

    static void fullReset() {
        black = 0;
        alpha = 0.0f;
        StrungClient.reset();
        lieT = 0;
    }

    static void stopLoops() {
        for (Loop l : new Loop[]{wind, rumbleA, rumbleB}) {
            if (l == null) continue;
            l.kill();
        }
        rumbleB = null;
        rumbleA = null;
        wind = null;
        SoundManager sm = Minecraft.m_91087_().m_91106_();
        for (SoundInstance i : SHOTS) {
            sm.m_120399_(i);
        }
        SHOTS.clear();
    }

    public static void fx(int mode, double x, double z, int arg) {
        switch (mode) {
            case 20: {
                if (black != 0) break;
                black = 1;
                primeLost = 0;
                break;
            }
            case 21: {
                if (black != 1) break;
                black = 0;
                alpha = 0.0f;
                break;
            }
            case 22: {
                black = 2;
                alpha0 = alpha;
                fadeT = 0;
                fadeLen = Math.max(1, arg);
                break;
            }
            case 23: {
                black = 0;
                alpha = 0.0f;
                dream = true;
                SoundManager sm = Minecraft.m_91087_().m_91106_();
                if (wind != null) break;
                wind = new Loop(StrungClient.ev("minecraft", "item.elytra.flying"), 0.035f, 0.55f);
                sm.m_120367_((SoundInstance)wind);
                break;
            }
            case 24: {
                if (!shown) {
                    LOG.info("[strung] torso shown at {} {}", (Object)((int)x), (Object)((int)z));
                }
                shown = true;
                tx = x;
                tz = z;
                dream = true;
                break;
            }
            case 25: {
                if (!inStrung() || Minecraft.m_91087_().f_91074_ == null) break;
                if (!chasing) {
                    LocalPlayer player = Minecraft.m_91087_().f_91074_;
                    previousCamera = Minecraft.m_91087_().f_91066_.m_92176_();
                    lockYaw = previousYaw = player.m_146908_();
                    lockPitch = previousPitch = player.m_146909_();
                    chaseTicks = 0;
                    // Wind ends too: no low drone, thunder, heartbeat or rumble ramp.
                    stopLoops();
                    oneShot("magiclives", "nightmare.scream", 0.45f, 1.0f);
                }
                chasing = true;
                rumble = false;
                chaseId = arg;
                chaseX = x;
                chaseZ = z;
                break;
            }
            case 26: {
                LOG.info("[strung] wake cut at rumble {}%", (Object)((int)(StrungClient.progress() * 100.0f)));
                StrungClient.reset();
                black = 4;
                alpha = 1.0f;
                holdT = 0;
                homeT = 0;
                break;
            }
            case 27: {
                StrungClient.reset();
                lieT = arg;
            }
        }
    }

    static SoundEvent ev(String ns, String path) {
        return SoundEvent.m_262824_((ResourceLocation)new ResourceLocation(ns, path));
    }

    static void oneShot(String ns, String path, float vol, float pitch) {
        SimpleSoundInstance i = new SimpleSoundInstance(new ResourceLocation(ns, path), SoundSource.HOSTILE, Mth.m_14036_((float)vol, (float)0.0f, (float)0.45f), pitch, RandomSource.m_216327_(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        if (SHOTS.size() > 24) {
            SHOTS.remove(0);
        }
        SHOTS.add((SoundInstance)i);
        Minecraft.m_91087_().m_91106_().m_120367_((SoundInstance)i);
    }

    static float shakeAmp(float p) {
        float end = Math.max(0.0f, (p - 0.72f) / 0.28f);
        return 0.22f * Math.min(0.75f, 0.03f + 0.35f * p * p + 0.35f * end * end);
    }

    static float progress() {
        return rumble ? Mth.m_14036_((float)((float)rumbleT / (float)rumbleLen), (float)0.0f, (float)1.0f) : 0.0f;
    }

    static boolean inStrung() {
        ClientLevel l = Minecraft.m_91087_().f_91073_;
        return l != null && l.m_46472_().equals(NightmareReg.STRUNG);
    }

    static void tick() {
        Minecraft mc = Minecraft.m_91087_();
        if (dream && (mc.f_91074_ == null || !mc.f_91074_.m_6084_() || !inStrung())) {
            fullReset();
            return;
        }
        if (chasing && mc.f_91074_ != null) {
            ++chaseTicks;
            Entity beast = mc.f_91073_.m_6815_(chaseId);
            Vec3 target = beast == null ? new Vec3(chaseX, 65.1, chaseZ)
                    : beast.m_20182_().m_82520_(0, 1.1, 0);
            Vec3 to = target.m_82546_(mc.f_91074_.m_146892_());
            float yaw = (float)(Math.atan2(to.f_82481_, to.f_82479_) * 180.0 / Math.PI) - 90;
            float pitch = (float)(-Math.atan2(to.f_82480_, Math.hypot(to.f_82479_, to.f_82481_)) * 180.0 / Math.PI);
            previousYaw = lockYaw;
            previousPitch = lockPitch;
            lockYaw += Mth.m_14036_(Mth.m_14177_(yaw - lockYaw), -30, 30);
            lockPitch += Mth.m_14036_(pitch - lockPitch, -20, 20);
            mc.f_91074_.m_146922_(lockYaw);
            mc.f_91074_.m_146926_(lockPitch);
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        }
        ++ticks;
        if (lieT > 0) {
            --lieT;
        }
        if (black == 1) {
            LocalPlayer p = mc.f_91074_;
            if (p != null && p.m_5803_()) {
                alpha = Math.max(alpha, Mth.m_14036_((float)((float)(p.m_36318_() - 10) / 85.0f), (float)0.0f, (float)1.0f));
                primeLost = 0;
            } else if (++primeLost > 80) {
                black = 0;
                alpha = 0.0f;
            }
        } else if (black == 2) {
            alpha = Mth.m_14179_((float)Mth.m_14036_((float)((float)(++fadeT) / (float)fadeLen), (float)0.0f, (float)1.0f), (float)alpha0, (float)1.0f);
            if (fadeT >= fadeLen) {
                black = 3;
            }
        } else if (black == 3) {
            alpha = 1.0f;
        } else if (black == 4) {
            boolean home;
            alpha = 1.0f;
            boolean bl = home = mc.f_91073_ != null && !StrungClient.inStrung() && !(mc.f_91080_ instanceof ReceivingLevelScreen);
            homeT = home ? ++homeT : 0;
            if (homeT >= 12 || ++holdT > 120) {
                black = 0;
                alpha = 0.0f;
                LOG.info("[strung] home after {} ticks of black", (Object)holdT);
            }
        }
        if (mc.f_91073_ != null && StrungClient.inStrung()) {
            mc.f_91073_.m_46734_(0.0f);
            mc.f_91073_.m_46707_(0.0f);
        }
    }

    static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (!chasing || !inStrung() || Minecraft.m_91087_().f_91074_ == null) return;
        float partial = (float)e.getPartialTick();
        float yaw = previousYaw + (lockYaw - previousYaw) * partial;
        float pitch = previousPitch + (lockPitch - previousPitch) * partial;
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setRoll(0);
    }

    static void redSky(RenderLevelStageEvent e) {
        if (!chasing || !inStrung()) return;
        // Sky-only backdrop, drawn before terrain; no persistent world color mutation.
        Matrix4f m = e.getPoseStack().m_85850_().m_252922_();
        BufferBuilder bb = Tesselator.m_85913_().m_85915_();
        RenderSystem.setShader(GameRenderer::m_172811_);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        bb.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        float d = 18;
        float[][] v = {{-d,-d,-d},{d,-d,-d},{d,d,-d},{-d,d,-d},
                       {-d,-d,d},{d,-d,d},{d,d,d},{-d,d,d}};
        int[][] faces = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        for (int[] face : faces) for (int index : face) {
            float[] q = v[index];
            bb.m_252986_(m,q[0],q[1],q[2]).m_6122_(125,4,8,255).m_5752_();
        }
        BufferUploader.m_231202_(bb.m_231175_());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    static void render(RenderLevelStageEvent e) {
        float v1;
        float v0;
        int i;
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !shown || !StrungClient.inStrung()) {
            return;
        }
        redSky(e);
        Minecraft mc = Minecraft.m_91087_();
        Vec3 cam = e.getCamera().m_90583_();
        float t = (float)ticks + e.getPartialTick();
        float p = StrungClient.progress();
        Vec3 c = new Vec3(tx, 814.0, tz);
        double hd = Math.hypot(tx - cam.f_82479_, tz - cam.f_82481_);
        if (hd < 3400.0) {
            double sc = hd < 0.001 ? 0.0 : 3400.0 / hd;
            c = hd < 0.001 ? new Vec3(cam.f_82479_, c.f_82480_, cam.f_82481_ + 3400.0) : new Vec3(cam.f_82479_ + (tx - cam.f_82479_) * sc, c.f_82480_, cam.f_82481_ + (tz - cam.f_82481_) * sc);
        }
        double far = (double)Math.max(2, mc.f_91066_.m_193772_()) * 64.0;
        double dist = c.m_82546_(cam).m_82553_();
        double k = Math.min(20.0, far * 0.1) / Math.max(1.0, dist);
        double maxR = far * 0.85;
        Vec3 f = new Vec3(c.f_82479_ - cam.f_82479_, 0.0, c.f_82481_ - cam.f_82481_);
        if (f.m_82556_() < 1.0E-6) {
            f = new Vec3(0.0, 0.0, 1.0);
        }
        f = f.m_82541_();
        Vec3 right = new Vec3(-f.f_82481_, 0.0, f.f_82479_);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double sway = Math.toRadians(0.28) * Math.sin((double)t * 0.018) + Math.toRadians(0.08) * Math.sin((double)t * 0.047) + Math.toRadians(0.1) * (double)p * (double)p * Math.sin((double)t * 0.9);
        Vec3 r2 = right.m_82490_(Math.cos(sway)).m_82549_(up.m_82490_(Math.sin(sway)));
        Vec3 u2 = up.m_82490_(Math.cos(sway)).m_82546_(right.m_82490_(Math.sin(sway)));
        Vec3 pivot = c.m_82520_(0.0, 6000.0, 0.0);
        Vec3 cc = pivot.m_82546_(u2.m_82490_(6000.0));
        float[] fogc = RenderSystem.getShaderFogColor();
        float fr = fogc[0];
        float fg = fogc[1];
        float fb = fogc[2];
        Matrix4f m = e.getPoseStack().m_85850_().m_252922_();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        BufferBuilder bb = Tesselator.m_85913_().m_85915_();
        double edge = Math.max(32.0, (double)mc.f_91066_.m_193772_() * 16.0);
        double hdist = Math.hypot(c.f_82479_ - cam.f_82479_, c.f_82481_ - cam.f_82481_);
        double yCut = cam.f_82480_ - (cam.f_82480_ - 64.0) * hdist / edge;
        float vCut = (float)Mth.m_14008_((double)(0.5 - (yCut - cc.f_82480_) / 4000.0), (double)0.05, (double)1.0);
        Vec3 sky = mc.f_91073_.m_171660_(cam, e.getPartialTick());
        float[] hzTop = new float[]{0.47f, 0.35f, 0.42f};
        float[] hzLow = new float[]{0.74f, 0.4f, 0.26f};
        HZ_TOP = hzTop;
        int STRIPS = 20;
        RenderSystem.setShader(GameRenderer::m_172820_);
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)TEX);
        bb.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        float flick = StrungClient.filmFlicker(t);
        for (i = 0; i < 20; ++i) {
            v0 = vCut * (float)i / 20.0f;
            v1 = vCut * (float)(i + 1) / 20.0f;
            float l0 = flick * (1.0f - StrungClient.haze(v0 / vCut));
            float l1 = flick * (1.0f - StrungClient.haze(v1 / vCut));
            int[] c0 = StrungClient.lit(l0);
            int[] c1 = StrungClient.lit(l1);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 0.0, v1 * 1024.0f), 0.0f, v1, c1[0], c1[1], c1[2], 255);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 512.0, v1 * 1024.0f), 1.0f, v1, c1[0], c1[1], c1[2], 255);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 512.0, v0 * 1024.0f), 1.0f, v0, c0[0], c0[1], c0[2], 255);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 0.0, v0 * 1024.0f), 0.0f, v0, c0[0], c0[1], c0[2], 255);
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)bb.m_231175_());
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)MASK);
        RenderSystem.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE);
        bb.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        for (i = 0; i < 20; ++i) {
            v0 = vCut * (float)i / 20.0f;
            v1 = vCut * (float)(i + 1) / 20.0f;
            int a0 = (int)(255.0f * StrungClient.haze(v0 / vCut));
            int a1 = (int)(255.0f * StrungClient.haze(v1 / vCut));
            int[] h0 = StrungClient.hz(hzTop, hzLow, v0 / vCut);
            int[] h1 = StrungClient.hz(hzTop, hzLow, v1 / vCut);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 0.0, v1 * 1024.0f), 0.0f, v1, h1[0], h1[1], h1[2], a1);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 512.0, v1 * 1024.0f), 1.0f, v1, h1[0], h1[1], h1[2], a1);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 512.0, v0 * 1024.0f), 1.0f, v0, h0[0], h0[1], h0[2], a0);
            StrungClient.vt(bb, m, cam, k, StrungClient.tex(cc, r2, u2, 0.0, v0 * 1024.0f), 0.0f, v0, h0[0], h0[1], h0[2], a0);
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)bb.m_231175_());
        RenderSystem.defaultBlendFunc();
        StrungClient.strings(bb, m, cam, k, maxR, f, right, up, r2, u2, c, cc, t, p);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static int[] lit(float l) {
        l = 0.25f + 0.75f * l;
        return new int[]{Mth.m_14045_((int)((int)(255.0f * l * 0.62f)), (int)0, (int)255), Mth.m_14045_((int)((int)(255.0f * l * 0.58f)), (int)0, (int)255), Mth.m_14045_((int)((int)(255.0f * l * 0.66f)), (int)0, (int)255)};
    }

    static int[] hz(float[] top, float[] low, float f) {
        float w = (float)Math.pow(Mth.m_14036_((float)f, (float)0.0f, (float)1.0f), 1.3);
        return new int[]{(int)(255.0f * Mth.m_14179_((float)w, (float)top[0], (float)low[0])), (int)(255.0f * Mth.m_14179_((float)w, (float)top[1], (float)low[1])), (int)(255.0f * Mth.m_14179_((float)w, (float)top[2], (float)low[2]))};
    }

    static float filmFlicker(float t) {
        int b = (int)(t / 2.0f);
        float r = (float)(b * 1103515245 + 12345 >>> 16 & 0xFF) / 255.0f;
        return 0.95f + 0.025f * Mth.m_14031_((float)(t * 0.7f)) + 0.03f * r;
    }

    static float haze(float f) {
        return 0.5f + 0.4f * (float)Math.pow(Mth.m_14036_((float)f, (float)0.0f, (float)1.0f), 1.7);
    }

    static Vec3 tex(Vec3 cc, Vec3 r2, Vec3 u2, double x, double y) {
        return cc.m_82549_(r2.m_82490_((x / 512.0 - 0.5) * 2.0 * 1000.0)).m_82549_(u2.m_82490_((0.5 - y / 1024.0) * 2.0 * 2000.0));
    }

    static void strings(BufferBuilder bb, Matrix4f m, Vec3 cam, double k, double maxR, Vec3 f, Vec3 right, Vec3 up, Vec3 r2, Vec3 u2, Vec3 c, Vec3 cc, float t, float p) {
        RenderSystem.setShader(GameRenderer::m_172811_);
        bb.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        double BAR_UP = 56000.0;
        for (int i = 0; i < ANCH.length; ++i) {
            Vec3 a = StrungClient.tex(cc, r2, u2, ANCH[i][0], ANCH[i][1]);
            double lat = 1000.0 * (-0.62 + 1.24 * (double)i / (double)(ANCH.length - 1));
            Vec3 top = c.m_82549_(right.m_82490_(lat)).m_82549_(up.m_82490_(56000.0));
            StrungClient.wire(bb, m, cam, k, maxR, a, top, right, t, p, i);
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)bb.m_231175_());
    }

    static Vec3 rel(Vec3 cam, double k, Vec3 p) {
        return p.m_82546_(cam).m_82490_(k);
    }

    static void vt(BufferBuilder bb, Matrix4f m, Vec3 cam, double k, Vec3 p, float u, float v, int r, int g, int b, int a) {
        Vec3 q = StrungClient.rel(cam, k, p);
        bb.m_252986_(m, (float)q.f_82479_, (float)q.f_82480_, (float)q.f_82481_).m_7421_(u, v).m_6122_(r, g, b, a).m_5752_();
    }

    static void wire(BufferBuilder bb, Matrix4f m, Vec3 cam, double k, double maxR, Vec3 a, Vec3 top, Vec3 right, float t, float p, int seed) {
        int N = 48;
        double vib = 3.90625 * (0.6 + 4.0 * (double)p * (double)p);
        double ph = (double)seed * 1.9;
        Vec3 prev = null;
        double prevW = 0.0;
        float prevS = 0.0f;
        for (int i = 0; i <= 48; ++i) {
            float s = (float)i / 48.0f;
            double u = s * s;
            Vec3 w = a.m_82490_(1.0 - u).m_82549_(top.m_82490_(u)).m_82549_(right.m_82490_(Math.sin(Math.PI * u) * vib * Math.sin((double)t * (0.035 + 1.2 * (double)p) + ph)));
            Vec3 q = StrungClient.rel(cam, k, w);
            double ql = q.m_82553_();
            if (ql > maxR) {
                q = q.m_82490_(maxR / ql);
            }
            double half = q.m_82553_() * (0.0019 - 0.001 * (double)s);
            if (prev != null) {
                StrungClient.quadSeg(bb, m, prev, q, prevW, half, StrungClient.strCol(prevS), StrungClient.strCol(s));
            }
            prev = q;
            prevW = half;
            prevS = s;
        }
    }

    static int[] strCol(float s) {
        float e = Mth.m_14036_((float)((s - 0.12f) / 0.55f), (float)0.0f, (float)1.0f);
        float al = 0.55f * (1.0f - e * e * (3.0f - 2.0f * e)) * Mth.m_14036_((float)(s * 40.0f + 0.35f), (float)0.0f, (float)1.0f);
        float[] h = HZ_TOP;
        return new int[]{(int)(130.90001f + 255.0f * h[0] * 0.45f), (int)(126.5f + 255.0f * h[1] * 0.45f), (int)(122.100006f + 255.0f * h[2] * 0.45f), (int)(255.0f * al)};
    }

    static void quadSeg(BufferBuilder bb, Matrix4f m, Vec3 qa, Vec3 qb, double wa, double wb, int[] ca, int[] cb) {
        Vec3 side = qb.m_82546_(qa).m_82537_(qa.m_82549_(qb).m_82490_(0.5));
        if (side.m_82556_() < 1.0E-12) {
            return;
        }
        side = side.m_82541_();
        Vec3 sa = side.m_82490_(wa);
        Vec3 sb = side.m_82490_(wb);
        for (int h = -1; h <= 1; h += 2) {
            bb.m_252986_(m, (float)qa.f_82479_, (float)qa.f_82480_, (float)qa.f_82481_).m_6122_(ca[0], ca[1], ca[2], ca[3]).m_5752_();
            bb.m_252986_(m, (float)(qa.f_82479_ + (double)h * sa.f_82479_), (float)(qa.f_82480_ + (double)h * sa.f_82480_), (float)(qa.f_82481_ + (double)h * sa.f_82481_)).m_6122_(ca[0], ca[1], ca[2], 0).m_5752_();
            bb.m_252986_(m, (float)(qb.f_82479_ + (double)h * sb.f_82479_), (float)(qb.f_82480_ + (double)h * sb.f_82480_), (float)(qb.f_82481_ + (double)h * sb.f_82481_)).m_6122_(cb[0], cb[1], cb[2], 0).m_5752_();
            bb.m_252986_(m, (float)qb.f_82479_, (float)qb.f_82480_, (float)qb.f_82481_).m_6122_(cb[0], cb[1], cb[2], cb[3]).m_5752_();
        }
    }

    static void drawBlack(GuiGraphics g, int w, int h) {
        if (alpha <= 0.001f) {
            return;
        }
        int a = Mth.m_14045_((int)((int)(alpha * 255.0f)), (int)0, (int)255);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_(0.0f, 0.0f, 900.0f);
        g.m_280509_(0, 0, w, h, a << 24);
        g.m_280168_().m_85849_();
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterDimensionSpecialEffectsEvent e) -> e.register(new ResourceLocation("magiclives", "strung_dream"), new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NORMAL, false, false){

            public Vec3 m_5927_(Vec3 c, float b) {
                return c.m_82542_((double)(b * 0.92f + 0.08f), (double)(b * 0.8f + 0.05f), (double)(b * 0.7f + 0.06f));
            }

            public boolean m_5781_(int x, int z) {
                return false;
            }

            public void adjustLightmapColors(ClientLevel level, float pt, float skyDarken, float skyLight, float blockLight, int px, int py, Vector3f col) {
                float w = Mth.m_14036_((float)skyLight, (float)0.0f, (float)1.0f);
                col.set(col.x() * Mth.m_14179_((float)w, (float)1.0f, (float)1.04f), col.y() * Mth.m_14179_((float)w, (float)1.0f, (float)0.8f), col.z() * Mth.m_14179_((float)w, (float)1.0f, (float)0.6f));
                col.set(Mth.m_14036_((float)col.x(), (float)0.0f, (float)1.0f), Mth.m_14036_((float)col.y(), (float)0.0f, (float)1.0f), Mth.m_14036_((float)col.z(), (float)0.0f, (float)1.0f));
            }
        }));
        modBus.addListener((RegisterGuiOverlaysEvent e) -> e.registerAboveAll("strung_black", (gui, g, pt, w, h) -> StrungClient.drawBlack(g, w, h)));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> StrungClient.drawBlack(e.getGuiGraphics(), e.getScreen().f_96543_, e.getScreen().f_96544_));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                StrungClient.tick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener(StrungClient::render);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> StrungClient.fullReset());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, StrungClient::camera);
        MinecraftForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor e) -> {
            if (chasing && inStrung()) { e.setRed(0.49f); e.setGreen(0.016f); e.setBlue(0.031f); }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderGuiOverlayEvent.Pre e) -> {
            if ((lieT > 0 || black > 0) && e.getOverlay().id().equals((Object)VanillaGuiOverlay.SLEEP_FADE.id())) {
                e.setCanceled(true);
            }
            if (alpha > 0.98f && e.getOverlay().id().equals((Object)VanillaGuiOverlay.CHAT_PANEL.id())) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (lieT > 0 && e.getNewScreen() instanceof InBedChatScreen) {
                e.setCanceled(true);
            }
        });
    }

    static {
        SHOTS = new ArrayList<SoundInstance>();
        ANCH = new float[][]{{40.0f, 482.0f}, {118.0f, 456.0f}, {200.0f, 40.0f}, {300.0f, 72.0f}, {405.0f, 448.0f}, {474.0f, 468.0f}};
        HZ_TOP = new float[]{0.7f, 0.6f, 0.7f};
    }

    static final class Loop
    extends AbstractTickableSoundInstance {
        float target;

        Loop(SoundEvent e, float vol, float pitch) {
            super(e, SoundSource.HOSTILE, RandomSource.m_216327_());
            this.f_119578_ = true;
            this.f_119579_ = 0;
            this.f_119582_ = true;
            this.f_119580_ = SoundInstance.Attenuation.NONE;
            this.f_119577_ = 0.0;
            this.f_119576_ = 0.0;
            this.f_119575_ = 0.0;
            this.f_119573_ = vol;
            this.target = vol;
            this.f_119574_ = pitch;
        }

        public void m_7788_() {
            this.f_119573_ += (this.target - this.f_119573_) * 0.25f;
        }

        public boolean m_7784_() {
            return true;
        }

        void kill() {
            this.m_119609_();
        }

        float vol() {
            return this.f_119573_;
        }
    }
}

