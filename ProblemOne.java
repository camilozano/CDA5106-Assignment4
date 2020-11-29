import java.util.EmptyStackException;
import java.util.concurrent.ThreadLocalRandom;

public class ProblemOne {
    static LockFreeStack<Integer> stack;
    static final int OPS_PER_THREAD = 150000;
    static final int NUM_THREADS = 8;
    static final int INIT_SIZE = 50000;

    static Thread[] threads = new Thread[NUM_THREADS];
    public static void main(String[] args) throws InterruptedException {
        // stack = new LockFreeStack<Integer>();
        stack = new EliminationBackoffStack<Integer>();
        long start, end;

        for (int i=0; i<INIT_SIZE; i++){
            int randInt = ThreadLocalRandom.current().nextInt();
            stack.push(randInt);
        }

        for (int i=0; i<NUM_THREADS; i++){
            threads[i] = new RandomOps(i);
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

    }

    static class RandomOps extends Thread{

        int thread_id;
        int numOps;

        RandomOps(int i){
            this.thread_id = i;
            this.numOps = 0;
        }

        public void run(){
            while (this.numOps < OPS_PER_THREAD){
                int randInt = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
                int op = randInt % 2;

                switch (op) {
                    case 0:
                        stack.push(randInt);
                        this.numOps++;
                        break;
                    case 1:
                        try {
                            stack.pop();
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
