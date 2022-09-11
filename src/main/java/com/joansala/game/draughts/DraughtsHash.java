package com.joansala.game.draughts;

/*
 * Copyright (c) 2022 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.joansala.util.hash.HashFunction;
import com.joansala.util.hash.BinomialHash;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.draughts.Draughts.*;


/**
 * Perfect near-minimal hash function for Draughts.
 *
 * This method can hash all positions containing {@code MAX_PIECES}
 * or less, and each hash can be converted back to a bitboards array.
 */
public class DraughtsHash implements HashFunction {

    /** Maximum number of pieces on the board */
    public static final int MAX_PIECES = 5;


    /**
     * Number of possible positions that contain a number of
     * pieces equal or less than {@code count}.
     *
     * @param count     Number of pieces on the board
     * @return          Population count
     */
    public long offset(int count) {
        return OFFSETS[count];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long hash(Object state) {
        return hash((long[]) state);
    }


    /**
     * Compute the hash of a position array.
     *
     * @param state     An array
     * @return          Hash value
     */
    public long hash(long[] state) {
        return computeHash(mapState(state));
    }


    /**
     * Convert a hash into its array representation.
     *
     * @param hash      Hash value
     * @return          A new array
     */
    public long[] unhash(long hash) {
        return unmapState(computeState(hash));
    }


    /**
     * Compute a unique hash for a game state.
     *
     * @param state     Game state array
     * @return          Unique hash code
     */
    private static long computeHash(long[] state) {
        long hash = 0L;

        final long taken = taken(state);
        final int count = count(taken);
        final BinomialHash hasher = HASHERS[count - 1];

        // Compute a hash that maps each piece on the board in
        // order, from checker zero to fifty-nine.

        for (int piece = SOUTH_MAN; piece <= NORTH_KING; piece++) {
            long pieces = state[piece];

            while (empty(pieces) == false) {
                final int checker = first(pieces);
                final long mask = (1L << checker) - 1;
                final int index = count(taken & mask);

                hash += piece << (index << 1);
                pieces ^= bit(checker);
            }
        }

        // Obtain the gaps between pieces, which tells us where
        // each piece is placed on the board

        final int[] gaps = new int[1 + count];
        long pieces = taken;
        int index = 0;

        gaps[count] = BOARD_SIZE;

        while (empty(pieces) == false) {
            final int gap = first(pieces);

            pieces = pieces >>> (1 + gap);
            gaps[index++] = gap;
            gaps[count] -= gap;
        }

        // Combine the component into a unique hash

        hash += OFFSETS[count - 1];
        hash += hasher.hash(gaps) << (count << 1);

        return hash;
    }


    /**
     * Converts a hash code to the game state it represents.
     *
     * @param hash      Unique hash code
     * @return          Game state array
     */
    private static long[] computeState(long hash) {
        final long[] state = new long[PIECE_COUNT];

        final int count = pieceCount(hash);
        final int slots = 1 << (count << 1);
        final BinomialHash hasher = HASHERS[count - 1];

        // Convert hash to an array of gaps between pieces

        hash -= OFFSETS[count - 1];
        int[] pieces = new int[count];
        int[] gaps = hasher.unhash(hash / slots);

        // Obtain the pieces placed on the board in order,
        // from checker zero to forty-nine.

        hash = hash & (slots - 1);

        for (int i = count - 1; i >= 0; i--) {
            final int base = 1 << (i << 1);

            pieces[i] = (int) hash / base;
            hash = hash & (base - 1);
        }

        // Place each piece on the corresponding checker

        int previous = 0;

        for (int i = 0; i < count; i++) {
            final int gap = gaps[i];
            final int piece = pieces[i];
            final int checker = previous + gap;

            state[piece] |= (1L << checker);
            previous += (1 + gap);
        }

        return state;
    }


    /**
     * Obtains a bitboard of occupied checkers.
     *
     * @param state     Position array
     * @return          A bitboard
     */
    private static long taken(long[] state) {
        return (
            state[SOUTH_MAN] | state[SOUTH_KING] |
            state[NORTH_MAN] | state[NORTH_KING]
        );
    }


    /**
     * Number of pieces on the position represented by a hash.
     *
     * @param hash      A hash value
     * @return          Number of pieces
     */
    private static int pieceCount(long hash) {
        for (int count = 1; count <= MAX_PIECES; count++) {
            if (hash < OFFSETS[count]) {
                return count;
            }
        }

        return -1;
    }


    /**
     * Removes the padding bits from the position representation.
     *
     * A draughts state is represented with one extra bit on positions
     * 6, 16, 27, 38 and 49 of the bitboards because that simplifies the
     * operations performed on them. This method removes those extra bits.
     *
     * @param position      Draughts state array
     * @return              A new state array
     */
    private static long[] mapState(long[] position) {
        long[] state = new long[position.length];

        for (int piece = 0; piece < state.length; piece++) {
            long bits = position[piece];

            if (empty(bits) == false) {
                bits = remove(bits, 5);
                bits = remove(bits, 15);
                bits = remove(bits, 25);
                bits = remove(bits, 35);
                bits = remove(bits, 45);
                state[piece] = bits;
            }
        }

        return state;
    }


    /**
     * Adds the padding bits to the position representation.
     * {@see #mapState}
     *
     * @param position      Draughts state array
     * @return              A new state array
     */
    private static long[] unmapState(long[] state) {
        long[] position = new long[state.length];

        for (int piece = 0; piece < state.length; piece++) {
            long bits = state[piece];

            if (empty(bits) == false) {
                bits = insert(bits, 45);
                bits = insert(bits, 35);
                bits = insert(bits, 25);
                bits = insert(bits, 15);
                bits = insert(bits, 5);
                position[piece] = bits;
            }
        }

        return position;
    }


    /**
     * Binomial hash functions for each partition.
     */
    private static final BinomialHash[] HASHERS = {
        new BinomialHash(BOARD_SIZE, 2),
        new BinomialHash(BOARD_SIZE, 3),
        new BinomialHash(BOARD_SIZE, 4),
        new BinomialHash(BOARD_SIZE, 5),
        new BinomialHash(BOARD_SIZE, 6)
    };


    /**
     * Number of positions with N or less pieces.
     */
    private static final long[] OFFSETS = {
              0L,         200L,      19800L,
        1274200L,    60231000L, 2229841240L
    };
}
