# intellij-metals
Experiments in Metallurgy

## Prerequisits

Install [metals](https://scalameta.org/metals/docs/#installation), e.g.:
```
brew install metals
```

## Run locally

```bash
sbt runIDE
```

Make sure the Scala plugin is installed in IntelliJ.

Go to `Language & Frameworks > Scala > Metals` and set the `metals` path, e.g. `/opt/homebrew/bin/metals`

Restart the IntelliJ, after that there should be a new icon in the bottom right corner of the IntelliJ.
