/*  Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.Starfish32RNG;
import squidpony.squidmath.StatefulRandomness;

import java.io.Serializable;

/**
 * A modification of Blackman and Vigna's xoroshiro64** generator; behaves like {@link Starfish32RNG}, but is designed
 * to make it less likely that users can reduce the quality by simple arithmetic on the result. Lobster (and Starfish)
 * are 2-dimensionally equidistributed, so it can return all long values except for one, while Lathe is 1-dimensionally
 * equidistributed so it can return all int values but not all longs. Lobster passes all 32TB of PractRand's statistical
 * tests, and does so with only one anomaly (considered "unusual") and no failures. In statistical testing,
 * xoroshiro128+ always fails some binary matrix rank tests, but that uses a pair of 64-bit states, and when the states
 * are reduced to 32-bits, these small-word versions fail other tests as well. Lobster uses a variant on xoroshiro64**
 * that reduces some issues with that generator and tries to "harden the defenses" on the generator's quality. Lobster
 * does not change xoroshiro's well-tested state transition, but it doesn't base the output on the sum of the two states
 * (like xoroshiro128+), instead using the first state only for output (exactly like xoroshiro64** and similar to
 * xoshiro256**). Any arithmetic it performs is safe for GWT. Lobster adds an extremely small amount of extra code to
 * xoroshiro, running xoroshiro's state transition as normal, using stateA (or s[0] in the original xoroshiro code)
 * multiplied by 31 as the initial result, then returning after a somewhat-unusual operation on that initial result that
 * isn't common in most RNGs. This last operation is {@code (result << 11) - Integer.rotateLeft(result, 5)}, and this is
 * one of a group of similar scrambling operations with varying effects on quality. It is a random reversible mapping, a
 * one-to-one operation from int to int, but it seems to be a challenge to actually reverse it without creating a lookup
 * table from all int inputs to their outputs. The period is identical to xoroshiro with two 32-bit states, at
 * 0xFFFFFFFFFFFFFFFF or 2 to the 64 minus 1. This generator is a little slower than xoroshiro64+ or Lathe, but has
 * better distribution than either; it can be contrasted with {@link Starfish32RNG} or {@link XoshiroAra32RNG} the
 * former of which is faster than this and the latter of which has a longer period, but both are fragile if users can
 * add integers of their choice.
 * <br>
 * This avoids an issue in xoroshiro** generators where many multipliers, when applied to the output of a xoroshiro**
 * generator, will cause the modified output to rapidly fail binary matrix rank tests. It also is immune to the "attack"
 * possible on Starfish and XoshiroAra, where the quality can be wrecked by subtracting a specific number or some number
 * similar to it. It's absolutely possible to make a table of inputs to outputs, run the scrambler through that table
 * (which would only take seconds, but would use 16GB of RAM), and multiply by the multiplicative inverse of 31 modulo
 * 2 to the 32, which would give you half of the state from one full output. It should be clear that this is not a
 * cryptographic generator, but I am not claiming this is a rock-solid or all-purpose generator either; if a hostile
 * user is trying to subvert a Lobster generator and can access full outputs, they can absolutely do so, but it's much
 * less likely that a non-hostile user could accidentally find an issue with this than with Starfish.
 * <br>
 * The name comes from the sea creature theme I'm using for this family of generators and the hard shell on a lobster.
 * <br>
 * <a href="http://xoshiro.di.unimi.it/xoroshiro64starstar.c">Original version here for xoroshiro64**</a>.
 * <br>
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 * Ported and modified in 2018 by Tommy Ettinger
 * @author Sebastiano Vigna
 * @author David Blackman
 * @author Tommy Ettinger (if there's a flaw, use SquidLib's or Sarong's issues and don't bother Vigna or Blackman, it's probably a mistake in SquidLib's implementation)
 */
public final class Lobster32RNG implements StatefulRandomness, Serializable {

    private static final long serialVersionUID = 1L;

    private int stateA, stateB;

