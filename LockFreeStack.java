import java.util.EmptyStackException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class LockFreeStack<T> {
    AtomicReference<Node<T>> top = new AtomicReference<Node<T>>(null);
    AtomicInteger atomicNumOps = new AtomicInteger(0);
    int numOps = 0;

    final boolean useAtomicOpsCounter = true;

    public void push(T value){
        Node<T> node = new Node<T>(value);
        
        while (true){
            Node<T> oldTop = this.top.get();
            node.next = oldTop;
            if (this.top.compareAndSet(oldTop, node)){
                this.incrementCounter();
                break;
            }
        }
    }

    public T pop() throws EmptyStackException {

        while (true){

            Node<T> returnNode = null;

            Node<T> oldTop = this.top.get();
            if (oldTop == null){
                throw new EmptyStackException();
            }
            Node<T> newTop = oldTop.next;
            if (this.top.compareAndSet(oldTop, newTop)){
                returnNode = oldTop;
            }

            if (returnNode != null){
                this.incrementCounter();
                return returnNode.value;
            }

        }
    }

    protected void incrementCounter(){
        if (this.useAtomicOpsCounter){
            this.atomicNumOps.incrementAndGet();
        }
        else{
            this.numOps++;
        }
    }

    public int getNumOps(){
        if (this.useAtomicOpsCounter){
            return this.atomicNumOps.get();
        }
        return this.numOps;
    }


}

class Node<T> {
    public T value;
    public Node<T> next;
    public Node(T value){
        this.value = value;
        this.next = null;
    }
    
}

