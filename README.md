Individuals Business End of Period Statement API
========================
The Individuals Business End of Period Statement API allows software packages to:

* to submit a declaration that the submission data for a business is complete.

## Requirements

- Scala 2.13.x
- Java 11
- sbt 1.7.x
- [Service Manager V2](https://github.com/hmrc/sm2)

## Development Setup

To run the microservice from console, use `sbt run`. (starts on port 7785 by default)

To start the service manager profile: `sm --start MTDFB_BUSINESS_EOPS`.

## Run Tests

```
sbt test
sbt it:test
```

## To view the OAS

To view documentation locally ensure the Individuals Business EOPS API is running, and run api-documentation-frontend:
`./run_local_with_dependencies.sh`

Then go to http://localhost:9680/api-documentation/docs/openapi/preview and use this port and version:
`http://localhost:7785/api/conf/3.0/application.yaml`

## Changelog

You can see our changelog [here](https://github.com/hmrc/income-tax-mtd-changelog)

## Support and Reporting Issues

You can create a GitHub issue [here](https://github.com/hmrc/income-tax-mtd-changelog/issues)

## API Reference / Documentation

Available on
the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-business-eops-api)

## License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
