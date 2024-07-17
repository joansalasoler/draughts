package com.joansala.game.draughts.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Scorer;
import com.joansala.game.draughts.DraughtsGame;
import static com.joansala.game.draughts.Draughts.*;
import static com.joansala.util.bits.Bits.*;


/**
 * Evaluate the current state using only a material balance heuristic.
 */
public final class MaterialScorer implements Scorer<DraughtsGame> {

    /** Heuristic value for crowning a king */
    public static final int CROWN_WEIGHT = 219;

    /** Heuristic value of each piece */
    public static final int PIECE_WEIGHT = 41;


    /**
     * {@inheritDoc}
     */
    public final int evaluate(DraughtsGame game) {
        int score = 0;

        final int south_man = count(game.state(SOUTH_MAN));
        final int north_man = count(game.state(NORTH_MAN));
        final int south_king = count(game.state(SOUTH_KING));
        final int north_king = count(game.state(NORTH_KING));

        score += PIECE_WEIGHT * (south_man - north_man);
        score += PIECE_WEIGHT * (south_king - north_king);
        score += south_king > 0 ? CROWN_WEIGHT : 0;
        score -= north_king > 0 ? CROWN_WEIGHT : 0;

        return score;
    }
}
