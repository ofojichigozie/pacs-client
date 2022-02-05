package app.services;

public class LFSR {

    private boolean[] seed;
    private int tap1;
    private int tap2;
    private int tap3;

    public LFSR(boolean[] seed, int tap1, int tap2, int tap3){
        this.seed = seed;
        this.tap1 = tap1;
        this.tap2 = tap2;
        this.tap3 = tap3;
    }

    private int length(){
        return seed.length;
    }

    private boolean getBitAt(int index){
        return seed[index];
    }

    private void setBitAt(int index, boolean bit){
        seed[index] = bit;
    }

    private int step(){
        boolean next = getBitAt(0) ^ getBitAt(tap1) ^ getBitAt(tap2) ^ getBitAt(tap3);

        for (int i = 0; i < length() - 1; i++){
            setBitAt(i, getBitAt(i + 1));
        }

        setBitAt(length() - 1, next);

        return (next == true ? 1 : 0);

    }

    public int generate(int count){

        int generated = 0;

        for(int i = 0; i < count; i++){
            int next = step();
            generated += next * Math.pow(2, i);
        }

        return generated;
    }
}
