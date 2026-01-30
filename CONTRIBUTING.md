First off, thanks for taking the time to contribute!

## Ask a question

Please do not create an issue if you want to ask a question.
Instead, you can use [Q&A Board](https://github.com/loki4j/loki-logback-appender/discussions/categories/q-a).

Before creating a new thread in the Q&A Board:

- Make sure the existing documentation does not already cover your question
- Check if there are already existing issues or Q&A threads about this question; in this case, please add a comment to the existing issue/thread instead of creating a new one

## Report a bug

Bug means something is clearly wrong or something is not working as expected.
If you don't know or are unsure - consider asking a question (see the section above) instead of reporting a bug.

Before submitting a bug report:

- Check if you can reproduce the problem in the latest version of Loki4j
- Check if this issue is already reported in Loki4j's Issue tracker on Github; in this case, please add a comment to the existing issue instead of creating a new one

What information to include in the bug report:

- Exact steps that reproduce the problem in as many details as possible
- What was expected, and what was the actual result
- Code snippets that help to reproduce a problem and don't contain any irrelevant parts
- Uncut stack traces copied with all the nested exceptions
- Your Loki4j configuration and logs (if relevant)
- Your Loki configuration and log (only if relevant!)
- Version of Loki4j, version of Loki, version of JDK, and OS you are running on
- Specify if you use frameworks or application servers that could affect the logging lifecycle (e.g., Tomcat, Spring Boot, etc.)
- Specify if you use hosted Loki (e.g., Grafana Cloud)(if relevant)

There are several advanced things you can do to help with reproducing and fixing a bug (none of these is required to file a bug):

- PR a unit- or integration test that reproduces the problem
- Provide a minimal runnable project reproducing the problem (shared as a gist, GitHub repo, etc.)
- Dig into the issue yourself, find the root cause of it, and describe it in the bug report
- Explicitly specify in your bug report if you would like to prepare a fix for it. It's not recommended to start working on the fix before your bug report is accepted

## Suggest an enhancement

You can suggest a completely new feature or a minor improvement to the existing functionality.

If you have multiple ideas, it is not recommended to post them all at once.
Features that are useful to most Loki4j users and fit well into the existing architecture have a higher chance of being accepted.
So please start with the one you think has the highest chance and wait for feedback.

Before suggesting a new enhancement:

- Make sure this feature is not already implemented; check the documentation and the current codebase, if necessary
- Check if there are already existing issues or discussion threads about this feature; in this case, please add a comment to the existing issue/thread instead of creating a new one

What information to include in the enhancement suggestion:

- Describe the current behavior, and explain which behavior you expected to see instead and why
- Provide specific examples to demonstrate different aspects of the feature (configs, code snippets, etc.)
- Explain why this enhancement would be useful to most Loki4j users
- Optionally list some other Loki clients (or Logback appenders) where this enhancement exists
- Explicitly specify if you would like to prepare a PR for it. It's not recommended to start working on the code change before your enhancement request is accepted

## Prepare a pull request

You pull request should explicitly reference the issue it addresses.
This issue should be preliminary submitted to Loki4j's Issue tracker on Github, and be accepted by project's core contributor.
It's not recommended to start working on the code change before your bug report or enhancement request is accepted.
However, you can omit creating an issue for PRs that change only docs, configs, tests, or comments.

Please follow the next steps to create a pull request:

1. Fork the project to your GitHub account using the fork button in the upper right corner of GitHub UI.
1. Make sure the project compiles and tests pass.
1. Create a new git branch with a brief and meaningful name that describes the changes you want to make.
1. Check if your changes are covered by unit tests; add new unit tests if necessary.
1. When finished, again make sure the project compiles with your changes and tests pass.
1. Create a pull request to the main project using GitHub UI.

Consider the guidelines below for your PRs:

- Use a clear and descriptive title
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the title to 72 characters or less
- Reference issues and pull requests liberally in the description
