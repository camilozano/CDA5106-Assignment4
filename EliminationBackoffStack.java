import java.util.EmptyStackException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationBackoffStack<T> extends LockFreeStack<T>{
    int capacity;
    EliminationArray<T> eliminationArray;
    static ThreadLocal<RangePolicy> policy; 

    public EliminationBackoffStack(int capacity){
        this(capacity, 1);
    }

    public EliminationBackoffStack(int capacity, int duration){
        this.capacity = capacity;
        eliminationArray = new EliminationArray<T>(capacity, duration);
        policy = new ThreadLocal<RangePolicy>() {
            protected synchronized RangePolicy initialValue(){
                return new RangePolicy(capacity);
            }
        };
    }

    protected boolean tryPush(Node<T> node){
        Node<T> oldTop = top.get();
        node.next = oldTop;
        return(top.compareAndSet(oldTop,node));
    }

    protected Node<T> tryPop() throws EmptyStackException{
        Node<T> oldTop = top.get();
        if (oldTop == null){
            throw new EmptyStackException();
        }
        Node<T> newTop = oldTop.next;
        if (top.compareAndSet(oldTop,newTop)){
            return oldTop;
        } else {
            return null;
        }
    }

    public void push(T value){
        RangePolicy rangePolicy = policy.get();
        Node<T> node = new Node<T>(value);
        while (true){
            if (this.tryPush(node)){
                this.incrementCounter();
                return;
            } else try {
                T otherValue = eliminationArray.visit(value, rangePolicy.getRange());
                if (otherValue == null){
                    this.incrementCounter();
                    rangePolicy.recordEliminationSuccess();
                    return;
                }
            } catch (TimeoutException ex){
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    public T pop() throws EmptyStackException {
        RangePolicy rangePolicy = policy.get();
        while (true){
            Node<T> returnNode = this.tryPop();
            if (returnNode != null){
                this.incrementCounter();
                return returnNode.value;
            } else try {
                T otherValue = eliminationArray.visit(null, rangePolicy.getRange());
                if (otherValue != null){
                    rangePolicy.recordEliminationSuccess();
                    this.incrementCounter();
                    return otherValue;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

}

class RangePolicy {
    int maxRange;
    int currentRange = 1;

    RangePolicy(int maxRange) {
        this.maxRange = maxRange;
    }

    public void recordEliminationSuccess() {
        if (currentRange < maxRange)
            currentRange++;
    }

    public void recordEliminationTimeout() {
        if (currentRange > 1)
            currentRange--;
    }

    public int getRange() {
        return currentRange;
    }
}

class EliminationArray<T>{
    int duration;
    LockFreeExchanger<T>[] exchanger;
    Random random;

    @SuppressWarnings("unchecked")
    public EliminationArray(int capacity, int duration){
        this.duration = duration;
        exchanger = (LockFreeExchanger<T>[]) new LockFreeExchanger[capacity];
        for (int i=0; i< capacity; i++){
            exchanger[i] = new LockFreeExchanger<T>();
        }
        random = new Random();
    }

    public T visit(T value, int range) throws TimeoutException{
        int slot = random.nextInt(range);
        return (exchanger[slot].exchange(value, duration, TimeUnit.MILLISECONDS));
    }

}


class LockFreeExchanger<T>{
    static final int EMPTY=0, WAITING=1, BUSY=2;
    AtomicStampedReference<T> slot = new AtomicStampedReference<T>(null, 0);

    public T exchange(T myItem, long timeout, TimeUnit unit) throws TimeoutException{
        long nanos = unit.toNanos(timeout);
        long timeBound = System.nanoTime() + nanos;
        int[] stampHolder = {EMPTY};

        while(true){
            if (System.nanoTime() > timeBound){
                throw new TimeoutException();
            }

            T yrItem = slot.get(stampHolder);
            int stamp = stampHolder[0];
            switch(stamp){
                case EMPTY:
                    if (slot.compareAndSet(yrItem,myItem, EMPTY, WAITING)){
                        while (System.nanoTime() < timeBound){
                            yrItem = slot.get(stampHolder);
                            if (stampHolder[0] == BUSY) {
                                slot.set(null, EMPTY);
                                return yrItem;
                            }
                        }
                        if (slot.compareAndSet(myItem, null, WAITING, EMPTY)){
                            throw new TimeoutException();
                        } else {
                            yrItem = slot.get(stampHolder);
                            slot.set(null, EMPTY);
                            return yrItem;
                        }
                    }
                    break;
                case WAITING:
                    if (slot.compareAndSet(yrItem, myItem, WAITING, BUSY)){
                        return yrItem;
                    }
                    break;
                case BUSY:
                    break;
                default:
                    break;
            }
        }
    }

}