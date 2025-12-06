package huff;

import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;




public class HuffFileReader {
    static PriorityQueue<HuffTree> minheap2 = new PriorityQueue<>(Comparator.comparingLong(HuffTree::weight));

    public static DecoderClass  readDecoder(String file){
        try(DataInputStream dos = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
            byte[] magic = new byte[4];
            dos.readFully(magic);
            String magicstr = new String(magic);
//            System.out.println("file name : " + magicstr);
            if(!magicstr.equals("HUF1")){
                throw new IllegalArgumentException("Not a valid Huffman file");
            }
            int uniquesymbols = dos.readInt();
//            System.out.println("Unique symbols : " + uniquesymbols);
            Map<Character,Integer> freq = new HashMap<>();
            for(int i=-0;i<uniquesymbols;i++){
                char ch = dos.readChar();
                int count = dos.readInt();
                freq.put(ch,count);

            }
//            for(Map.Entry<Character,Integer> entry : freq.entrySet()){
//                System.out.println(entry.getKey()+"  "+entry.getValue());
//            }
          int payloadcount = dos.readInt();
            int validBits = dos.readByte();
//            System.out.println("payloadcount "+ payloadcount + "   " +  "valid bits "+ validBits);


            byte[] payload = HuffDecoder.readPayload(dos,payloadcount);
//            for (int i = 0; i < Math.min(payload.length, 8); ++i) {
//                System.out.println("Decoded hex codes");
//                System.out.printf("%02X ", payload[i]);
//            }
//            System.out.println();


            for(Map.Entry<Character,Integer> entry : freq.entrySet()){
                char ch = entry.getKey();
                int wt = entry.getValue();
                HuffTree t1 = new HuffTree(wt,ch);
                minheap2.add(t1);
            }
            HuffTree root = buildTree();
            Integer totalBits = (payloadcount == 0) ? 0 : (payloadcount-1) * 8 + validBits;

            //Handle case where only one character is present in the file
            if(root.getRoot().isLeaf()){
                try(BufferedWriter writer = new BufferedWriter(new FileWriter("restored.txt"))){
                    for(int i=0;i<totalBits;i++){
                        writer.write(((HuffLeafNode)root.getRoot()).ch);
                    }
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
                return  new DecoderClass(freq,validBits,payloadcount);
            }


            HuffDecoder.decode(root.getRoot(),payload,totalBits,"restored.txt");







            return  new DecoderClass(freq,validBits,payloadcount);
//

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    public static class DecoderClass{
        public Map<Character,Integer> freqMap;
        public int validBits;
        public Integer payload;

        public DecoderClass(Map<Character, Integer> freqMap, int validBits, Integer payload) {
            this.freqMap = freqMap;
            this.validBits = validBits;
            this.payload = payload;
        }
    }


    public static  HuffTree buildTree(){
        HuffTree tmp1,tmp2,tmp3 = null;
        while(minheap2.size()>1){
            tmp1 = minheap2.remove();
            tmp2 = minheap2.remove();
            tmp3= new HuffTree(tmp1.weight()+tmp2.weight(),tmp1.getRoot(),tmp2.getRoot());
            minheap2.add(tmp3);
        }
        return minheap2.poll();


    }
}
