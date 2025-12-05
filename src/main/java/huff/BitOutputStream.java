package huff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class BitOutputStream implements AutoCloseable{
    public OutputStream out ;
    public int count;
    public int current;

    public BitOutputStream(OutputStream out) {
        this.out = out;
    }
    public void writBitString(String bits) throws IOException {
        for(char ch : bits.toCharArray()){
            if(ch=='0'){
                writeBits(0);
            }else if(ch=='1'){
                writeBits(1);
            }else{
                throw new RuntimeException("No valid binary found");
            }
        }
    }

    public void writeBits(int b) throws IOException {
        current = current<<1|(b&1);
        count++;
        if(count==8){
            out.write(current);
            current=0;
            count=0;
        }


    }

    public int flushAndGetValidBits() throws IOException {
        if(count==0){
            out.flush();
            return 8;
        }else{
            int pad = 8-count;
            current <<=pad;
            out.write(current);
            out.flush();
            return count;
        }
    }

    public static  byte[] encodeStringWithTable(String input , Map<Character,String> countTable,int[] validbits){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(BitOutputStream bos = new BitOutputStream(baos)){
           for(char ch : input.toCharArray()){
               String bits = countTable.get(ch);
               bos.writBitString(bits);
           }
            validbits[0] = bos.flushAndGetValidBits();
           return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        out.close();
    }
}
