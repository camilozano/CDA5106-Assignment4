public class Main {
    public static void main(String[] args) throws InterruptedException {
        int OPS_PER_THREAD = 150000;
        int NUM_THREADS = 8;
        int INIT_SIZE = 50000;
        int duration = 1;
        int num_retries = 3;

        double ratioPush = 1.0/3.0;
        double ratioPop = 1.0/3.0;
        double ratioSize = 1.0/3.0;

        Runner run = new Runner(OPS_PER_THREAD, NUM_THREADS, INIT_SIZE, duration, ratioPush, ratioPop, ratioSize);
        
        long res = run.run(Runner.STACK_TYPE.LOCK_STACK, Runner.RUN_TYPE.RAND);
        System.out.printf("Problem 1:\n\t%d ms\n",res);

        System.out.println("Problem 2:");
        run.one_to_n_threads(num_retries, Runner.RUN_TYPE.RATIO);
    }
}
