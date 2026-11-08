package com.idir.magiclives.nightmare;

import com.idir.magiclives.nightmare.NightmareManager;
import com.idir.magiclives.nightmare.NightmareNet;
import com.idir.magiclives.nightmare.NightmareReg;
import com.idir.magiclives.nether.BeastEntity;
import com.idir.magiclives.nether.NetherReg;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class StrungDream {
    public static final int KIND = 2;
    public static final int GROUND = 64;
    public static final double TORSO_H = 4000.0;
    public static final double TORSO_BOTTOM = -1186.0;
    public static final double TORSO_CY = 814.0;
    public static final int FADE_SLEEP = 10;
    public static final int FADE_CMD = 30;
    public static final int HOLD = 40;
    public static final int QUIET_MIN = 80;
    public static final int QUIET_MAX = 140;
    public static final int SEEN_CAP = 900;
    public static final int RUMBLE = 260;
    public static final int LIE = 30;
    public static final double MIN_DIST = 3400.0;
    static final double SEEN_DOT = Math.cos(Math.toRadians(35.0));
    static final double DIST_MIN = 3800.0;
    static final double DIST_MAX = 4200.0;
    public static final Set<UUID> HELD = new HashSet<UUID>();

    static boolean start(ServerPlayer sp, int fade, BlockPos bed) {
        MinecraftServer srv = sp.m_20194_();
        ServerLevel dl = srv.m_129880_(NightmareReg.STRUNG);
        if (dl == null || NightmareManager.inNightmare((Player)sp) || !sp.m_6084_()) {
            return false;
        }
        if (sp.m_5803_()) {
            if (bed == null) {
                bed = sp.m_21257_().orElse(null);
            }
            sp.m_6145_(true, false);
        }
        int n = NightmareManager.nextSlot(srv);
        NightmareManager.Session s = new NightmareManager.Session(sp.m_20148_(), new BlockPos(8192 + n % 256 * 8192, 64, 8192 + n / 256 * 8192));
        s.kind = 2;
        s.phase = 4;
        s.fadeT = Math.max(1, fade);
        s.bed = bed;
        s.quietT = QUIET_MIN + sp.m_217043_().m_188503_(QUIET_MAX - QUIET_MIN + 1);
        s.spawn = new Vec3((double)s.origin.m_123341_() + 0.5, 64.0, (double)s.origin.m_123343_() + 0.5);
        NightmareManager.SESSIONS.put(sp.m_20148_(), s);
        NightmareNet.strung(sp, 22, 0.0, 0.0, s.fadeT);
        NightmareManager.LOG.info("[nightmare] START {} variant=strung origin={} fade={}t quiet={}s bed={}", new Object[]{sp.m_36316_().getName(), s.origin.m_123344_(), s.fadeT, s.quietT / 20, bed == null ? "-" : bed.m_123344_()});
        return true;
    }

    static void enter(ServerPlayer sp, NightmareManager.Session s) {
        ServerLevel dl = sp.m_20194_().m_129880_(NightmareReg.STRUNG);
        sp.m_8127_();
        sp.m_6915_();
        int items = NightmareManager.saveState(sp, 2, s.bed);
        if (sp.f_8941_.m_9290_() != GameType.SPECTATOR) {
            sp.m_143403_(GameType.ADVENTURE);
        }
        float yaw = -90.0f + (sp.m_217043_().m_188501_() - 0.5f) * 70.0f;
        sp.m_8999_(dl, s.spawn.f_82479_, s.spawn.f_82480_, s.spawn.f_82481_, yaw, 0.0f);
        NightmareManager.arrive(sp);
        sp.f_19789_ = 0.0f;
        sp.m_20256_(Vec3.f_82478_);
        sp.m_20095_();
        s.inside = true;
        NightmareManager.LOG.info("[nightmare] STRUNG enter {} items={} hp={} yaw={}", new Object[]{sp.m_36316_().getName(), items, Float.valueOf(sp.m_21223_()), String.format("%.0f", Float.valueOf(yaw))});
    }

    static Vec3 torso(NightmareManager.Session s) {
        return new Vec3(s.tx, 814.0, s.tz);
    }

    static Vec3 seenAt(NightmareManager.Session s, ServerPlayer sp) {
        double dz;
        double dx = s.tx - sp.m_20185_();
        double d = Math.hypot(dx, dz = s.tz - sp.m_20189_());
        if (d >= 3400.0 || d < 0.001) {
            return StrungDream.torso(s);
        }
        return new Vec3(sp.m_20185_() + dx * 3400.0 / d, 814.0, sp.m_20189_() + dz * 3400.0 / d);
    }

    static void tick(ServerPlayer sp, NightmareManager.Session s) {
        int t;
        ++s.st;
        if (!s.inside) {
            if (s.st >= s.fadeT) {
                StrungDream.enter(sp, s);
            }
            return;
        }
        if (!sp.m_9236_().m_46472_().equals(NightmareReg.STRUNG)) {
            NightmareManager.finish(sp, "left", false);
            return;
        }
        if (sp.m_20186_() < 56.0) {
            sp.m_8999_(sp.m_284548_(), sp.m_20185_(), 64.0, sp.m_20189_(), sp.m_146908_(), sp.m_146909_());
            sp.f_19789_ = 0.0f;
        }
        if ((t = s.st - s.fadeT) == 40) {
            NightmareNet.strung(sp, 23);
            NightmareManager.LOG.info("[nightmare] STRUNG cut to day for {}", (Object)sp.m_36316_().getName());
        }
        if (!s.revealed) {
            if (t >= 40 + s.quietT) {
                StrungDream.reveal(sp, s, t);
            }
            return;
        }
        if (t % 100 == 0) {
            NightmareNet.strung(sp, 24, s.tx, s.tz, 0);
        }
        if (HELD.contains(sp.m_20148_())) return;
        if (s.rumbleT < 0) {
            Vec3 eye = sp.m_146892_();
            Vec3 to = seenAt(s, sp).m_82546_(eye).m_82541_();
            double dot = sp.m_20252_(1.0f).m_82526_(to);
            // The torso is a sky-shell image, not geometry four thousand blocks away.
            // Trace to its rendered shell rather than loading distant terrain.
            Vec3 shell = eye.m_82549_(to.m_82490_(20.0));
            boolean clear = sp.m_284548_().m_45547_(new ClipContext(eye, shell,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp)).m_6662_() == HitResult.Type.MISS;
            s.seenT = dot > SEEN_DOT && clear ? s.seenT + 1 : 0;
            if (s.seenT >= 2) {
                s.rumbleT = 0;
                NightmareManager.LOG.info("[nightmare] STRUNG torso seen; chase in two seconds");
            }
            return;
        }
        if (s.hbRef == null) {
            if (++s.rumbleT >= 40) startChase(sp, s);
            return;
        }
        BeastEntity beast = (BeastEntity)s.hbRef;
        if (!beast.m_6084_()) {
            NightmareNet.strung(sp, 26);
            NightmareManager.finish(sp, "wake", true);
            return;
        }
        Vec3 before = beast.m_20182_();
        Vec3 delta = sp.m_20182_().m_82546_(before);
        double distance = delta.m_82553_();
        double speed = Math.max(1.05, sp.m_20184_().m_165925_() + 0.65);
        if (distance <= speed + 1.8) {
            NightmareNet.strung(sp, 26);
            NightmareManager.LOG.info("[nightmare] STRUNG crawler caught player; wake");
            NightmareManager.finish(sp, "wake", true);
            return;
        }
        Vec3 step = delta.m_82490_(speed / Math.max(0.001, distance));
        beast.m_6034_(before.f_82479_ + step.f_82479_, before.f_82480_ + step.f_82480_, before.f_82481_ + step.f_82481_);
        beast.m_20256_(Vec3.f_82478_);
        float yaw = (float)(Math.atan2(delta.f_82481_, delta.f_82479_) * 180.0 / Math.PI) - 90.0f;
        beast.m_146922_(yaw);
        beast.f_20883_ = yaw;
        beast.f_20885_ = yaw;
        beast.m_5616_(yaw);
        beast.dreamPose(sp, true, true);
    }

    static void startChase(ServerPlayer sp, NightmareManager.Session s) {
        ServerLevel level = sp.m_284548_();
        BeastEntity beast = (BeastEntity)((EntityType<?>)NetherReg.BEAST.get()).m_20615_(level);
        if (beast == null) {
            NightmareNet.strung(sp, 26);
            NightmareManager.finish(sp, "wake", true);
            return;
        }
        Vec3 back = sp.m_20252_(1.0f).m_82542_(-1.0, 0.0, -1.0).m_82541_();
        if (back.m_82556_() < 0.01) back = new Vec3(1, 0, 0);
        Vec3 at = sp.m_20182_().m_82549_(back.m_82490_(40.0));
        beast.m_21557_(true);
        beast.m_21530_();
        beast.m_20049_("ml_dream");
        beast.m_20049_("ml_manual");
        beast.m_20242_(true);
        beast.m_20331_(true);
        beast.m_20225_(true);
        beast.f_19794_ = true;
        beast.m_7678_(at.f_82479_, at.f_82480_, at.f_82481_, sp.m_146908_(), 0);
        beast.dreamPose(sp, true, true);
        // Reuse the existing session entity reference so all wake/logout/death paths discard it.
        s.hbRef = beast;
        s.herobrine = beast.m_20148_();
        if (!level.m_7967_(beast)) {
            NightmareNet.strung(sp, 26);
            NightmareManager.finish(sp, "wake", true);
            return;
        }
        NightmareNet.strung(sp, 25, at.f_82479_, at.f_82481_, beast.m_19879_());
        NightmareManager.LOG.info("[nightmare] STRUNG red-sky crawler starts");
    }

    static void reveal(ServerPlayer sp, NightmareManager.Session s, int t) {
        Vec3 v = sp.m_20252_(1.0f);
        Vec3 back = new Vec3(-v.f_82479_, 0.0, -v.f_82481_);
        if (back.m_82556_() < 1.0E-4) {
            back = new Vec3((double)Mth.m_14031_((float)(sp.m_146908_() * ((float)Math.PI / 180))), 0.0, (double)(-Mth.m_14089_((float)(sp.m_146908_() * ((float)Math.PI / 180)))));
        }
        back = back.m_82541_().m_82524_((sp.m_217043_().m_188501_() - 0.5f) * 0.6f);
        double d = 3800.0 + sp.m_217043_().m_188500_() * 400.0;
        s.tx = sp.m_20185_() + back.f_82479_ * d;
        s.tz = sp.m_20189_() + back.f_82481_ * d;
        s.revealed = true;
        s.revealT = t;
        NightmareNet.strung(sp, 24, s.tx, s.tz, 0);
        NightmareManager.LOG.info("[nightmare] STRUNG torso shown for {} at {} {} dist={} (behind them, dot={})", new Object[]{sp.m_36316_().getName(), String.format("%.0f", s.tx), String.format("%.0f", s.tz), String.format("%.0f", d), String.format("%.2f", v.m_82526_(StrungDream.torso(s).m_82546_(sp.m_146892_()).m_82541_()))});
    }

    static void skip(ServerPlayer sp, NightmareManager.Session s) {
        if (!s.inside) {
            return;
        }
        int t = s.st - s.fadeT;
        if (!s.revealed) {
            s.quietT = Math.max(0, t - 40);
            return;
        }
        if (s.rumbleT < 0) {
            s.rumbleT = 0;
        }
    }

    static String status(NightmareManager.Session s, ServerPlayer p) {
        int t = s.st - s.fadeT;
        String ph = !s.inside ? "fade" : (t < 40 ? "black" : (!s.revealed ? "quiet(" + (40 + s.quietT - t) / 20 + "s left)" : (s.rumbleT < 0 ? "shown" : (s.hbRef == null ? "seen-delay(" + s.rumbleT + "t)" : "crawler"))));
        String d = s.revealed && p != null ? String.format(" torsoDist=%.0f", Math.hypot(s.tx - p.m_20185_(), s.tz - p.m_20189_())) : "";
        return " phase=strung/" + ph + d;
    }
}

