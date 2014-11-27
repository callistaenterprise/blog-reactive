package se.callista.springmvc.asynch.common;

import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
public class AkkaUtils {

    public static <F,T> Mapper<F,T> mapper(Function<F,T> f) {
        return new Mapper<F, T>() {
            @Override
            public T apply(F parameter) {
                return f.apply(parameter);
            }
        };
    }

    public static <T> OnComplete<T> completer(Consumer<Throwable> f, Consumer<T> s) {
        return new OnComplete<T>() {
            @Override
            public void onComplete(Throwable failure, T success) throws Throwable {
                if(failure != null)
                    f.accept(failure);
                else
                    s.accept(success);
            }
        };
    }

    public static <T> Runnable runnable(Supplier<T> f) {
        return f::get;
    }

}
