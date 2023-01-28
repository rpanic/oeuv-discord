### ÖUV Discord Bot

#### Setup

Setup is described for Docker build, for standalone build see [Dockerfile](Dockerfile)

1. Create Config `config.yml`

First, create a bot token for your bot on the discord developers site

File layout:

```yaml
channels:
  drehscheibe: <channel-id>
  oefsv-announcements: <channel-id>
  infos: <channel-id>
bot:
  playing: <playing-string>
  token: <bot-token>
```

4. Build with Docker

`docker build -t oeuv-bot .`

3. Run with docker-compose

```yaml

version: '3.3'
services:
    oeuv-bot:
        container_name: oeuv-bot
        restart: unless-stopped
        image: rpanic.registry.jetbrains.space/p/projects/containers/oeuv-bot:latest
        volumes:
          - ./config.yml:/app/config.yml:ro
        labels:
          - "com.centurylinklabs.watchtower.scope=watchtower"
```

If you don't use watchtower, remove the `watchtower.scope` label

#### Other

CI Script for DroneCI under [.drone.yml](.drone.yml) build the docker image and pushes it to a remote repository

