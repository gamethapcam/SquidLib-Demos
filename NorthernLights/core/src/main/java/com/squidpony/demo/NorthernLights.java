package com.squidpony.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.TimeUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class NorthernLights extends ApplicationAdapter {
    private static final float RATE = 0.75f;
    private int seed;
    private SpriteBatch batch;
    private Texture tiny;
    private long startTime;
    private int width, height;
    private float iw, ih;
    private final float[] con = new float[3];
    @Override
    public void create() {
        super.create();
        startTime = TimeUtils.millis();
        long state = TimeUtils.nanoTime() - startTime;
        // Sarong's DiverRNG.randomize()
        seed = (int)
                ((state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L) ^ state >>> 43 ^ state >>> 31 ^ state >>> 23) * 0xDB4F0B9175AE2165L) ^ state >>> 28);
        startTime -= seed >>> 16;
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch = new SpriteBatch();
        batch.disableBlending();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pm.drawPixel(0, 0, -1);
        tiny = new Texture(pm);
        width = 480;
        height = 320;
        iw = 1f / width;
        ih = 1f / height;
//        width = Gdx.graphics.getWidth();
//        height = Gdx.graphics.getHeight();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        this.width = width;
        this.height = height;
		iw = 1f / width;
		ih = 1f / height;
    }
