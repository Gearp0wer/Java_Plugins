package com.ivory.commands.name.nms;

@FunctionalInterface
public interface FunctionWithException<A, B>
{
    B apply(A value) throws Exception;
}
