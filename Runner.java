import java.util.EmptyStackException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Runner {
    LockFreeStack<Integer> lockstack;
    LockFreeStack<Integer> elimbackstack;
    int OPS_PER_THREAD, NUM_THREADS, INIT_SIZE, duration;
    long pushOps, popOps, sizeOps;
    Thread[] threads;

    public enum STACK_TYPE{ LOCK_STACK, ELIM_STACK }
    public enum RUN_TYPE { RAND, RATIO }
    
    static final int LOCK_STACK = 0, ELIM_STACK = 1;

    public Runner(int OPS_PER_THREAD, int NUM_THREADS, int INIT_SIZE, int duration, int ratioPush, int ratioPop, int ratioSize){
        this.OPS_PER_THREAD = 150000;
        this.NUM_THREADS = 8;
        this.INIT_SIZE = 50000;
        this.duration = duration;
        this.threads = new Thread[NUM_THREADS];

        this.pushOps = Double.valueOf(OPS_PER_THREAD*(ratioPush/100.0)).longValue();
        this.popOps = Double.valueOf(OPS_PER_THREAD*(ratioPop/100.0)).longValue();
        this.sizeOps = Double.valueOf(OPS_PER_THREAD*(ratioSize/100.0)).longValue();

        long diff = OPS_PER_THREAD - (this.pushOps + this.popOps + this.sizeOps);
        if (diff != 0){
            System.out.println("Ratios don't divide evenly, off by " + diff + " ops");
        }

    }

    public long run(STACK_TYPE stack_type, RUN_TYPE run_type) throws InterruptedException {
        LockFreeStack<Integer> stack;
        if (STACK_TYPE.LOCK_STACK == stack_type){
            stack = new LockFreeStack<Integer>();
        }
        else if (STACK_TYPE.ELIM_STACK == stack_type){
            stack = new EliminationBackoffStack<Integer>(this.INIT_SIZE, this.duration);
        }
        else {
            return -1;
        }

        long start, end;

        for (int i=0; i<INIT_SIZE; i++){
            int randInt = ThreadLocalRandom.current().nextInt();
            stack.push(randInt);
        }

        for (int i=0; i<NUM_THREADS; i++){
            if (run_type == RUN_TYPE.RAND){
                threads[i] = new RandomOps(i, this.OPS_PER_THREAD, stack);
            } else if (run_type == RUN_TYPE.RATIO) {
                threads[i] = new RatioOps(i, this.OPS_PER_THREAD, stack, this.pushOps, this.popOps, this.sizeOps);
            }
        }

        start = System.currentTimeMillis();
        for (int i=0; i<NUM_THREADS; i++){
            threads[i].start();
        }

        for(int i=0; i<NUM_THREADS; i++){
            threads[i].join();
        }
        end = System.currentTimeMillis();

        long elapsedTime = end-start;

        // System.out.println(stack.getNumOps());
        // System.out.println(elapsedTime);
        return elapsedTime;

    }


    public void one_to_n_threads(int numTries, RUN_TYPE run_type)throws InterruptedException{

        int num_threads = this.NUM_THREADS;

        long[] lockTimeList = new long[num_threads];
        long[] elimTimeList = new long[num_threads];

        for (int i=0; i<num_threads; i++){
            this.NUM_THREADS = i+1;
            lockTimeList[i] = this.run(STACK_TYPE.LOCK_STACK, run_type);
            elimTimeList[i] = this.run(STACK_TYPE.ELIM_STACK, run_type);

            long lockTimeAvg = 0;
            long elimTimeAvg = 0;
            for (int j=0; j<numTries; j++){
                lockTimeAvg += this.run(STACK_TYPE.LOCK_STACK, run_type);
                elimTimeAvg += this.run(STACK_TYPE.ELIM_STACK, run_type);
            }
            lockTimeAvg /= (long)numTries;
            elimTimeAvg /= (long)numTries;

            lockTimeList[i] = lockTimeAvg;
            elimTimeList[i] = elimTimeAvg;


        }
        
        System.out.println("\tLockFreeStack\tElimBackoffStack");
        
        for (int i=0; i<num_threads; i++){
            System.out.printf("%d\t\t%d ms\t\t%d ms\n" ,i+1, lockTimeList[i], elimTimeList[i]);
        }


    }

    static class RandomOps extends Thread{

        int thread_id;
        int numOps;
        int OPS_PER_THREAD;
        LockFreeStack<Integer> stack;

        RandomOps(int i, int OPS_PER_THREAD, LockFreeStack<Integer> stack){
            this.thread_id = i;
            this.numOps = 0;
            this.OPS_PER_THREAD = OPS_PER_THREAD;
            this.stack = stack;
        }

        public void run(){
            while (this.numOps < this.OPS_PER_THREAD){
                int randInt = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
                int op = randInt % 2;

                switch (op) {
                    case 0:
                        this.stack.push(randInt);
                        this.numOps++;
                        break;
                    case 1:
                        try {
                            this.stack.pop();
                            this.numOps++;
                        } catch (EmptyStackException e) {
                            System.out.println("Stack Empty");
                        }
                        break;
                    default:
                        break;
                }
            }

        }

    }

    static class RatioOps extends Thread{

        int thread_id;
        int numOps;
        int OPS_PER_THREAD;
        long pushOps, popOps, sizeOps;
        LockFreeStack<Integer> stack;

        RatioOps(int i, int OPS_PER_THREAD, LockFreeStack<Integer> stack, long pushOps, long popOps, long sizeOps){
            this.thread_id = i;
            this.numOps = 0;
            this.OPS_PER_THREAD = OPS_PER_THREAD;
            this.stack = stack;

            this.pushOps = pushOps;
            this.popOps = popOps;
            this.sizeOps = sizeOps;
        }

        public void run(){

            int op = this.shuffleArray();
            while (op != -1){
                switch (op) {
                    case 0:
                        int randInt = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
                        this.stack.push(randInt);
                        this.numOps++;
                        this.pushOps--;
                        op = this.shuffleArray();
                        break;
                    case 1:
                        try {
                            this.stack.pop();
                            this.numOps++;
                            this.popOps--;
                            op = this.shuffleArray();
                        } catch (EmptyStackException e) {
                            System.out.println("Stack Empty");
                        }
                        break;
                    case 2:
                        this.stack.getNumOps();
                        this.sizeOps--;
                        op = this.shuffleArray();
                    default:
                        break;
                }

            }

        }
        private int shuffleArray() {
            int[] ops = {0,1,2};
            long[] vals = {this.pushOps, this.popOps, this.sizeOps};
            Random rnd = ThreadLocalRandom.current();
            for (int i = ops.length - 1; i > 0; i--) {
                int index = rnd.nextInt(i + 1);
                // Simple swap
                if (vals[ops[index]] > 0){
                    return index;
                }
                int a = ops[index];
                ops[index] = ops[i];
                ops[i] = a;
            }
            return -1;
        }

    }


}
