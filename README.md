# smtss :floppy_disk:

Application that's try to find best tssid match from tss, based on given parameters

## Technologies used
* Kotlin
* Ktor
* Gradle
* Junit
* Docker
* MQ


#### Requirements

* JDK 17

## FlowChart
This the high level flow of the application
```mermaid
  graph LR
      EPJ --- eMottak
      eMottak --- id1([SYFOSMMOTTAK.INPUT])
      id1([SYFOSMMOTTAK.INPUT]) --> syfosmmottak
      syfosmmottak --- smtss
      syfosmmottak --- eMottak-subscription
      smtss --- tss
      smtss --- azure_ad
```

## Getting started
### Getting github-package-registry packages NAV-IT
Some packages used in this repo is uploaded to the GitHub Package Registry which requires authentication. 
It can, for example, be solved like this in Gradle:
```
val githubUser: String by project
val githubPassword: String by project
repositories {
    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/syfosm-common")
    }
}
```

`githubUser` and `githubPassword` can be put into a separate file `~/.gradle/gradle.properties`
with the following content:

```                                                     
githubUser=x-access-token
githubPassword=[token]
```

Replace `[token]` with a personal access token with scope `read:packages`.
See githubs guide [creating-a-personal-access-token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) on
how to create a personal access token.

Alternatively, the variables can be configured via environment variables:

* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

or the command line:

```
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` shell
./gradlew shadowJar
```
or on windows
``` shell
gradlew.bat shadowJar
```

### Add new application to consume api (only NAV applications is supported)
You can create a PR: example https://github.com/navikt/smtss/pull/14
That's updates the naiserator files [dev](naiserator-dev.yaml) and [prod](naiserator-prod.yaml)
with the following:
Remember to replace, $yourapplication, $yournamespace, $yourcluster with real values
``` 
  accessPolicy:
    inbound:
      rules:
        - application: $yourapplication
          namespace: $yournamespace
          cluster: $yourcluster
```

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` shell
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests?
Please create an [issue](https://github.com/navikt/smtss/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
