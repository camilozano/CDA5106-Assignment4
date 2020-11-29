import java.util.EmptyStackException;
import java.util.concurrent.ThreadLocalRandom;

public class Runner {
    LockFreeStack<Integer> lockstack;
    LockFreeStack<Integer> elimbackstack;
    int OPS_PER_THREAD, NUM_THREADS, INIT_SIZE, duration;
    Thread[] threads;

    public enum STACK_TYPE{ LOCK_STACK, ELIM_STACK }
    
    static final int LOCK_STACK = 0, ELIM_STACK = 1;

    public Runner(int OPS_PER_THREAD, int NUM_THREADS, int INIT_SIZE){
        this(OPS_PER_THREAD, NUM_THREADS, INIT_SIZE, 1);
    }

    public Runner(int OPS_PER_THREAD, int NUM_THREADS, int INIT_SIZE, int duration){
        this.OPS_PER_THREAD = 150000;
        this.NUM_THREADS = 8;
        this.INIT_SIZE = 50000;
        this.duration = duration;
        this.threads = new Thread[NUM_THREADS];
    }

    public long run(STACK_TYPE stack_type) throws InterruptedException {
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
            threads[i] = new RandomOps(i, this.OPS_PER_THREAD, stack);
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
        System.out.println(elapsedTime);
        return elapsedTime;

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
            // System.out.printf("ID: %d %d done\n", this.thread_id, this.numOps);

        }

    }


}
