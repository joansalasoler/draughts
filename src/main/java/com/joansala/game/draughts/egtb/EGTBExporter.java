package com.joansala.game.draughts.egtb;

import static com.joansala.engine.base.BaseGame.MAX_SCORE;

import java.io.DataOutputStream;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Date;

import com.joansala.util.bits.BitsetMap;


/**
 * Exports an EGTB database to a more compact format.
 */
public class EGTBExporter {

    /** Default book signature */
    private static final String signature = "Draughts Endgames 2.1";

    /** Each node is encoded with 2 bits */
    private static final int WORD_SIZE = 2;

    /** Store to export */
    private EGTBStore store;


    /**
     * Creates a exporter for the given store.
     *
     * @param store     EGTB database store
     */
    public EGTBExporter(EGTBStore store) {
        this.store = store;
    }


    /**
     * Exports the database nodes to a file.
     *
     * @param path      File were to save the book
     *
     * @return          Number of nodes exported
     */
    public long export(String path) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "rw");
        OutputStream os = Channels.newOutputStream(file.getChannel());
        DataOutputStream stream = new DataOutputStream(os);

        long size = store.count();
        BitsetMap bitsetMap = new BitsetMap(WORD_SIZE, size);
        String date = String.valueOf(new Date());

        file.writeChars(String.format("%s\n", signature));
        file.writeChars(String.format("Date: %s\n", date));
        file.writeChars(String.format("Entries: %d\n", size));
        file.writeChar('\n');

        for (long hash = 1; hash < size; hash++) {
            EGTBNode node = store.read(hash);
            long score = 2 + node.score / MAX_SCORE;
            bitsetMap.put(hash, score);
        }

        bitsetMap.writeToFile(stream);
        file.close();

        return size;
    }
}
