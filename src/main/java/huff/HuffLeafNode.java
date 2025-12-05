package huff;

public class HuffLeafNode implements HuffBaseNode {
    public int weight;
    public char ch;
    public HuffLeafNode(int w, char ch){
        this.weight=w;
        this.ch= ch;
    }

    public int weight() {
        return weight;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

}
