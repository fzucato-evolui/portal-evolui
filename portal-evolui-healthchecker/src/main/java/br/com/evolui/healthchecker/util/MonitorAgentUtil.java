package br.com.evolui.healthchecker.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;

public class MonitorAgentUtil {

    // Thread-local storage to hold the method stack for each thread
    private static final ThreadLocal<Stack<MethodNode>> threadMethodStack = ThreadLocal.withInitial(Stack::new);

    // A flag to track whether we've already printed the method tree for the current thread
    private static final ThreadLocal<Boolean> hasPrintedTree = ThreadLocal.withInitial(() -> false);

    // Limit on the number of child methods a method can have
    private static final int MAX_CHILDREN = 20;  // Adjust as needed to limit the number of child nodes

    // Track the start time and add the method to the stack
    public static void trackStart(String methodName) {
        long threadId = Thread.currentThread().getId();

        // Get the current thread's method stack
        Stack<MethodNode> stack = threadMethodStack.get();

        // Create a new MethodNode for the current method
        MethodNode methodNode = new MethodNode(methodName, threadId, System.nanoTime());

        // If the stack is not empty, set the current method as a child of the last method
        if (!stack.isEmpty()) {
            MethodNode parentMethod = stack.peek();

            // Limit the number of child methods for the current parent method
            if (parentMethod.getChildren().size() < MAX_CHILDREN) {
                parentMethod.addChild(methodNode);  // Add the current method as a child
            } else {
                // Optionally, handle case when too many children are added
                //parentMethod.addChild(new MethodNode("Too many child methods...", threadId, System.nanoTime()));
            }
        }

        // Push the current method onto the stack
        stack.push(methodNode);
    }

    // Track the end time, pop the method from the stack and print the tree when done
    public static void trackEnd() {
        long threadId = Thread.currentThread().getId();
        long endTime = System.nanoTime();

        // Get the thread-local stack for this thread
        Stack<MethodNode> stack = threadMethodStack.get();

        if (!stack.isEmpty()) {
            MethodNode currentMethod = stack.pop(); // The method that just ended
            long elapsedTime = endTime - currentMethod.getStartTime();
            currentMethod.setElapsedTime(elapsedTime);

            // If the stack is empty after popping, it means we've finished all method calls for this thread
            if (stack.isEmpty() && !hasPrintedTree.get()) {
                // Print the method call tree for the thread, starting from the root method
                printMethodCallTree(currentMethod, 0);
                // Mark that we've printed the tree for this thread
                hasPrintedTree.set(true);
            }
        }
    }

    private static void printMethodCallTree(MethodNode node, int level) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Files.write(Paths.get("D:/Lixo/methodNode.json"), mapper.writeValueAsString(node).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /*
        if (level > MAX_CHILDREN) {
            System.out.println("... (too deep to display further)");
            return;
        }

        // Generate indentation based on the level of depth
        String indent = repeatString("  ", level);

        // Print method name with its thread id and time spent
        System.out.println(indent + "[" + level + "]" + node.getMethodName() + " (Thread ID: " + node.getThreadId() +
                ") - Time Spent: " + node.getElapsedTime() / 1000000 + " ms");

        // Recursively print all child methods
        for (MethodNode child : node.getChildren()) {
            printMethodCallTree(child, level + 1);
        }

         */
    }

    // Optimized version of repeatString to avoid excessive memory use
    private static String repeatString(String str, int times) {
        if (times <= 0) {
            return "";  // Avoid unnecessary allocations
        }
        char[] chars = new char[str.length() * times];
        for (int i = 0; i < times; i++) {
            str.getChars(0, str.length(), chars, i * str.length());
        }
        return new String(chars);
    }

    // Helper class to represent a method node
    public static class MethodNode {
        private final String methodName;
        private final long threadId;
        private final long startTime;
        private long elapsedTime; // Time spent in the method
        private final Stack<MethodNode> children = new Stack<>();

        public MethodNode(String methodName, long threadId, long startTime) {
            this.methodName = methodName;
            this.threadId = threadId;
            this.startTime = startTime;
        }

        public MethodNode() {
            this.methodName = "";
            this.threadId = 0;
            this.startTime = 0;
        }

        public String getMethodName() {
            return methodName;
        }

        public long getThreadId() {
            return threadId;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        public void setElapsedTime(long elapsedTime) {
            this.elapsedTime = elapsedTime;
        }

        public Stack<MethodNode> getChildren() {
            return children;
        }

        // Add a child method to the stack
        public void addChild(MethodNode child) {
            children.push(child);
        }
    }
}
