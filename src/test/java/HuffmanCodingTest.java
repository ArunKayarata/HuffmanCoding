import org.example.Main;
import org.junit.Test;
import huff.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;


public class HuffmanCodingTest {

    // Helper: create a temp input file containing text and return Path
    private Path writeTempInput(String content) throws IOException {
        Path tmp = Files.createTempFile("huff-input-", ".txt");
        Files.write(tmp, Collections.singletonList(content), StandardOpenOption.TRUNCATE_EXISTING);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    @Test
    public void testSmallRoundtripEncodeDecode() throws Exception {
        String input = "aaabbc";              // tiny known example
        Path in = writeTempInput(input);
        Path out = Files.createTempFile("huff-out-", ".huf");
        out.toFile().deleteOnExit();
        Path decoded = Files.createTempFile("huff-decoded-", ".txt");
        decoded.toFile().deleteOnExit();

        // 1) Build frequency table (same logic as your Main)
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);

        // 2) Build HuffTree using the same flow as Main
        PriorityQueue<HuffTree> pq = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));
        for (Map.Entry<Character, Integer> e : freq.entrySet()) {
            // constructor used in your Main is (int wt, char ch)
            pq.add(new HuffTree(e.getValue(), e.getKey()));
        }
        // merge
        while (pq.size() > 1) {
            HuffTree t1 = pq.remove();
            HuffTree t2 = pq.remove();
            pq.add(new HuffTree(t1.weight() + t2.weight(), t1.getRoot(), t2.getRoot()));
        }
        HuffTree tree = pq.poll();
        assertNotNull(tree.toString(), "Tree should not be null");

        // 3) Build code table using your Main.buildCodes (or copy its logic)
        Map<Character, String> codeTable = new HashMap<>();
        Main.buildCodes(tree.getRoot(), "", codeTable);
        assertFalse("code table must not be empty", codeTable.isEmpty());
        // ensure prefix-free property: no code is prefix of another
        List<String> codes = new ArrayList<>(codeTable.values());
        for (int i = 0; i < codes.size(); ++i) {
            for (int j = 0; j < codes.size(); ++j) {
                if (i == j) continue;
                assertFalse("Codes must be prefix-free: " + codes.get(i) + " starts with " + codes.get(j),
                        codes.get(i).startsWith(codes.get(j)));
            }
        }

        // 4) Encode the input using BitOutputStream helper from your code
        int[] validBitsArr = new int[1];
        byte[] encoded = BitOutputStream.encodeStringWithTable(input, codeTable, validBitsArr);
        assertNotNull(encoded);

        // 5) Write compressed file using HuffFileWriter (your code)
        // caution: HuffFileWriter expects Map<Character, Long> frequencies in its writer;
        Map<Character, Integer> freqLong = new HashMap<>();
        freq.forEach((k, v) -> freqLong.put(k, v.intValue()));
        HuffFileWriter.writeCompressedFile(out.toString(), freqLong, encoded, validBitsArr[0]);

        // 6) Read header + payload back using HuffFileReader (your code)
        HuffFileReader.DecoderClass header = HuffFileReader.readDecoder(out.toString());
        assertEquals(freqLong.size(), header.freqMap.size());
        assertEquals(Optional.of(encoded.length).get(), header.payload);

        // 7) Read payload bytes from file (use DataInputStream same offsets as HuffFileReader.readPayload)
        // HuffFileReader.readDecoder or existing method may already return payload; here we reuse its method:
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(out.toFile())))) {
            // skip header exactly same way HuffFileReader does
            byte[] magic = new byte[4];
            dis.readFully(magic);
            int n = dis.readInt();
            for (int i = 0; i < n; ++i) {
                dis.readChar(); dis.readInt();
            }
            int  payloadBytes = dis.readInt();
            int validBits = dis.readByte();
            byte[] payload = new byte[payloadBytes];
            dis.readFully(payload);

            int  totalBits = (payloadBytes == 0) ? 0 : (payloadBytes-1) * 8 + validBits;

            // 8) Rebuild tree from header.freqMap and use your HuffDecoder.decode
            // Rebuild tree:
            PriorityQueue<HuffTree> pq2 = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));
            for (Map.Entry<Character, Integer> e : header.freqMap.entrySet()) {
                pq2.add(new HuffTree(e.getValue().intValue(), e.getKey()));
            }
            while (pq2.size() > 1) {
                HuffTree t1 = pq2.remove();
                HuffTree t2 = pq2.remove();
                pq2.add(new HuffTree(t1.weight() + t2.weight(), t1.getRoot(), t2.getRoot()));
            }
            HuffTree decodeTree = pq2.poll();
            assertNotNull(decodeTree);

            // decode to file
            HuffDecoder.decode(decodeTree.getRoot(), payload, totalBits, decoded.toString());

            // verify decoded equals original
            byte[] orig = input.getBytes();
            byte[] outBytes = Files.readAllBytes(decoded);
            assertArrayEquals(orig, outBytes);
        }
    }

    @Test
    public void testSingleSymbolFile() throws Exception {
        String input = "kkkkkkkk";
        Path in = writeTempInput(input);
        Path out = Files.createTempFile("huff-out-", ".huf");
        out.toFile().deleteOnExit();
        Path decoded = Files.createTempFile("huff-decoded-", ".txt");
        decoded.toFile().deleteOnExit();

        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);

        PriorityQueue<HuffTree> pq = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));
        for (Map.Entry<Character, Integer> e : freq.entrySet()) {
            pq.add(new HuffTree(e.getValue(), e.getKey()));
        }
        HuffTree tree = pq.poll();
        assertNotNull(tree);

        Map<Character, String> codeTable = new HashMap<>();
        Main.buildCodes(tree.getRoot(), "", codeTable);
        assertEquals(1, codeTable.size());

        int[] validBitsArr = new int[1];
        byte[] encoded = BitOutputStream.encodeStringWithTable(input, codeTable, validBitsArr);
        HuffFileWriter.writeCompressedFile(out.toString(),
                Collections.singletonMap(input.charAt(0), Math.toIntExact((long) input.length())),
                encoded, validBitsArr[0]);



        HuffFileReader.DecoderClass header = HuffFileReader.readDecoder(out.toString());

        // rebuild tree and decode using same logic as previous test
        PriorityQueue<HuffTree> pq2 = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));
        for (Map.Entry<Character, Integer> e : header.freqMap.entrySet()) {
            pq2.add(new HuffTree(e.getValue().intValue(), e.getKey()));
        }
        HuffTree decodeTree = pq2.poll();
        // single symbol special-case: write freq times
        if (decodeTree.getRoot().isLeaf()) {
            HuffLeafNode leaf = (HuffLeafNode) decodeTree.getRoot();
            try (FileOutputStream fos = new FileOutputStream(decoded.toFile())) {
                for (int i = 0; i < header.freqMap.get(leaf.ch); ++i) {
                    fos.write((byte) leaf.ch);
                }
            }
        }

        byte[] outBytes = Files.readAllBytes(decoded);
//        HuffFileWriter.writeCompressedFile("output.huf",freq,encoded,validbits[0]);
        assertArrayEquals(input.getBytes(), outBytes);
    }
}
