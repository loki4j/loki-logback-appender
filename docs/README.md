## How to deploy Docusaurus locally

Start a Docker container from this directory as a current:

```
$ docker run -it -u "node" -v "$PWD":/app -w /app/docus/website -p 127.0.0.1:3000:3000 node:23 bash
```

Then inside the container start Node.js:

```
$ npm run start
```

Open http://localhost:3000/loki-logback-appender/