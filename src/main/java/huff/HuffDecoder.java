package huff;

import java.io.*;
//import java.util.Map;
//
public class HuffDecoder {

    // BitInputStream over a byte[] (MSB-first)
    static class BitInputStream implements AutoCloseable {
        private final byte[] data;
        private final int length; // number of bytes in data
        private int bytePos = 0;  // next byte index to load
        private int bitPos = 0;   // bits consumed in current byte (0..7), reading MSB-first
        private int current = 0;  // current byte value (0..255)

        BitInputStream(byte[] data) {
            this.data = data;
            this.length = data.length;
            this.bytePos = 0;
            this.bitPos = 8; // forces loadFirstByte on first read
        }

        // load next byte into current, return false if none left
        private boolean loadNextByte() {
            if (bytePos >= length) return false;
            current = data[bytePos] & 0xFF;
            bytePos++;
            bitPos = 0; // next read will take bit at position 0 -> MSB (7)
            return true;
        }

        /**
         * Read a single bit (MSB-first).
         * Returns -1 if no more bytes left.
         */
        public int readBit() {
            if (bitPos == 8) {
                // current byte exhausted, load next
                if (!loadNextByte()) return -1;
            }
            // If we just started on a new byte and bitPos==0, we want the bit at position 7 (MSB).
            int shift = 7 - bitPos;
            int bit = (current >> shift) & 1;
            bitPos++;
            if (bitPos == 8) {
                // mark as exhausted; next call will loadNextByte
                bitPos = 8;
            }
            return bit;
        }

        @Override
        public void close() { /* nothing */ }
    }

    /**
     * Decode into output file.
     * - root: HuffBaseNode root of the tree
     * - payload: compressed bytes (length payloadBytes)
     * - totalBits: total meaningful bits to read (calculated above)
     * - outPath: where to write decoded bytes
     */
    public static void decode(HuffBaseNode root, byte[] payload, long totalBits, String outPath) throws IOException {
        // Edge: single-symbol tree
        if (root.isLeaf()) {
            HuffLeafNode leaf = (HuffLeafNode) root;
            // get frequency from tree? If you have freqMap, use that. Here assume you have freqMap available.
            throw new IllegalStateException("Single-symbol tree: write symbol freq times instead of bit-decoding.");
        }

        try (BitInputStream bis = new BitInputStream(payload);
             FileOutputStream fos = new FileOutputStream(outPath)) {

            HuffBaseNode node = root;
            long bitsRead = 0L;

            while (bitsRead < totalBits) {
                int bit = bis.readBit();
                if (bit == -1) break; // safety

                // Walk tree: define convention (0 = left, 1 = right). Use same you used when encoding.
                HuffInternalNode inode = (HuffInternalNode) node;
                node = (bit == 0) ? inode.getLeft() : inode.getRight();

                if (node.isLeaf()) {
                    HuffLeafNode leaf = (HuffLeafNode) node;
                    // write symbol: if your leaf stores char, cast/convert appropriately
                    char ch = leaf.ch; // adjust if your API differs
                    fos.write((byte) ch);   // if original file was bytes; if chars are 16-bit, use different write
                    node = root;            // reset to root
                }

                bitsRead++;
            }
            fos.flush();
        }
    }

    // Utility: read payload bytes after header using existing DataInputStream positioned after header.
    public static byte[] readPayload(DataInputStream dis, int  payloadBytes) throws IOException {
        if (payloadBytes == 0) return new byte[0];
        if (payloadBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Payload too large to buffer in memory for this simple decoder.");
        }
        byte[] payload = new byte[payloadBytes];
        dis.readFully(payload); // assumes stream is positioned at start of payload
        return payload;
    }
}