    /**
     * Creates a new generator seeded using two calls to Math.random().
     */
    public Lobster32RNG() {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    /**
     * Constructs this Lathe32RNG by dispersing the bits of seed using {@link #setSeed(int)} across the two parts of state
     * this has.
     * @param seed an int that won't be used exactly, but will affect both components of state
     */
    public Lobster32RNG(final int seed) {
        setSeed(seed);
    }
    /**
     * Constructs this Lathe32RNG by splitting the given seed across the two parts of state this has with
     * {@link #setState(long)}.
     * @param seed a long that will be split across both components of state
     */
    public Lobster32RNG(final long seed) {
        setState(seed);
    }
    /**
     * Constructs this Lathe32RNG by calling {@link #setState(int, int)} on stateA and stateB as given; see that method
     * for the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public Lobster32RNG(final int stateA, final int stateB) {
        setState(stateA, stateB);
    }
    
    @Override
    public final int next(int bits) {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9); // a, b
        stateB = (s1 << 13 | s1 >>> 19); // c
        return (result << 11) - (result << 5 | result >>> 27) >>> (32 - bits);
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public final int nextInt() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9); // a, b
        stateB = (s1 << 13 | s1 >>> 19); // c
        return (result << 11) - (result << 5 | result >>> 27) | 0;
    }

    @Override
    public final long nextLong() {
        int s0 = stateA;
        int s1 = stateB ^ s0;
        final int high = s0 * 31;
        s0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        s1 = (s1 << 13 | s1 >>> 19) ^ s0;
        final int low = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        final long result = (high << 11) - (high << 5 | high >>> 27);
        return result << 32 ^ ((low << 11) - (low << 5 | low >>> 27));
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public Lobster32RNG copy() {
        return new Lobster32RNG(stateA, stateB);
    }

    /**
     * Sets the state of this generator using one int, running it through Zog32RNG's algorithm two times to get 
     * two ints. If the states would both be 0, state A is assigned 1 instead.
     * @param seed the int to use to produce this generator's state
     */
    public void setSeed(final int seed) {
        int z = seed + 0xC74EAD55 | 0, a = seed ^ z;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3 | 0;
        a ^= a >>> 15;
        stateA = (z ^ z >>> 20) + (a ^= a << 13) | 0;
        z = seed + 0x8E9D5AAA | 0;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3 | 0;
        a ^= a >>> 15;
        stateB = (z ^ z >>> 20) + (a ^ a << 13) | 0;
        if((stateA | stateB) == 0)
            stateA = 1;
    }

    public int getStateA()
    {
        return stateA;
    }
    /**
     * Sets the first part of the state to the given int. As a special case, if the parameter is 0 and stateB is
     * already 0, this will set stateA to 1 instead, since both states cannot be 0 at the same time. Usually, you
     * should use {@link #setState(int, int)} to set both states at once, but the result will be the same if you call
     * setStateA() and then setStateB() or if you call setStateB() and then setStateA().
     * @param stateA any int
     */

    public void setStateA(int stateA)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
    }
    public int getStateB()
    {
        return stateB;
    }

    /**
     * Sets the second part of the state to the given int. As a special case, if the parameter is 0 and stateA is
     * already 0, this will set stateA to 1 and stateB to 0, since both cannot be 0 at the same time. Usually, you
     * should use {@link #setState(int, int)} to set both states at once, but the result will be the same if you call
     * setStateA() and then setStateB() or if you call setStateB() and then setStateA().
     * @param stateB any int
     */
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
        if((stateB | stateA) == 0) stateA = 1;
    }

    /**
     * Sets the current internal state of this Lathe32RNG with three ints, where stateA and stateB can each be any int
     * unless they are both 0 (which will be treated as if stateA is 1 and stateB is 0).
     * @param stateA any int (if stateA and stateB are both 0, this will be treated as 1)
     * @param stateB any int
     */
    public void setState(int stateA, int stateB)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
        this.stateB = stateB;
    }

    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return (stateA & 0xFFFFFFFFL) | ((long)stateB) << 32;
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     *
     * @param state a 64-bit long. You should avoid passing 0; this implementation will treat it as 1.
     */
    @Override
    public void setState(long state) {
        stateA = state == 0 ? 1 : (int)(state & 0xFFFFFFFFL);
        stateB = (int)(state >>> 32);
    }

    @Override
    public String toString() {
        return "Lobster32RNG with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lobster32RNG lobster32RNG = (Lobster32RNG) o;

        if (stateA != lobster32RNG.stateA) return false;
        return stateB == lobster32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }
}
