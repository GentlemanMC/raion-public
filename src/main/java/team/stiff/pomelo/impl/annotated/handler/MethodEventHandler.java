package team.stiff.pomelo.impl.annotated.handler;

import team.stiff.pomelo.filter.EventFilter;
import team.stiff.pomelo.handler.EventHandler;
import team.stiff.pomelo.handler.ListenerPriority;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * A basic implementation of the {@link EventHandler} used to
 * mark a method as an event listener via handle.
 *
 * @author Daniel
 * @since May 31, 2017
 */
public final class MethodEventHandler implements EventHandler {
    /**
     * The class instance and parent of the method.
     */
    private final Object listenerParent;

    /**
     * The method object that has been marked as a listener.
     */
    private final Method method;
	
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
	/**
	 * Method Handle
	 */
	private MethodHandle methodHandle;
	
	/**
     * A filter predicate used to test the passed method against
     * all registered filters on the handler.
     */
    private final Set<EventFilter> eventFilters;

    /**
     * The annotation to the event listener.
     */
    private final Listener listenerAnnotation;
    
    private static boolean failed = false;

    public MethodEventHandler(final Object listenerParent, final Method method,
            final Set<EventFilter> eventFilters) {
        this.listenerParent = listenerParent;
        if (!method.isAccessible())
            method.setAccessible(true);
        
        this.method = method;
        
        try {
        	methodHandle = lookup.unreflect(method);
		} catch (Throwable t) {
        	if (!failed) { // Only print stacktrace once - dont spam
				new RuntimeException("Couldnt unreflect handler", t).printStackTrace();
				failed = true;
			}
		}
        
        this.eventFilters = eventFilters;
        this.listenerAnnotation = method.getAnnotation(Listener.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> void handle(final E event) {
        // iterate all event filters given to the handler
        for (final EventFilter filter : eventFilters)
            if (!filter.test(this, event))
                return;

		if (methodHandle != null) {
			try {
				if (listenerParent != null) {
					methodHandle.invoke(listenerParent, event);
				} else {
					methodHandle.invoke(event);
				}
				return;
			} catch (Throwable t) {
				t.printStackTrace();
				return;
			}
		}
        try {
            // invoke the listener with the current event
            method.invoke(listenerParent, event);
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Object getListener() {
        return method;
    }

    @Override
    public ListenerPriority getPriority() {
        return listenerAnnotation.priority();
    }

    @Override
    public Iterable<EventFilter> getFilters() {
        return eventFilters;
    }

    @Override
    public int compareTo(final EventHandler eventHandler) {
        return Integer.compare(eventHandler.getPriority().getPriorityLevel(),
                getPriority().getPriorityLevel());
    }
}
