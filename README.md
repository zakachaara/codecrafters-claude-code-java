[![progress-banner](https://backend.codecrafters.io/progress/claude-code/1cb13c01-d7be-4388-8c8a-9072511dd683)](https://app.codecrafters.io/users/zakachaara?r=2qF)

This is a starting point for Java solutions to the
["Build Your own Claude Code" Challenge](https://codecrafters.io/challenges/claude-code).

Claude Code is an AI coding assistant that uses Large Language Models (LLMs) to
understand code and perform actions through tool calls. In this challenge,
you'll build your own Claude Code from scratch by implementing an LLM-powered
coding assistant.

Along the way you'll learn about HTTP RESTful APIs, OpenAI-compatible tool
calling, agent loop, and how to integrate multiple tools into an AI assistant.

**Note**: If you're viewing this repo on GitHub, head over to
[codecrafters.io](https://codecrafters.io) to try the challenge.

# Key Notes

1. The Solution uses OpenRouter's LLM and OpenAi java SDK
2. First, we build the Read Capability via ChatCompletionTool builder.
3. Second, we test this Read Tool if requested in `toolCalls`
4. Third, we build an `Agent Loop`, to keep the agent working until no tool is called
5. Forth, we add the write capability to write and create files
6. Fifth, we add the bash tool to run commands using bash.
7. the Tests are performed by CodeCrafters tester

# What i have learned :

1. How we can build a chat completion and persist messages using openai's sdk.
2. How to parse json schema to jsonField compatible with the sdk in-use.
3. Refresh memory on how we read files' content using `BufferedReader`, write files using `BufferedWriter , Files.newBufferedWriter()`
4. Learn how to execute bash command form a Java program using `processBuilder.command("bash" , "-c", command)`, and how to get the message error or/and the output of the command using `process.getInputStream , process.getErrorStream` , and how to constracte a string from a stream.

# CodeCrafters Challenge setup

Note: This section is for stages 2 and beyond.

1. Ensure you have `mvn` installed locally.
2. Run `./your_program.sh` to run your program, which is implemented in
   `src/main/java/Main.java`.
3. Run `codecrafters submit` to submit your solution to CodeCrafters. Test
   output will be streamed to your terminal.
