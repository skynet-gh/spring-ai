# spring-ai

Learning about Spring RTS AIs.

Tested with [Spring 104.0.1-1510](https://springrts.com/dl/buildbot/default/maintenance/104.0.1-1510-g89bb8e3/) and [Spring 104.0.1-1510](https://springrts.com/dl/buildbot/default/maintenance/104.0.1-1510-g89bb8e3/) with [Balanced Annihilation 10864-ccc9630](https://github.com/Balanced-Annihilation/Balanced-Annihilation).

Although Spring claims to use the AI's local `jlib` folder for library jars, I've found that it only loads the shared `jlib` folder under `AI/Interfaces` onto the classpath, so for now I copy the AI jar there as well as the AI folder.

## Build

Requires [Leiningen](https://leiningen.org/), build with

```bash
lein uberjar
```

Then copy the files into place with

```bash
./copy.sh <path to spring engine>
```

You can chain these together

```bash
lein clean && lein uberjar && ./copy <path to spring engine>
```

Now when you start that engine, you should see the AI listed.

## Dev

Start a repl to connector your editor to with

```bash
clj -Anrepl
```
