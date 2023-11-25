package utils;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

public class FixedSizeStack<T> implements Cloneable {
    private Deque<T> stack;
    private int size = 2;

    public FixedSizeStack() {
        stack = new LinkedList<>();
    }

    public FixedSizeStack(int size) {
        stack = new LinkedList<>();
        this.size = size;
    }

    public void pushBack(T item) {
        if (stack.size() == size) {
            stack.pollLast();
        }
        stack.offerLast(item);
    }

    public void pushFront(T item) {
        if (stack.size() == size) {
            stack.pollLast();
        }
        stack.offerFirst(item);
    }

    public T popBack() {
        return stack.pollLast();
    }

    public T popFront() {
        return stack.pollFirst();
    }

    public T getfrontElement() {
        return stack.peekFirst();
    }

    public T getlastElement() {
        return stack.peekLast();
    }

    public int size() {
        return stack.size();
    }

    @Override
    public String toString() {
        return stack.toString();
    }

    @Override
    public FixedSizeStack<T> clone() {
        FixedSizeStack<T> clonedStack = new FixedSizeStack<>();
        for (T item : stack) {
            try {
                if (item instanceof Cloneable) {
                    clonedStack.pushBack(cloneItem(item));
                } else {
                    clonedStack.pushBack(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return clonedStack;
    }

    @SuppressWarnings("unchecked")
    private T cloneItem(T item) {
        try {
            return (T) item.getClass().getMethod("clone").invoke(item);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FixedSizeStack<?> other = (FixedSizeStack<?>) obj;
        return Objects.equals(stack, other.stack);
    }
}