package com.atguigu.gmall.product.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture.supplyAsync(() -> {
            return "hello";
        }).thenApplyAsync(t -> { // 获取上一个任务的返回结果，并返回当前任务的返回值
            return t + " world!";
        }).thenCombineAsync(CompletableFuture.completedFuture(" CompletableFuture"), (t, u) -> {
            System.out.println("thenCombineAsync:" + t + "\t" + u);
            return t + u; // 组合两个future，获取两个future任务的返回结果，并返回当前任务的返回值
        }).whenComplete((t, u) -> { // 处理正常或异常的结果
            System.out.println(t);
            //System.out.println(u);
        });

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName() + "\t completableFuture");
//                //int i = 10 / 0;
//                return 1024;
//            }
//        }).thenApply(new Function<Integer, Integer>() { 当一个线程依赖另一个线程时，获取上一个线程的返回结果，并返回当前任务的返回值
//            @Override
//            public Integer apply(Integer o) {
//                System.out.println("thenApply方法，上次返回结果：" + o);
//                return  o * 2;
//            }
//        }).whenComplete(new BiConsumer<Integer, Throwable>() {
//            @Override
//            public void accept(Integer o, Throwable throwable) {
//                System.out.println("-------o=" + o);
//                System.out.println("-------throwable=" + throwable);
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable=" + throwable);
//                return 6666;
//            }
//        }).handle(new BiFunction<Integer, Throwable, Integer>() { // 任务完成后再执行
//            @Override
//            public Integer apply(Integer integer, Throwable throwable) {
//                System.out.println("handle o=" + integer);
//                System.out.println("handle throwable=" + throwable);
//                return 8888;
//            }
//        });
//        System.out.println(future.get());

//        CompletableFuture future = CompletableFuture.supplyAsync(new Supplier<Object>() {
//            @Override
//            public Object get() {
//                System.out.println(Thread.currentThread().getName() + "\t completableFuture");
//                //int i = 10 / 0;
//                return 1024;
//            }
//        }).whenComplete(new BiConsumer<Object, Throwable>() { // 处理正常或异常的计算结果
//            @Override
//            public void accept(Object o, Throwable throwable) {
//                System.out.println("-------o=" + o.toString());
//                System.out.println("-------throwable=" + throwable);
//            }
//        }).exceptionally(new Function<Throwable, Object>() { // 处理异常
//            @Override
//            public Object apply(Throwable throwable) {
//                System.out.println("throwable=" + throwable);
//                return 6666;
//            }
//        });
//        System.out.println(future.get());
    }
}
