First off, thanks for taking the time to contribute!

# Pull Request

Please make sure the following software is installed on you machine
so you can build and test the project:

- Java 11 or later

- Gradle 6.7 or later

Fork the project to you GitHub account using fork button in right upper
corner of GitHub UI.

Clone forked project to the directory on you local machine.

Make sure the project compiles and tests pass:

```sh
gradle check
```

Create a new branch with brief and meaningfull name that describes the changes
you want to make.

```sh
git checkout -b ...
```

Apply you changes.

Check you changes are covered by unit tests, add new unit tests if necessary.

When finished, again make sure the project compiles with your changes and tests pass:

```sh
gradle check
```

Commit and push the changes to you fork.

Create a pull request to the main project using GitHub UI:

- Use a clear and descriptive title for your PR

- Use the present tense ("Add feature" not "Added feature")

- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")

- Limit the title to 72 characters or less

- Reference issues and pull requests liberally in the description