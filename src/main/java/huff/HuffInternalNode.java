package huff;

public class HuffInternalNode implements HuffBaseNode{
    public int weight;
    public HuffBaseNode left;
    public  HuffBaseNode right;

    public HuffInternalNode(int weight, HuffBaseNode left, HuffBaseNode right) {
        this.weight = weight;
        this.left = left;
        this.right = right;
    }

    public HuffBaseNode getLeft() {
        return left;
    }

    public HuffBaseNode getRight() {
        return right;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
