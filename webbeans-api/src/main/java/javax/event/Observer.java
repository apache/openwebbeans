package javax.event;

public interface Observer<T>
{
    public void notify(T event);
}