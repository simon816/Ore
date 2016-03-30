Ore
===

*Catchy tagline here*
 
Ore is written in Scala using the [Play](https://www.playframework.com/) framework.

## Running

Running Ore is relatively simple. If you are using IntelliJ you should be able to import the `build.sbt` file and
just run it. Dependencies will be automatically resolved.

**With Activator:**
* Download and install the latest [Activator](https://www.lightbend.com/activator/download) distribution.
* Execute `activator run` in the project root.

## Contributing

Ore's code-style is not unlike the rest of the Sponge projects code-style with some leniency for readability.
Namely, the differences include:
* Non-uniform whitespace (see [schema.sql](app/models/db/schema.scala))
* Inline conditionals / statements
* Public fields permitted on database properties in models

### Setup

In production, Ore currently requires the DISCOURSE_SSO_SECRET and DISCOURSE_SSO_URL environment variables to be set.
If you don't have access to these, you can bypass the standard authentication method by setting the 
`application.fakeUser` setting to `true` in `conf/application.conf`. This will allow you to work with Ore in a 
development environment without requiring authentication to Sponge services.

You must also set some other environment variables:

| Variable               | Description                                          | Typical Value                   | Required |
| ---------------------- | ---------------------------------------------------- | ------------------------------- | -------- |
| BASE_URL               | The full root URL of the instance                    | http://localhost:9000           | Yes      |
| JDBC_DATABASE_URL      | PostgreSQL JDBC Database URL                         | jdbc:postgresql://localhost/ore | Yes      |
| JDBC_DATABASE_USERNAME | PostgreSQL Database username                         | root                            | Yes      |
| APPLICATION_SECRET     | The application secret for cryptography              | `REDACTED`                      | No       |
| JDBC_DATABASE_PASSWORD | PostgreSQL Database password                         | hunter2                         | No       |
| DISCOURSE_SSO_SECRET   | The secret key for authentication against the forums | `REDACTED`                      | No       |
| DISCOURSE_SSO_URL      | The URL to redirect to for authentication            | `REDACTED`                      | No       |
