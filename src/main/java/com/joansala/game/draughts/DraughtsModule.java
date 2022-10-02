package com.joansala.game.draughts;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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


import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.joansala.cli.*;
import com.joansala.engine.*;
import com.joansala.cache.GameCache;
import com.joansala.engine.base.BaseLeaves;
import com.joansala.engine.base.BaseModule;
import com.joansala.engine.negamax.Negamax;


/**
 * Binds together the components of the Draughts engine.
 */
public class DraughtsModule extends BaseModule {

    /**
     * Command line interface.
     */
    @Command(
      name = "draughts",
      version = "1.0.0",
      description = "International draughts is a strategy board game"
    )
    private static class DraughtsCommand extends MainCommand {

        @Option(
          names = "--leaves",
          description = "Endgames book path"
        )
        private static String leaves = DraughtsLeaves.LEAVES_PATH;
    }


    /**
     * Game module configuration.
     */
    @Override protected void configure() {
        bind(Game.class).to(DraughtsGame.class);
        bind(Board.class).to(DraughtsBoard.class);
        bind(Engine.class).to(Negamax.class);
        bind(Cache.class).to(GameCache.class);
    }


    /**
     * Endgames book provider.
     */
    @Provides @Singleton @SuppressWarnings("rawtypes")
    public static Leaves provideLeaves() {
        String path = DraughtsCommand.leaves;

        try {
            return new DraughtsLeaves(path);
        } catch (Exception e) {
            logger.warning("Cannot open endgames book: " + path);
        }

        return new BaseLeaves();
    }


    /**
     * Executes the command line interface.
     *
     * @param args      Command line parameters
     */
    public static void main(String[] args) throws Exception {
        BaseModule module = new DraughtsModule();
        DraughtsCommand main = new DraughtsCommand();
        System.exit(main.execute(module, args));
    }
}
