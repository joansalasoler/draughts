package com.joansala.game.draughts;

/*
 * Copyright (C) 2022-2024 Joan Sala Soler <contact@joansala.com>
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

import java.io.*;
import java.nio.channels.Channels;

import com.joansala.engine.Flag;
import com.joansala.engine.Game;
import com.joansala.engine.Leaves;
import com.joansala.engine.base.BaseBook;
import com.joansala.util.bits.BitsetMap;

import static com.joansala.engine.Game.DRAW_SCORE;
import static com.joansala.engine.Game.NORTH;
import static com.joansala.engine.base.BaseGame.MAX_SCORE;
import static com.joansala.game.draughts.Draughts.*;
import static com.joansala.game.draughts.DraughtsBoard.*;
import static com.joansala.util.bits.Bits.*;


/**
 * Endgame WDL bitbases for Draughts.
 */
public class DraughtsLeaves extends BaseBook implements Leaves<Game> {

    /** Default path to the endgames book binary file */
    public static final String LEAVES_PATH = "draughts-leaves.bin";

    /** Maximum number of pieces a state can contain */
    public static final int MAX_PIECES = 5;

    /** Each node is encoded with 2 bits */
    private static final int WORD_SIZE = 2;

    /** Hash function */
    private final DraughtsHash hasher = new DraughtsHash();

    /** Entries of the endgames database */
    private final BitsetMap store;

    /** Score of the last found entry */
    private int score = DRAW_SCORE;


    /**
     * Create a new endgames book instance.
     */
    public DraughtsLeaves() throws IOException {
        this(LEAVES_PATH);
    }


    /**
     * Instantiates a new endgames book object.
     *
     * @param path      Book file path
     */
    public DraughtsLeaves(String path) throws IOException {
        super(path);
        store = readFromFile();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlag() {
        return Flag.EXACT;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return score;
    }


    /**
     * Check if this book contains an entry for a game state.
     *
     * @param state     Game position array
     * @return          If the state is found on this book
     */
    private boolean contains(long[] state) {
        return count(taken(state)) <= MAX_PIECES;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean find(Game game) {
        return find((DraughtsGame) game);
    }


    /**
     * @see find(Game)
     */
    public boolean find(DraughtsGame game) {
        long[] state = game.state();

        if (contains(state) == false) {
            return false;
        }

        if (game.turn() == NORTH) {
            state = rotateState(state);
        }

        final long hash = hasher.hash(state);
        final long entry = store.get(hash);
        score = game.turn() * outcome(entry);

        return true;
    }


    /**
     * Obtain the exact score for a found position.
     *
     * @param state         Game position
     * @return              Exact score value
     */
    private int outcome(long entry) {
        final int value = (int) entry;
        return MAX_SCORE * (value - 2);
    }


    /**
     * Obtains a bitboard of occupied checkers.
     *
     * @param state     Position array
     * @return          A bitboard
     */
    private static long taken(long[] state) {
        final long south = state[SOUTH_MAN] | state[SOUTH_KING];
        final long north = state[NORTH_MAN] | state[NORTH_KING];
        return south | north;
    }


    /**
     * Computes a unique hash code for a game state.
     *
     * @param game          Game state
     */
    public long computeHash(Game game) {
        DraughtsGame g = (DraughtsGame) game;
        long[] state = g.state();

        if (game.turn() == NORTH) {
            state = rotateState(state);
        }

        return hasher.hash(state);
    }


    /**
     * Reads the endgames from the book file.
     *
     * @return          Book entries store
     */
    private BitsetMap readFromFile() throws IOException {
        InputStream is = Channels.newInputStream(file.getChannel());
        BufferedInputStream bis = new BufferedInputStream(is);
        DataInputStream stream = new DataInputStream(bis);

        final long capacity = hasher.offset(MAX_PIECES);
        BitsetMap store = new BitsetMap(WORD_SIZE, capacity);
        store.readFromFile(stream);

        return store;
    }
}
