package moe.him188.assembly.interpreter

suspend fun commonRunInAppMode(fileContent: String) {
    val callback = RecorderInvocationCallback<InvokeAllAssemblyInterpreter>()
    val result = executeAll(
        fileContent,
        invocationCallback = callback,
    )
    println("Trace table:")
    println(result.interpreter.dump(callback))
    println()
    println("Result: ${result.output}")
}