//    public static float swayRandomized(int seed, float value)
//    {
//        final int floor = value >= 0f ? (int) value : (int) value - 1;
//        final float start = (((seed += floor * 0xD6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 10) * 0x4.ffffffp-24f,
//                end = (((seed += 0xD6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 10) * 0x4.ffffffp-24f;
//        value -= floor;
//        value *= value * (3f - 2f * value);
//        return ((1f - value) * start + value * end);
//    }
    public static float swayRandomized(int seed, float value) {
        final int floor = value >= 0f ? (int) value : (int) value - 1;
        final float start = (((seed += floor * 0x9E377) ^ 0xD1B54A35) * 0x1D2473 & 0x3FFFFF) * 0x3.FFFFFp-23f - 1f,
                end = ((seed + 0x9E377 ^ 0xD1B54A35) * 0x1D2473 & 0x3FFFFF) * 0x3.FFFFFp-23f - 1f;
        value -= floor;
        value *= value * (3f - 2f * value);
        return (1f - value) * start + value * end;
    }
        // cubic, not quintic like in SquidLib.
    public static float swayTight(float value)
    {
        int floor = (value >= 0f ? (int) value : (int) value - 1);
        value -= floor;
        floor &= 1;
        return value * value * (3f - 2f * value) * (-floor | 1) + floor;
//        return value * value * value * (value * (value * 6f - 15f) + 10f) * (-floor | 1) + floor;
    }

    /**
     * Based on CosmicNumbering in SquidLib.
     * @param c0 connection 0
     * @param c1 connection 1
     * @param c2 connection 2
     * @return a continuous noise-like value between -0.25f and 4.25f
     */
    private float cosmic(float c0, float c1, float c2)
    {
        final float sum = swayRandomized(seed, c0 + c1 + c2) * 1.5f;
//        float sum = swayRandomized(seed, c2 + c0);
//        sum += swayRandomized(~seed, sum + c0 + c1);
//        sum += swayRandomized(seed ^ 0x9E3779B9, sum + c1 + c2);
//        return sum + 0.5f + 2.5f * swayRandomized(seed ^ seed >>> 16, sum + c0 + c1 + c2);
        return sum + swayRandomized(-seed, sum * 0.5698402909980532f + 0.7548776662466927f * (c0 - c1 - c2));
    }

    private void cosmic(int seed, float[] con, float x, float y, float z)
    {
        con[0] += (x = swayRandomized(seed, x + z)) * 1.2f;
        con[1] += (y = swayRandomized(~seed, y + x)) * 1.2f;
        con[2] += (swayRandomized(seed ^ 0x9E3779B9, z + y)) * 1.2f;
    }

	@Override
    public void render() {
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
        final int tm = (int) TimeUtils.timeSinceMillis(startTime);
        final float rt = tm * RATE,
                ftm = rt * 0x5p-15f;
//                s0 = swayRandomized(0x9E3779B9, ftm - 1.11f) * 0x1p-6f,
//                c0 = swayRandomized(0xC13FA9A9, ftm - 1.11f) * 0x1p-6f, 
//                s1 = swayRandomized(0xD1B54A32, ftm + 1.41f) * 0x1p-6f,
//                c1 = swayRandomized(0xDB4F0B91, ftm + 1.41f) * 0x1p-6f, 
//                s2 = swayRandomized(0xE19B01AA, ftm + 2.61f) * 0x1p-6f,
//                c2 = swayRandomized(0xE60E2B72, ftm + 2.61f) * 0x1p-6f;

        
//        final float r0 = rt * 0x3.cac1p-13f;//swayRandomized(0x12345678, rt * 0x3.cac1p-13f);
//        final float r1 = rt * 0x4.e6e9p-13f;//swayRandomized(0x81234567, rt * 0x4.e6e9p-13f);
//        final float r2 = rt * 0x5.09fcp-13f;//swayRandomized(0x78123456, rt * 0x5.09fcp-13f);

        batch.begin();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
				float ax = x * 0.0075f, ay = y * 0.005f; // adjusted for starting dimensions
				con[0] = ftm + ay;
				con[1] = ftm + ax;
				con[2] = ax + ay;
				//conn0 = swayRandomized(-1052792407, yt - 1.11f) * ax + swayRandomized(-1640531527, xt - 3.11f) * ay + swayRandomized(924071052, -2.4375f - xy) * ftm;
				//conn1 = swayRandomized(-615576687, yt + 2.41f) * ax + swayRandomized(776648142, 1.41f - xt) * ay + swayRandomized(-566875093, xy + 1.5625f) * ftm;
				//conn2 = swayRandomized(435278990, 3.61f - yt) * ax + swayRandomized(-509935190, xt + 2.61f) * ay + swayRandomized(-284277664, xy + -3.8125f) * ftm;
                //conn0 = cosmic(conn0, conn1, conn2);
                //conn1 = cosmic(conn0, conn1, conn2);
                //conn2 = cosmic(conn0, conn1, conn2);
                cosmic(seed ^ 0xC13FA9A9, con, con[1], con[2], con[0]);
                cosmic(seed ^ 0xDB4F0B91, con, con[2], con[0], con[1]);
                cosmic(seed ^ 0x19F1D48E, con, con[0], con[1], con[2]);
                
//                zone  = cosmic(conn0, conn1, conn2);
//                conn0 = /*r0*/ + x * c0 - y * s0;
//                conn1 = /*r1*/ - x * c1 + y * s1;
//                conn2 = /*r2*/ + x * c2 + y * s2;
//
//                zone = 0f;//cosmic(x * 0x1p-7f, y * 0x1p-7f, ftm);
//                conn0 = cosmic(conn0, conn1, conn2) + zone;
//                conn1 = cosmic(conn0, conn1, conn2) + zone;
//                conn2 = cosmic(conn0, conn1, conn2) + zone;
                batch.setColor(swayTight(con[0]), swayTight(con[1]), swayTight(con[2]), 1f);
//                batch.setColor(lerpFloatColors(
//                        floatGet(swayTight(conn0), swayTight(conn1), swayTight(conn2))
//                        , floatGetHSV(swayTight(conn2), 1f, 1f), swayTight(0.5f - conn1))
//            );

//                conn0 = swayTight(conn0 + conn1 + conn2);
//                conn0 = swayTight(cosmic(conn0, conn1, conn2));
//                conn0 = swayTight(conn2 + zone);
//                batch.setColor(conn0, conn0, conn0, 1f);
                batch.draw(tiny, x, y);
            }
        }
        batch.end();
    }


    public static float floatGetHSV(float hue, float saturation, float value) {
        if (saturation <= 0.0039f) {
            return floatGet(value, value, value);
        } else if (value <= 0.0039f) {
            return NumberUtils.intBitsToFloat(0xFE000000);
        } else {
            final float h = ((hue + 6f) % 1f) * 6f;
            final int i = (int) h;
            value = MathUtils.clamp(value, 0f, 1f);
            saturation = MathUtils.clamp(saturation, 0f, 1f);
            final float a = value * (1 - saturation);
            final float b = value * (1 - saturation * (h - i));
            final float c = value * (1 - saturation * (1 - (h - i)));

            switch (i) {
                case 0:
                    return floatGet(value, c, a);
                case 1:
                    return floatGet(b, value, a);
                case 2:
                    return floatGet(a, value, c);
                case 3:
                    return floatGet(a, b, value);
                case 4:
                    return floatGet(c, a, value);
                default:
                    return floatGet(value, a, b);
            }
        }
    }
    public static float floatGet(float r, float g, float b) {
        return NumberUtils.intBitsToFloat(0xFE000000 | ((int) (b * 255) << 16)
                | ((int) (g * 255) << 8) | (int) (r * 255));
    }
    public static float lerpFloatColors(final float start, final float end, final float change) {
        final int s = NumberUtils.floatToIntBits(start), e = NumberUtils.floatToIntBits(end),
                rs = (s & 0xFF), gs = (s >>> 8) & 0xFF, bs = (s >>> 16) & 0xFF,
                re = (e & 0xFF), ge = (e >>> 8) & 0xFF, be = (e >>> 16) & 0xFF;
        return NumberUtils.intBitsToFloat(((int) (rs + change * (re - rs)) & 0xFF)
                | (((int) (gs + change * (ge - gs)) & 0xFF) << 8)
                | (((int) (bs + change * (be - bs)) & 0xFF) << 16)
                | 0xFE000000);
    }

}