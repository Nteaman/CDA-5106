import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.lang.Math;

public class Main {
    static ArrayList<LinkedList<Long>> sets = new ArrayList<LinkedList<Long>>();
    static ArrayList<HashSet<Long>> dirtyBits = new ArrayList<HashSet<Long>>();
    static ArrayList<HashMap<Long, Integer>> freqUsed = new ArrayList<HashMap<Long, Integer>>();
    static int cacheSize;
    static int assoc;
    static int replacementPolicy; //0 = LRU, 1 = FIFO
    static boolean writeBack; //false = write-through, true = write-back
    static int setAmt;
    static int blockSize = 64;
    static int setOffset;
    static int blockOffset;
    static String traceFile;
    static int hitsAmount = 0;
    static int missAmount = 0;
    static int memoryAccesses = 0;
    static int writesAmt = 0;
    static int readsAmt = 0;
    static int num = 0;
    static int collisionMiss = 0;


    public static void main(String[] args) {


        cacheSize = Integer.parseInt(args[0]);
        assoc = Integer.parseInt(args[1]);
        replacementPolicy = Integer.parseInt(args[2]);
        if(Integer.parseInt(args[3]) == 1){
            writeBack = true;
        }else{
            writeBack = false;
        }
        traceFile = args[4];

        /*
        cacheSize = 32768;
        assoc = 8;
        replacementPolicy = 0;
        writeBack = false;
        traceFile = "smallTest.t";
         */


        setAmt = cacheSize /(assoc * blockSize);
        setOffset = (int) (Math.log((double) cacheSize /(assoc * blockSize))/Math.log(2));
        blockOffset = (int) (Math.log(blockSize)/Math.log(2));

        for (int i = 0; i < setAmt; i++) {
            sets.add(new LinkedList<Long>());
            dirtyBits.add(new HashSet<Long>());
            freqUsed.add(new HashMap<Long, Integer>());
        }

        String data;
        try {
            File inputFile = new File(traceFile);
            Scanner scan = new Scanner(inputFile);
            while(scan.hasNextLine()){
                data = scan.nextLine();
                newOperation(data);
            }
            scan.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not Found");
        }

        double ratio = (double)missAmount/(hitsAmount + missAmount);
        System.out.println("Miss ratio " + String.format("%.6f", ratio));
        System.out.println("write " + writesAmt);
        System.out.println("read " + readsAmt);
        //System.out.println("hit " + hitsAmount);
        //System.out.println("miss " + missAmount);
        System.out.println("Collisions: " + collisionMiss);


    }

    public static void newOperation(String input) {
        String[] operationString = input.split(" "); //0 is operation
        String operation = operationString[0];
        String[] addressString = operationString[1].split("x");
        Long address = Long.parseUnsignedLong(addressString[1], 16);
        //int setNum = (int) (Long.divideUnsigned(address, blockSize) % setAmt);
        int setNum = (int) (Long.remainderUnsigned(Long.divideUnsigned(address, blockSize), setAmt));
        Long tag = Long.divideUnsigned(address, blockSize);

        if (replacementPolicy == 0) {
            if (sets.get(setNum).contains(tag)) {
                RU_Hit(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                hitsAmount++;
                //System.out.println("hit " + tag + " set: " + setNum);
            } else {
                LRU_Miss(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                missAmount++;
                //System.out.println("miss " + tag + " set: " + setNum);
            }
        } else if (replacementPolicy == 1) {
            if (sets.get(setNum).contains(tag)) {
                FO_Hit(tag, operation, dirtyBits.get(setNum));
                hitsAmount++;
            } else {
                FIFO_Miss(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                missAmount++;
            }
        } else if (replacementPolicy == 2) {
            if (sets.get(setNum).contains(tag)) {
                FO_Hit(tag, operation, dirtyBits.get(setNum));
                hitsAmount++;
            } else {
                LIFO_Miss(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                missAmount++;
            }
        } else if (replacementPolicy == 3) {
            if (sets.get(setNum).contains(tag)) {
                RU_Hit(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                hitsAmount++;
            } else {
                MRU_Miss(sets.get(setNum), tag, operation, dirtyBits.get(setNum));
                missAmount++;
            }
        }   else if (replacementPolicy == 4) {
            if (sets.get(setNum).contains(tag)) {
                LFU_Hit(sets.get(setNum), tag, operation, dirtyBits.get(setNum), freqUsed.get(setNum));
                hitsAmount++;
            } else {
                LFU_Miss(sets.get(setNum), tag, operation, dirtyBits.get(setNum), freqUsed.get(setNum));
                missAmount++;
            }
        }
    }

    public static void RU_Hit(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit){
        set.remove(tag);
        set.addFirst(tag);
        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void LRU_Miss(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit){
        Long LRU;
        set.addFirst(tag);
        readsAmt++;
        if(set.size() > assoc){
            collisionMiss++;
            LRU = set.pollLast();
            if(writeBack){
                if(dirtyBit.remove(LRU)){
                    memoryAccesses++;
                    writesAmt++;
                }
            }
        }

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void FO_Hit(Long tag, String operation, HashSet<Long> dirtyBit){
        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }
    }

    public static void FIFO_Miss(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit){
        Long firstIn;
        set.add(tag);
        readsAmt++;
        if(set.size() > assoc){
            firstIn = set.pollFirst();
            if(writeBack){
                if(dirtyBit.remove(firstIn)){
                    memoryAccesses++;
                    writesAmt++;
                }
            }
        }

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void LIFO_Miss(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit){
        Long lastIn;
        if(set.size() >= assoc){
            lastIn = set.pollLast();
            if(writeBack){
                if(dirtyBit.remove(lastIn)){
                    memoryAccesses++;
                    writesAmt++;
                }
            }
        }
        set.add(tag);
        readsAmt++;

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void MRU_Miss(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit){
        Long MRU;
        if(set.size() >= assoc){
            MRU = set.pollFirst();
            if(writeBack){
                if(dirtyBit.remove(MRU)){
                    memoryAccesses++;
                    writesAmt++;
                }
            }
        }
        set.addFirst(tag);
        readsAmt++;

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void LFU_Hit(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit, HashMap<Long, Integer> freq){
        freq.put(tag, (freq.get(tag) + 1));

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

    public static void LFU_Miss(LinkedList<Long> set, Long tag, String operation, HashSet<Long> dirtyBit, HashMap<Long, Integer> freq){
        if(set.size() >= assoc){
            Long LFU = set.get(0);
            int min = freq.get(set.get(0));
            for(Long num : set){
                if(freq.get(num) < min){
                    min = freq.get(num);
                    LFU = num;
                }
            }

            set.remove(LFU);
            freq.remove(LFU);

            if(writeBack){
                if(dirtyBit.remove(LFU)){
                    memoryAccesses++;
                    writesAmt++;
                }
            }
        }
        set.add(tag);
        readsAmt++;
        if(!freq.containsKey(tag)){
            freq.put(tag, 1);
        }else{
            freq.put(tag, (freq.get(tag) + 1));
        }

        if(operation.equals("W")){
            if(writeBack){
                dirtyBit.add(tag);
            }else{
                memoryAccesses++;
                writesAmt++;
            }
        }

    }

}