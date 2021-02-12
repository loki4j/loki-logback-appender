First off, thanks for taking the time to contribute!

## Report a bug

Before submitting a bug report:

- Check if you can reproduce the problem in the latest version of Loki4j
- Check if this issue is already reported in Loki4j's Issue tracker in Github, in this case please add a comment to the existing issue instead of creating a new one

What information to include in the bug report:

- Exact steps which reproduce the problem in as many details as possible
- Code snippets that help to reproduce a problem and don't contain any irrelevant parts
- Uncut stack traces copied with all the nested exceptions
- Your Loki4j configuration and logs (if relevant)
- Your Loki configuration and log (only if relevant!)
- Version of Loki4j, version of Loki, version of JDK and OS you are running on
- Specify if you use frameworks or application servers that could affect logging lifecycle (e.g. Tomcat, Spring Boot, etc.)
- Specify if you use hosted Loki (e.g. Grafana Cloud)(if relevant)

There are several advanced things you can do to help with reproducing and fixing a bug (none of these is required to file a bug):

- PR a unit- or integration test that reproduces the problem
- Provide a minimal runnable project that reproduces the problem (shared as a gist, GitHub repo etc.)
- Dig into the issue yourself, find the root cause of it, and describe it in the bug report
- Explicitly specify in your bug report if you would like to prepare a fix for it. It's not recommended to start working on the fix before your bug report is accepted

## Prepare a pull request

1. Fork the project to your GitHub account using fork button in right upper
corner of GitHub UI.
1. Make sure the project compiles and tests pass.
1. Create a new git branch with brief and meaningful name that describes the changes
you want to make.
1. Check if your changes are covered by unit tests, add new unit tests if necessary.
1. When finished, again make sure the project compiles with your changes and tests pass.
1. Create a pull request to the main project using GitHub UI.

Please follow the guideline below for your PRs:

- Use a clear and descriptive title
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the title to 72 characters or less
- Reference issues and pull requests liberally in the description
