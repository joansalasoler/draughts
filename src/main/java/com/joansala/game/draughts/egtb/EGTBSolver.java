package com.joansala.game.draughts.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.*;
import com.joansala.game.draughts.*;
import static com.joansala.engine.Game.*;
import static com.joansala.game.draughts.Draughts.*;
import static com.joansala.engine.base.BaseGame.MAX_SCORE;


/**
 * Draughts endgames book builder.
 */
public class EGTBSolver {

    /** Hash function */
    private DraughtsHash hasher = new DraughtsHash();

    /** Database of expanded nodes */
    private EGTBStore store;

    /** Current game state */
    private EGTBGame game = new EGTBGame();

    /** Placeholder for rotated game states */
    private long[] rotated = new long[POSITION_SIZE];

    /** Set to true to abort all expansions */
    private volatile boolean aborted = false;


    /**
     * Creates a new endgames solver instance.
     */
    public EGTBSolver(EGTBStore store) {
        this.store = store;
    }


    /**
     * Stops all the ongoing computations.
     */
    public void abortComputation() {
        this.aborted = true;
    }


    /**
     * Solves all positions with up to {@code maxPieces} pieces.
     *
     * Because position repetitions change the evaluation of a node, not
     * all the nodes will receive an exact score.
     *
     * @param maxPieces      Maximum number of pieces on a board
     */
    public synchronized void solve(int maxPieces) throws Exception {
        aborted = false;

        for (int pieces = 1; !aborted && pieces <= maxPieces; pieces++) {
            System.out.format("Solving nodes with %d pieces%n", pieces);

            final long first = hasher.offset(pieces - 1);
            final long last = hasher.offset(pieces) - 1;

            create(first, last, pieces);
            propagate(first, last);
            resolve(first, last);
        }
    }


    /**
     * Creates all the nodes on the given set and initializes them.
     *
     * @param first         Hash of the first node
     * @param last          Hash of the last node
     */
    private void create(long first, long last, int pieces) {
        for (long hash = first; !aborted && hash <= last; hash++) {
            EGTBNode node = new EGTBNode(hash);
            game.setBoard(toBoard(hash));

            if (game.hasEnded()) {
                game.endMatch();
                node.flag = Flag.EXACT;
                node.score = game.outcome();
                node.known = true;
            }

            node.pieces = pieces;
            store.write(node);
        }
    }


    /**
     * Backpropagates the exact scores for the given set of node.
     *
     * @param first         Hash of the first node
     * @param last          Hash of the last node
     */
    private void propagate(long first, long last) {
        boolean assigned = true;

        while (assigned && !aborted) {
            assigned = false;

            for (long hash = first; !aborted && hash <= last; hash++) {
                EGTBNode node = store.read(hash);

                if (node.known == false) {
                    boolean known = assign(node);
                    assigned = assigned || known;
                }
            }
        }
    }


    /**
     * Assigns a predefined score to all nodes that could not be solved
     * by backpropagation.
     *
     * @param first         Hash of the first node
     * @param last          Hash of the last node
     */
    private void resolve(long first, long last) {
        for (long hash = first; !aborted && hash <= last; hash++) {
            EGTBNode node = store.read(hash);

            if (node.known == false) {
                node.score = DRAW_SCORE;
                node.known = true;
                store.write(node);
            }
        }
    }


    /**
     * Computes the best score obtainable from the given game state.
     *
     * @param node          Node to evaluate
     * @return              If an exact score was assigned
     */
    private boolean assign(EGTBNode node) {
        int move = NULL_MOVE;
        int bestScore = -maxScore(node);
        boolean known = true;

        game.setBoard(toBoard(node.hash));

        while ((move = game.nextMove()) != NULL_MOVE) {
            game.makeMove(move);
            EGTBNode child = store.read(toHash(game));
            int score = edgeScore(node, child);
            game.unmakeMove();

            if (score == maxScore(node)) {
                bestScore = score;
                known = true;
                break;
            }

            bestScore = Math.max(score, bestScore);
            known = known && child.known;
        }

        if (known == true) {
            node.score = bestScore;
            node.flag = Flag.EXACT;
            node.known = true;
            store.write(node);
        }

        return known;
    }


    /**
     * Maximum obtainable score on the edge of a parent node.
     */
    private int maxScore(EGTBNode parent) {
        return MAX_SCORE;
    }


    /**
     * Computes the score of an edge.
     */
    private int edgeScore(EGTBNode parent, EGTBNode child) {
        return -child.score;
    }


    /**
     * Converts the current state of a game to a unique binomial
     * hash code suitable for use as an endgames book entry.
     */
    private long toHash(EGTBGame game) {
        final DraughtsBoard board = game.toBoard();
        final long[] state = board.position();
        return hasher.hash(rotate(state));
    }


    /**
     * Convert a hash to its board representation.
     */
    private Board toBoard(long hash) {
        final long[] state = hasher.unhash(hash);
        return new EGTBBoard(state);
    }


    /**
     * Rotates an Draughts position array so it is seen from the point
     * of view of the opponent player.
     */
    private long[] rotate(long[] state) {
        rotated[0] = Long.reverse(state[1]) >>> 9;
        rotated[1] = Long.reverse(state[0]) >>> 9;
        rotated[2] = Long.reverse(state[3]) >>> 9;
        rotated[3] = Long.reverse(state[2]) >>> 9;

        return rotated;
    }
}
