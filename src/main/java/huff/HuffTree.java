package huff;

public class HuffTree  {
    private HuffBaseNode root;
    public HuffTree(int wt , char ch){
        this.root= new HuffLeafNode(wt,ch);
    }

    public HuffBaseNode getRoot() {
        return root;
    }
    public int weight(){
        return root.weight();
    }

    public HuffTree(int wt, HuffBaseNode l, HuffBaseNode r){
        this.root= new HuffInternalNode(wt,l,r) ;

    }

//
//    @Override
//    public int compareTo(Object o) {
//        HuffTree tr =(HuffTree) o;
//        if(root.weight()< tr.weight()){
//            return -1;
//        }else if (root.weight()==tr.weight()){
//            return 0;
//        }else{
//            return 1;
//        }
//    }


}
