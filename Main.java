
public class Main {
    public static void main(String[] args) throws InterruptedException {
        int OPS_PER_THREAD = 150000;
        int NUM_THREADS = 8;
        int INIT_SIZE = 50000;
        int duration = 1;
        int num_retries = 3;
        Runner run = new Runner(OPS_PER_THREAD, NUM_THREADS, INIT_SIZE, duration);
        
        run.one_to_n_threads(num_retries);
    }
}
