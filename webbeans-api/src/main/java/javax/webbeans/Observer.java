package javax.webbeans;

public interface Observer<T>
{
    public void notify(T event);
}