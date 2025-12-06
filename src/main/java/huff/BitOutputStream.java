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
    // here we iterate on each char within the bits string
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

    // here we write the 'b' bit into the current integer and iterate the count  and whenever the count is 8 i.e
    // we have stored 8 bits i.e 1 byte
    // we write to a outputStream whenever one byte of bits is stored
    public void writeBits(int b) throws IOException {
        current = current<<1|(b&1);
        count++;
        if(count==8){
//            Writes the specified byte to this output stream. The general contract for write is that one byte is written to the output stream.
//            The byte to be written is the eight low-order bits of the argument b.
            out.write(current);
            current=0;
            count=0;
        }


    }

    // We should also store how many bits are valid in the last byte in the outputStream let's say we have 30 bits in total and total bytes it took os 4 bytes
    // in first 3 bytes total 24 bits will fit in and  in last byte we have written only 6 bits so we need to store some where till which bit in the last byte do we need
    // to care

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
               // if for char a string is "001" we should not store it as such since each char in String is 1 byte and total it will be 3 bytes we could have
               // saved char 'a' as such since it will only cost 1 byte so we need to convert this to a bits which will make char 'a' can be stored as 3 bits instead
               //of 8 bits(1 byte)
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
