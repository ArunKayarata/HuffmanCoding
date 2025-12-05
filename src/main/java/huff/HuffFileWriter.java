package huff;

import java.io.*;
import java.util.Map;

public class HuffFileWriter {

    /**
     * Write header + payload to outFile.
     *
     * @param outFile       destination path (String)
     * @param freqMap       Map<Character, Long> frequency table used to build tree
     *                      (if you used bytes, use Map<Integer, Long> or long[] freq)
     * @param payload       compressed payload bytes (from BitOutputStream)
     * @param validBitsLast number of valid bits in payload[payload.length-1] (1..8)
     */
    public static void writeCompressedFile(String outFile,
                                           Map<Character, Integer> freqMap,
                                           byte[] payload,
                                           int validBitsLast) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(outFile)))) {

            // 1) Magic
            dos.writeBytes("HUF1"); // 4 bytes

            // 2) Number of distinct symbols
            dos.writeInt(freqMap.size());

            // 3) Symbol + frequency pairs
            // Note: using char (2 bytes). If you're compressing raw bytes, write a single byte symbol.
            for (Map.Entry<Character, Integer> e : freqMap.entrySet()) {
                char sym = e.getKey();
                Integer freq = e.getValue();
                dos.writeChar(sym);   // 2 bytes (big-endian), or use writeByte(sym) if symbols were bytes
                dos.writeInt(freq);  // 8 bytes
            }

            // 4) Payload length in bytes
            Integer payloadByteCount = payload.length;
            dos.writeInt(payloadByteCount); // 8 bytes

            // 5) Valid bits in final payload byte (1..8)
            dos.writeByte(validBitsLast); // 1 byte

            // 6) Write the actual payload bytes
            dos.write(payload); // payloadByteCount bytes

            dos.flush();
        }
    }
}
