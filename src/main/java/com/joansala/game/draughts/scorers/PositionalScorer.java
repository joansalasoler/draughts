package com.joansala.game.draughts.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  either version 3 of the License,  or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not,  see <http://www.gnu.org/licenses/>.
 */

import com.joansala.engine.Scorer;
import com.joansala.engine.mcts.Montecarlo;
import com.joansala.engine.negamax.Negamax;
import com.joansala.game.draughts.DraughtsGame;
import static com.joansala.game.draughts.Draughts.*;
import static com.joansala.util.bits.Bits.*;


/**
 * This simple heuristic function evaluates the position of pieces on
 * a draughts board to estimate who has the advantage.
 *
 * Here's the method that was used to derive this function:
 *
 *  1. Generate a set of random game states that are not final. Ensure
 *     this states include both positions with regular pieces and
 *     positions with many kings
 *  2. Focus only on positions that contain kings and use a MCTS algorithm
 *     with random simulations ({@link Montecarlo}) to estimate their
 *     value. Each position is evaluated for a short time (100 ms).
 *  3. For each position, we record information about each player, such
 *     as the number of regular pieces, number of kings, and whether one
 *     player has a king advantage.
 *  4. Use linear regression to determine how important each feature is
 *     in predicting the advantage. These weights are used to build a
 *     basic heuristic evaluation function {@link MaterialScorer}.
 *  5. Now we want to refine the {@link MaterialScorer} with positional
 *     information. Use the {@link MaterialScorer} function along with
 *     the {@link Negamax} engine to evaluate the full training set
 *     again for a short time (100 ms).
 *  6. Remove positions where the exact outcome (win or loss) is known
 *     because we want to predict the material advantage rather than the
 *     exact game outcome.
 *  7. For each position, create a set of features representing a piece
 *     type occupying a checker. Each feature is a binary value (1 or 0)
 *     indicating whether a specific piece type (man or king) occupies
 *     that checker. For the opponent, rotate the board 180 degrees.
 *  8. Augment the training set. To make the evaluation function symmetric,
 *     mirror each position in the training set. Flip the board and adjust
 *     the scores to create a mirrored version for the other player.
 *  9. Use linear regression again, to further improve the accuracy of
 *     the {@link MaterialScorer} function using positinal information.
 *     Determine how much weight to give to each type of piece on each
 *     square of the board.
 * 10. The final heuristic function combines {@link MaterialScorer}
 *     with the newly learned weights for each piece on each square.
 *     Adjust the weights to guarantee that the heuristic function always
 *     outputs scores between {@code ±MAX_SCORE} (exclusive).
 */
public final class PositionalScorer implements Scorer<DraughtsGame> {

    /** Heuristic value for crowning a king */
    public static final int CROWN_WEIGHT = 78;

    /** Index of men on the piece-square tables */
    public static final int MAN = 0;

    /** Index of kings on the piece-square tables */
    public static final int KING = 1;


    /**
     * {@inheritDoc}
     */
    public final int evaluate(DraughtsGame game) {
        int score = 0;

        long south_man = game.state(SOUTH_MAN);
        long north_man = game.state(NORTH_MAN);
        long south_king = game.state(SOUTH_KING);
        long north_king = game.state(NORTH_KING);

        north_man = Long.reverse(north_man) >>> 9;
        north_king = Long.reverse(north_king) >>> 9;

        score += count(south_king) > 0 ? CROWN_WEIGHT : 0;
        score -= count(north_king) > 0 ? CROWN_WEIGHT : 0;

        while (empty(south_man) == false) {
            final int checker = first(south_man);
            score += WEIGHTS[MAN][checker];
            south_man ^= bit(checker);
        }

        while (empty(south_king) == false) {
            final int checker = first(south_king);
            score += WEIGHTS[KING][checker];
            south_king ^= bit(checker);
        }

        while (empty(north_man) == false) {
            final int checker = first(north_man);
            score -= WEIGHTS[MAN][checker];
            north_man ^= bit(checker);
        }

        while (empty(north_king) == false) {
            final int checker = first(north_king);
            score -= WEIGHTS[KING][checker];
            north_king ^= bit(checker);
        }

        return score;
    }


    /** Piece-square weights */
    private static final int[][] WEIGHTS = {{
        28,    29,    29,    29,    30,     0,
            26,    27,    27,    27,    27,
        26,    27,    27,    27,    26,     0,
            24,    25,    25,    26,    26,
        26,    25,    25,    24,    24,     0,
            22,    23,    24,    24,    27,
        27,    27,    26,    24,    24,     0,
            35,    38,    40,    36,    30,
        48,    52,    56,    54,    51,     0,
             0,     0,     0,     0,     0
    }, {
        41,    45,    48,    47,    40,     0,
            39,    43,    46,    43,    40,
        44,    40,    44,    41,    40,     0,
            40,    39,    36,    42,    43,
        44,    39,    28,    35,    36,     0,
            32,    29,    28,    36,    46,
        44,    30,    29,    23,    27,     0,
            14,    30,    29,    31,    46,
        47,    27,    28,    28,    22,     0,
            43,    36,    38,    42,    43
    }};
}
