package com.darren.download.thread;

import java.util.concurrent.Callable;

public abstract class ThreadTask<T> implements Callable<T> {
    @Override
    public T call() throws Exception {
        return this.execute();
    }

    protected abstract T execute();
}
