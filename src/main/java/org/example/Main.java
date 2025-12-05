package org.example;

import huff.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
//    static PriorityQueue<HuffTree> minheap = new PriorityQueue<>();
        static PriorityQueue<HuffTree> minheap = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));

    public static void main(String[] args) throws IOException {
        // get the file and read it
        // construct the frequency table and sort it
        //encode it using huffman algo
//        HuffBaseNode--> weight,isLeaf
//        HuffLeafNode implements HuffBaseNode --> int weight, char c
        //HuffInternalNode --> weight,leftBaseNode, RightBaseNode
        // why do we have BaseNode as interface and implemented by HuffLeafNode --> HuffInternalNode can have HuffLeafNode or HuffInternalNode since bothof them
        // implements interface it can have HuffBaseNode left and HuffBaseNode right as variables
        Path path = Paths.get("/Users/arunkumar/Desktop/untitled folder/Javaa/HuffmanCoding/src/main/java/org/example/readFile.txt");
        List<String> content = Files.readAllLines(path);
        String fulltext = "";
        Map<Character, Integer> freq = new HashMap<>();
        for(String str : content){
            fulltext+=str;
            for(Character c : str.toCharArray()){
                freq.put(c,freq.getOrDefault(c,0)+1);
            }
        }
        System.out.println("As full text string: "+ fulltext);

        for(Map.Entry<Character,Integer> entry : freq.entrySet()){
            System.out.println(entry.getKey()+" : "+ entry.getValue());
            char ch = entry.getKey();
            int wt = entry.getValue();
            HuffTree t1 = new HuffTree(wt,ch);
            minheap.add(t1);
        }
        HuffTree root = buildTree();
        Map<Character,String> countTable = new HashMap<>();
        buildCodes(root.getRoot(),"",countTable);


       for(Map.Entry<Character,String> entry : countTable.entrySet()){
           System.out.println(entry.getKey() +":  " + entry.getValue());
       }

       int [] validbits = new int[1];
       byte[] encoded = BitOutputStream.encodeStringWithTable(fulltext,countTable,validbits);
        System.out.println("Original bytes: " + fulltext.getBytes().length);
        System.out.println("Encoded bytes : " + encoded.length);
        System.out.println("Valid bits in last byte: " + validbits[0]);

// print first few bytes in hex for inspection
        for (int i = 0; i < Math.min(encoded.length, 8); ++i) {
            System.out.printf("%02X ", encoded[i]);
        }
        System.out.println();


        HuffFileWriter.writeCompressedFile("output.huf",freq,encoded,validbits[0]);
        try (FileInputStream fis = new FileInputStream("output.huf")) {
            byte[] head = new byte[64];
            int r = fis.read(head);
            for (int i = 0; i < r; ++i) System.out.printf("%02X ", head[i]);
            System.out.println();
        }

    }
    public static  HuffTree buildTree(){
        HuffTree tmp1,tmp2,tmp3 = null;
        while(minheap.size()>1){
            tmp1 = minheap.remove();
            tmp2 = minheap.remove();
            tmp3= new HuffTree(tmp1.weight()+tmp2.weight(),tmp1.getRoot(),tmp2.getRoot());
            minheap.add(tmp3);
        }
        return minheap.poll();


    }
    public static void buildCodes(HuffBaseNode node,String codeBits,Map<Character,String> countTable ) {
        if (node.isLeaf()) {
            char  symbol = ((HuffLeafNode) node).ch;
//            if (len == 0) { // single-symbol file -> give it one bit "0"
//                codeBits[symbol] = 0;
//                codeLen[symbol]  = 1;
//            } else {
//                codeBits[symbol] = bits;
//                codeLen[symbol]  = len;
//            }
            if(codeBits.isEmpty()){
                 countTable.put(symbol,codeBits+'0');
                 return;
            }
            countTable.put(symbol,codeBits);
            return;
        }
        HuffInternalNode inode = (HuffInternalNode) node;
        // left = add 0
        buildCodes(inode.getLeft(),  codeBits+'0',countTable);
        // right = add 1
        buildCodes(inode.getRight(), codeBits+'1',countTable);
    }




}